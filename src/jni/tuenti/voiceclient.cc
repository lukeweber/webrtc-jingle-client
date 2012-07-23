#include <string.h>
#include <vector>
#include <memory>
#include <assert.h>

#include "tuenti/logging.h"
#include "tuenti/presenceouttask.h"
#include "tuenti/presencepushtask.h"
#include "tuenti/threadpriorityhandler.h"
#include "tuenti/voiceclient.h"

#include "talk/base/logging.h"
#include "talk/base/ssladapter.h"
#include "talk/base/thread.h"
#include "talk/base/signalthread.h"
#include "talk/examples/login/xmppauth.h"
#include "talk/examples/login/xmppsocket.h"
#include "talk/p2p/base/session.h"
#include "talk/p2p/base/sessionmanager.h"
#include "talk/p2p/client/basicportallocator.h"
#include "talk/p2p/client/sessionmanagertask.h"

enum {
    MSG_INVALID=-1,
    MSG_INIT,
    MSG_DESTROY,
    MSG_START,
    MSG_DISCONNECT,
    MSG_CALL,
    MSG_END_CALL,
    MSG_ACCEPT_CALL,
    MSG_DECLINE_CALL,
    MSG_STATE_CHANGE,
    MSG_NUM
};
const char* msgNames[] = 
  {
//    "MSG_INVALID",
    "MSG_INIT",
    "MSG_DESTROY",
    "MSG_START",
    "MSG_DISCONNECT",
    "MSG_CALL",
    "MSG_END_CALL",
    "MSG_ACCEPT_CALL",
    "MSG_DECLINE_CALL",
    "MSG_STATE_CHANGE",
    "MSG_NUM"
  };

struct StringData: public talk_base::MessageData {
    StringData(std::string s) :
            s_(s) {
    }
    std::string s_;
};

struct StateChangeData: public talk_base::MessageData {
    StateChangeData(buzz::XmppEngine::State state) :
            s_(state) {
    }
    buzz::XmppEngine::State s_;
};

VoiceClient::VoiceClient(VoiceClientNotify *notify)
:notify_(notify)
,worker_thread_(NULL)
,signaling_thread_(NULL)
,media_engine_(NULL)
,media_client_(NULL)
,data_engine_(NULL)
,call_(NULL)
,session_(NULL)
,session_manager_(NULL)
,session_manager_task_(NULL)
,incoming_call_(false)
,auto_accept_(false)
,releasing_(false)
,network_manager_(NULL)
,roster_(NULL)
,presence_push_(NULL)
,presence_out_(NULL)
,port_allocator_(NULL)
,portallocator_flags_(0)
,initial_protocol_(cricket::PROTOCOL_HYBRID)
,secure_policy_(cricket::SEC_DISABLED){
    LOGI("VoiceClient::VoiceClient");


    // reset the main, signaling and worker threads
    main_thread_.reset(new talk_base::AutoThread());
    main_thread_->Start();
    //signaling_thread_ = new talk_base::SignalThread();
    if(signaling_thread_ == NULL){
        signaling_thread_ = new talk_base::Thread();
        LOGI("VoiceClient::VoiceClient - creating signaling_thread_ at %x", signaling_thread_);
        signaling_thread_->Start();
    }

    // a few standard logs not sure why they are not working
    talk_base::LogMessage::LogThreads();
    talk_base::LogMessage::LogTimestamps();

    my_status_.set_caps_node("http://github.com/lukeweber/webrtc-jingle");
    my_status_.set_version("1.0-SNAPSHOT");

    //this creates all objects on the signaling thread
    signaling_thread_->Send(this, MSG_INIT);
}

