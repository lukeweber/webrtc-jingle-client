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
#include "client/clientsignalingthread.h"
#include "client/logging.h"
#include "client/client_defines.h"
#include "client/presenceouttask.h"
#include "client/presencepushtask.h"
#include "client/voiceclient.h"  // Needed for notify_ would be nice to remove
#include "talk/base/logging.h"
#include "talk/base/signalthread.h"
#include "talk/base/ssladapter.h"
#include "talk/p2p/base/session.h"
#include "talk/session/media/call.h"
#include "talk/session/media/mediasessionclient.h"
#include "talk/p2p/base/sessionmanager.h"
#include "talk/p2p/client/basicportallocator.h"
#include "talk/p2p/client/sessionmanagertask.h"


#include "talk/xmllite/xmlelement.h"
#include "talk/xmpp/constants.h"

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
  MSG_END_CALL,
  MSG_KEEPALIVE
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

ClientSignalingThread::ClientSignalingThread(
    talk_base::Thread *signal_thread)
    : signal_thread_(signal_thread),
    roster_(NULL),
    buddy_list_(NULL),
    presence_push_(NULL),
    presence_out_(NULL),
    ping_(NULL),
    session_manager_task_(NULL),
    call_(NULL),
    port_allocator_flags_(0),
    use_ssl_(false),
    auto_accept_(false),
    xmpp_state_(buzz::XmppEngine::STATE_NONE) {
  // int numRelayPorts = 0;
  LOGI("ClientSignalingThread::ClientSignalingThread");
  assert(talk_base::Thread::Current() == signal_thread_);
#if LOGGING
  // Set debugging to verbose in libjingle if LOGGING on android.
  talk_base::LogMessage::LogToDebug(talk_base::LS_VERBOSE);
#endif
  if (roster_ == NULL) {
    roster_ = new RosterMap();
    LOGI("ClientSignalingThread::ClientSignalingThread - new RosterMap "
            "roster_@(0x%x)", reinterpret_cast<int>(roster_));
  }
  if (buddy_list_ == NULL) {
    buddy_list_ = new BuddyListMap();
  }
  sp_network_manager_.reset(new talk_base::BasicNetworkManager());
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

  LOGI("ClientSignalingThread::~ClientSignalingThread - done");
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
      SignalBuddyListAdd(bare_jid_str.c_str(), item.nick.c_str());
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
        SignalBuddyListRemove(bare_jid_str.c_str());
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
  case cricket::Session::STATE_RECEIVEDINITIATE_ACK:
    LOGI("VoiceClient::OnSessionState - STATE_RECEIVEDINITIATE_ACK");
    break;
  case cricket::Session::STATE_SENTINITIATE:
    LOGI("VoiceClient::OnSessionState - STATE_SENTINITIATE doing nothing...");
    break;
  case cricket::Session::STATE_RECEIVEDACCEPT:
    LOGI("VoiceClient::OnSessionState - "
      "STATE_RECEIVEDACCEPT transfering data.");
    //Last accept has focus
    sp_media_client_->SetFocus(call);
    call_ = call;
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

  std::string jid_str = "";
  //Session has already been terminated, so lets not do this.
  if (!(state == cricket::Session::STATE_RECEIVEDTERMINATE || state == cricket::Session::STATE_DEINIT)) {
    jid_str = session->remote_name();
  }
  SignalCallStateChange(state, jid_str.c_str(), call->id());
}

void ClientSignalingThread::OnSessionError(cricket::Call* call,
    cricket::Session* session, cricket::Session::Error error) {
  switch ( error ){
  case cricket::Session::ERROR_NONE:
    // no error
    break;
  case cricket::Session::ERROR_TIME:
    // no response to signaling
    break;
  case cricket::Session::ERROR_RESPONSE:
    // error during signaling
    break;
  case cricket::Session::ERROR_NETWORK:
    // network error, could not allocate network resources
    break;
  case cricket::Session::ERROR_CONTENT:
    // channel errors in SetLocalContent/SetRemoteContent
    break;
  case cricket::Session::ERROR_TRANSPORT:
    // transport error of some kind
    break;
  case cricket::Session::ERROR_ACK_TIME:
    // no ack response to signaling
    break;
  }
  SignalCallError(error, call->id());
}

void ClientSignalingThread::OnXmppError(buzz::XmppEngine::Error error) {
  SignalXmppError(error);
}

void ClientSignalingThread::OnXmppSocketClose(int state) {
  SignalXmppSocketClose(state);
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
#if XMPP_WHITESPACE_KEEPALIVE_ENABLED
    ScheduleKeepAlive();
#endif
#if XMPP_PING_ENABLED
    InitPing();
#endif
    break;
  case buzz::XmppEngine::STATE_CLOSED:
    ResetMedia();
    break;
  }
  xmpp_state_ = state;
  SignalXmppStateChange(state);
}

