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

#include "client/xmpplog.h"
#include "client/txmpppump.h"
#include "client/txmppauth.h"
#include "client/txmppsocket.h"
#include "talk/base/logging.h"

namespace tuenti {
TXmppPump::TXmppPump(TXmppPumpNotify * notify){
  LOGI("TXmppPump::TXmppPump");
  state_ = buzz::XmppEngine::STATE_NONE;
  notify_ = notify;
  client_ = NULL;
  socket_ = NULL;
  auth_ = NULL;
  xmpp_log_ = NULL;
}

TXmppPump::~TXmppPump() {
  LOGI("TXmppPump::~TXmppPump this@(0x%x)",
          reinterpret_cast<int>(this));
}

void TXmppPump::DoLogin(const buzz::XmppClientSettings & xcs) {
  LOGI("TXmppPump::DoLogin");
  xcs_ = xcs;

  if (socket_ == NULL) {
    socket_ = new TXmppSocket(xcs_.use_tls());  // NOTE: deleted by TaskRunner
  }
  if (auth_ == NULL) {
    auth_ = new TXmppAuth();  // NOTE: deleted by TaskRunner
  }
  if (client_ == NULL) {
    client_ = new buzz::XmppClient(this);  // NOTE: deleted by TaskRunner
  }

#if LOGGING
  xmpp_log_ = new XmppLog();
  if (client_ ) {
    client_->SignalLogInput.connect(xmpp_log_, &XmppLog::Input);
    client_->SignalLogOutput.connect(xmpp_log_, &XmppLog::Output);
  }
#endif  // LOGGING

  if (client_ && !AllChildrenDone()) {
    OnStateChange(buzz::XmppEngine::STATE_START);
    LOGI("TXmppPump::DoLogin - logging on");
    client_->SignalStateChange.connect(this, &TXmppPump::OnStateChange);
    socket_->SignalCloseEvent.connect(this, &TXmppPump::OnXmppSocketClose);
    client_->Connect(xcs, "", socket_, auth_);
    client_->Start();
  }
}

void TXmppPump::DoDisconnect() {
  LOGI("TXmppPump::DoDisconnect");
  // Maybe we don't need both conditions
  if (client_ && !AllChildrenDone()) {
    client_->Disconnect();
#if LOGGING
    delete xmpp_log_;
    xmpp_log_ = NULL;
#endif
  }
  OnStateChange(buzz::XmppEngine::STATE_CLOSED);
}

void TXmppPump::OnStateChange(buzz::XmppEngine::State state) {
  if (state_ == state)
    return;
  state_ = state;
  if (notify_ != NULL) {
    if ( state_ == buzz::XmppEngine::STATE_CLOSED) {
      notify_->OnXmppError(client_->GetError(NULL));
    }
    notify_->OnStateChange(state);
  }
}

void TXmppPump::OnXmppSocketClose(int state) {
  if (notify_ != NULL) {
    notify_->OnXmppSocketClose(state);
  }
}

void TXmppPump::WakeTasks() {
  talk_base::Thread::Current()->Post(this);
}

int64 TXmppPump::CurrentTime() {
  return static_cast<int64>(talk_base::Time());
}

void TXmppPump::OnMessage(talk_base::Message *pmsg) {
  RunTasks();
}

buzz::XmppReturnStatus TXmppPump::SendStanza(const buzz::XmlElement *stanza) {
  if (client_ && !AllChildrenDone())
    return client_->SendStanza(stanza);
  return buzz::XMPP_RETURN_BADSTATE;
}
}  // namespace tuenti
