/*
 * webrtc-jingle
 * Copyright 2012 Tuenti Technologies
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
#include <assert.h>
#include "tuenti/clientsignalingthread.h"
#include "tuenti/logging.h"
#include "tuenti/presenceouttask.h"
#include "tuenti/presencepushtask.h"
#include "tuenti/voiceclient.h"  // Needed for notify_ would be nice to remove
#include "talk/base/signalthread.h"
#include "talk/base/ssladapter.h"
#include "talk/session/media/call.h"
#include "talk/session/media/mediasessionclient.h"
#include "talk/p2p/base/sessionmanager.h"
#include "talk/p2p/client/basicportallocator.h"
#include "talk/p2p/client/sessionmanagertask.h"

namespace tuenti {

enum {
  // ST_MSG_WORKER_DONE is defined in SignalThread.h
  MSG_LOGIN = talk_base::SignalThread::ST_MSG_FIRST_AVAILABLE,
  MSG_DISCONNECT,  // Logout
  MSG_CALL,
  MSG_ACCEPT_CALL,
  MSG_HOLD_CALL,
  MSG_DECLINE_CALL,
  MSG_MUTE_CALL,
  MSG_END_CALL
//  , MSG_DESTROY
};

struct ClientSignalingData: public talk_base::MessageData {
  ClientSignalingData(uint32 call_id, bool b) :
      call_id_(call_id),
      b_(b) {}
  ClientSignalingData(std::string s) :
      s_(s) {}
  ClientSignalingData(uint32 call_id) :
      call_id_(call_id) {}
  uint32 call_id_;
  std::string s_;
  bool b_;
};

///////////////////////////////////////////////////////////////////////////////
// ClientSignalingThread
///////////////////////////////////////////////////////////////////////////////

ClientSignalingThread::ClientSignalingThread(VoiceClientNotify *notifier,
    talk_base::Thread *signal_thread, StunConfig *stun_config)
    : talk_base::SignalThread(),
    notify_(notifier),
    signal_thread_(signal_thread),
    roster_(NULL),
    buddy_list_(NULL),
    pump_(NULL),
    presence_push_(NULL),
    presence_out_(NULL),
    ping_(NULL),
    network_manager_(NULL),
    port_allocator_(NULL),
    session_manager_(NULL),
    session_manager_task_(NULL),
    call_(NULL),
    media_client_(NULL),
    port_allocator_flags_(0),
    use_ssl_(false),
    auto_accept_(false) {
  int numRelayPorts = 0;
  LOGI("ClientSignalingThread::ClientSignalingThread");
  assert(talk_base::Thread::Current() == signal_thread_);
  // Overriding name
  // worker_.SetName("ClientSignalingThread", this);
  // simple initializers
  if (roster_ == NULL) {
    roster_ = new RosterMap();
    LOGI("ClientSignalingThread::ClientSignalingThread - new RosterMap "
            "roster_@(0x%x)", reinterpret_cast<int>(roster_));
  }

  if (buddy_list_ == NULL) {
    buddy_list_ = new BuddyListMap();
  }
  if (network_manager_ == NULL) {
    network_manager_ = new talk_base::BasicNetworkManager();
    LOGI("ClientSignalingThread::ClientSignalingThread - new "
            "BasicNetworkManager network_manager_@(0x%x)",
            reinterpret_cast<int>(network_manager_));
  }
  if (port_allocator_ == NULL) {
    talk_base::SocketAddress stun = talk_base::SocketAddress();
    talk_base::SocketAddress relay_udp = talk_base::SocketAddress();
    talk_base::SocketAddress relay_tcp = talk_base::SocketAddress();
    talk_base::SocketAddress relay_ssl = talk_base::SocketAddress();
    talk_base::SocketAddress turn = talk_base::SocketAddress();

    if (stun_config->stun.empty() || !stun.FromString(stun_config->stun)) {
      stun.Clear();
      port_allocator_flags_ |= cricket::PORTALLOCATOR_DISABLE_STUN;
    }
    if (stun_config->relay_udp.empty() ||
            !relay_udp.FromString(stun_config->relay_udp)) {
      relay_udp.Clear();
      ++numRelayPorts;
    }

    if (stun_config->relay_tcp.empty() ||
            !relay_tcp.FromString(stun_config->relay_tcp)) {
      relay_tcp.Clear();
      ++numRelayPorts;
    }
    if (stun_config->relay_ssl.empty() ||
            !relay_ssl.FromString(stun_config->relay_ssl)) {
      relay_ssl.Clear();
      ++numRelayPorts;
    }
    if (stun_config->turn.empty() ||
            !turn.FromString(stun_config->turn)) {
      turn.Clear();
      port_allocator_flags_ |= cricket::PORTALLOCATOR_DISABLE_TURN;
    }
    if (numRelayPorts == 0) {
      port_allocator_flags_ |= cricket::PORTALLOCATOR_DISABLE_RELAY;
    }

    port_allocator_ = new cricket::BasicPortAllocator(network_manager_,
        stun, relay_udp, relay_tcp, relay_ssl, turn);
    LOGI("ClientSignalingThread::ClientSignalingThread - "
        "new BasicPortAllocator port_allocator_@(0x%x)",
        reinterpret_cast<int>(port_allocator_));
    if (port_allocator_flags_ != 0) {
      LOGI("LOGT ClientSignalingThread::ClientSignalingThread - "
        "setting port_allocator_flags_=%d", port_allocator_flags_);
      port_allocator_->set_flags(port_allocator_flags_);
    }
  }
  if (session_manager_ == NULL) {
    session_manager_ = new cricket::SessionManager(port_allocator_, worker());
    LOGI("ClientSignalingThread::ClientSignalingThread - new "
      "SessionManager session_manager_@(0x%x)",
      reinterpret_cast<int>(session_manager_));
  }
  if (pump_ == NULL) {
    pump_ = new TXmppPump(this);
    LOGI("ClientSignalingThread::ClientSignalingThread - new TXmppPump "
      "pump_@(0x%x)", reinterpret_cast<int>(pump_));
  }
  my_status_.set_caps_node("http://github.com/lukeweber/webrtc-jingle");
  my_status_.set_version("1.0-SNAPSHOT");
}

ClientSignalingThread::~ClientSignalingThread() {
  LOGI("ClientSignalingThread::~ClientSignalingThread");
  assert(talk_base::Thread::Current() == signal_thread_);
  if (roster_) {
    LOGI("ClientSignalingThread::~ClientSignalingThread - "
        "deleting roster_@(0x%x)", reinterpret_cast<int>(roster_));
    delete roster_;
    roster_ = NULL;
  }
  if (buddy_list_) {
    delete buddy_list_;
    buddy_list_ = NULL;
  }
  if (network_manager_ != NULL) {
    LOGI("ClientSignalingThread::~ClientSignalingThread - "
            "deleting network_manager_@(0x%x)",
            reinterpret_cast<int>(network_manager_));
    delete network_manager_;
    network_manager_ = NULL;
  }
  if (port_allocator_ != NULL) {
    LOGI("ClientSignalingThread::~ClientSignalingThread - "
            "deleting port_allocator_@(0x%x)",
            reinterpret_cast<int>(port_allocator_));
    delete port_allocator_;
    port_allocator_ = NULL;
  }
  if (session_manager_ != NULL) {
    LOGI("ClientSignalingThread::~ClientSignalingThread - "
            "deleting session_manager_@(0x%x)",
            reinterpret_cast<int>(session_manager_));
    delete session_manager_;
    session_manager_ = NULL;
  }
  if (pump_ != NULL) {
    LOGI("ClientSignalingThread::~ClientSignalingThread - "
            "deleting pump_@(0x%x)", reinterpret_cast<int>(pump_));
    delete pump_;
    pump_ = NULL;
  }
}

void ClientSignalingThread::OnStatusUpdate(const buzz::Status& status) {
  LOGI("ClientSignalingThread::OnStatusUpdate");
  assert(talk_base::Thread::Current() == signal_thread_);
  RosterItem item;
  item.jid = status.jid();
  item.show = status.show();
  item.status = status.status();
  item.nick = status.nick();
  std::string key = item.jid.Str();

  std::string bare_jid_str = item.jid.BareJid().Str();
  BuddyListMap::iterator buddy_iter = buddy_list_->find(bare_jid_str);

  RosterMap::iterator iter = roster_->find(key);
  if (status.available() && status.voice_capability()) {
    // New buddy, add and notify
    if (buddy_iter == buddy_list_->end()) {
      // LOGI("Adding to roster: %s, %s", key.c_str(), item.nick.c_str());
      (*buddy_list_)[bare_jid_str] = 1;
      notify_->OnBuddyListAdd(bare_jid_str, item.nick);
    // New Available client of existing buddy
    } else if (iter == roster_->end()) {
      (*buddy_iter).second++;
    // Something changed in a roster item, but we already have it.
    } else {
      LOGI("Updating roster info: %s", key.c_str());
    }
    (*roster_)[key] = item;
  } else {
    if (iter != roster_->end()) {
      roster_->erase(iter);
      // Last available endpoint gone, remove the buddy and notify
      if (buddy_iter != buddy_list_->end() && --((*buddy_iter).second) == 0) {
        // LOGI("Removing from roster: %s", key.c_str());
        buddy_list_->erase(buddy_iter);
        notify_->OnBuddyListRemove(bare_jid_str);
      }
    }
  }
}

void ClientSignalingThread::OnSessionState(cricket::Call* call,
    cricket::Session* session, cricket::Session::State state) {
  LOGI("ClientSignalingThread::OnSessionState");
  assert(talk_base::Thread::Current() == signal_thread_);
  switch (state) {
  default:
    LOGI("VoiceClient::OnSessionState - UNKNOWN_STATE");
    break;
  case cricket::BaseSession::STATE_SENTTERMINATE:
    LOGI("VoiceClient::OnSessionState - STATE_SENTTERMINATE doing nothing...");
    break;
  case cricket::BaseSession::STATE_DEINIT:
    LOGI("VoiceClient::OnSessionState - STATE_DEINIT doing nothing...");
    break;
  case cricket::Session::STATE_RECEIVEDINITIATE:
    {
    LOGI("VoiceClient::OnSessionState - "
      "STATE_RECEIVEDINITIATE setting up call...");
    buzz::Jid jid(session->remote_name());
    LOGI("Incoming call from '%s', call_id %d", jid.Str().c_str(), call->id());
    if (auto_accept_) {
      AcceptCall(call->id());
    }
    break;
    }
  case cricket::Session::STATE_SENTINITIATE:
    LOGI("VoiceClient::OnSessionState - STATE_SENTINITIATE doing nothing...");
    break;
  case cricket::Session::STATE_RECEIVEDACCEPT:
    LOGI("VoiceClient::OnSessionState - "
      "STATE_RECEIVEDACCEPT transfering data.");
    break;
  case cricket::Session::STATE_RECEIVEDREJECT:
    LOGI("VoiceClient::OnSessionState - STATE_RECEIVEDREJECT doing nothing...");
    break;
  case cricket::Session::STATE_INPROGRESS:
    LOGI("VoiceClient::OnSessionState - STATE_INPROGRESS monitoring...");
    call->StartSpeakerMonitor(session);
    break;
  case cricket::Session::STATE_RECEIVEDTERMINATE:
    LOGI("VoiceClient::OnSessionState - STATE_RECEIVEDTERMINATE");
    break;
  case cricket::Session::STATE_SENTBUSY:
    LOGI("VoiceClient::OnSessionState - Sent Busy");
    break;
  case cricket::Session::STATE_RECEIVEDBUSY:
    LOGI("VoiceClient::OnSessionState - Received Busy");
    break;
  }
  if (notify_) {
    notify_->OnCallStateChange(session, state, call->id());
  }
}

void ClientSignalingThread::OnStateChange(buzz::XmppEngine::State state) {
  LOGI("ClientSignalingThread::OnStateChange");
  assert(talk_base::Thread::Current() == signal_thread_);
  switch (state) {
  default:
    LOGI("ClientSignalingThread::OnStateChange - Unknown State (---) "
            "doing nothing...");
    break;
  case buzz::XmppEngine::STATE_NONE:
    LOGI("ClientSignalingThread::OnStateChange - State (STATE_NONE) "
            "doing nothing...");
    break;
  case buzz::XmppEngine::STATE_START:
    LOGI("ClientSignalingThread::OnStateChange - State (STATE_START) "
            "doing nothing...");
    break;
  case buzz::XmppEngine::STATE_OPENING:
    LOGI("ClientSignalingThread::OnStateChange - State (STATE_OPENING) "
            "doing nothing...");
    break;
  case buzz::XmppEngine::STATE_OPEN:
    LOGI("ClientSignalingThread::OnStateChange - State (STATE_OPEN) "
            "initing media & presence...");
    InitMedia();
    InitPresence();
    InitPing();
    break;
  case buzz::XmppEngine::STATE_CLOSED:
    LOGI("ClientSignalingThread::OnStateChange - State (STATE_CLOSED) "
            "cleaning up ssl, deleting media client & clearing roster...");
    if (xcs_.use_tls() == buzz::TLS_REQUIRED) {
      talk_base::CleanupSSL();
    }
    // Remove everyone from your roster
    if (roster_) {
      roster_->clear();
    }
    if (buddy_list_) {
      notify_->OnBuddyListReset();
      buddy_list_->clear();
    }
    // NFHACK we should probably do something with the media_client_ here
    if (media_client_) {
      delete media_client_;
      media_client_ = NULL;
    }
    break;
  }
  if (notify_) {
    notify_->OnXmppStateChange(state);
  }

  // main_thread_->Post(this, MSG_STATE_CHANGE, new StateChangeData(state));
}

void ClientSignalingThread::OnRequestSignaling() {
  LOGI("ClientSignalingThread::OnRequestSignaling");
  assert(talk_base::Thread::Current() == signal_thread_);
  session_manager_->OnSignalingReady();
}

void ClientSignalingThread::OnSessionCreate(cricket::Session* session,
    bool initiate) {
  LOGI("ClientSignalingThread::OnSessionCreate");
  assert(talk_base::Thread::Current() == signal_thread_);
  session->set_current_protocol(cricket::PROTOCOL_HYBRID);
}

void ClientSignalingThread::OnCallCreate(cricket::Call* call) {
  LOGI("ClientSignalingThread::OnCallCreate");
  assert(talk_base::Thread::Current() == signal_thread_);
  call->SignalSessionState.connect(this,
      &ClientSignalingThread::OnSessionState);
}

void ClientSignalingThread::OnCallDestroy(cricket::Call* call) {
  LOGI("ClientSignalingThread::OnCallDestroy");
  assert(talk_base::Thread::Current() == signal_thread_);
  if (call == call_) {
    LOGI("internal delete found a valid call_@(0x%x) "
            "to destroy ", reinterpret_cast<int>(call_));
    call_ = NULL;
  }
}

void ClientSignalingThread::OnMediaEngineTerminate() {
  LOGI("ClientSignalingThread::OnMediaEngineTerminate");
  assert(talk_base::Thread::Current() == signal_thread_);
}

void ClientSignalingThread::OnPingTimeout() {
  LOGE("XMPP ping timeout. Will keep trying...");
  InitPing();
}

// ================================================================
// THESE ARE THE ONLY FUNCTIONS THAT CAN BE CALLED USING ANY THREAD
// vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
void ClientSignalingThread::Login(const std::string &username,
    const std::string &password, const std::string &xmpp_host, int xmpp_port,
    bool use_ssl) {
  LOGI("ClientSignalingThread::Login");

  buzz::Jid jid = buzz::Jid(username);

  talk_base::InsecureCryptStringImpl pass;
  pass.password() = password;

  xcs_.set_user(jid.node());
  xcs_.set_resource("TuentiVoice");
  xcs_.set_host(jid.domain());
  xcs_.set_use_tls(use_ssl ? buzz::TLS_REQUIRED : buzz::TLS_DISABLED);
  xcs_.set_pass(talk_base::CryptString(pass));
  xcs_.set_server(talk_base::SocketAddress(xmpp_host, xmpp_port));
  signal_thread_->Post(this, MSG_LOGIN);
}

void ClientSignalingThread::Disconnect() {
  LOGI("ClientSignalingThread::Disconnect");
  // assert(talk_base::Thread::Current() == signal_thread_);
  signal_thread_->Post(this, MSG_DISCONNECT);
}

void ClientSignalingThread::Call(std::string remoteJid) {
  LOGI("ClientSignalingThread::Call");
  // assert(talk_base::Thread::Current() == signal_thread_);
  signal_thread_->Post(this, MSG_CALL, new ClientSignalingData(remoteJid));
}

void ClientSignalingThread::MuteCall(uint32 call_id, bool mute) {
  signal_thread_->Post(this, MSG_MUTE_CALL, new ClientSignalingData(call_id,
    mute));
}

void ClientSignalingThread::HoldCall(uint32 call_id, bool hold) {
  signal_thread_->Post(this, MSG_HOLD_CALL, new ClientSignalingData(call_id,
      hold));
}

void ClientSignalingThread::AcceptCall(uint32 call_id) {
  LOGI("ClientSignalingThread::AcceptCall %d", call_id);
  // assert(talk_base::Thread::Current() == signal_thread_);
  signal_thread_->Post(this, MSG_ACCEPT_CALL, new ClientSignalingData(call_id));
}

void ClientSignalingThread::DeclineCall(uint32 call_id, bool busy) {
  LOGI("ClientSignalingThread::DeclineCall %d", call_id);
  // assert(talk_base::Thread::Current() == signal_thread_);
  signal_thread_->Post(this, MSG_DECLINE_CALL, new ClientSignalingData(call_id,
      busy));
}

void ClientSignalingThread::EndCall(uint32 call_id) {
  LOGI("ClientSignalingThread::EndCall %d", call_id);
  // assert(talk_base::Thread::Current() == signal_thread_);
  signal_thread_->Post(this, MSG_END_CALL, new ClientSignalingData(call_id));
}

// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
// THESE ARE THE ONLY FUNCTIONS THAT CAN BE CALLED USING ANY THREAD
// ================================================================

bool ClientSignalingThread::Destroy() {
  LOGI("ClientSignalingThread::Destroy");
  assert(talk_base::Thread::Current() == signal_thread_);
  bool destroyed = false;
  // Single threaded shuts everything down
  Disconnect();
  if (media_client_ == NULL) {
    SignalThread::Destroy(true);
    destroyed = true;
  }
  return destroyed;
}

void ClientSignalingThread::OnMessage(talk_base::Message* message) {
  LOGI("ClientSignalingThread::OnMessage");
  assert(talk_base::Thread::Current() == signal_thread_);
  ClientSignalingData* data;
  switch (message->message_id) {
  case MSG_LOGIN:
    LOGI("ClientSignalingThread::OnMessage - MSG_LOGIN");
    LoginS();
    break;
  case MSG_DISCONNECT:
    LOGI("ClientSignalingThread::OnMessage - MSG_DISCONNECT");
    DisconnectS();
    break;
  case MSG_CALL:
    LOGI("ClientSignalingThread::OnMessage - MSG_CALL");
    CallS(static_cast<ClientSignalingData*>(message->pdata)->s_);
    delete message->pdata;
    break;
  case MSG_MUTE_CALL:
    LOGI("ClientSignallingThread::OnMessage - MSG_MUTE_CALL");
    data = static_cast<ClientSignalingData*>(message->pdata);
    MuteCallS(data->call_id_, data->b_);
    delete message->pdata;
    break;
  case MSG_ACCEPT_CALL:
    LOGI("ClientSignalingThread::OnMessage - MSG_ACCEPT_CALL");
    AcceptCallS(static_cast<ClientSignalingData*>(message->pdata)->call_id_);
    delete message->pdata;
    break;
  case MSG_DECLINE_CALL:
    LOGI("ClientSignalingThread::OnMessage - MSG_DECLINE_CALL");
    data = static_cast<ClientSignalingData*>(message->pdata);
    DeclineCallS(data->call_id_, data->b_);
    delete message->pdata;
    break;
  case MSG_HOLD_CALL:
    LOGI("ClientSignalingThread::OnMessage - MSG_HOLD_CALL");
    data = static_cast<ClientSignalingData*>(message->pdata);
    HoldCallS(data->call_id_, data->b_);
    delete message->pdata;
    break;
  case MSG_END_CALL:
    LOGI("ClientSignalingThread::OnMessage - MSG_END_CALL");
    EndCallS(static_cast<ClientSignalingData*>(message->pdata)->call_id_);
    delete message->pdata;
    break;
  default:
    LOGI("ClientSignalingThread::OnMessage - UNKNOWN "
            "falling back to base class");
    SignalThread::OnMessage(message);
    break;
  }
}

void ClientSignalingThread::DoWork() {
  LOGI("ClientSignalingThread::DoWork");
  assert(talk_base::Thread::Current() == worker());
  worker()->ProcessMessages(talk_base::kForever);
}

void ClientSignalingThread::LoginS() {
  LOGI("ClientSignalingThread::LoginS");
  assert(talk_base::Thread::Current() == signal_thread_);
  if (media_client_ == NULL) {
    if (xcs_.use_tls() == buzz::TLS_REQUIRED) {
      talk_base::InitializeSSL();
    }

    if (pump_->AllChildrenDone()) {
      LOGE("AllChildrenDone doing required "
        "{delete pump_;pump_ = new TXmppPump(this);} yet...");
    }
    pump_->DoLogin(xcs_);
  }
}

void ClientSignalingThread::DisconnectS() {
  LOGI("ClientSignalingThread::DisconnectS");
  assert(talk_base::Thread::Current() == signal_thread_);
  if (call_) {
    // TODO(Luke): Gate EndAllCalls whether this has already been called.
    // On a shutdown, it should only be called once otherwise, you'll
    // end up with asyncronous double deletes of call objects/SEGFAULT.
    EndAllCalls();
    signal_thread_->PostDelayed(100, this, MSG_DISCONNECT);
  } else if (media_client_) {
    if (pump_->AllChildrenDone()) {
      LOGE("AllChildrenDone NOT doing required "
              "{delete pump_;pump_ = new TXmppPump(this);} yet...");
    }
    pump_->DoDisconnect();
    signal_thread_->PostDelayed(100, this, MSG_DISCONNECT);
  } else {
    LOGI("Already disconnected nothing to do");
  }
}

void ClientSignalingThread::CallS(const std::string &remoteJid) {
  LOGI("ClientSignalingThread::CallS");
  assert(talk_base::Thread::Current() == signal_thread_);

  cricket::CallOptions options;
  options.is_muc = false;

  bool found = false;
  buzz::Jid callto_jid(remoteJid);
  buzz::Jid found_jid;

  // otherwise, it's a friend
  for (RosterMap::iterator iter = roster_->begin(); iter != roster_->end();
      ++iter) {
    if (iter->second.jid.BareEquals(callto_jid)) {
      found = true;
      found_jid = iter->second.jid;
      break;
    }
  }

  if (found) {
    LOGI("Found online friend '%s'", found_jid.Str().c_str());
    if (!call_) {
      call_ = media_client_->CreateCall();
      call_->InitiateSession(found_jid, media_client_->jid(), options);  // REQ_MAIN_THREAD
    }
    media_client_->SetFocus(call_);
  } else {
    LOGI("Could not find online friend '%s'", remoteJid.c_str());
  }
}

void ClientSignalingThread::MuteCallS(uint32 call_id, bool mute) {
  cricket::Call* call = GetCall(call_id);
  if(call) {
    call->Mute(mute);
    LOGI("Toggled mute, call_id %d", call_id);
  }
}

void ClientSignalingThread::HoldCallS(uint32 call_id, bool hold) {
  cricket::Call* call = GetCall(call_id);
  if(call) {
    if(hold && call == call_) {
      media_client_->SetFocus(NULL);
      call_ = NULL;
    } else if (!hold) {
      media_client_->SetFocus(call);
      call_ = call;
    }
    LOGI("Toggled hold call_id %d", call_id);
  }
}

void ClientSignalingThread::AcceptCallS(uint32 call_id) {
  LOGI("ClientSignalingThread::AcceptCallS, call_id %d", call_id);
  assert(talk_base::Thread::Current() == signal_thread_);
  cricket::Call* call = GetCall(call_id);
  if (call && call->sessions().size() == 1) {
    call_ = call;
    cricket::CallOptions options;
    call->AcceptSession(call->sessions()[0], options);
    media_client_->SetFocus(call);
  } else {
    LOGE("ClientSignalingThread::AcceptCallW - No incoming call to accept");
  }
}

void ClientSignalingThread::DeclineCallS(uint32 call_id, bool busy) {
  LOGI("ClientSignalingThread::DeclineCallS call_id %d, busy: %d", call_id, busy);
  cricket::Call* call = GetCall(call_id);
  assert(talk_base::Thread::Current() == signal_thread_);
  if (call && call->sessions().size() == 1) {
    call->RejectSession(call->sessions()[0], busy);
  } else {
    LOGE("ClientSignalingThread::DeclineCallW - No incoming call to decline");
  }
}

void ClientSignalingThread::EndCallS(uint32 call_id) {
  LOGI("ClientSignalingThread::EndCallS %d", call_id);
  assert(talk_base::Thread::Current() == signal_thread_);
  cricket::Call* call = GetCall(call_id);
  if (call) {
    call->Terminate();
  }
}

void ClientSignalingThread::InitMedia() {
  LOGI("ClientSignalingThread::InitMedia");
  assert(talk_base::Thread::Current() == signal_thread_);
  std::string client_unique = pump_->client()->jid().Str();
  talk_base::InitRandom(client_unique.c_str(), client_unique.size());

  // TODO(alex) We need to modify the last params of this to add TURN servers
  session_manager_->SignalRequestSignaling.connect(this,
      &ClientSignalingThread::OnRequestSignaling);
  session_manager_->SignalSessionCreate.connect(this,
      &ClientSignalingThread::OnSessionCreate);
  session_manager_->OnSignalingReady();

  session_manager_task_ =
      new cricket::SessionManagerTask(pump_->client(), session_manager_);
  session_manager_task_->EnableOutgoingMessages();
  session_manager_task_->Start();

  media_client_ =
      new cricket::MediaSessionClient(pump_->client()->jid(), session_manager_);
  media_client_->SignalCallCreate.connect(this,
      &ClientSignalingThread::OnCallCreate);
  media_client_->SignalCallDestroy.connect(this,
      &ClientSignalingThread::OnCallDestroy);
  media_client_->set_secure(cricket::SEC_DISABLED);
}

void ClientSignalingThread::InitPresence() {
  // NFHACK Fix the news
  LOGI("ClientSignalingThread::InitPresence");
  assert(talk_base::Thread::Current() == signal_thread_);
  presence_push_ = new buzz::PresencePushTask(pump_->client());
  presence_push_->SignalStatusUpdate.connect(this,
      &ClientSignalingThread::OnStatusUpdate);
  presence_push_->Start();

  my_status_.set_jid(pump_->client()->jid());
  my_status_.set_available(true);
  my_status_.set_show(buzz::Status::SHOW_ONLINE);
  my_status_.set_know_capabilities(true);
  my_status_.set_pmuc_capability(false);

  int capabilities = media_client_->GetCapabilities();
  my_status_.set_voice_capability((capabilities & cricket::AUDIO_RECV) != 0);
  my_status_.set_video_capability((capabilities & cricket::VIDEO_RECV) != 0);
  my_status_.set_camera_capability((capabilities & cricket::VIDEO_SEND) != 0);

  presence_out_ = new buzz::PresenceOutTask(pump_->client());
  presence_out_->Send(my_status_);
  presence_out_->Start();
}

void ClientSignalingThread::InitPing() {
  LOGI("ClientSignalingThread::InitPing");
  assert(talk_base::Thread::Current() == signal_thread_);
  ping_ = new buzz::PingTask(pump_->client(), talk_base::Thread::Current(),
      10000, 10000);
  ping_->SignalTimeout.connect(this, &ClientSignalingThread::OnPingTimeout);
  ping_->Start();
}

cricket::Call* ClientSignalingThread::GetCall(uint32 call_id) {
  const std::map<uint32, cricket::Call*>& calls = media_client_->calls();
  for (std::map<uint32, cricket::Call*>::const_iterator i = calls.begin();
       i != calls.end(); ++i) {
    if (i->first == call_id) {
        return i->second;
    }
  }
  return NULL;
}

bool ClientSignalingThread::EndAllCalls() {
  bool calls_processed = false;
  const std::map<uint32, cricket::Call*>& calls = media_client_->calls();
  for (std::map<uint32, cricket::Call*>::const_iterator i = calls.begin();
       i != calls.end(); ++i) {
    signal_thread_->Post(this, MSG_END_CALL, new ClientSignalingData(i->first));
    calls_processed = true;
  }
  call_  = NULL;
  return calls_processed;
}
}  // namespace tuenti
