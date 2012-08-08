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

#include <string>
#include "tuenti/presencepushtask.h"

#include "talk/base/stringencode.h"
#include "talk/xmpp/constants.h"

namespace buzz {

// string helper functions -----------------------------------------------------

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

bool PresencePushTask::HandleStanza(const XmlElement * stanza) {
  if (stanza->Name() != QN_PRESENCE)
    return false;
  QueueStanza(stanza);
  return true;
}

static bool IsUtf8FirstByte(int c) {
  return (((c) & 0x80) == 0) ||  // is single byte
      ((unsigned char) ((c) - 0xc0) < 0x3e);  // or is lead byte
}

int PresencePushTask::ProcessStart() {
  const XmlElement * stanza = NextStanza();
  if (stanza == NULL)
    return STATE_BLOCKED;

  Jid from(stanza->Attr(QN_FROM));
  HandlePresence(from, stanza);

  return STATE_START;
}

void PresencePushTask::HandlePresence(const Jid& from,
    const XmlElement* stanza) {
  if (stanza->Attr(QN_TYPE) == STR_ERROR)
    return;

  Status s;
  FillStatus(from, stanza, &s);
  SignalStatusUpdate(s);
}

void PresencePushTask::FillStatus(const Jid& from, const XmlElement* stanza,
    Status* s) {
  s->set_jid(from);
  if (stanza->Attr(QN_TYPE) == STR_UNAVAILABLE) {
    s->set_available(false);
  } else {
    s->set_available(true);
    const XmlElement * status = stanza->FirstNamed(QN_STATUS);
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

    const XmlElement * priority = stanza->FirstNamed(QN_PRIORITY);
    if (priority != NULL) {
      int pri = 0;
      if (talk_base::FromString(priority->BodyText(), &pri)) {
        s->set_priority(pri);
      }
    }

    const XmlElement * show = stanza->FirstNamed(QN_SHOW);
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
    const XmlElement * td = stanza->FirstNamed(QN_TUENTI_DATA);
    if (td != NULL) {
      const XmlElement * caps = td->FirstNamed(QN_TUENTI_CAPS);
      const XmlElement * voice_v1 = caps->FirstNamed(QN_TUENTI_VOICE);
      if (voice_v1 != NULL) {
        s->set_voice_capability(true);
        s->set_know_capabilities(true);
        s->set_caps_node("http://www.google.com/xmpp/client/caps");
        s->set_version("1.0");
      }
    }
#else
    const XmlElement * caps = stanza->FirstNamed(QN_CAPS_C);
    if (caps != NULL) {
      std::string node = caps->Attr(QN_NODE);
      std::string ver = caps->Attr(QN_VER);
      std::string exts = caps->Attr(QN_EXT);

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

    const XmlElement* delay = stanza->FirstNamed(kQnDelayX);
    if (delay != NULL) {
      // Ideally we would parse this according to the Psuedo ISO-8601 rules
      // that are laid out in JEP-0082:
      // http://www.jabber.org/jeps/jep-0082.html
      std::string stamp = delay->Attr(kQnStamp);
      s->set_sent_time(stamp);
    }

    const XmlElement* nick = stanza->FirstNamed(QN_NICKNAME);
    if (nick) {
      s->set_nick(nick->BodyText());
    }
  }
}
}
