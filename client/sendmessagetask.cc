
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

  buzz::XmlElement* state = NULL;
  switch (pending_xmpp_msg_.state) {
    case XMPP_CHAT_NONE:
      //NO OP
      break;
    case XMPP_CHAT_ACTIVE:
      state = new buzz::XmlElement(buzz::QN_CS_ACTIVE);
      break;
    case XMPP_CHAT_COMPOSING:
      state = new buzz::XmlElement(buzz::QN_CS_COMPOSING);
      break;
    case XMPP_CHAT_PAUSED:
      state = new buzz::XmlElement(buzz::QN_CS_PAUSED);
      break;
    case XMPP_CHAT_INACTIVE:
      state = new buzz::XmlElement(buzz::QN_CS_INACTIVE);
      break;
    case XMPP_CHAT_GONE:
      state = new buzz::XmlElement(buzz::QN_CS_GONE);
      break;
  }
  stanza->AddAttr(buzz::QN_ID, task_id());
  stanza->AddAttr(buzz::QN_TYPE, "chat");

  if (pending_xmpp_msg_.body != ""){
    buzz::XmlElement* body = new buzz::XmlElement(buzz::QN_BODY);
    body->SetBodyText(pending_xmpp_msg_.body);
    stanza->AddElement(body);
  }
  if(state){
      stanza->AddElement(state);
  }

  SendStanza(stanza);

  //TODO: If message receipts worked.
  //buzz::XmlElement* requestAck = new buzz::XmlElement(buzz::QN_REQUEST, true);
  //stanza->AddElement(requestAck);
  //set_timeout_seconds(5);

  return STATE_DONE;
}

} // namespace tuenti
