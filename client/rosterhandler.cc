#include "client/rosterhandler.h"
#include "talk/base/logging.h"
#include "talk/xmpp/constants.h"
#include "talk/xmpp/rostermodule.h"
#include "talk/xmpp/jid.h"

using buzz::Jid;
using buzz::Status;
using buzz::XmppRosterModule;
using buzz::XmlElement;
using buzz::XmppSubscriptionRequestType;
using buzz::XmppRosterModule;
using buzz::XmppPresence;
using buzz::XmppRosterContact;

namespace tuenti {

static bool IsUtf8FirstByte(int c) {
  return (((c) & 0x80) == 0) ||  // is single byte
         ((unsigned char) ((c) - 0xc0) < 0x3e);  // or is lead byte
}

static bool IsXmlSpace(int ch) {
  return ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t';
}

static bool ListContainsToken(const std::string & list,
    const std::string & token) {
  size_t i = list.find(token);
  if (i == std::string::npos || token.empty())
    return false;
  bool boundary_before = (i == 0 || IsXmlSpace(list[i - 1]));
  bool boundary_after = (i == list.length() - token.length()
      || IsXmlSpace(list[i + token.length()]));
  return boundary_before && boundary_after;
}

RosterHandler::RosterHandler(){
  LOG(LS_INFO) << "RosterHandler::RosterHandler()";
}
  //! A request for a subscription has come in.
  //! Typically, the UI will ask the user if it is okay to let the requester
  //! get presence notifications for the user.  The response is send back
  //! by calling ApproveSubscriber or CancelSubscriber.
void RosterHandler::SubscriptionRequest(XmppRosterModule* roster,
                                   const Jid& requesting_jid,
                                   XmppSubscriptionRequestType type,
                                   const XmlElement* raw_xml){
  LOG(LS_INFO) << "RosterHandler::SubscriptionRequest()";
}

  //! Some type of presence error has occured
void RosterHandler::SubscriptionError(XmppRosterModule* roster, const Jid& from,
                                      const XmlElement* raw_xml){
  LOG(LS_INFO) << "RosterHandler::SubscriptionError()";
}

void RosterHandler::RosterError(XmppRosterModule* roster,
                                const XmlElement* raw_xml){
  LOG(LS_INFO) << "RosterHandler::RosterError()";
}

  //! New presence information has come in
  //! The user is notified with the presence object directly.  This info is also
  //! added to the store accessable from the engine.
void RosterHandler::IncomingPresenceChanged(XmppRosterModule* roster,
                               const XmppPresence* presence){
  LOG(LS_INFO) << "RosterHandler::IncomingPresenceChanged()";
  const XmlElement* elem = presence->raw_xml();
  Status s;
  FillStatus(presence->jid(), elem, &s);
  //SignalStatusUpdate(s);
}

  //! A contact has changed
  //! This indicates that the data for a contact may have changed.  No
  //! contacts have been added or removed.
void RosterHandler::ContactChanged(XmppRosterModule* roster,
                      const XmppRosterContact* old_contact,
                      size_t index){
  LOG(LS_INFO) << "RosterHandler::ContactChanged()";
}

  //! A set of contacts have been added
  //! These contacts may have been added in response to the original roster
  //! request or due to a "roster push" from the server.
void RosterHandler::ContactsAdded(XmppRosterModule* roster,
                     size_t index, size_t number){
  LOG(LS_INFO) << "RosterHandler::ContactsAdded()";
  for (size_t i = 0; i < number; ++i) {
	  const XmppRosterContact *contact = roster->GetRosterContact(index+i);
	  //int available = roster->GetIncomingPresence(index+i)->available();
	  SignalContactAdded(contact->jid().Str(), contact->name(), 0);
  }
}

