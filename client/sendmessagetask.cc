
#include "assert.h"
#include "client/sendmessagetask.h"
#include "talk/base/scoped_ptr.h"
#include "talk/xmpp/constants.h"
#include "talk/xmpp/jid.h"

namespace tuenti {

SendMessageTask::SendMessageTask(buzz::XmppTaskParentInterface* parent)
    : buzz::XmppTask(parent, buzz::XmppEngine::HL_SINGLE){
    //
}

void SendMessageTask::Send(const tuenti::XmppMessage &msg) {
    pending_xmpp_msg_ = msg;
}

bool SendMessageTask::HandleStanza(const buzz::XmlElement* stanza) {
  //Should never hit this function, because sending is fire and forget.
  assert(false);
  return false;
}

//Sends the pending message and exits
int SendMessageTask::ProcessStart() {
  buzz::XmlElement* stanza = new buzz::XmlElement(buzz::QN_MESSAGE);
  stanza->AddAttr(buzz::QN_TO, pending_xmpp_msg_.jid.BareJid().Str());
  stanza->AddAttr(buzz::QN_ID, task_id());
  stanza->AddAttr(buzz::QN_TYPE, "chat");
  buzz::XmlElement* body = new buzz::XmlElement(buzz::QN_BODY);
  body->SetBodyText(pending_xmpp_msg_.body);
  stanza->AddElement(body);

  SendStanza(stanza);

  //TODO: If message receipts worked.
  //buzz::XmlElement* requestAck = new buzz::XmlElement(buzz::QN_REQUEST, true);
  //stanza->AddElement(requestAck);
  //set_timeout_seconds(5);

  return STATE_DONE;
}

} // namespace tuenti
