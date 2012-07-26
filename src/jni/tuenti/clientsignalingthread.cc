#include <assert.h>
#include "tuenti/clientsignalingthread.h"
#include "tuenti/logging.h"
#include "tuenti/presenceouttask.h"
#include "tuenti/presencepushtask.h"
#include "tuenti/voiceclient.h"//Needed for notify_ would be nice to remove

#include "tuenti/txmppauth.h"
#include "tuenti/txmppsocket.h"
#include "talk/base/signalthread.h"
#include "talk/base/ssladapter.h"
#include "talk/session/phone/dataengine.h"
#include "talk/session/phone/call.h"
#include "talk/session/phone/mediasessionclient.h"
#include "talk/p2p/base/sessionmanager.h"
#include "talk/p2p/client/basicportallocator.h"
#include "talk/p2p/client/sessionmanagertask.h"
namespace tuenti{
enum {
  //ST_MSG_WORKER_DONE is defined in SignalThread.h
  MSG_LOGIN = talk_base::SignalThread::ST_MSG_FIRST_AVAILABLE
  , MSG_DISCONNECT//Logout
  , MSG_CALL
  , MSG_ACCEPT_CALL
  , MSG_DECLINE_CALL
  , MSG_END_CALL
//  , MSG_DESTROY
};

struct StringData: public talk_base::MessageData {
    StringData(std::string s) :
            s_(s) {
    }
    std::string s_;
};

///////////////////////////////////////////////////////////////////////////////
// ClientSignalingThread
///////////////////////////////////////////////////////////////////////////////

ClientSignalingThread::ClientSignalingThread(VoiceClientNotify *notifier, talk_base::Thread *signal_thread)
: talk_base::SignalThread()
, notify_(notifier)
, signal_thread_(signal_thread)
, roster_(NULL)
, pump_(NULL)
, presence_push_(NULL)
, presence_out_(NULL)
, network_manager_(NULL)
, port_allocator_(NULL)
, session_(NULL)
, session_manager_(NULL)
, session_manager_task_(NULL)
, call_(NULL)
, media_client_(NULL)
#ifndef SIMPLIFY_MEDIA_CLIENT
, media_engine_(NULL)
, data_engine_(NULL)
#endif
, port_allocator_flags_(0)
, use_ssl_(false)
, incoming_call_(false)
, auto_accept_(false)
{
  LOGI("ClientSignalingThread::ClientSignalingThread");
  assert(talk_base::Thread::Current() == signal_thread_);
  //Overriding name
  //worker_.SetName("ClientSignalingThread", this);
  // simple initializers
  if(roster_ == NULL){
    roster_ = new RosterMap();
    LOGI("ClientSignalingThread::ClientSignalingThread - new RosterMap roster_@(0x%x)", reinterpret_cast<int>(roster_));
  }
  if(network_manager_ == NULL){
    network_manager_ = new talk_base::BasicNetworkManager();
    LOGI("ClientSignalingThread::ClientSignalingThread - new BasicNetworkManager network_manager_@(0x%x)", reinterpret_cast<int>(network_manager_));
  }
#ifndef SIMPLIFY_MEDIA_CLIENT
  if(data_engine_ == NULL){
    data_engine_ = new cricket::DataEngine();
    LOGI("ClientSignalingThread::ClientSignalingThread - new DataEngine data_engine_@(0x%x)", reinterpret_cast<int>(data_engine_));
  }
#endif
  if(port_allocator_ == NULL){
    talk_base::SocketAddress stun_addr("stun.l.google.com", 19302);
    port_allocator_ = new cricket::BasicPortAllocator(network_manager_, stun_addr,
      talk_base::SocketAddress(), talk_base::SocketAddress(), talk_base::SocketAddress());
    LOGI("ClientSignalingThread::ClientSignalingThread - new BasicPortAllocator port_allocator_@(0x%x)", reinterpret_cast<int>(port_allocator_));
    if (port_allocator_flags_ != 0) {
      port_allocator_->set_flags(port_allocator_flags_);
    }
  }
  if(session_manager_ == NULL){
    session_manager_ = new cricket::SessionManager(port_allocator_, worker());
    LOGI("ClientSignalingThread::ClientSignalingThread - new SessionManager session_manager_@(0x%x)", reinterpret_cast<int>(session_manager_));
  }
  if(pump_ == NULL){
    pump_ = new TXmppPump(this);
    LOGI("ClientSignalingThread::ClientSignalingThread - new TXmppPump pump_@(0x%x)", reinterpret_cast<int>(pump_));
  }
  my_status_.set_caps_node("http://github.com/lukeweber/webrtc-jingle");
  my_status_.set_version("1.0-SNAPSHOT");
}

ClientSignalingThread::~ClientSignalingThread() {
  LOGI("ClientSignalingThread::~ClientSignalingThread");
  assert(talk_base::Thread::Current() == signal_thread_);
  if(roster_){
    LOGI("ClientSignalingThread::~ClientSignalingThread - deleting roster_@(0x%x)", reinterpret_cast<int>(roster_));
    delete roster_;
    roster_ = NULL;
  }
  if(network_manager_ != NULL){
    LOGI("ClientSignalingThread::~ClientSignalingThread - deleting network_manager_@(0x%x)", reinterpret_cast<int>(network_manager_));
    delete network_manager_;
    network_manager_ = NULL;
  }
#ifndef SIMPLIFY_MEDIA_CLIENT
  if (data_engine_ != NULL) {
    LOGI("ClientSignalingThread::~ClientSignalingThread - deleting data_engine_@(0x%x)", reinterpret_cast<int>(data_engine_));
    delete data_engine_;
    data_engine_ = NULL;
  }
#endif
  if (port_allocator_ != NULL){
    LOGI("ClientSignalingThread::~ClientSignalingThread - deleting port_allocator_@(0x%x)", reinterpret_cast<int>(port_allocator_));
    delete port_allocator_;
    port_allocator_ = NULL;
  }
  if (session_manager_ != NULL){
    LOGI("ClientSignalingThread::~ClientSignalingThread - deleting session_manager_@(0x%x)", reinterpret_cast<int>(session_manager_));
    delete session_manager_;
    session_manager_ = NULL;
  }
  if (pump_ != NULL){
    LOGI("ClientSignalingThread::~ClientSignalingThread - deleting pump_@(0x%x)", reinterpret_cast<int>(pump_));
    delete pump_;
    pump_ = NULL;
  }
}

void ClientSignalingThread::OnStatusUpdate(const buzz::Status& status) {
    LOGI("ClientSignalingThread::OnStatusUpdate");
    assert(talk_base::Thread::Current() == signal_thread_);
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
    assert(talk_base::Thread::Current() == signal_thread_);
    switch(state){
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
        {
            LOGI("VoiceClient::OnSessionState - STATE_RECEIVEDINITIATE setting up call...");
            buzz::Jid jid(session->remote_name());
            LOGI("Incoming call from '%s'", jid.Str().c_str());
            call_ = call;
            session_ = session;
            incoming_call_ = true;
            if (auto_accept_) {
                AcceptCall();
            }
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
    assert(talk_base::Thread::Current() == signal_thread_);
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
        LOGI("ClientSignalingThread::OnStateChange - State (STATE_OPEN) initing media & presence...");
        InitMedia();
        InitPresence();
        break;
    case buzz::XmppEngine::STATE_CLOSED:
        LOGI("ClientSignalingThread::OnStateChange - State (STATE_CLOSED) cleaning up ssl, deleting media client & clearing roster...");
        if (use_ssl_)
            talk_base::CleanupSSL();
#ifndef SIMPLIFY_MEDIA_CLIENT
        if (media_engine_) {
          media_engine_->Terminate();
        }
#endif
        //Remove everyone from your roster
        if(roster_) {
          roster_->clear();
        }
        //NFHACK we should probably do something with the media_client_ here
        if(media_client_) {
          delete media_client_;
          media_client_ = NULL;
        }
        break;
    }
    //main_thread_->Post(this, MSG_STATE_CHANGE, new StateChangeData(state));
}
void ClientSignalingThread::OnDataReceived(cricket::Call*, const cricket::ReceiveDataParams& params, const std::string& data) {
    LOGI("ClientSignalingThread::OnDataReceived");
    assert(talk_base::Thread::Current() == signal_thread_);
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
    assert(talk_base::Thread::Current() == signal_thread_);
    session_manager_->OnSignalingReady();
}

void ClientSignalingThread::OnSessionCreate(cricket::Session* session, bool initiate) {
    LOGI("ClientSignalingThread::OnSessionCreate");
    assert(talk_base::Thread::Current() == signal_thread_);
    session->set_current_protocol(cricket::PROTOCOL_HYBRID);
}

void ClientSignalingThread::OnCallCreate(cricket::Call* call) {
    LOGI("ClientSignalingThread::OnCallCreate");
    assert(talk_base::Thread::Current() == signal_thread_);
    call->SignalSessionState.connect(this, &ClientSignalingThread::OnSessionState);
}

void ClientSignalingThread::OnCallDestroy(cricket::Call* call) {
    LOGI("ClientSignalingThread::OnCallDestroy");
    assert(talk_base::Thread::Current() == signal_thread_);
    if (call == call_) {
        LOGI("internal delete found a valid call_@(0x%x) and session_@(0x%x) to destroy ", reinterpret_cast<int>(call_), reinterpret_cast<int>(session_));
        call_ = NULL;
        session_ = NULL;
    }
}

void ClientSignalingThread::OnMediaEngineTerminate() {
    LOGI("ClientSignalingThread::OnMediaEngineTerminate");
    assert(talk_base::Thread::Current() == signal_thread_);
#ifndef SIMPLIFY_MEDIA_CLIENT
    if (media_engine_) {
        delete media_engine_;
        media_engine_ = NULL;
    }
#endif
}
// ================================================================
// THESE ARE THE ONLY FUNCTIONS THAT CAN BE CALLED USING ANY THREAD
// vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
void ClientSignalingThread::Login(bool use_ssl, buzz::XmppClientSettings settings){
    LOGI("ClientSignalingThread::Login");
    //assert(talk_base::Thread::Current() == signal_thread_);
    xcs_ = settings;
    use_ssl_ = use_ssl;
    signal_thread_->Post(this, MSG_LOGIN);
}

void ClientSignalingThread::Disconnect() {
    LOGI("ClientSignalingThread::Disconnect");
    //assert(talk_base::Thread::Current() == signal_thread_);
    signal_thread_->Post(this, MSG_DISCONNECT);
}

void ClientSignalingThread::Call(std::string &remoteJid) {
    LOGI("ClientSignalingThread::Call");
    //assert(talk_base::Thread::Current() == signal_thread_);
    signal_thread_->Post(this, MSG_CALL, new StringData(remoteJid));
}

void ClientSignalingThread::AcceptCall() {
    LOGI("ClientSignalingThread::AcceptCall");
    //assert(talk_base::Thread::Current() == signal_thread_);
    signal_thread_->Post(this, MSG_ACCEPT_CALL);
}

void ClientSignalingThread::DeclineCall() {
    LOGI("ClientSignalingThread::DeclineCall");
    //assert(talk_base::Thread::Current() == signal_thread_);
    signal_thread_->Post(this, MSG_DECLINE_CALL);
}

void ClientSignalingThread::EndCall() {
    LOGI("ClientSignalingThread::EndCall");
    //assert(talk_base::Thread::Current() == signal_thread_);
    signal_thread_->Post(this, MSG_END_CALL);
}
// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
// THESE ARE THE ONLY FUNCTIONS THAT CAN BE CALLED USING ANY THREAD
// ================================================================

bool ClientSignalingThread::Destroy() {
    LOGI("ClientSignalingThread::Destroy");
    assert(talk_base::Thread::Current() == signal_thread_);
    bool destroyed = false;
    //Single threaded shuts everything down
    EndCall();
    if(call_ == NULL){
        Disconnect();
        if (media_client_ == NULL) {
            SignalThread::Destroy(true);
            destroyed = true;
        }
    }
  return destroyed;
}

void ClientSignalingThread::OnMessage(talk_base::Message* message) {
  LOGI("ClientSignalingThread::OnMessage");
  assert(talk_base::Thread::Current() == signal_thread_);
  switch (message->message_id) {
  case MSG_LOGIN:
    LOGI("ClientSignalingThread::OnMessage - MSG_LOGIN");
    LoginS();
    break;
  case MSG_DISCONNECT:
    LOGI("ClientSignalingThread::OnMessage - MSG_DISCONNECT");
    DisconnectS();
    break;
  case MSG_CALL:
    LOGI("ClientSignalingThread::OnMessage - MSG_CALL");
    CallS(static_cast<StringData*>(message->pdata)->s_);
    delete message->pdata;
    break;
  case MSG_ACCEPT_CALL:
    LOGI("ClientSignalingThread::OnMessage - MSG_ACCEPT_CALL");
    AcceptCallS();
    break;
  case MSG_DECLINE_CALL:
    LOGI("ClientSignalingThread::OnMessage - MSG_DECLINE_CALL");
    DeclineCallS();
    break;
  case MSG_END_CALL:
    LOGI("ClientSignalingThread::OnMessage - MSG_END_CALL");
    EndCallS();
    break;
  default:
    LOGI("ClientSignalingThread::OnMessage - UNKNOWN falling back to base class");
    SignalThread::OnMessage(message);
    break;
  }
}

void ClientSignalingThread::DoWork() {
  LOGI("ClientSignalingThread::DoWork");
  assert(talk_base::Thread::Current() == worker());
  worker()->ProcessMessages(talk_base::kForever);
}

void ClientSignalingThread::LoginS() {
    LOGI("ClientSignalingThread::LoginS");
    assert(talk_base::Thread::Current() == signal_thread_);
    if (use_ssl_) {
        talk_base::InitializeSSL();
    }
    buzz::TlsOptions auth_type;
    if (use_ssl_) {
        auth_type = buzz::TLS_REQUIRED;
    } else {
        auth_type = buzz::TLS_DISABLED;
    }

    if (pump_->AllChildrenDone()) {
      LOGE("AllChildrenDone doing required {delete pump_;pump_ = new TXmppPump(this);} yet...");
    }

    //I don't like this where does it get deleted? feels like a memory leak.
    TXmppSocket *sock = new TXmppSocket(auth_type);
    TXmppAuth *auth = new TXmppAuth();
    //I don't like this where does it get deleted? feels like a memory leak.
    LOGE("Where do we delete sock@(0x%x) and auth@(0x%x)", reinterpret_cast<int>(sock), reinterpret_cast<int>(auth));
    pump_->DoLogin(xcs_, sock, auth);
}

void ClientSignalingThread::DisconnectS() {
    LOGI("ClientSignalingThread::DisconnectS");
    assert(talk_base::Thread::Current() == signal_thread_);
    if(call_){
      signal_thread_->Post(this, MSG_END_CALL);
      signal_thread_->PostDelayed(100, this, MSG_DISCONNECT);
    }else{
        if (pump_->AllChildrenDone()) {
          LOGE("AllChildrenDone NOT doing required {delete pump_;pump_ = new TXmppPump(this);} yet...");
        }
        pump_->DoDisconnect();
    }
}

void ClientSignalingThread::CallS(const std::string &remoteJid) {
    LOGI("ClientSignalingThread::CallS");
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
void ClientSignalingThread::AcceptCallS() {
    LOGI("ClientSignalingThread::AcceptCallS");
    assert(talk_base::Thread::Current() == signal_thread_);
    if(call_ && incoming_call_ && call_->sessions().size() == 1){
        cricket::CallOptions options;
        call_->AcceptSession(call_->sessions()[0], options);
        media_client_->SetFocus(call_);
        if (call_->has_data()) {
            call_->SignalDataReceived.connect(this, &ClientSignalingThread::OnDataReceived);
        }
        incoming_call_ = false;
    }else{
      LOGE("ClientSignalingThread::AcceptCallW - No incoming call to accept");
    }
}
void ClientSignalingThread::DeclineCallS() {
    LOGI("ClientSignalingThread::DeclineCallS");
    assert(talk_base::Thread::Current() == signal_thread_);
    if(call_ && incoming_call_ && call_->sessions().size() == 1){
        call_->RejectSession(call_->sessions()[0]);
        incoming_call_ = false;
    }else{
      LOGE("ClientSignalingThread::DeclineCallW - No incoming call to decline");
    }
}
void ClientSignalingThread::EndCallS() {
    LOGI("ClientSignalingThread::EndCallS");
    assert(talk_base::Thread::Current() == signal_thread_);
    if (call_) {
        call_->Terminate();
    }
}

void ClientSignalingThread::InitMedia() {
    LOGI("ClientSignalingThread::InitMedia");
    assert(talk_base::Thread::Current() == signal_thread_);
    std::string client_unique = pump_->client()->jid().Str();
    talk_base::InitRandom(client_unique.c_str(), client_unique.size());


    //TODO: We need to modify the last params of this to add TURN server addresses.
    session_manager_->SignalRequestSignaling.connect(this, &ClientSignalingThread::OnRequestSignaling);
    session_manager_->SignalSessionCreate.connect(this, &ClientSignalingThread::OnSessionCreate);
    session_manager_->OnSignalingReady();

    session_manager_task_ = new cricket::SessionManagerTask(pump_->client(), session_manager_);
    session_manager_task_->EnableOutgoingMessages();
    session_manager_task_->Start();

    
#ifdef SIMPLIFY_MEDIA_CLIENT
    media_client_ = new cricket::MediaSessionClient(pump_->client()->jid(), session_manager_);
#else
    if(media_engine_ != NULL){
      delete media_engine_;
      media_engine_ = NULL;
    }
    media_engine_ = cricket::MediaEngineFactory::Create();
    media_engine_->SignalTerminate.connect(this, &ClientSignalingThread::OnMediaEngineTerminate);
    media_client_ = new cricket::MediaSessionClient(pump_->client()->jid(), session_manager_, media_engine_, data_engine_, cricket::DeviceManagerFactory::Create());
#endif
    media_client_->SignalCallCreate.connect(this, &ClientSignalingThread::OnCallCreate);
    media_client_->SignalCallDestroy.connect(this, &ClientSignalingThread::OnCallDestroy);
    media_client_->set_secure(cricket::SEC_DISABLED);
}
void ClientSignalingThread::InitPresence() {
    //NFHACK Fix the news
    LOGI("ClientSignalingThread::InitPresence");
    assert(talk_base::Thread::Current() == signal_thread_);
    presence_push_ = new buzz::PresencePushTask(pump_->client());
    presence_push_->SignalStatusUpdate.connect(this, &ClientSignalingThread::OnStatusUpdate);
    presence_push_->Start();

    presence_out_ = new buzz::PresenceOutTask(pump_->client());
    SetAvailable(pump_->client()->jid(), &my_status_);
    SetCaps(media_client_->GetCapabilities(), &my_status_);
    presence_out_->Send(my_status_);
    presence_out_->Start();
}
void ClientSignalingThread::SetMediaCaps(int media_caps, buzz::Status* status) {
    LOGI("ClientSignalingThread::SetMediaCaps");
    assert(talk_base::Thread::Current() == signal_thread_);
    status->set_voice_capability((media_caps & cricket::AUDIO_RECV) != 0);
    status->set_video_capability((media_caps & cricket::VIDEO_RECV) != 0);
    status->set_camera_capability((media_caps & cricket::VIDEO_SEND) != 0);
}

void ClientSignalingThread::SetCaps(int media_caps, buzz::Status* status) {
    LOGI("ClientSignalingThread::SetCaps");
    assert(talk_base::Thread::Current() == signal_thread_);
    status->set_know_capabilities(true);
    status->set_pmuc_capability(false);
    SetMediaCaps(media_caps, status);
}

void ClientSignalingThread::SetAvailable(const buzz::Jid& jid, buzz::Status* status) {
    LOGI("ClientSignalingThread::SetAvailable");
    assert(talk_base::Thread::Current() == signal_thread_);
    status->set_jid(jid);
    status->set_available(true);
    status->set_show(buzz::Status::SHOW_ONLINE);
}
}  // namespace tuenti
