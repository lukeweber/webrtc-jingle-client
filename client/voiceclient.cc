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

#include "client/voiceclient.h"
#include "client/logging.h"
#include "client/xmppmessage.h"

#ifdef ANDROID
#include "client/threadpriorityhandler.h"
#endif

#include "talk/base/thread.h"
#include "talk/base/logging.h"

namespace tuenti {

VoiceClient::VoiceClient() {
    Init();
}

#ifdef IOS_XMPP_FRAMEWORK
VoiceClient::VoiceClient(VoiceClientDelegate* voiceClientDelegate)
    :voiceClientDelegate_(voiceClientDelegate)
{
    Init();
}
#endif

VoiceClient::~VoiceClient() {
  LOGI("VoiceClient::~VoiceClient");
  delete client_signaling_thread_;
  client_signaling_thread_ = NULL;
}

void VoiceClient::Init() {
  LOGI("VoiceClient::VoiceClient - new ClientSignalingThread "
          "client_signaling_thread_@(0x%x)",
          reinterpret_cast<int>(client_signaling_thread_));

#ifdef IOS_XMPP_FRAMEWORK
  client_signaling_thread_  = new tuenti::ClientSignalingThread(voiceClientDelegate_);
#else
  client_signaling_thread_  = new tuenti::ClientSignalingThread();
#endif
}

void VoiceClient::Login(const std::string &username,
  const std::string &password, StunConfig* stun_config,
  const std::string &xmpp_host, int xmpp_port, bool use_ssl,
  int port_allocator_filter, bool is_gtalk) {
  LOGI("VoiceClient::Login");
  LOG(INFO) << "LOGT " << stun_config->ToString();
  if (client_signaling_thread_) {
    client_signaling_thread_->Login(username, password, stun_config,
        xmpp_host, xmpp_port, use_ssl, port_allocator_filter, is_gtalk);
  }
}

void VoiceClient::Ping() {
  if (client_signaling_thread_) {
    client_signaling_thread_->Ping();
  }
}

void VoiceClient::ReplaceTurn(const std::string &turn) {
  LOGI("VoiceClient::ReplaceTurn");
  LOG(INFO) << "NewTurn " << turn;
  if (client_signaling_thread_) {
    client_signaling_thread_->ReplaceTurn(turn);
  }
}

void VoiceClient::SendMessage(const std::string &remote_jid, const int &state,
                              const std::string &msg){
  if (client_signaling_thread_){
    XmppMessage xmpp_to_send(remote_jid, static_cast<XmppMessageState>(state), msg);
    client_signaling_thread_->SendXmppMessage(xmpp_to_send);
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
    client_signaling_thread_->Call(remoteJid, "");
  }
}

void VoiceClient::CallWithTracker(std::string remoteJid, std::string call_tracker_id){
  LOGI("VoiceClient::Call");
  if (client_signaling_thread_) {
    client_signaling_thread_->Call(remoteJid, call_tracker_id);
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

ClientSignalingThread* VoiceClient::SignalingThread() {
	return client_signaling_thread_;
}

#if IOS_XMPP_FRAMEWORK
talk_base::Thread* VoiceClient::GetSignalThread() {
    return client_signaling_thread_->GetSignalThread();
}
#endif

}  // namespace tuenti