VoiceClient::~VoiceClient() {
    LOGI("VoiceClient::~VoiceClient");
    //this creates all objects on the signaling thread
    if (talk_base::Thread::Current() != signaling_thread_ && signaling_thread_) {
        signaling_thread_->ProcessMessages(1000);
        LOGI("-- deleting signaling thread %x", signaling_thread_);
        signaling_thread_->Quit();
        delete signaling_thread_;
        signaling_thread_ = NULL;
    }
    /*if (talk_base::Thread::Current() != main_thread_.get() && main_thread_.get()) {
        LOGI("-- deleting main thread %x", main_thread_.get());
        main_thread_.get()->Quit();
        main_thread_.reset(NULL);
    }
    /*LOGI("VoiceClient::~VoiceClient - reseting pump");
    pump_.reset(NULL);
    LOGI("VoiceClient::~VoiceClient - deleting roster");
    if(roster_){
        delete roster_;
        roster_ = NULL;
    }
    LOGI("VoiceClient::~VoiceClient - deleting network manager");
    if(network_manager_ != NULL){
        delete network_manager_;
        network_manager_ = NULL;
    }
    LOGI("VoiceClient::~VoiceClient - deleting data engine");
    if (data_engine_ != NULL) {
        delete data_engine_;
        data_engine_ = NULL;
    }
    LOGI("VoiceClient::~VoiceClient - deleting port allocator");
    if (port_allocator_ != NULL){
        delete port_allocator_;
        port_allocator_ = NULL;
    }
    if (session_manager_ != NULL){
        delete session_manager_;
        session_manager_ = NULL;
    }*/
}
void VoiceClient::InitializeS() {
    LOGI("VoiceClient::InitializeS");
    assert(talk_base::Thread::Current() == signaling_thread_);
    // creating working thread
    if(worker_thread_ == NULL){
        worker_thread_ = new talk_base::Thread();
        LOGI("VoiceClient::InitializeS - creating worker_thread_ at %x", worker_thread_);
        worker_thread_->Start();
    }
    // simple initializers
    if(roster_ == NULL){
        roster_ = new RosterMap();
        LOGI("VoiceClient::InitializeS - new RosterMap %x", roster_);
    }
    if(network_manager_ == NULL){
        network_manager_ = new talk_base::BasicNetworkManager();
        LOGI("VoiceClient::InitializeS - new BasicNetworkManager %x", network_manager_);
    }
    if(data_engine_ == NULL){
        data_engine_ = new cricket::DataEngine();
        LOGI("VoiceClient::InitializeS - new DataEngine %x", data_engine_);
    }
    if(port_allocator_ == NULL){
        talk_base::SocketAddress stun_addr("stun.l.google.com", 19302);
        port_allocator_ = new cricket::BasicPortAllocator(network_manager_, stun_addr,
            talk_base::SocketAddress(), talk_base::SocketAddress(), talk_base::SocketAddress());
        LOGI("VoiceClient::InitializeS - new BasicPortAllocator %x", port_allocator_);
        if (portallocator_flags_ != 0) {
            port_allocator_->set_flags(portallocator_flags_);
        }
    }

    if(session_manager_ == NULL){
        session_manager_ = new cricket::SessionManager(port_allocator_, worker_thread_);
        LOGI("VoiceClient::InitializeS - new SessionManager %x", session_manager_);
    }

    // reset the pump
    if(pump_.get() == NULL){
        pump_.reset(new XmppPump(this));
        LOGI("VoiceClient::InitializeS - new XmpPPump %x", pump_.get());
    }
}
void VoiceClient::DestroyS() {
    LOGI("VoiceClient::DestroyS");
    assert(talk_base::Thread::Current() == signaling_thread_);
    if(call_){
        //early out
        releasing_ = true;
        LOGI("VoiceClient::DestroyS - we still have an active call so delaying destruction");
        return;
    }
    //delting media engine
    if (media_engine_.get()) {
      LOGI("-- deleting media engine%x", media_engine_.get());
      media_engine_->Terminate();
    }
    //deleting worker thread
    if (talk_base::Thread::Current() != worker_thread_ && worker_thread_) {
        worker_thread_->ProcessMessages(1000);
        LOGI("-- deleting worker thread %x", worker_thread_);
        worker_thread_->Quit();
        delete worker_thread_;
        worker_thread_ = NULL;
    }
    // reset the pump
    if(pump_.get() != NULL){
        LOGI("VoiceClient::DestroyS - delete pump_ %x", pump_.get());
        pump_.reset(NULL);
    }
    // simple destructors
    if(roster_ != NULL){
        LOGI("VoiceClient::DestroyS - delete roster_ %x", roster_);
        delete roster_;
        roster_ = NULL;
    }
    if(network_manager_ != NULL){
        LOGI("VoiceClient::DestroyS - delete network_manager_ %x", network_manager_);
        delete network_manager_;
        network_manager_ = NULL;
    }
    if(data_engine_ != NULL){
        delete data_engine_;
        LOGI("VoiceClient::DestroyS - delete data_engine_ %x", data_engine_);
        data_engine_ = NULL;
    }
    if(port_allocator_ != NULL){
        LOGI("VoiceClient::DestroyS - delete port_allocator_ %x", port_allocator_);
        delete port_allocator_;
        port_allocator_ = NULL;
    }

    if(session_manager_ != NULL){
        LOGI("VoiceClient::DestroyS - delete session_manager_ %x", session_manager_);
        delete session_manager_;
        session_manager_ = NULL;
    }
    delete this;
}

