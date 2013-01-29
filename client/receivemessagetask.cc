
#include "assert.h"
#include "client/receivemessagetask.h"
#include "talk/base/scoped_ptr.h"
#include "talk/xmpp/constants.h"
#include "talk/xmpp/jid.h"

namespace tuenti {

ReceiveMessageTask::ReceiveMessageTask(buzz::XmppTaskParentInterface* parent, buzz::XmppEngine::HandlerLevel level)
    : buzz::XmppTask(parent, level){
    //
}

// Runs forever.
bool ReceiveMessageTask::HandleStanza(const buzz::XmlElement* stanza) {
  if (stanza->Name() !=  buzz::QN_MESSAGE){
    return false;
  }

  if (!stanza->HasAttr(buzz::QN_FROM)){
    return false;
  }

  const XmppMessage msg = XmppMessage(stanza->Attr(buzz::QN_FROM), stanza->FirstNamed(buzz::QN_BODY)->BodyText());
  SignalIncomingXmppMessage(msg);
  return true;
}

int ReceiveMessageTask::ProcessStart() {
  return STATE_BLOCKED;
}

} // namespace tuenti
