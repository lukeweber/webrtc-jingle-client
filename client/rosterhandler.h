#ifndef CLIENT_ROSTERHANDLER_H_
#define CLIENT_ROSTERHANDLER_H_

#include "talk/base/sigslot.h"
#include "talk/xmpp/rostermodule.h"
#include "talk/xmpp/jid.h"

#include "client/status.h"

namespace buzz {
class XmppEngine;
enum XmppReturnStatus;
}

namespace tuenti {
class RosterHandler: public buzz::XmppRosterHandler,
                     public sigslot::has_slots<> {
public:
  RosterHandler();

  //! A request for a subscription has come in.
  //! Typically, the UI will ask the user if it is okay to let the requester
  //! get presence notifications for the user.  The response is send back
  //! by calling ApproveSubscriber or CancelSubscriber.
  void SubscriptionRequest(buzz::XmppRosterModule* roster,
                                   const buzz::Jid& requesting_jid,
                                   buzz::XmppSubscriptionRequestType type,
                                   const buzz::XmlElement* raw_xml);

  //! Some type of presence error has occured
  void SubscriptionError(buzz::XmppRosterModule* roster, const buzz::Jid& from,
                         const buzz::XmlElement* raw_xml);

  void RosterError(buzz::XmppRosterModule* roster, const buzz::XmlElement* raw_xml);

  //! New presence information has come in
  //! The user is notified with the presence object directly.  This info is also
  //! added to the store accessable from the engine.
  void IncomingPresenceChanged(buzz::XmppRosterModule* roster,
                               const buzz::XmppPresence* presence);

  //! A contact has changed
  //! This indicates that the data for a contact may have changed.  No
  //! contacts have been added or removed.
  void ContactChanged(buzz::XmppRosterModule* roster,
                      const buzz::XmppRosterContact* old_contact,
                      size_t index);

  //! A set of contacts have been added
  //! These contacts may have been added in response to the original roster
  //! request or due to a "roster push" from the server.
  void ContactsAdded(buzz::XmppRosterModule* roster,
                     size_t index, size_t number);

  //! A contact has been removed
  //! This contact has been removed form the list.
  void ContactRemoved(buzz::XmppRosterModule* roster,
                      const buzz::XmppRosterContact* removed_contact,
                      size_t index);

  sigslot::signal3<const std::string&, const std::string&, int> SignalContactAdded;

private:
  void FillStatus(const buzz::Jid& from, const buzz::XmlElement* stanza,
    buzz::Status* s);

};

}  // namespace tuenti
#endif //CLIENT_ROSTERHANDLER_H_
