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
#ifndef CLIENT_VOICECLIENT_H_
#define CLIENT_VOICECLIENT_H_

#include <string>

#include "talk/p2p/base/session.h"
#include "talk/media/base/mediachannel.h"
#include "talk/session/media/mediamessages.h"
#include "talk/session/media/mediasessionclient.h"
#include "talk/xmpp/xmppclient.h"
#include "talk/base/criticalsection.h"

#include "client/clientsignalingthread.h"
#include "client/status.h"
#include "client/xmppmessage.h"
#include "client/txmpppump.h"

#ifdef IOS_XMPP_FRAMEWORK
#include "VoiceClientExample/VoiceClientDelegate.h"
#endif

namespace tuenti {

typedef enum {
  ADD,
  REMOVE,
} BuddyList;


class ClientSignalingThread;

class VoiceClient: public sigslot::has_slots<> {
 public:
  // initialization
#if IOS_XMPP_FRAMEWORK
  explicit VoiceClient(VoiceClientDelegate* voiceClientDelegate);
#endif
  explicit VoiceClient();
  ~VoiceClient();
  void Init();
  void Destroy();

  // passthru functions
  void Login(const std::string &username, const std::string &password,
    StunConfig *stun_config, const std::string &xmpp_host,
    int xmpp_port, bool use_ssl, int port_allocator_filter, bool is_gtalk);
  void Disconnect();
  void Ping();
  void Call(std::string remote_jid);
  void SendMessage(const std::string &remote_jid, const int &state, const std::string &msg);
  void CallWithTracker(std::string remoteJid, std::string call_tracker_id);
  void MuteCall(uint32 call_id, bool mute);
  void HoldCall(uint32 call_id, bool hold);
  void EndCall(uint32 call_id);
  void AcceptCall(uint32 call_id);
  void DeclineCall(uint32 call_id, bool busy);
  void ReplaceTurn(const std::string &turn);

  ClientSignalingThread* SignalingThread();

#if IOS_XMPP_FRAMEWORK
  talk_base::Thread* GetSignalThread();
  VoiceClientDelegate* voiceClientDelegate_;
#endif
  std::string stunserver_;
  std::string relayserver_;
  tuenti::ClientSignalingThread *client_signaling_thread_;
  StunConfig *stun_config_;
  talk_base::CriticalSection destroy_cs_;
};

}  // namespace tuenti
#endif  // CLIENT_VOICECLIENT_H_