  //! A contact has been removed
  //! This contact has been removed form the list.
void RosterHandler::ContactRemoved(XmppRosterModule* roster,
                      const XmppRosterContact* removed_contact,
                      size_t index){
  LOG(LS_INFO) << "RosterHandler::ContactRemoved()";
}

void RosterHandler::FillStatus(const Jid& from, const XmlElement* stanza,
    Status* s) {
  s->set_jid(from);
  if (stanza->Attr(buzz::QN_TYPE) == buzz::STR_UNAVAILABLE) {
    s->set_available(false);
  } else {
    s->set_available(true);
    const XmlElement * status = stanza->FirstNamed(buzz::QN_STATUS);
    if (status != NULL) {
      s->set_status(status->BodyText());

      // Truncate status messages longer than 300 bytes
      if (s->status().length() > 300) {
        size_t len = 300;

        // Be careful not to split legal utf-8 chars in half
        while (!IsUtf8FirstByte(s->status()[len]) && len > 0) {
          len -= 1;
        }
        std::string truncated(s->status(), 0, len);
        s->set_status(truncated);
      }
    }

    //TODO:: fix priority and by importing FromString from the correct place
    //const XmlElement * priority = stanza->FirstNamed(buzz::QN_PRIORITY);
    //if (priority != NULL) {
    //  int pri = 0;
    //  if (talk_base::FromString(priority->BodyText(), &pri)) {
    //    s->set_priority(pri);
    //  }
    //}
    //
    //

    const XmlElement * show = stanza->FirstNamed(buzz::QN_SHOW);
    if (show == NULL || show->FirstChild() == NULL) {
      s->set_show(Status::SHOW_ONLINE);
    } else {
      if (show->BodyText() == "away") {
        s->set_show(Status::SHOW_AWAY);
      } else if (show->BodyText() == "xa") {
        s->set_show(Status::SHOW_XA);
      } else if (show->BodyText() == "dnd") {
        s->set_show(Status::SHOW_DND);
      } else if (show->BodyText() == "chat") {
        s->set_show(Status::SHOW_CHAT);
      } else {
        s->set_show(Status::SHOW_ONLINE);
      }
    }

#ifdef TUENTI_CUSTOM_BUILD
    const XmlElement * td = stanza->FirstNamed(buzz::QN_TUENTI_DATA);
    if (td != NULL) {
      const XmlElement * caps = td->FirstNamed(buzz::QN_TUENTI_CAPS);
      const XmlElement * voice_v1 = caps->FirstNamed(buzz::QN_TUENTI_VOICE);
      if (voice_v1 != NULL) {
        s->set_voice_capability(true);
        s->set_know_capabilities(true);
        s->set_caps_node("http://www.google.com/xmpp/client/caps");
        s->set_version("1.0");
      }
    }
#else
    const XmlElement * caps = stanza->FirstNamed(buzz::QN_CAPS_C);
    if (caps != NULL) {
      std::string node = caps->Attr(buzz::QN_NODE);
      std::string ver = caps->Attr(buzz::QN_VER);
      std::string exts = caps->Attr(buzz::QN_EXT);

      s->set_know_capabilities(true);
      s->set_caps_node(node);
      s->set_version(ver);

      if (ListContainsToken(exts, "voice-v1")) {
        s->set_voice_capability(true);
      }
      if (ListContainsToken(exts, "video-v1")) {
        s->set_video_capability(true);
      }
    }
#endif

    const XmlElement* delay = stanza->FirstNamed(buzz::kQnDelayX);
    if (delay != NULL) {
      // Ideally we would parse this according to the Psuedo ISO-8601 rules
      // that are laid out in JEP-0082:
      // http://www.jabber.org/jeps/jep-0082.html
      std::string stamp = delay->Attr(buzz::kQnStamp);
      s->set_sent_time(stamp);
    }

    const XmlElement* nick = stanza->FirstNamed(buzz::QN_NICKNAME);
    if (nick) {
      s->set_nick(nick->BodyText());
    }
  }
}
}  // namespace tuenti
