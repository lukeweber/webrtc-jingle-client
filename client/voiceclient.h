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

#ifdef ANDROID
#include <jni.h>
#include "client/helpers.h"
#endif

#include <string>

#include "talk/p2p/base/session.h"
#include "talk/media/base/mediachannel.h"
#include "talk/session/media/mediamessages.h"
#include "talk/session/media/mediasessionclient.h"
#include "talk/xmpp/xmppclient.h"
#include "talk/examples/login/xmpppump.h"
#include "talk/base/criticalsection.h"

#include "client/status.h"

namespace tuenti {

typedef enum {
  ADD,
  REMOVE,
  RESET
} BuddyList;

typedef struct {
  std::string stun;
  std::string turn;
  std::string turn_username;
  std::string turn_password;
  std::string ToString() {
    std::stringstream stream;
    stream << "[stun=(" << stun << "),";
    stream << "turn=(" << turn << "),";
    return stream.str();
  }
} StunConfig;

class ClientSignalingThread;

class VoiceClient: public sigslot::has_slots<>, talk_base::MessageHandler {
 public:
  // initialization
#ifdef ANDROID
  explicit VoiceClient(JavaObjectReference *reference);
#elif IPHONE
  explicit VoiceClient();
#endif
  ~VoiceClient();
  void Init();
  void Destroy();

  // passthru functions
  void Login(const std::string &username, const std::string &password,
    StunConfig *stun_config, const std::string &xmpp_host,
    int xmpp_port, bool use_ssl);
  void Disconnect();
  void Call(std::string remoteJid);
  void MuteCall(uint32 call_id, bool mute);
  void HoldCall(uint32 call_id, bool hold);
  void EndCall(uint32 call_id);
  void AcceptCall(uint32 call_id);
  void DeclineCall(uint32 call_id, bool busy);

 private:

  // signals
  void OnSignalCallStateChange(int state, const char *remote_jid, int call_id);
  void OnSignalCallError(int error, int call_id);
  void OnSignalAudioPlayout();

  void OnSignalXmppError(int error);
  void OnSignalXmppSocketClose(int state);
  void OnSignalXmppStateChange(int state);
  void OnSignalBuddyListReset();
  void OnSignalBuddyListRemove(const char *remote_jid);
  void OnSignalBuddyListAdd(const char *remote_jid, const char *nick);

  // signaling thread functions initialization
  void InitializeS();
  void DestroyS();

  // signaling thread functions other
  void OnMessage(talk_base::Message *msg);

  std::string stunserver_;
  std::string relayserver_;
#ifdef ANDROID
  JavaObjectReference *reference_;
#endif
  talk_base::Thread *signal_thread_;
  tuenti::ClientSignalingThread *client_signaling_thread_;
  StunConfig *stun_config_;
  talk_base::CriticalSection destroy_cs_;
};

}  // namespace tuenti
#endif  // CLIENT_VOICECLIENT_H_