void VoiceClient::OnMessage(talk_base::Message *msg) {
    LOGI("VoiceClient::OnMessage");
    if(talk_base::Thread::Current() == signaling_thread_){
        OnMessageS(msg);
    }else if(talk_base::Thread::Current() == main_thread_.get()){
        OnMessageM(msg);
    }else{
        LOGE("OnMessage is only allowed from the main &| signaling threads");
    }
}
void VoiceClient::OnMessageM(talk_base::Message *msg) {
    LOGI("VoiceClient::OnMessageM");
    assert(talk_base::Thread::Current() == main_thread_.get());
    switch (msg->message_id) {
    default:
        LOGE("VoiceClient::OnMessageM - Unknown State (%s) doing nothing...", msgNames[msg->message_id]);
        break;
    case MSG_STATE_CHANGE:
        LOGI("VoiceClient::OnMessageM - (%s)", msgNames[msg->message_id]);
        OnStateChangeM(static_cast<StateChangeData*>(msg->pdata)->s_);
        //NFHACK this looks confusing deletes should be in destructors
        //delete msg->pdata;
        break;
    }
}
void VoiceClient::OnMessageS(talk_base::Message *msg) {
    LOGI("VoiceClient::OnMessageS");
    assert(talk_base::Thread::Current() == signaling_thread_);
    switch (msg->message_id) {
    default:
        LOGE("VoiceClient::OnMessageS - Unknown State (%s) doing nothing...", msgNames[msg->message_id]);
        break;
    case MSG_INIT:
        LOGI("VoiceClient::OnMessageS - (%s)", msgNames[msg->message_id]);
        InitializeS();
        break;
    case MSG_DESTROY:
        LOGI("VoiceClient::OnMessageS - (%s)", msgNames[msg->message_id]);
        DestroyS();
        break;
    case MSG_START:
        LOGI("VoiceClient::OnMessageS - (%s)", msgNames[msg->message_id]);
        LoginS();
        break;
    case MSG_DISCONNECT:
        LOGI("VoiceClient::OnMessageS - (%s)", msgNames[msg->message_id]);
        DisconnectS();
        break;
    case MSG_CALL:
        LOGI("VoiceClient::OnMessageS - (%s)", msgNames[msg->message_id]);
        CallS(static_cast<StringData*>(msg->pdata)->s_);
        //NFHACK this looks confusing deletes should be in destructors
        //delete msg->pdata;
        break;
    case MSG_END_CALL:
        LOGI("VoiceClient::OnMessageS - (%s)", msgNames[msg->message_id]);
        EndCallS();
        break;
    case MSG_ACCEPT_CALL:
        LOGI("VoiceClient::OnMessageS - (%s)", msgNames[msg->message_id]);
        AcceptCallS();
        break;
    case MSG_DECLINE_CALL:
        LOGI("VoiceClient::OnMessageS - (%s)", msgNames[msg->message_id]);
        DeclineCallS();
        break;
    case MSG_STATE_CHANGE:
        LOGI("VoiceClient::OnMessageS - (%s)", msgNames[msg->message_id]);
        OnStateChangeS(static_cast<StateChangeData*>(msg->pdata)->s_);
        //NFHACK this looks confusing deletes should be in destructors
        //delete msg->pdata;
        break;
    }
}
//This class just forwards requests to the appropriate function depending on the thread
void VoiceClient::OnStateChange(buzz::XmppEngine::State state) {
    LOGI("VoiceClient::OnStateChange");
    if(talk_base::Thread::Current() == signaling_thread_){
        OnStateChangeS(state);
    }else if(talk_base::Thread::Current() == main_thread_.get()){
        OnStateChangeM(state);
    }else{
        LOGE("OnStateChange is only allowed from the main &| worker threads");
    }
}

