/*
 * libjingle
 * Copyright 2004--2005, Google Inc.
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

#include "tuenti/logging.h"
#include "tuenti/txmpppump.h"
#include "tuenti/txmppauth.h"

namespace tuenti {
TXmppPump::TXmppPump(TXmppPumpNotify * notify) {
  LOGI("TXmppPump::TXmppPump");
  state_ = buzz::XmppEngine::STATE_NONE;
  notify_ = notify;
  //NFHACK where does this get deleted?
  client_ = new buzz::XmppClient(this);  // NOTE: deleted by TaskRunner
  LOGI("TXmppPump::TXmppPump - new XmppClient client_@(0x%x)", reinterpret_cast<int>(client_));
}

void TXmppPump::DoLogin(const buzz::XmppClientSettings & xcs,
                       buzz::AsyncSocket* socket,
                       buzz::PreXmppAuth* auth) {
  LOGI("TXmppPump::DoLogin");
  OnStateChange(buzz::XmppEngine::STATE_START);
  if (!AllChildrenDone()){
    client_->SignalStateChange.connect(this, &TXmppPump::OnStateChange);
    client_->Connect(xcs, "", socket, auth);
    client_->Start();
  }
}

void TXmppPump::DoDisconnect() {
  if (!AllChildrenDone()){
    client_->Disconnect();
  }
  OnStateChange(buzz::XmppEngine::STATE_CLOSED);
}

void TXmppPump::OnStateChange(buzz::XmppEngine::State state) {
  if (state_ == state)
    return;
  state_ = state;
  if (notify_ != NULL)
    notify_->OnStateChange(state);
}

void TXmppPump::WakeTasks() {
  talk_base::Thread::Current()->Post(this);
}

int64 TXmppPump::CurrentTime() {
  return (int64)talk_base::Time();
}

void TXmppPump::OnMessage(talk_base::Message *pmsg) {
  RunTasks();
}

buzz::XmppReturnStatus TXmppPump::SendStanza(const buzz::XmlElement *stanza) {
  if (!AllChildrenDone())
    return client_->SendStanza(stanza);
  return buzz::XMPP_RETURN_BADSTATE;
}

}// namespace tuenti
