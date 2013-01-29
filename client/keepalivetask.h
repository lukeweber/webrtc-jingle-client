/*
 * libjingle
 * Copyright 2011, Google Inc.
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

#ifndef CLIENT_KEEPALIVE_H_
#define CLIENT_KEEPALIVE_H_

#include "talk/base/messagehandler.h"
#include "talk/base/messagequeue.h"
#include "talk/xmpp/xmppclient.h"
#include "talk/xmpp/xmpptask.h"

namespace tuenti {

class KeepAliveTask : public buzz::XmppTask, private talk_base::MessageHandler {
 public:
  KeepAliveTask(buzz::XmppClient* parent,
      talk_base::MessageQueue* message_queue, uint32 keepalive_period_millis);

  virtual bool HandleStanza(const buzz::XmlElement* stanza);
  virtual int ProcessStart();

 private:
  // Implementation of MessageHandler.
  virtual void OnMessage(talk_base::Message* msg);

  talk_base::MessageQueue* message_queue_;
  uint32 keepalive_period_millis_;
  uint32 next_keepalive_time_;
  buzz::XmppClient* client_;
};

} // namespace tuenti

#endif  // CLIENT_KEEPALIVETASK_H_
