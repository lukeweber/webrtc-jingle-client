#ifndef CLIENT_XMPPMESSAGE_H
#define CLIENT_XMPPMESSAGE_H

#include <string>
#include "talk/xmpp/jid.h"

namespace tuenti {

struct XmppMessage {
    std::string body;
    buzz::Jid jid;
    XmppMessage(std::string jid_str, std::string body)
      : body(body),
        jid(jid_str)
    {
    }
    XmppMessage(){};
};

}  // namespace tuenti
#endif  // CLIENT_XMPPMESSAGE_H
