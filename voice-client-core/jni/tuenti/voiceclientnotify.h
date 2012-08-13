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
#ifndef _JNI_TUENTI_VOICECLIENTNOTIFY_H_
#define _JNI_TUENTI_VOICECLIENTNOTIFY_H_

#include <string>
#include "talk/p2p/base/session.h"
#include "talk/xmpp/xmppclient.h"

namespace tuenti {
class VoiceClientNotify {
 public:
  virtual ~VoiceClientNotify() {
  }

  /* Called when the connection state changes */
  virtual void OnXmppStateChange(buzz::XmppEngine::State) = 0;

  /* Called when the call state changes */
  virtual void OnCallStateChange(cricket::Session* session,
      cricket::Session::State state) = 0;

  /* Called when there is a xmpp error */
  virtual void OnXmppError(buzz::XmppEngine::Error) = 0;

  virtual void OnBuddyListAdd(std::string user_key, std::string nick) = 0;
  virtual void OnBuddyListRemove(std::string user_key) = 0;
  virtual void OnBuddyListReset() = 0;
};
}  // Namespace tuenti

#endif  // _JNI_TUENTI_VOICECLIENTNOTIFY_H_
