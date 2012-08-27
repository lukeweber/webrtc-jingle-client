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

#include "tuenti/voiceclient.h"
#include "tuenti/logging.h"
#include "tuenti/threadpriorityhandler.h"
#include "tuenti/clientsignalingthread.h"
#include "talk/base/thread.h"
#include "talk/base/logging.h"

// #include "talk/base/signalthread.h"

namespace tuenti {

enum {
  MSG_INIT, MSG_DESTROY,
};

const char* msgNames[] = { "MSG_INIT", "MSG_DESTROY", };

VoiceClient::VoiceClient(VoiceClientNotify *notify, StunConfig *stun_config)
    : notify_(notify),
    signal_thread_(NULL),
    client_signaling_thread_(NULL),
    stun_config_(stun_config) {
  LOGI("VoiceClient::VoiceClient");

  // a few standard logs not sure why they are not working
  talk_base::LogMessage::LogThreads();
  talk_base::LogMessage::LogTimestamps();

  // this creates all objects on the signaling thread
  if (signal_thread_ == NULL) {
    signal_thread_ = new talk_base::Thread();
    signal_thread_->Post(this, MSG_INIT);
    signal_thread_->Start();
  }
}

VoiceClient::~VoiceClient() {
  LOGI("VoiceClient::~VoiceClient");
  if (signal_thread_ != NULL) {
    signal_thread_->Quit();
    signal_thread_ = NULL;
  }
}

void VoiceClient::Destroy(int delay) {
  LOGI("VoiceClient::Destroy");
  if (signal_thread_ != NULL) {
    if (delay <= 0) {
      signal_thread_->Post(this, MSG_DESTROY);
    } else {
      signal_thread_->PostDelayed(delay, this, MSG_DESTROY);
    }
  }
}

void VoiceClient::InitializeS() {
  LOGI("VoiceClient::InitializeS");
  if (client_signaling_thread_ == NULL) {
    client_signaling_thread_ = new tuenti::ClientSignalingThread(notify_,
        signal_thread_, stun_config_);
    LOGI("VoiceClient::VoiceClient - new ClientSignalingThread "
            "client_signaling_thread_@(0x%x)",
            reinterpret_cast<int>(client_signaling_thread_));
    client_signaling_thread_->Start();
  }
}
void VoiceClient::DestroyS() {
  LOGI("VoiceClient::DestroyS");
  if (client_signaling_thread_ != NULL) {
    LOGI("VoiceClient::VoiceClient - destroy ClientSignalingThread "
            "client_signaling_thread_@(0x%x)",
            reinterpret_cast<int>(client_signaling_thread_));
    if (client_signaling_thread_->Destroy()) {
      // NFHACK Pretty ugly should probably call a alL
      // good to delete callback in voiceclient_main
      delete this;
    } else {
      Destroy(100);
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
  const std::string &password, const std::string &xmpp_host, int xmpp_port,
  bool use_ssl) {
  LOGI("VoiceClient::Login");
  if (client_signaling_thread_) {
    client_signaling_thread_->Login(username, password, xmpp_host, xmpp_port,
        use_ssl);
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

void VoiceClient::MuteCall(bool mute) {
  if (client_signaling_thread_) {
    client_signaling_thread_->MuteCall(mute);
  }
}

void VoiceClient::EndCall() {
  LOGI("VoiceClient::EndCall");
  if (client_signaling_thread_) {
    client_signaling_thread_->EndCall();
  }
}

void VoiceClient::AcceptCall() {
  LOGI("VoiceClient::AcceptCall");
  if (client_signaling_thread_) {
    client_signaling_thread_->AcceptCall();
  }
}

void VoiceClient::DeclineCall() {
  LOGI("VoiceClient::DeclineCall");
  if (client_signaling_thread_) {
    client_signaling_thread_->DeclineCall();
  }
}

}  // namespace tuenti
