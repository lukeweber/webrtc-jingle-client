#ifndef CLIENT_XMPPMESSAGE_H
#define CLIENT_XMPPMESSAGE_H

#include <string>
#include "talk/xmpp/jid.h"

namespace tuenti {

enum XmppMessageState {
   XMPP_CHAT_NONE,
   XMPP_CHAT_ACTIVE,
   XMPP_CHAT_COMPOSING,
   XMPP_CHAT_PAUSED,
   XMPP_CHAT_INACTIVE,
   XMPP_CHAT_GONE
};

struct XmppMessage {
    std::string body;
    buzz::Jid jid;
    tuenti::XmppMessageState state;
    XmppMessage(std::string jid_str, tuenti::XmppMessageState state_,
                std::string body_)
      : body(body_),
        jid(jid_str),
        state(state_) {}
    XmppMessage(){}
};

}  // namespace tuenti
#endif  // CLIENT_XMPPMESSAGE_H
