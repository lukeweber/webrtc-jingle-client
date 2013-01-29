// Copyright 2011 Google Inc. All Rights Reserved.

#include "client/keepalivetask.h"

#include "talk/base/logging.h"
#include "talk/base/scoped_ptr.h"
#include "talk/xmpp/constants.h"
#include "talk/xmpp/xmppclient.h"

namespace tuenti {

KeepAliveTask::KeepAliveTask(buzz::XmppClient* client,
                   talk_base::MessageQueue* message_queue,
                   uint32 keepalive_period_millis)
    : buzz::XmppTask(client, buzz::XmppEngine::HL_NONE),
      message_queue_(message_queue),
      keepalive_period_millis_(keepalive_period_millis),
      next_keepalive_time_(0),
      client_(client) {
}

bool KeepAliveTask::HandleStanza(const buzz::XmlElement* stanza) {
  //Shouldn't hit this.
  ASSERT(false);
  return false;
}

// This task runs indefinitely and remains in either the start or blocked
// states.
int KeepAliveTask::ProcessStart() {
  LOG(LS_INFO) << "Processing keepalive";

  uint32 now = talk_base::Time();

  // Send a keepalive if it's time.
  if (now >= next_keepalive_time_) {
    client_->SendRaw(" ");

    next_keepalive_time_ = now + keepalive_period_millis_;

    // Wake ourselves up when it's time to send another ping or when the ping
    // times out (so we can fire a signal).
    message_queue_->PostDelayed(keepalive_period_millis_, this);
  }

  return STATE_BLOCKED;
}

void KeepAliveTask::OnMessage(talk_base::Message* msg) {
  // Get the task manager to run this task so we can send a ping or signal or
  // process a ping response.
  Wake();
}

} // namespace tuenti
