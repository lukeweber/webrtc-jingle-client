#include <string.h>
#include <vector>
#include <memory>
#include <assert.h>

#include "tuenti/voiceclient.h"
#include "tuenti/logging.h"
#include "tuenti/threadpriorityhandler.h"
#include "tuenti/clientsignalingthread.h"
#include "talk/base/thread.h"
#include "talk/base/logging.h"

//#include "talk/base/signalthread.h"

namespace tuenti{
enum {
    MSG_INIT,
    MSG_DESTROY,
};
const char* msgNames[] = 
  {
    "MSG_INIT",
    "MSG_DESTROY",
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
,signal_thread_(NULL)
,client_signaling_thread_(NULL)
{
    LOGI("VoiceClient::VoiceClient");


    // a few standard logs not sure why they are not working
    talk_base::LogMessage::LogThreads();
    talk_base::LogMessage::LogTimestamps();

    my_status_.set_caps_node("http://github.com/lukeweber/webrtc-jingle");
    my_status_.set_version("1.0-SNAPSHOT");

    //this creates all objects on the signaling thread
    if(signal_thread_ == NULL){
      signal_thread_ = new talk_base::Thread();
      signal_thread_->Post(this, MSG_INIT);
      signal_thread_->Start();
    }
}

VoiceClient::~VoiceClient() {
    LOGI("VoiceClient::~VoiceClient");
    if(signal_thread_ != NULL){
      signal_thread_->Quit();
      signal_thread_ = NULL;
    }
}
void VoiceClient::Destroy(int delay) {
    LOGI("VoiceClient::Destroy");
    if(signal_thread_ != NULL){
      if(delay <= 0){
        signal_thread_->Post(this, MSG_DESTROY);
      }else{
        signal_thread_->PostDelayed(delay, this, MSG_DESTROY);
      }
    }
}

void VoiceClient::InitializeS() {
    LOGI("VoiceClient::InitializeS");
    if(client_signaling_thread_ == NULL){
        client_signaling_thread_ = new tuenti::ClientSignalingThread(notify_, signal_thread_);
        LOGI("VoiceClient::VoiceClient - new ClientSignalingThread client_signaling_thread_@(0x%x)", reinterpret_cast<int>(client_signaling_thread_));
        client_signaling_thread_->Start();
    }
}
void VoiceClient::DestroyS() {
    LOGI("VoiceClient::DestroyS");
    if(client_signaling_thread_ != NULL){
        LOGI("VoiceClient::VoiceClient - destroy ClientSignalingThread client_signaling_thread_@(0x%x)", reinterpret_cast<int>(client_signaling_thread_));
        if(client_signaling_thread_->Destroy()){
          delete this; //NFHACK Pretty ugly should probably call a all good to delete callback in voiceclient_main
        }else{
          Destroy(100);
        }
    }
}

void VoiceClient::OnMessage(talk_base::Message *msg) {
    LOGI("VoiceClient::OnMessage");
    assert(talk_base::Thread::Current() == signal_thread_);
    switch (msg->message_id) {
    default:
        LOGE("VoiceClient::OnMessage - Unknown State (%s) doing nothing...", msgNames[msg->message_id]);
        break;
    case MSG_INIT:
        LOGI("VoiceClient::OnMessage - (%s)", msgNames[msg->message_id]);
        InitializeS();
        break;
    case MSG_DESTROY:
        LOGI("VoiceClient::OnMessage - (%s)", msgNames[msg->message_id]);
        DestroyS();
        break;
    }
}

void VoiceClient::Login(std::string &username, std::string &password, std::string &server, bool use_ssl) {
    LOGI("VoiceClient::Login");
    buzz::XmppClientSettings xcs;
    buzz::Jid jid = buzz::Jid(username);
    talk_base::InsecureCryptStringImpl pass;
    pass.password() = password;
    xcs.set_user(jid.node());
    xcs.set_resource("Tuenti Voice");
    xcs.set_host(jid.domain());
    xcs.set_use_tls(buzz::TLS_REQUIRED);
    xcs.set_pass(talk_base::CryptString(pass));
    xcs.set_server(talk_base::SocketAddress(server, 5222));

    client_signaling_thread_->Login(use_ssl, xcs);
}

void VoiceClient::Disconnect() {
    LOGI("VoiceClient::Disconnect");
    client_signaling_thread_->Disconnect();
}

void VoiceClient::Call(std::string &remoteJid) {
    LOGI("VoiceClient::Call");
    client_signaling_thread_->Call(remoteJid);
}

void VoiceClient::EndCall() {
    LOGI("VoiceClient::EndCall");
    client_signaling_thread_->EndCall();
}

void VoiceClient::AcceptCall() {
    LOGI("VoiceClient::AcceptCall");
    client_signaling_thread_->AcceptCall();
}

void VoiceClient::DeclineCall() {
    LOGI("VoiceClient::DeclineCall");
    client_signaling_thread_->DeclineCall();
}

}// namespace tuenti
