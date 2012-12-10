/*
 * webrtc-jingle
 * Copyright 2012 Tuenti Technologies
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
#include <string.h>
#include <assert.h>
#include <vector>
#include <memory>

#ifdef ANDROID
#include "com_tuenti_voice_core_VoiceClient.h"
#endif

#include "client/voiceclient.h"
#include "client/logging.h"
#include "client/threadpriorityhandler.h"
#include "client/clientsignalingthread.h"
#include "talk/base/thread.h"
#include "talk/base/logging.h"

namespace tuenti {

enum {
  MSG_INIT, MSG_DESTROY,
};

const char* msgNames[] = { "MSG_INIT", "MSG_DESTROY", };

#ifdef ANDROID
VoiceClient::VoiceClient(JavaObjectReference *reference)
    : reference_(reference),
    signal_thread_(NULL),
    client_signaling_thread_(NULL) {
    Init();
}
#elif IPHONE
VoiceClient::VoiceClient()
    : signal_thread_(NULL),
    client_signaling_thread_(NULL) {
    Init();
}
#endif

void VoiceClient::Init(){
#ifdef TUENTI_CUSTOM_BUILD
  LOG(INFO) << "LOGT RUNNING WITH TUENTI_CUSTOM_BUILD";
#endif //TUENTI_CUSTOM_BUILD
  // a few standard logs not sure why they are not working
  talk_base::LogMessage::LogThreads();
  talk_base::LogMessage::LogTimestamps();

  // this creates all objects on the signaling thread
  if (signal_thread_ == NULL) {
    signal_thread_ = new talk_base::Thread();
    signal_thread_->Start();
    signal_thread_->Post(this, MSG_INIT);
  }

}

VoiceClient::~VoiceClient() {
  LOGI("VoiceClient::~VoiceClient");
}

void VoiceClient::Destroy() {
  LOGI("VoiceClient::Destroy");
  signal_thread_->Post(this, MSG_DESTROY);
  while( client_signaling_thread_ != NULL ){
    LOGI("VoiceClient::Destroy - loop");
    // Investigate if we could add Chromium base thread logic
    // PlatformThread::YieldCurrentThread()
    talk_base::Thread::Current()->SleepMs(10);
  }
}

void VoiceClient::InitializeS() {
  assert(talk_base::Thread::Current() == signal_thread_);
  LOGI("VoiceClient::InitializeS");

  if (client_signaling_thread_ == NULL) {
    client_signaling_thread_ = new tuenti::ClientSignalingThread(
        signal_thread_);
    LOGI("VoiceClient::VoiceClient - new ClientSignalingThread "
            "client_signaling_thread_@(0x%x)",
            reinterpret_cast<int>(client_signaling_thread_));

    client_signaling_thread_->SignalCallStateChange.connect(
        this, &VoiceClient::OnSignalCallStateChange);
    client_signaling_thread_->SignalCallError.connect(
        this, &VoiceClient::OnSignalCallError);
    client_signaling_thread_->SignalAudioPlayout.connect(
        this, &VoiceClient::OnSignalAudioPlayout);

    client_signaling_thread_->SignalXmppError.connect(
        this, &VoiceClient::OnSignalXmppError);
    client_signaling_thread_->SignalXmppSocketClose.connect(
        this, &VoiceClient::OnSignalXmppSocketClose);;
    client_signaling_thread_->SignalXmppStateChange.connect(
        this, &VoiceClient::OnSignalXmppStateChange);

    client_signaling_thread_->SignalBuddyListReset.connect(
        this, &VoiceClient::OnSignalBuddyListReset);
    client_signaling_thread_->SignalBuddyListRemove.connect(
        this, &VoiceClient::OnSignalBuddyListRemove);
    client_signaling_thread_->SignalBuddyListAdd.connect(
        this, &VoiceClient::OnSignalBuddyListAdd);

    client_signaling_thread_->Start();

    //We know the client is alive when we get this state.
    OnSignalXmppStateChange(buzz::XmppEngine::STATE_NONE);
  }
}

void VoiceClient::DestroyS() {
  LOGI("VoiceClient::DestroyS");
  talk_base::CritScope lock(&destroy_cs_);
  if (client_signaling_thread_ != NULL) {
    LOGI("VoiceClient::VoiceClient - destroy ClientSignalingThread "
            "client_signaling_thread_@(0x%x)",
            reinterpret_cast<int>(client_signaling_thread_));
    LOGI("VoiceClient::DestroyS client_signaling_thread_ - Destroy()");
    // This call, will call SignalThread::Destroy(true)
    // This will quit the thread and join it, thus deleting client_signaling_thread
    client_signaling_thread_->Destroy();
    client_signaling_thread_ = NULL;
    LOGI("VoiceClient::DestroyS client_signaling_thread_ = NULL");
    if (signal_thread_ != NULL) {
      signal_thread_->Clear(NULL);
      signal_thread_->Quit();
      signal_thread_ = NULL;
    }
  }
}

void VoiceClient::OnMessage(talk_base::Message *msg) {
  LOGI("VoiceClient::OnMessage");
  assert(talk_base::Thread::Current() == signal_thread_);
  switch (msg->message_id) {
  default:
    LOGE("VoiceClient::OnMessage - Unknown State (%s) doing nothing...",
            msgNames[msg->message_id]);
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

void VoiceClient::Login(const std::string &username,
  const std::string &password, StunConfig* stun_config,
  const std::string &xmpp_host, int xmpp_port, bool use_ssl) {
  LOGI("VoiceClient::Login");
  LOG(INFO) << "LOGT " << stun_config->ToString();
  if (client_signaling_thread_) {
    client_signaling_thread_->Login(username, password, stun_config,
        xmpp_host, xmpp_port, use_ssl);
  }
}

void VoiceClient::Disconnect() {
  LOGI("VoiceClient::Disconnect");
  if (client_signaling_thread_) {
    client_signaling_thread_->Disconnect();
  }
}

void VoiceClient::Call(std::string remoteJid) {
  LOGI("VoiceClient::Call");
  if (client_signaling_thread_) {
    client_signaling_thread_->Call(remoteJid);
  }
}

void VoiceClient::MuteCall(uint32 call_id, bool mute) {
  LOGI("VoiceClient::MuteCall");
  if (client_signaling_thread_) {
    client_signaling_thread_->MuteCall(call_id, mute);
  }
}

void VoiceClient::HoldCall(uint32 call_id, bool hold) {
  LOGI("VoiceClient::HoldCall");
  if (client_signaling_thread_) {
    client_signaling_thread_->HoldCall(call_id, hold);
  }
}

void VoiceClient::EndCall(uint32 call_id) {
  LOGI("VoiceClient::EndCall");
  if (client_signaling_thread_) {
    client_signaling_thread_->EndCall(call_id);
  }
}

void VoiceClient::AcceptCall(uint32 call_id) {
  LOGI("VoiceClient::AcceptCall");
  if (client_signaling_thread_) {
    client_signaling_thread_->AcceptCall(call_id);
  }
}

void VoiceClient::DeclineCall(uint32 call_id, bool busy) {
  LOGI("VoiceClient::DeclineCall");
  if (client_signaling_thread_) {
    client_signaling_thread_->DeclineCall(call_id, busy);
  }
}

#ifdef ANDROID
void VoiceClient::OnSignalCallStateChange(int state, const char *remote_jid, int call_id) {
  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_CALL_STATE_EVENT, state, remote_jid, call_id);
}

void VoiceClient::OnSignalAudioPlayout() {
  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_AUDIO_PLAYOUT_EVENT, 0, "", 0);
}

void VoiceClient::OnSignalCallError(int error, int call_id) {
  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_CALL_ERROR_EVENT, error, "", call_id);
}

void VoiceClient::OnSignalXmppError(int error) {
  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_XMPP_ERROR_EVENT, error, "", 0);
}

void VoiceClient::OnSignalXmppSocketClose(int state) {
  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_XMPP_SOCKET_CLOSE_EVENT, state, "", 0);
}

void VoiceClient::OnSignalXmppStateChange(int state) {
  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_XMPP_STATE_EVENT, state, "", 0);
}

void VoiceClient::OnSignalBuddyListReset() {
  LOGI("Resetting buddy list");
  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_BUDDY_LIST_EVENT, RESET, "", 0);
}

void VoiceClient::OnSignalBuddyListRemove(const char *remote_jid) {
  LOGI("Removing from buddy list: %s", remote_jid);
  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_BUDDY_LIST_EVENT, REMOVE, remote_jid, 0);
}

void VoiceClient::OnSignalBuddyListAdd(const char *remote_jid, const char *nick) {
  LOGI("Adding to buddy list: %s, %s", remote_jid, nick);
  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_BUDDY_LIST_EVENT, ADD, remote_jid, 0);
}
#elif IPHONE

void VoiceClient::OnSignalCallStateChange(int state, const char *remote_jid, int call_id) {
}

void VoiceClient::OnSignalAudioPlayout() {
}

void VoiceClient::OnSignalCallError(int error, int call_id) {
}

void VoiceClient::OnSignalXmppError(int error) {
}

void VoiceClient::OnSignalXmppSocketClose(int state) {
}

void VoiceClient::OnSignalXmppStateChange(int state) {
}

void VoiceClient::OnSignalBuddyListReset() {
}

void VoiceClient::OnSignalBuddyListRemove(const char *remote_jid) {
}

void VoiceClient::OnSignalBuddyListAdd(const char *remote_jid, const char *nick) {
}
#endif  //IPHONE
}  // namespace tuenti
