#include <assert.h>

#include "tuenti/clientsignalingthread.h"
#include "tuenti/logging.h"
#include "tuenti/presenceouttask.h"
#include "tuenti/presencepushtask.h"
#include "tuenti/voiceclient.h"//Needed for notify_ would be nice to remove
#include "talk/examples/login/xmppauth.h"
#include "talk/examples/login/xmppsocket.h"
#include "talk/base/signalthread.h"
#include "talk/base/ssladapter.h"
#include "talk/session/phone/dataengine.h"
#include "talk/session/phone/call.h"
#include "talk/session/phone/mediasessionclient.h"
#include "talk/xmpp/pingtask.h"
#include "talk/p2p/base/sessionmanager.h"
#include "talk/p2p/client/basicportallocator.h"
#include "talk/p2p/client/sessionmanagertask.h"

namespace tuenti {

enum {
    //ST_MSG_WORKER_DONE is defined in SignalThread.h
    MSG_LOGIN = talk_base::SignalThread::ST_MSG_FIRST_AVAILABLE,
    MSG_DISCONNECT //Logout
    ,
    MSG_CALL,
    MSG_ACCEPT_CALL,
    MSG_DECLINE_CALL,
    MSG_END_CALL
//  , MSG_DESTROY
};

struct StringData: public talk_base::MessageData {
    StringData(std::string s)
            : s_(s) {
    }
    std::string s_;
};

///////////////////////////////////////////////////////////////////////////////
// ClientSignalingThread
///////////////////////////////////////////////////////////////////////////////

ClientSignalingThread::ClientSignalingThread(VoiceClientNotify *notifier, talk_base::Thread *signal_thread)
        : talk_base::SignalThread(),
          notify_(notifier),
          signal_thread_(signal_thread_),
          roster_(NULL),
          pump_(NULL),
          presence_push_(NULL),
          presence_out_(NULL),
          ping_(NULL),
          network_manager_(NULL),
          data_engine_(NULL),
          port_allocator_(NULL),
          session_(NULL),
          session_manager_(NULL),
          session_manager_task_(NULL),
          call_(NULL),
          media_client_(NULL),
          media_engine_(NULL),
          incoming_call_(false),
          auto_accept_(false) {

    LOGI("ClientSignalingThread::ClientSignalingThread");
    assert(talk_base::Thread::Current() == signal_thread_);
    //Overriding name
    //worker_.SetName("ClientSignalingThread", this);
    // simple initializers
    if (roster_ == NULL) {
        roster_ = new RosterMap();
        LOGI( "ClientSignalingThread::ClientSignalingThread - new RosterMap roster_@(0x%x)", roster_);
    }
    if (network_manager_ == NULL) {
        network_manager_ = new talk_base::BasicNetworkManager();
        LOGI(
                "ClientSignalingThread::ClientSignalingThread - new BasicNetworkManager network_manager_@(0x%x)", network_manager_);
    }
    if (data_engine_ == NULL) {
        data_engine_ = new cricket::DataEngine();
        LOGI( "ClientSignalingThread::ClientSignalingThread - new DataEngine data_engine_@(0x%x)", data_engine_);
    }
    if (session_manager_ == NULL) {
        session_manager_ = new cricket::SessionManager(port_allocator_, worker());
        LOGI( "ClientSignalingThread::ClientSignalingThread - new SessionManager session_manager_@(0x%x)", session_manager_);
    }
    if (pump_ == NULL) {
        pump_ = new XmppPump(this);
        LOGI("ClientSignalingThread::ClientSignalingThread - new XmppPump pump_@(0x%x)", pump_);
    }
    my_status_.set_caps_node("http://github.com/lukeweber/webrtc-jingle");
    my_status_.set_version("1.0-SNAPSHOT");
}

ClientSignalingThread::~ClientSignalingThread() {
    LOGI("ClientSignalingThread::~ClientSignalingThread");
    if (roster_) {
        LOGI("ClientSignalingThread::~ClientSignalingThread - deleting roster_@(0x%x)", roster_);
        delete roster_;
        roster_ = NULL;
    }
    if (network_manager_ != NULL) {
        LOGI( "ClientSignalingThread::~ClientSignalingThread - deleting network_manager_@(0x%x)", network_manager_);
        delete network_manager_;
        network_manager_ = NULL;
    }
    if (data_engine_ != NULL) {
        LOGI( "ClientSignalingThread::~ClientSignalingThread - deleting data_engine_@(0x%x)", data_engine_);
        delete data_engine_;
        data_engine_ = NULL;
    }
    if (port_allocator_ != NULL) {
        LOGI( "ClientSignalingThread::~ClientSignalingThread - deleting port_allocator_@(0x%x)", port_allocator_);
        delete port_allocator_;
        port_allocator_ = NULL;
    }
    if (session_manager_ != NULL) {
        LOGI( "ClientSignalingThread::~ClientSignalingThread - deleting session_manager_@(0x%x)", session_manager_);
        delete session_manager_;
        session_manager_ = NULL;
    }
    if (pump_ != NULL) {
        LOGI("ClientSignalingThread::~ClientSignalingThread - deleting pump_@(0x%x)", pump_);
        delete pump_;
        pump_ = NULL;
    }
}

void ClientSignalingThread::OnStatusUpdate(const buzz::Status& status) {
    LOGI("ClientSignalingThread::OnStatusUpdate");
    RosterItem item;
    item.jid = status.jid();
    item.show = status.show();
    item.status = status.status();

    std::string key = item.jid.Str();

    if (status.available() && status.voice_capability()) {
        LOGI("Adding to roster: %s", key.c_str());
        (*roster_)[key] = item;
    } else {
        LOGI("Removing from roster: %s", key.c_str());
        RosterMap::iterator iter = roster_->find(key);
        if (iter != roster_->end())
            roster_->erase(iter);
    }
}

void ClientSignalingThread::OnSessionState(cricket::Call* call, cricket::Session* session, cricket::Session::State state) {
    LOGI("ClientSignalingThread::OnSessionState");
    switch (state) {
    default:
        LOGI("VoiceClient::OnSessionState - UNKNOWN_STATE");
        break;
    case cricket::BaseSession::STATE_SENTTERMINATE:
        LOGI("VoiceClient::OnSessionState - STATE_SENTTERMINATE doing nothing...");
        break;
    case cricket::BaseSession::STATE_DEINIT:
        LOGI("VoiceClient::OnSessionState - STATE_DEINIT doing nothing...");
        break;
    case cricket::Session::STATE_RECEIVEDINITIATE:
        LOGI("VoiceClient::OnSessionState - STATE_RECEIVEDINITIATE setting up call...");
        buzz::Jid jid(session->remote_name());
        LOGI("Incoming call from '%s'", jid.Str().c_str());
        call_ = call;
        session_ = session;
        incoming_call_ = true;
        if (auto_accept_) {
            AcceptCall();
        }
        break;
    case cricket::Session::STATE_SENTINITIATE:
        LOGI("VoiceClient::OnSessionState - STATE_SENTINITIATE doing nothing...");
        break;
    case cricket::Session::STATE_RECEIVEDACCEPT:
        LOGI("VoiceClient::OnSessionState - STATE_RECEIVEDACCEPT transfering data...");
        if (call_->has_data()) {
            call_->SignalDataReceived.connect(this, &ClientSignalingThread::OnDataReceived);
        }
        break;
    case cricket::Session::STATE_RECEIVEDREJECT:
        LOGI("VoiceClient::OnSessionState - STATE_RECEIVEDREJECT doing nothing...");
        break;
    case cricket::Session::STATE_INPROGRESS:
        LOGI("VoiceClient::OnSessionState - STATE_INPROGRESS monitoring...");
        call->StartSpeakerMonitor(session);
        break;
    case cricket::Session::STATE_RECEIVEDTERMINATE:
        LOGI("VoiceClient::OnSessionState - STATE_RECEIVEDTERMINATE doing nothing...");
        break;
    }
    if (notify_) {
        notify_->OnCallStateChange(session, state);
    }
}

void ClientSignalingThread::OnStateChange(buzz::XmppEngine::State state) {
    LOGI("ClientSignalingThread::OnStateChange");
    switch (state) {
    default:
        LOGI("ClientSignalingThread::OnStateChange - Unknown State (---) doing nothing...");
        break;
    case buzz::XmppEngine::STATE_NONE:
        LOGI("ClientSignalingThread::OnStateChange - State (STATE_NONE) doing nothing...");
        break;
    case buzz::XmppEngine::STATE_START:
        LOGI("ClientSignalingThread::OnStateChange - State (STATE_START) doing nothing...");
        break;
    case buzz::XmppEngine::STATE_OPENING:
        LOGI("ClientSignalingThread::OnStateChange - State (STATE_OPENING) doing nothing...");
        break;
    case buzz::XmppEngine::STATE_OPEN:
        LOGI( "ClientSignalingThread::OnStateChange - State (STATE_OPEN) initing media & presence...");
        InitMedia();
        InitPresence();
        InitPing();
        break;
    case buzz::XmppEngine::STATE_CLOSED:
        LOGI( "ClientSignalingThread::OnStateChange - State (STATE_CLOSED) commented out terminating media engine...");
        if (xcs_.use_tls() == buzz::TLS_REQUIRED) {
            talk_base::CleanupSSL();
        }
        if (media_engine_) {
            media_engine_->Terminate();
        }
        break;
    }
    //main_thread_->Post(this, MSG_STATE_CHANGE, new StateChangeData(state));
}

void ClientSignalingThread::OnDataReceived(cricket::Call*, const cricket::ReceiveDataParams& params,
        const std::string& data) {
    LOGI("ClientSignalingThread::OnDataReceived");
    cricket::StreamParams stream;
    if (GetStreamBySsrc(call_->data_recv_streams(), params.ssrc, &stream)) {
        LOGI(
                "Received data from '%s' on stream '%s' (ssrc=%u): %s", stream.nick.c_str(), stream.name.c_str(), params.ssrc, data.c_str());
    } else {
        LOGI("Received data (ssrc=%u): %s", params.ssrc, data.c_str());
    }
}

void ClientSignalingThread::OnRequestSignaling() {
    LOGI("ClientSignalingThread::OnRequestSignaling");
    session_manager_->OnSignalingReady();
}

void ClientSignalingThread::OnSessionCreate(cricket::Session* session, bool initiate) {
    LOGI("ClientSignalingThread::OnSessionCreate");
    session->set_current_protocol(cricket::PROTOCOL_HYBRID);
}

void ClientSignalingThread::OnCallCreate(cricket::Call* call) {
    LOGI("ClientSignalingThread::OnCallCreate");
    call->SignalSessionState.connect(this, &ClientSignalingThread::OnSessionState);
}

void ClientSignalingThread::OnCallDestroy(cricket::Call* call) {
    LOGI("ClientSignalingThread::OnCallDestroy");
    if (call == call_) {
        LOGI( "internal delete found a valid call_@(0x%x) and session_@(0x%x) to destroy ", call_, session_);
        call_ = NULL;
        session_ = NULL;
    }
}

void ClientSignalingThread::OnMediaEngineTerminate() {
    LOGI("ClientSignalingThread::OnMediaEngineTerminate");
    if (media_engine_) {
        delete media_engine_;
        media_engine_ = NULL;
    }
}

void ClientSignalingThread::OnPingTimeout() {
    LOGE("XMPP ping timeout. Will keep trying...");
    InitPing();
}

void ClientSignalingThread::Login(const std::string &username, const std::string &password, const std::string &xmpp_host,
        int xmpp_port, bool use_ssl, const std::string &stun_host, int stun_port) {
    LOGI("ClientSignalingThread::Login");

    if (use_ssl) {
        talk_base::InitializeSSL();
    }

    buzz::Jid jid = buzz::Jid(username);

    talk_base::InsecureCryptStringImpl pass;
    pass.password() = password;

    xcs_.set_user(jid.node());
    xcs_.set_resource("TuentiVoice");
    xcs_.set_host(jid.domain());
    xcs_.set_use_tls(use_ssl ? buzz::TLS_REQUIRED : buzz::TLS_DISABLED);
    xcs_.set_pass(talk_base::CryptString(pass));
    xcs_.set_server(talk_base::SocketAddress(xmpp_host, xmpp_port));

    // stun server socket address
    talk_base::SocketAddress stun_addr(stun_host, stun_port);
    port_allocator_ = new cricket::BasicPortAllocator(network_manager_, stun_addr, talk_base::SocketAddress(),
            talk_base::SocketAddress(), talk_base::SocketAddress());
    LOGI( "ClientSignalingThread::Login - new BasicPortAllocator port_allocator_@(0x%x)", port_allocator_);

    signal_thread_->Post(this, MSG_LOGIN);
}

void ClientSignalingThread::Disconnect() {
    LOGI("ClientSignalingThread::Disconnect");
    signal_thread_->Post(this, MSG_END_CALL);
    signal_thread_->Post(this, MSG_DISCONNECT);
}

void ClientSignalingThread::Call(std::string &remoteJid) {
    LOGI("ClientSignalingThread::Call");
    signal_thread_->Post(this, MSG_CALL, new StringData(remoteJid));
}

void ClientSignalingThread::AcceptCall() {
    LOGI("ClientSignalingThread::AcceptCall");
    signal_thread_->Post(this, MSG_ACCEPT_CALL);
}

void ClientSignalingThread::DeclineCall() {
    LOGI("ClientSignalingThread::DeclineCall");
    signal_thread_->Post(this, MSG_DECLINE_CALL);
}

void ClientSignalingThread::EndCall() {
    LOGI("ClientSignalingThread::EndCall");
    signal_thread_->Post(this, MSG_END_CALL);
}

void ClientSignalingThread::Destroy() {
    LOGI("ClientSignalingThread::Destroy");
    assert(talk_base::Thread::Current() == signal_thread_);
    Disconnect();
    SignalThread::Destroy(true);
}

void ClientSignalingThread::OnMessage(talk_base::Message* message) {
    LOGI("ClientSignalingThread::OnMessage");
    switch (message->message_id) {
    case MSG_LOGIN:
        LOGI("ClientSignalingThread::OnMessage - MSG_LOGIN");
        LoginW();
        break;
    case MSG_DISCONNECT:
        LOGI("ClientSignalingThread::OnMessage - MSG_DISCONNECT");
        DisconnectW();
        break;
    case MSG_CALL:
        LOGI("ClientSignalingThread::OnMessage - MSG_CALL");
        CallW(static_cast<StringData*>(message->pdata)->s_);
        break;
    case MSG_ACCEPT_CALL:
        LOGI("ClientSignalingThread::OnMessage - MSG_ACCEPT_CALL");
        AcceptCallW();
        break;
    case MSG_END_CALL:
        LOGI("ClientSignalingThread::OnMessage - MSG_END_CALL");
        EndCallW();
        break;
    default:
        LOGI("ClientSignalingThread::OnMessage - UNKNOWN falling back to base class");
        SignalThread::OnMessage(message);
        break;
    }
}

void ClientSignalingThread::DoWork() {
    LOGI("ClientSignalingThread::DoWork");
    worker()->ProcessMessages(talk_base::kForever);
}

void ClientSignalingThread::LoginW() {
    LOGI("ClientSignalingThread::LoginW");
    //assert(talk_base::Thread::Current() == signal_thread_);

    if (pump_->AllChildrenDone()) {
        LOGE( "AllChildrenDone NOT doing required {delete pump_;pump_ = new XmppPump(this);} yet...");
    }

    //I don't like this where does it get deleted? feels like a memory leak.
    XmppSocket *sock = new XmppSocket(xcs_.use_tls());
    XmppAuth *auth = new XmppAuth();
    //I don't like this where does it get deleted? feels like a memory leak.
    LOGE("Where do we delete sock@(0x%x) and auth@(0x%x)", sock, auth);
    pump_->DoLogin(xcs_, sock, auth);
}

void ClientSignalingThread::DisconnectW() {
    LOGI("ClientSignalingThread::DisconnectW");
    if (pump_->AllChildrenDone()) {
        LOGE( "AllChildrenDone NOT doing required {delete pump_;pump_ = new XmppPump(this);} yet...");
    }
    pump_->DoDisconnect();
}

void ClientSignalingThread::CallW(const std::string &remoteJid) {
    LOGI("ClientSignalingThread::CallW");
    assert(talk_base::Thread::Current() == signal_thread_);

    cricket::CallOptions options;
    options.is_muc = false;

    bool found = false;
    buzz::Jid callto_jid(remoteJid);
    buzz::Jid found_jid;

    // otherwise, it's a friend
    for (RosterMap::iterator iter = roster_->begin(); iter != roster_->end(); ++iter) {
        if (iter->second.jid.BareEquals(callto_jid)) {
            found = true;
            found_jid = iter->second.jid;
            break;
        }
    }

    if (found) {
        LOGI("Found online friend '%s'", found_jid.Str().c_str());
        if (!call_) {
            call_ = media_client_->CreateCall();
            session_ = call_->InitiateSession(found_jid, options); //REQ_MAIN_THREAD
        }
        media_client_->SetFocus(call_);
    } else {
        LOGI("Could not find online friend '%s'", remoteJid.c_str());
    }
}

void ClientSignalingThread::AcceptCallW() {
    LOGI("ClientSignalingThread::AcceptCallW");
    assert(talk_base::Thread::Current() == signal_thread_);
    assert(call_ && incoming_call_);
    assert(call_->sessions().size() == 1);

    cricket::CallOptions options;
    call_->AcceptSession(call_->sessions()[0], options);
    media_client_->SetFocus(call_);
    if (call_->has_data()) {
        call_->SignalDataReceived.connect(this, &ClientSignalingThread::OnDataReceived);
    }
    incoming_call_ = false;
}

void ClientSignalingThread::DeclineCallW() {
    LOGI("ClientSignalingThread::DeclineCallW");
    assert(talk_base::Thread::Current() == signal_thread_);
    assert(call_ && incoming_call_);
    call_->RejectSession(call_->sessions()[0]);
    incoming_call_ = false;
}

void ClientSignalingThread::EndCallW() {
    LOGI("ClientSignalingThread::EndCallW");
    assert(talk_base::Thread::Current() == signal_thread_);
    if (call_) {
        call_->Terminate();
    }
}

void ClientSignalingThread::InitMedia() {
    LOGI("ClientSignalingThread::InitMedia");
    //assert(talk_base::Thread::Current() == signal_thread_);
    std::string client_unique = pump_->client()->jid().Str();
    talk_base::InitRandom(client_unique.c_str(), client_unique.size());

    //TODO: We need to modify the last params of this to add TURN server addresses.
    session_manager_->SignalRequestSignaling.connect(this, &ClientSignalingThread::OnRequestSignaling);
    session_manager_->SignalSessionCreate.connect(this, &ClientSignalingThread::OnSessionCreate);
    session_manager_->OnSignalingReady();

    session_manager_task_ = new cricket::SessionManagerTask(pump_->client(), session_manager_);
    session_manager_task_->EnableOutgoingMessages();
    session_manager_task_->Start();

    if (media_engine_ != NULL) {
        delete media_engine_;
        media_engine_ = NULL;
    }
    media_engine_ = cricket::MediaEngineFactory::Create();
    media_engine_->SignalTerminate.connect(this, &ClientSignalingThread::OnMediaEngineTerminate);

    media_client_ = new cricket::MediaSessionClient(pump_->client()->jid(), session_manager_, media_engine_, data_engine_,
            cricket::DeviceManagerFactory::Create());
    media_client_->SignalCallCreate.connect(this, &ClientSignalingThread::OnCallCreate);
    media_client_->SignalCallDestroy.connect(this, &ClientSignalingThread::OnCallDestroy);
    media_client_->set_secure(cricket::SEC_DISABLED);
}

void ClientSignalingThread::InitPresence() {
    //NFHACK Fix the news
    LOGI("ClientSignalingThread::InitPresence");
    //assert(talk_base::Thread::Current() == signal_thread_);
    presence_push_ = new buzz::PresencePushTask(pump_->client());
    presence_push_->SignalStatusUpdate.connect(this, &ClientSignalingThread::OnStatusUpdate);
    presence_push_->Start();

    my_status_.set_jid(pump_->client()->jid());
    my_status_.set_available(true);
    my_status_.set_show(buzz::Status::SHOW_ONLINE);
    my_status_.set_know_capabilities(true);
    my_status_.set_pmuc_capability(false);

    int capabilities = media_client_->GetCapabilities();
    my_status_.set_voice_capability((capabilities & cricket::AUDIO_RECV) != 0);
    my_status_.set_video_capability((capabilities & cricket::VIDEO_RECV) != 0);
    my_status_.set_camera_capability((capabilities & cricket::VIDEO_SEND) != 0);

    presence_out_ = new buzz::PresenceOutTask(pump_->client());
    presence_out_->Send(my_status_);
    presence_out_->Start();
}

void ClientSignalingThread::InitPing() {
    LOGI("ClientSignalingThread::InitPing");
    ping_ = new buzz::PingTask(pump_->client(), talk_base::Thread::Current(), 10000, 10000);
    ping_->SignalTimeout.connect(this, &ClientSignalingThread::OnPingTimeout);
    ping_->Start();
}

}  // namespace tuenti