void VoiceClient::OnStateChangeS(buzz::XmppEngine::State state) {
    LOGI("VoiceClient::OnStateChangeS");
    assert(talk_base::Thread::Current() == signaling_thread_);
    switch (state) {
    default:
        LOGE("VoiceClient::OnStateChangeS - Unknown State (???) doing nothing...");
        break;
    case buzz::XmppEngine::STATE_START:
        LOGE("VoiceClient::OnStateChangeS - State (STATE_START) doing nothing...");
        break;
    case buzz::XmppEngine::STATE_OPENING:
        LOGE("VoiceClient::OnStateChangeS - State (STATE_OPENING) doing nothing...");
        break;
    case buzz::XmppEngine::STATE_OPEN:
        LOGI("VoiceClient::OnStateChangeS - State (STATE_OPEN) initing media & presence...");
        InitMediaS();
        InitPresenceS();
        break;
    case buzz::XmppEngine::STATE_CLOSED:
        LOGI("VoiceClient::OnStateChangeS - State (STATE_CLOSED) commented out terminating media engine...");
        /*if (media_engine_.get()) {
          media_engine_->Terminate();
        }*/
        break;
    }
    main_thread_->Post(this, MSG_STATE_CHANGE, new StateChangeData(state));
}

/* Executed in the main thread */
void VoiceClient::OnStateChangeM(buzz::XmppEngine::State state) {
    LOGI("VoiceClient::OnStateChangeM");
    assert(talk_base::Thread::Current() == main_thread_.get());
    switch (state) {
    default:
        LOGE("VoiceClient::OnStateChangeM - Unknown State (????) doing nothing...");
        break;
    case buzz::XmppEngine::STATE_NONE:
        LOGE("VoiceClient::OnStateChangeM - State (STATE_NONE) doing nothing...");
        break;
    case buzz::XmppEngine::STATE_START:
        LOGE("VoiceClient::OnStateChangeM - State (STATE_START) doing nothing...");
        break;
    case buzz::XmppEngine::STATE_OPENING:
        LOGE("VoiceClient::OnStateChangeM - State (STATE_OPENING) doing nothing...");
        break;
    case buzz::XmppEngine::STATE_OPEN:
        LOGE("VoiceClient::OnStateChangeM - State (STATE_OPEN) doing nothing...");
        break;
    case buzz::XmppEngine::STATE_CLOSED:
        LOGI("VoiceClient::OnStateChangeM - State (STATE_CLOSED) cleaning up ssl...");
        if (use_ssl_)
            talk_base::CleanupSSL();
        break;
    }
    if (notify_) {
        notify_->OnStateChange(state);
    }
}

void VoiceClient::InitMediaS() {
    assert(talk_base::Thread::Current() == signaling_thread_);
    LOGI("VoiceClient::InitMediaS");
    std::string client_unique = pump_->client()->jid().Str();
    talk_base::InitRandom(client_unique.c_str(), client_unique.size());


    //TODO: We need to modify the last params of this to add TURN server addresses.
    session_manager_->SignalRequestSignaling.connect(this, &VoiceClient::OnRequestSignaling);
    session_manager_->SignalSessionCreate.connect(this, &VoiceClient::OnSessionCreate);
    session_manager_->OnSignalingReady();

    session_manager_task_ = new cricket::SessionManagerTask(pump_->client(), session_manager_);
    session_manager_task_->EnableOutgoingMessages();
    session_manager_task_->Start();

    media_engine_.reset(cricket::MediaEngineFactory::Create());
    media_engine_->SignalTerminate.connect(this, &VoiceClient::OnMediaEngineTerminate);

    media_client_ = new cricket::MediaSessionClient(pump_->client()->jid(), session_manager_,
            media_engine_.get(), data_engine_, cricket::DeviceManagerFactory::Create());
    media_client_->SignalCallCreate.connect(this, &VoiceClient::OnCallCreate);
    media_client_->SignalCallDestroy.connect(this, &VoiceClient::OnCallDestroy);
    media_client_->set_secure(secure_policy_);
}

