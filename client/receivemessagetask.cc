#include "assert.h"
#include "client/receivemessagetask.h"
#include "talk/base/scoped_ptr.h"
#include "talk/xmpp/constants.h"
#include "talk/xmpp/jid.h"
#include "talk/xmllite/qname.h"

namespace tuenti {

ReceiveMessageTask::ReceiveMessageTask(buzz::XmppTaskParentInterface* parent, buzz::XmppEngine::HandlerLevel level)
    : buzz::XmppTask(parent, level) {
    //
}

// Runs forever.
bool ReceiveMessageTask::HandleStanza(const buzz::XmlElement* stanza) {
  if (stanza->Name() != buzz::QN_MESSAGE && !stanza->HasAttr(buzz::QN_FROM)){
    return false;
  }

  if (stanza->Attr(buzz::QN_TYPE) != "chat"
          // TODO: We may want to handle group chat messages here.
          // Have to see if it's worth it.
          /*&& stanza->Attr(buzz::QN_TYPE) != "groupchat"*/){
    return false;
  }

  std::string body = "";
  if (stanza->FirstNamed(buzz::QN_BODY) != NULL) {
    body = stanza->FirstNamed(buzz::QN_BODY)->BodyText();
  }

  tuenti::XmppMessageState state = XMPP_CHAT_NONE;
  if (stanza->FirstWithNamespace(buzz::NS_CHATSTATE) != NULL){
    const buzz::QName& qname = stanza->FirstWithNamespace(buzz::NS_CHATSTATE)->Name();
    if (qname == buzz::QN_CS_ACTIVE){
      state = XMPP_CHAT_ACTIVE;
    } else if (qname == buzz::QN_CS_COMPOSING){
      state = XMPP_CHAT_COMPOSING;
    } else if (qname == buzz::QN_CS_PAUSED){
      state = XMPP_CHAT_PAUSED;
    } else if (qname == buzz::QN_CS_INACTIVE){
      state = XMPP_CHAT_INACTIVE;
    } else if (qname == buzz::QN_CS_GONE){
      state = XMPP_CHAT_GONE;
    }
  }

  const XmppMessage msg = XmppMessage(stanza->Attr(buzz::QN_FROM), state, body);
  SignalIncomingXmppMessage(msg);
  return true;
}

int ReceiveMessageTask::ProcessStart() {
  return STATE_BLOCKED;
}

} // namespace tuenti
