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
#elif IOS
#include "VoiceClientExample/VoiceClientDelegate.h"
#endif

#include "client/voiceclient.h"
#include "client/logging.h"
#include "client/xmppmessage.h"
#include "client/threadpriorityhandler.h"
#include "client/clientsignalingthread.h"
#include "talk/base/thread.h"
#include "talk/base/logging.h"

namespace tuenti {

#ifdef ANDROID
VoiceClient::VoiceClient(JavaObjectReference *reference) {
    reference_ = reference;
    Init();
}
#elif IOS
VoiceClient::VoiceClient() {
    Init();
}
#endif

VoiceClient::~VoiceClient() {
  LOGI("VoiceClient::~VoiceClient");
  delete client_signaling_thread_;
}

void VoiceClient::Init() {
  LOGI("VoiceClient::VoiceClient - new ClientSignalingThread "
          "client_signaling_thread_@(0x%x)",
          reinterpret_cast<int>(client_signaling_thread_));

  client_signaling_thread_  = new tuenti::ClientSignalingThread();
  client_signaling_thread_->SignalCallStateChange.connect(
      this, &VoiceClient::OnSignalCallStateChange);
  client_signaling_thread_->SignalCallError.connect(
      this, &VoiceClient::OnSignalCallError);
  client_signaling_thread_->SignalAudioPlayout.connect(
      this, &VoiceClient::OnSignalAudioPlayout);
  client_signaling_thread_->SignalCallTrackerId.connect(
      this, &VoiceClient::OnSignalCallTrackerId);

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
  client_signaling_thread_->SignalXmppMessage.connect(
      this, &VoiceClient::OnSignalXmppMessage);
  #ifdef LOGGING
  client_signaling_thread_->SignalStatsUpdate.connect(
      this, &VoiceClient::OnSignalStatsUpdate);
  #endif
}

void VoiceClient::Login(const std::string &username,
  const std::string &password, StunConfig* stun_config,
  const std::string &xmpp_host, int xmpp_port, bool use_ssl, int port_allocator_filter) {
  LOGI("VoiceClient::Login");
  LOG(INFO) << "LOGT " << stun_config->ToString();
  if (client_signaling_thread_) {
    client_signaling_thread_->Login(username, password, stun_config,
        xmpp_host, xmpp_port, use_ssl, port_allocator_filter);
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

void VoiceClient::OnSignalBuddyListRemove(const RosterItem item) {
  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_BUDDY_LIST_EVENT, REMOVE, item.jid.BareJid().Str().c_str(), 0);
}

void VoiceClient::OnSignalBuddyListAdd(const RosterItem item) {
  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_BUDDY_LIST_EVENT, ADD, item.jid.BareJid().Str().c_str(), 0);
}

void VoiceClient::OnSignalStatsUpdate(const char *stats) {
  LOGI("Updating stats=%s", stats);
  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_STATS_UPDATE_EVENT, 0, stats, 0);
}

void VoiceClient::OnSignalCallTrackerId(int call_id, const char* call_tracker_id) {
  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_CALL_TRACKER_ID_EVENT, 0, call_tracker_id, call_id);
}

void VoiceClient::OnSignalXmppMessage(const XmppMessage m){
  //Implement me.
}
#elif IOS

void VoiceClient::OnSignalCallStateChange(int state, const char *remote_jid, int call_id) {
    VoiceClientDelegate::getInstance()->OnSignalCallStateChange(state, remote_jid, call_id);
}

void VoiceClient::OnSignalAudioPlayout() {
    VoiceClientDelegate::getInstance()->OnSignalAudioPlayout();
}

void VoiceClient::OnSignalCallError(int error, int call_id) {
    VoiceClientDelegate::getInstance()->OnSignalCallError(error, call_id);
}

void VoiceClient::OnSignalXmppError(int error) {
    VoiceClientDelegate::getInstance()->OnSignalXmppError(error);
}

void VoiceClient::OnSignalXmppSocketClose(int state) {
    VoiceClientDelegate::getInstance()->OnSignalXmppSocketClose(state);
}

void VoiceClient::OnSignalXmppStateChange(int state) {
    VoiceClientDelegate::getInstance()->OnSignalXmppStateChange(state);
}

void VoiceClient::OnSignalBuddyListReset() {
    VoiceClientDelegate::getInstance()->OnSignalBuddyListReset();
}

void VoiceClient::OnSignalBuddyListRemove(const RosterItem item) {
    VoiceClientDelegate::getInstance()->OnSignalBuddyListRemove(item.jid.BareJid().Str().c_str());
}

void VoiceClient::OnSignalBuddyListAdd(const RosterItem item) {
    VoiceClientDelegate::getInstance()->OnSignalBuddyListAdd(item.jid.BareJid().Str().c_str(), item.nick.c_str());
}

void VoiceClient::OnSignalStatsUpdate(const char *stats) {
    VoiceClientDelegate::getInstance()->OnSignalStatsUpdate(stats);
}

void VoiceClient::OnSignalCallTrackerId(int call_id, const char *call_tracker_id) {
    VoiceClientDelegate::getInstance()->OnSignalCallTrackingId(call_id, call_tracker_id);
}

void VoiceClient::OnSignalXmppMessage(const XmppMessage m){
  printf("Message from: %s\n", m.jid.BareJid().Str().c_str());
  printf("Message body: %s\n", m.body.c_str());
}
#endif  //IOS

}  // namespace tuenti