void VoiceClient::OnMediaEngineTerminate() {
    LOGI("VoiceClient::OnMediaEngineTerminate");
    if (media_engine_.get()) {
        media_engine_.reset();
    }
}

void SetMediaCaps(int media_caps, buzz::Status* status) {
    LOGI("VoiceClient::SetMediaCaps");
    status->set_voice_capability((media_caps & cricket::AUDIO_RECV) != 0);
    status->set_video_capability((media_caps & cricket::VIDEO_RECV) != 0);
    status->set_camera_capability((media_caps & cricket::VIDEO_SEND) != 0);
}

void SetCaps(int media_caps, buzz::Status* status) {
    LOGI("VoiceClient::SetCaps");
    status->set_know_capabilities(true);
    status->set_pmuc_capability(false);
    SetMediaCaps(media_caps, status);
}

void SetAvailable(const buzz::Jid& jid, buzz::Status* status) {
    LOGI("VoiceClient::SetAvailable");
    status->set_jid(jid);
    status->set_available(true);
    status->set_show(buzz::Status::SHOW_ONLINE);
}

void VoiceClient::InitPresenceS() {
    assert(talk_base::Thread::Current() == signaling_thread_);
    //NFHACK Fix the news
    LOGI("VoiceClient::InitPresenceS");
    presence_push_ = new buzz::PresencePushTask(pump_->client(), this);
    presence_push_->SignalStatusUpdate.connect(this, &VoiceClient::OnStatusUpdate);
    presence_push_->Start();

    presence_out_ = new buzz::PresenceOutTask(pump_->client());
    SetAvailable(pump_->client()->jid(), &my_status_);
    SetCaps(media_client_->GetCapabilities(), &my_status_);
    presence_out_->Send(my_status_);
    presence_out_->Start();
}

void VoiceClient::OnRequestSignaling() {
    LOGI("VoiceClient::OnRequestSignaling");
    session_manager_->OnSignalingReady();
}

void VoiceClient::OnSessionCreate(cricket::Session* session, bool initiate) {
    LOGI("VoiceClient::OnSessionCreate");
    session->set_current_protocol(initial_protocol_);
}

void VoiceClient::OnCallCreate(cricket::Call* call) {
    LOGI("VoiceClient::OnCallCreate");
    call->SignalSessionState.connect(this, &VoiceClient::OnSessionState);
}

void VoiceClient::OnCallDestroy(cricket::Call* call) {
    LOGI("VoiceClient::OnCallDestroy");
    if (call == call_) {
        LOGI("call destroyed");
        call_ = NULL;
        session_ = NULL;
    }
    if(releasing_ && signaling_thread_){
        signaling_thread_->Send(this, MSG_DESTROY);
    }
}