void ClientSignalingThread::OnAudioPlayout(){
  SignalAudioPlayout();
}

void ClientSignalingThread::OnRequestSignaling() {
  LOGI("ClientSignalingThread::OnRequestSignaling");
  assert(talk_base::Thread::Current() == signal_thread_);
  sp_session_manager_->OnSignalingReady();
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
  call->SignalSessionError.connect(this,
      &ClientSignalingThread::OnSessionError);
  call->SignalAudioPlayout.connect(this,
      &ClientSignalingThread::OnAudioPlayout);
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
    const std::string &password, StunConfig* stun_config,
    const std::string &xmpp_host, int xmpp_port, bool use_ssl) {
  LOGI("ClientSignalingThread::Login");

  stun_config_ = stun_config;

  buzz::Jid jid = buzz::Jid(username);
  talk_base::InsecureCryptStringImpl pass;
  pass.password() = password;

  xcs_.set_user(jid.node());
  xcs_.set_resource("voice");
#if ADD_RANDOM_RESOURCE_TO_JID
  std::string random_chunk;
  const std::string alphanum_table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
  //Only use first 62 chars in table to avoid "/ and +"
  talk_base::CreateRandomString(10, alphanum_table, &random_chunk);
  xcs_.set_resource("voice" + random_chunk);
#endif
  xcs_.set_host(jid.domain());
  xcs_.set_use_tls(use_ssl ? buzz::TLS_REQUIRED : buzz::TLS_DISABLED);
  xcs_.set_pass(talk_base::CryptString(pass));
  xcs_.set_server(talk_base::SocketAddress(xmpp_host, xmpp_port));
  signal_thread_->Post(this, MSG_LOGIN);
}

void ClientSignalingThread::Disconnect() {
  LOGI("ClientSignalingThread::Disconnect");
  signal_thread_->Post(this, MSG_DISCONNECT);
}

void ClientSignalingThread::Call(std::string remoteJid) {
  LOGI("ClientSignalingThread::Call");
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
  signal_thread_->Post(this, MSG_ACCEPT_CALL, new ClientSignalingData(call_id));
}

void ClientSignalingThread::DeclineCall(uint32 call_id, bool busy) {
  LOGI("ClientSignalingThread::DeclineCall %d", call_id);
  signal_thread_->Post(this, MSG_DECLINE_CALL, new ClientSignalingData(call_id,
      busy));
}

void ClientSignalingThread::EndCall(uint32 call_id) {
  LOGI("ClientSignalingThread::EndCall %d", call_id);
  signal_thread_->Post(this, MSG_END_CALL, new ClientSignalingData(call_id));
}

// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
// THESE ARE THE ONLY FUNCTIONS THAT CAN BE CALLED USING ANY THREAD
// ================================================================

void ClientSignalingThread::Destroy() {
  LOGI("ClientSignalingThread::Destroy");
  assert(talk_base::Thread::Current() == signal_thread_);
  DisconnectS();
  // These depend on SignalThred::worker(), so delete them first.
  sp_session_manager_.reset(NULL);
  sp_socket_factory_.reset(NULL);
  SignalThread::Destroy(true);
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
  case MSG_KEEPALIVE:
    OnKeepAliveS();
    break;
  default:
    LOGI("ClientSignalingThread::OnMessage - UNKNOWN "
            "falling back to base class");
    SignalThread::OnMessage(message);
    break;
  }
}

void ClientSignalingThread::OnKeepAliveS(){
  if(sp_pump_->client()){
    sp_pump_->client()->SendRaw(" ");
    ScheduleKeepAlive();
  }
}

void ClientSignalingThread::ScheduleKeepAlive() {
  //We ping a white space every 10 minutes to avoid the connection dropping.
  signal_thread_->PostDelayed(XmppKeepAlivePingInterval, this, MSG_KEEPALIVE);
}


void ClientSignalingThread::DoWork() {
  LOGI("ClientSignalingThread::DoWork");
  assert(talk_base::Thread::Current() == worker());
  worker()->ProcessMessages(talk_base::kForever);
}

void ClientSignalingThread::ResetMedia() {
  LOGI("ClientSignalingThread::ResetMedia");
  // Remove everyone from your roster
  if (xcs_.use_tls() == buzz::TLS_REQUIRED) {
    talk_base::CleanupSSL();
  }
  if (roster_) {
    roster_->clear();
  }
  if (buddy_list_) {
    SignalBuddyListReset();
    buddy_list_->clear();
  }

  sp_media_client_.reset(NULL);
}

