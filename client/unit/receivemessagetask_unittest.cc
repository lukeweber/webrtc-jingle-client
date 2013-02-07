#include <string>
#include <vector>

#include "talk/base/faketaskrunner.h"
#include "talk/base/gunit.h"
#include "talk/base/sigslot.h"
#include "talk/xmllite/xmlelement.h"
#include "talk/xmpp/constants.h"
#include "talk/xmpp/fakexmppclient.h"

#include "client/receivemessagetask.h"
#include "client/xmppmessage.h"

class ReceiveMessageTaskTest;

class ReceiveMessageXmppClient : public buzz::FakeXmppClient {
 public:
  ReceiveMessageXmppClient(talk_base::TaskParent* parent, ReceiveMessageTaskTest* tst) :
      FakeXmppClient(parent), test(tst) {
  }

 private:
  ReceiveMessageTaskTest* test;
};

class ReceiveMessageTaskTest: public testing::Test, public sigslot::has_slots<> {
 public:
  ReceiveMessageTaskTest() {
  }

  virtual void SetUp() {
    runner = new talk_base::FakeTaskRunner();
    xmpp_client = new ReceiveMessageXmppClient(runner, this);
  }

  virtual void TearDown() {
    // delete xmpp_client;  Deleted by deleting runner.
    delete runner;
  }

  void ConnectMessageSignal(tuenti::ReceiveMessageTask* task) {
    task->SignalIncomingXmppMessage.connect(this, &ReceiveMessageTaskTest::OnXmppMessage);
  }

  void OnXmppMessage(const tuenti::XmppMessage msg) {
    received_message = msg;
  }

  talk_base::FakeTaskRunner* runner;
  ReceiveMessageXmppClient* xmpp_client;
  tuenti::XmppMessage received_message;
};

TEST_F(ReceiveMessageTaskTest, TestNormalMessageWithStatus) {
  tuenti::ReceiveMessageTask* task = new tuenti::ReceiveMessageTask(xmpp_client,buzz::XmppEngine::HL_ALL);
  task->Start();
  ConnectMessageSignal(task);
  std::string incoming_message =
        "<message"
        " from='tester@blah.com/res1'"
        " to='tester2@blah.com/res2'"
        " type='chat'>"
        "</message>";
  buzz::XmlElement* to_msg = buzz::XmlElement::ForStr(incoming_message);
  buzz::XmlElement* body = new buzz::XmlElement(buzz::QN_BODY, true);
  body->AddText("Test body text.");
  to_msg->AddElement(body);
  to_msg->AddElement(new buzz::XmlElement(buzz::QN_CS_ACTIVE, true));
  task->HandleStanza(to_msg);
  EXPECT_EQ(received_message.body, "Test body text.");
  EXPECT_EQ(received_message.state, tuenti::XMPP_CHAT_ACTIVE);
  EXPECT_EQ(received_message.jid.Str(), "tester@blah.com/res1");
  EXPECT_FALSE(task->IsDone());
}

TEST_F(ReceiveMessageTaskTest, TestStatusOnly) {
  tuenti::ReceiveMessageTask* task = new tuenti::ReceiveMessageTask(xmpp_client,buzz::XmppEngine::HL_ALL);
  task->Start();
  ConnectMessageSignal(task);
  std::string incoming_message =
        "<message"
        " from='tester@blah.com/res1'"
        " to='tester2@blah.com/res2'"
        " type='chat'>"
        "</message>";
  buzz::XmlElement* to_msg = buzz::XmlElement::ForStr(incoming_message);
  to_msg->AddElement(new buzz::XmlElement(buzz::QN_CS_INACTIVE, true));
  task->HandleStanza(to_msg);
  EXPECT_EQ(received_message.state, tuenti::XMPP_CHAT_INACTIVE);
  EXPECT_EQ(received_message.body, "");
}

TEST_F(ReceiveMessageTaskTest, TestMessageBodyOnly) {
  tuenti::ReceiveMessageTask* task = new tuenti::ReceiveMessageTask(xmpp_client,buzz::XmppEngine::HL_ALL);
  task->Start();
  ConnectMessageSignal(task);
  std::string incoming_message =
        "<message"
        " from='tester@blah.com/res1'"
        " to='tester2@blah.com/res2'"
        " type='chat'>"
        "</message>";
  buzz::XmlElement* to_msg = buzz::XmlElement::ForStr(incoming_message);
  buzz::XmlElement* body = new buzz::XmlElement(buzz::QN_BODY, true);
  body->AddText("Test body text.");
  to_msg->AddElement(body);
  task->HandleStanza(to_msg);
  EXPECT_EQ(received_message.state, tuenti::XMPP_CHAT_NONE);
  EXPECT_EQ(received_message.body, "Test body text.");
}