void VoiceClient::OnSessionState(cricket::Call* call, cricket::Session* session,
        cricket::Session::State state) {
    LOGI("VoiceClient::OnSessionState");
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
                call_->SignalDataReceived.connect(this, &VoiceClient::OnDataReceived);
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
    /*if (state == cricket::Session::STATE_RECEIVEDINITIATE) {
        LOGI("VoiceClient::OnSessionState - STATE_RECEIVEDINITIATE");
        buzz::Jid jid(session->remote_name());
        LOGI("Incoming call from '%s'", jid.Str().c_str());
        call_ = call;
        session_ = session;
        incoming_call_ = true;
        if (auto_accept_) {
            AcceptCall();
        }
    } else if (state == cricket::Session::STATE_SENTINITIATE) {
        LOGI("VoiceClient::OnSessionState - STATE_SENTINITIATE");
    } else if (state == cricket::Session::STATE_RECEIVEDACCEPT) {
        LOGI("VoiceClient::OnSessionState - STATE_RECEIVEDACCEPT");
        if (call_->has_data()) {
            call_->SignalDataReceived.connect(this, &VoiceClient::OnDataReceived);
        }
    } else if (state == cricket::Session::STATE_RECEIVEDREJECT) {
        LOGI("VoiceClient::OnSessionState - STATE_RECEIVEDREJECT");
    } else if (state == cricket::Session::STATE_INPROGRESS) {
        LOGI("VoiceClient::OnSessionState - STATE_INPROGRESS");
        call->StartSpeakerMonitor(session);
    } else if (state == cricket::Session::STATE_RECEIVEDTERMINATE) {
        LOGI("VoiceClient::OnSessionState - STATE_RECEIVEDTERMINATE");
    }*/
    if (notify_) {
        notify_->OnCallStateChange(session, state);
    }
}