void ClientSignalingThread::LoginS() {
  LOGI("ClientSignalingThread::LoginS");
  assert(talk_base::Thread::Current() == signal_thread_);

  if (xmpp_state_ != buzz::XmppEngine::STATE_CLOSED
      && xmpp_state_ != buzz::XmppEngine::STATE_NONE) {
    // Media client needs to be reinitialized, but since we're not forcing
    // STATE_CLOSED, by calling DisconnectS, it won't happen without this.
    ResetMedia();
  }

  talk_base::SocketAddress stun = talk_base::SocketAddress();
  if (stun_config_->stun.empty() || !stun.FromString(stun_config_->stun)) {
    stun.Clear();
    port_allocator_flags_ |= cricket::PORTALLOCATOR_DISABLE_STUN;
  }

  // TODO(Luke): Add option to force relay, ie DISABLE_UDP,DISABLE_TCP,
  // DISABLE_STUN
  sp_socket_factory_.reset(new talk_base::BasicPacketSocketFactory(worker()));
  sp_port_allocator_.reset(
          new cricket::BasicPortAllocator(sp_network_manager_.get(),
                                          sp_socket_factory_.get(), stun));

  talk_base::SocketAddress turn_socket = talk_base::SocketAddress();
  if (!stun_config_->turn.empty() &&
      turn_socket.FromString(stun_config_->turn)) {
    cricket::RelayCredentials credentials(stun_config_->turn_username,
                                          stun_config_->turn_password);
    cricket::RelayServerConfig relay_server(cricket::RELAY_TURN);
    relay_server.ports.push_back(cricket::ProtocolAddress(
        turn_socket, cricket::PROTO_UDP));
    relay_server.credentials = credentials;
    sp_port_allocator_->AddRelay(relay_server);
  } else {
    turn_socket.Clear();
  }

  if (port_allocator_flags_ != 0) {
    LOGI("LOGT ClientSignalingThread::ClientSignalingThread - "
      "setting port_allocator_flags_=%d", port_allocator_flags_);
    sp_port_allocator_->set_flags(port_allocator_flags_);
  }
  sp_session_manager_.reset(
      new cricket::SessionManager(sp_port_allocator_.get(), worker()));

  if (xcs_.use_tls() == buzz::TLS_REQUIRED) {
    talk_base::InitializeSSL();
  }

  sp_pump_.reset(new TXmppPump(this));
  sp_pump_->DoLogin(xcs_);
}

void ClientSignalingThread::DisconnectS() {
  LOGI("ClientSignalingThread::DisconnectS");
  assert(talk_base::Thread::Current() == signal_thread_);
  if (call_) {
    // TODO(Luke): Gate EndAllCalls whether this has already been called.
    // On a shutdown, it should only be called once otherwise, you'll
    // end up with asyncronous double deletes of call objects/SEGFAULT.
    EndAllCalls();
  }
  if (xmpp_state_ != buzz::XmppEngine::STATE_CLOSED &&
      xmpp_state_ != buzz::XmppEngine::STATE_NONE && sp_pump_.get() != NULL) {
    if (sp_pump_->AllChildrenDone()) {
      LOGE("AllChildrenDone NOT doing required "
              "{delete sp_pump_;sp_pump_ = new TXmppPump(this);} yet...");
    }
    sp_pump_->DoDisconnect();
  }
}

void ClientSignalingThread::CallS(const std::string &remoteJid) {
  LOGI("ClientSignalingThread::CallS");
  assert(talk_base::Thread::Current() == signal_thread_);

  cricket::Call* call;
  cricket::CallOptions options;
  options.is_muc = false;

#if XMPP_DISABLE_ROSTER
  // Just call whichever JID we get.
  buzz::Jid remote_jid(remoteJid);
  call = sp_media_client_->CreateCall();
  call->InitiateSession(remote_jid, sp_media_client_->jid(), options);
#else // Check the roster
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
    call = sp_media_client_->CreateCall();
    call->InitiateSession(found_jid, sp_media_client_->jid(), options);
  } else {
    LOGI("Could not find online friend '%s'", remoteJid.c_str());
  }
#endif  // !XMPP_DISABLE_ROSTER
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
      sp_media_client_->SetFocus(NULL);
      call_ = NULL;
    } else if (!hold) {
      sp_media_client_->SetFocus(call);
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
    sp_media_client_->SetFocus(call);
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
  std::string client_unique = sp_pump_->client()->jid().Str();
  talk_base::InitRandom(client_unique.c_str(), client_unique.size());

  // TODO(alex) We need to modify the last params of this to add TURN servers
  sp_session_manager_->SignalRequestSignaling.connect(this,
      &ClientSignalingThread::OnRequestSignaling);
  sp_session_manager_->SignalSessionCreate.connect(this,
      &ClientSignalingThread::OnSessionCreate);
  sp_session_manager_->OnSignalingReady();

  // This is deleted by the task runner.
  session_manager_task_ =
      new cricket::SessionManagerTask(sp_pump_->client(),
                                      sp_session_manager_.get());
  session_manager_task_->EnableOutgoingMessages();
  session_manager_task_->Start();

  sp_media_client_.reset(new cricket::MediaSessionClient(
                                      sp_pump_->client()->jid(),
                                      sp_session_manager_.get()));
  sp_media_client_->SignalCallCreate.connect(this,
      &ClientSignalingThread::OnCallCreate);
  sp_media_client_->SignalCallDestroy.connect(this,
      &ClientSignalingThread::OnCallDestroy);
#if ENABLE_SRTP
  sp_media_client_->set_secure(cricket::SEC_ENABLED);
#else
  sp_media_client_->set_secure(cricket::SEC_DISABLED);
#endif
}

