#include <string>
#include <vector>

#include "talk/base/faketaskrunner.h"
#include "talk/base/gunit.h"
#include "talk/xmllite/xmlelement.h"
#include "talk/xmpp/constants.h"
#include "talk/xmpp/fakexmppclient.h"

#include "client/sendmessagetask.h"
#include "client/xmppmessage.h"

class SendMessageTaskTest;

class SendMessageXmppClient : public buzz::FakeXmppClient {
 public:
  SendMessageXmppClient(talk_base::TaskParent* parent, SendMessageTaskTest* tst) :
      FakeXmppClient(parent), test(tst) {
  }

 private:
  SendMessageTaskTest* test;
};

class SendMessageTaskTest: public testing::Test, public sigslot::has_slots<> {
 public:
  SendMessageTaskTest() {
  }

  virtual void SetUp() {
    runner = new talk_base::FakeTaskRunner();
    xmpp_client = new SendMessageXmppClient(runner, this);
  }

  virtual void TearDown() {
    // delete xmpp_client;  Deleted by deleting runner.
    delete runner;
  }

  talk_base::FakeTaskRunner* runner;
  SendMessageXmppClient* xmpp_client;
};

TEST_F(SendMessageTaskTest, TestNormalMessageWithStatus) {
  tuenti::SendMessageTask* task = new tuenti::SendMessageTask(xmpp_client);
  tuenti::XmppMessage out_msg = tuenti::XmppMessage("tester@blah.com/res1",
                            tuenti::XMPP_CHAT_ACTIVE, "Test body text.");
  task->Send(out_msg);
  task->Start();
  const buzz::XmlElement* msg = xmpp_client->sent_stanzas().front();

  EXPECT_EQ("Test body text.", msg->FirstNamed(buzz::QN_BODY)->BodyText());
  EXPECT_EQ("chat", msg->Attr(buzz::QN_TYPE));
  EXPECT_EQ("tester@blah.com", msg->Attr(buzz::QN_TO));
  EXPECT_EQ(buzz::QN_CS_ACTIVE, msg->FirstWithNamespace(buzz::NS_CHATSTATE)->Name());
  EXPECT_TRUE(task->IsDone());
}

TEST_F(SendMessageTaskTest, TestStatusOnly) {
  tuenti::SendMessageTask* task = new tuenti::SendMessageTask(xmpp_client);
  tuenti::XmppMessage out_msg = tuenti::XmppMessage();
  out_msg.jid = buzz::Jid("tester@blah.com/res1");
  out_msg.state = tuenti::XMPP_CHAT_ACTIVE;
  task->Send(out_msg);
  task->Start();
  const buzz::XmlElement* msg = xmpp_client->sent_stanzas().front();

  EXPECT_EQ(NULL, msg->FirstNamed(buzz::QN_BODY));
  EXPECT_EQ("chat", msg->Attr(buzz::QN_TYPE));
  EXPECT_EQ("tester@blah.com", msg->Attr(buzz::QN_TO));
  EXPECT_EQ(buzz::QN_CS_ACTIVE, msg->FirstWithNamespace(buzz::NS_CHATSTATE)->Name());
}

TEST_F(SendMessageTaskTest, TestMessageBodyOnly) {
  tuenti::SendMessageTask* task = new tuenti::SendMessageTask(xmpp_client);
  tuenti::XmppMessage out_msg = tuenti::XmppMessage("tester@blah.com/res1",
                            tuenti::XMPP_CHAT_NONE, "Test body text.");
  task->Send(out_msg);
  task->Start();
  const buzz::XmlElement* msg = xmpp_client->sent_stanzas().front();

  EXPECT_EQ("Test body text.", msg->FirstNamed(buzz::QN_BODY)->BodyText());
  EXPECT_EQ("chat", msg->Attr(buzz::QN_TYPE));
  EXPECT_EQ(NULL, msg->FirstWithNamespace(buzz::NS_CHATSTATE));
}