void VoiceClient::OnStatusUpdate(const buzz::Status& status) {
    LOGI("VoiceClient::OnStatusUpdate");
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

void VoiceClient::OnDataReceived(cricket::Call*, const cricket::ReceiveDataParams& params,
        const std::string& data) {
    LOGI("VoiceClient::OnDataReceived");
    cricket::StreamParams stream;
    if (GetStreamBySsrc(call_->data_recv_streams(), params.ssrc, &stream)) {
        LOGI(
                "Received data from '%s' on stream '%s' (ssrc=%u): %s", stream.nick.c_str(), stream.name.c_str(), params.ssrc, data.c_str());
    } else {
        LOGI("Received data (ssrc=%u): %s", params.ssrc, data.c_str());
    }
}

void VoiceClient::Login(std::string &username, std::string &password, std::string &server,
        bool use_ssl) {
    LOGI("VoiceClient::Login");
    //assert(talk_base::Thread::Current() == main_thread_.get());
    assert(talk_base::Thread::Current() != signaling_thread_ && talk_base::Thread::Current() != worker_thread_);

    use_ssl_ = use_ssl;

    if (use_ssl_)
        talk_base::InitializeSSL();

    buzz::Jid jid = buzz::Jid(username);

    talk_base::InsecureCryptStringImpl pass;
    pass.password() = password;

    xcs_.set_user(jid.node());
    xcs_.set_resource("Tuenti Voice");
    xcs_.set_host(jid.domain());
    xcs_.set_use_tls(buzz::TLS_REQUIRED);
    xcs_.set_pass(talk_base::CryptString(pass));
    xcs_.set_server(talk_base::SocketAddress(server, 5222));

    signaling_thread_->Send(this, MSG_START);
}

void VoiceClient::Disconnect() {
    LOGI("VoiceClient::Disconnect");
    //assert(talk_base::Thread::Current() == main_thread_.get());
    assert(talk_base::Thread::Current() != signaling_thread_ && talk_base::Thread::Current() != worker_thread_);
    if (signaling_thread_)
        signaling_thread_->Send(this, MSG_END_CALL);
        signaling_thread_->Send(this, MSG_DISCONNECT);
}

void VoiceClient::Call(std::string &remoteJid) {
    LOGI("VoiceClient::Call");
    //assert(talk_base::Thread::Current() == main_thread_.get());
    assert(talk_base::Thread::Current() != signaling_thread_ && talk_base::Thread::Current() != worker_thread_);
    if (signaling_thread_)
        //Possible memory leak
        //worker_thread_->Post(this, MSG_CALL, new StringData(remoteJid));
        signaling_thread_->Post(this, MSG_CALL, new StringData(remoteJid));
}

void VoiceClient::EndCall() {
    LOGI("VoiceClient::EndCall");
    //assert(talk_base::Thread::Current() == main_thread_.get());
    assert(talk_base::Thread::Current() != signaling_thread_ && talk_base::Thread::Current() != worker_thread_);
    if (signaling_thread_)
        signaling_thread_->Send(this, MSG_END_CALL);
}

void VoiceClient::AcceptCall() {
    LOGI("VoiceClient::AcceptCall");
    //assert(talk_base::Thread::Current() == main_thread_.get());
    assert(talk_base::Thread::Current() != signaling_thread_ && talk_base::Thread::Current() != worker_thread_);
    if (signaling_thread_)
        signaling_thread_->Send(this, MSG_ACCEPT_CALL);
}

void VoiceClient::DeclineCall() {
    LOGI("VoiceClient::DeclineCall");
    //assert(talk_base::Thread::Current() == main_thread_.get());
    assert(talk_base::Thread::Current() != signaling_thread_ && talk_base::Thread::Current() != worker_thread_);
    if (signaling_thread_)
        signaling_thread_->Send(this, MSG_DECLINE_CALL);
}

void VoiceClient::Destruct() {
    LOGI("VoiceClient::TearDown");
    //assert(talk_base::Thread::Current() == main_thread_.get());//not sure if this is neccessary or if it it the below conditional is not
    assert(talk_base::Thread::Current() != signaling_thread_ && talk_base::Thread::Current() != worker_thread_);
    EndCall();
    Disconnect();
    signaling_thread_->Send(this, MSG_DESTROY);
    /*if (signaling_thread_ && talk_base::Thread::Current() != signaling_thread_){
        signaling_thread_->Send(this, MSG_END_CALL);
        signaling_thread_->Send(this, MSG_DISCONNECT);
        signaling_thread_->ProcessMessages(1000);
    }*/
}

void VoiceClient::LoginS() {
    LOGI("VoiceClient::LoginS");
    assert(talk_base::Thread::Current() == signaling_thread_);

    buzz::TlsOptions auth_type;
    if (use_ssl_) {
        auth_type = buzz::TLS_REQUIRED;
    } else {
        auth_type = buzz::TLS_DISABLED;
    }

    if (!pump_->AllChildrenDone()) {
        //NFHACK removing to avoid errors
        //pump_.reset(new XmppPump(this));
        LOGI("Blocked by running children");
        //pump_->DoDisconnect();
    }

    //I don't like this where does it get deleted? feels like a memory leak.
    pump_->DoLogin(xcs_, new XmppSocket(auth_type), new XmppAuth());
}

void VoiceClient::DisconnectS() {
    LOGI("VoiceClient::DisconnectS");
    assert(talk_base::Thread::Current() == signaling_thread_);
    pump_->DoDisconnect();
}

void VoiceClient::CallS(const std::string &remoteJid) {
    LOGI("VoiceClient::CallS");
    //assert(talk_base::Thread::Current() == signaling_thread_);
    assert(talk_base::Thread::Current() == signaling_thread_);

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
            session_ = call_->InitiateSession(found_jid, options);
        }
        media_client_->SetFocus(call_);
    } else {
        LOGI("Could not find online friend '%s'", remoteJid.c_str());
    }
}

void VoiceClient::EndCallS() {
    LOGI("VoiceClient::EndCallS");
    assert(talk_base::Thread::Current() == signaling_thread_);
    if (call_) {
        call_->Terminate();
    }
}

void VoiceClient::AcceptCallS() {
    LOGI("VoiceClient::AcceptCallS");
    assert(talk_base::Thread::Current() == signaling_thread_);
    assert(call_ && incoming_call_);
    assert(call_->sessions().size() == 1);

    cricket::CallOptions options;
    call_->AcceptSession(call_->sessions()[0], options);
    media_client_->SetFocus(call_);
    if (call_->has_data()) {
        call_->SignalDataReceived.connect(this, &VoiceClient::OnDataReceived);
    }
    incoming_call_ = false;
}

void VoiceClient::DeclineCallS() {
    LOGI("VoiceClient::DeclineCallS");
    assert(talk_base::Thread::Current() == signaling_thread_);
    assert(call_ && incoming_call_);
    call_->RejectSession(call_->sessions()[0]);
    incoming_call_ = false;
}