void ClientSignalingThread::InitPresence() {
  LOGI("ClientSignalingThread::InitPresence");
  assert(talk_base::Thread::Current() == signal_thread_);

  my_status_.set_jid(sp_pump_->client()->jid());
  my_status_.set_available(true);
  my_status_.set_know_capabilities(true);
  my_status_.set_pmuc_capability(false);

#if XMPP_DISABLE_INCOMING_PRESENCE
  PresenceInPrivacy(STR_DENY);
#endif

#ifdef TUENTI_CUSTOM_BUILD
  //Set status to negative to not interfere with messaging clients.
  my_status_.set_priority(-25);
  //Set away so we don't interfere with custom activity logic
  my_status_.set_show(buzz::Status::SHOW_AWAY);
#else
  my_status_.set_show(buzz::Status::SHOW_ONLINE);
#endif

#if not XMPP_DISABLE_ROSTER
  presence_push_ = new buzz::PresencePushTask(sp_pump_->client());
  presence_push_->SignalStatusUpdate.connect(this,
      &ClientSignalingThread::OnStatusUpdate);
  presence_push_->Start();
#endif

  int capabilities = sp_media_client_->GetCapabilities();
  my_status_.set_voice_capability((capabilities & cricket::AUDIO_RECV) != 0);
  my_status_.set_video_capability((capabilities & cricket::VIDEO_RECV) != 0);
  my_status_.set_camera_capability((capabilities & cricket::VIDEO_SEND) != 0);

  presence_out_ = new buzz::PresenceOutTask(sp_pump_->client());
  presence_out_->Send(my_status_);
  presence_out_->Start();
}

void ClientSignalingThread::PresenceInPrivacy(const std::string& action){
  if (sp_pump_.get()){
    buzz::XmlElement* xmlblockpresence = new buzz::XmlElement(buzz::QN_IQ);
    xmlblockpresence->AddAttr(buzz::QN_TYPE, buzz::STR_SET);
    xmlblockpresence->AddElement(new buzz::XmlElement(buzz::QN_PRIVACY_QUERY, true));
    buzz::XmlElement* xmlprivacylist = new buzz::XmlElement(buzz::QN_PRIVACY_LIST, true);
    buzz::XmlElement* xmlprivacyitem = new buzz::XmlElement(buzz::QN_PRIVACY_ITEM, true);
    xmlprivacyitem->AddAttr(buzz::QN_ACTION, action);
    xmlprivacyitem->AddElement(new buzz::XmlElement(buzz::QN_PRIVACY_PRESENCE_IN, true));
    xmlprivacylist->AddElement(xmlprivacyitem);
    xmlblockpresence->AddElement(xmlprivacylist);
    sp_pump_->client()->SendStanza(xmlblockpresence);
  }
}

void ClientSignalingThread::InitPing() {
  LOGI("ClientSignalingThread::InitPing");
  assert(talk_base::Thread::Current() == signal_thread_);
  ping_ = new buzz::PingTask(sp_pump_->client(), talk_base::Thread::Current(),
      PingInterval, PingTimeout);
  ping_->SignalTimeout.connect(this, &ClientSignalingThread::OnPingTimeout);
  ping_->Start();
}

cricket::Call* ClientSignalingThread::GetCall(uint32 call_id) {
  const std::map<uint32, cricket::Call*>& calls = sp_media_client_->calls();
  for (std::map<uint32, cricket::Call*>::const_iterator i = calls.begin();
       i != calls.end(); ++i) {
    if (i->first == call_id) {
        return i->second;
    }
  }
  return NULL;
}

bool ClientSignalingThread::EndAllCalls() {
  assert(talk_base::Thread::Current() == signal_thread_);
  bool calls_processed = false;
  const std::map<uint32, cricket::Call*>& calls = sp_media_client_->calls();
  for (std::map<uint32, cricket::Call*>::const_iterator i = calls.begin();
       i != calls.end(); ++i) {
    EndCallS(i->first);
    calls_processed = true;
  }
  call_  = NULL;
  return calls_processed;
}
}  // namespace tuenti
