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
#include "client/client_defines.h"
#include "client/xmppmessage.h"
#include "client/clientsignalingthread.h"
#include "client/logging.h"
#include "client/receivemessagetask.h"
#include "client/keepalivetask.h"
#include "client/rosterhandler.h"
#include "client/sendmessagetask.h"
#include "client/presenceouttask.h"
#include "talk/base/logging.h"
#include "client/voiceclient.h"//TODO: Remove
#include "talk/base/signalthread.h"
#include "talk/base/ssladapter.h"
#include "talk/p2p/base/session.h"
#include "talk/session/media/call.h"
#include "talk/session/media/mediasessionclient.h"
#include "talk/p2p/base/sessionmanager.h"
#include "talk/p2p/base/portallocator.h"
#include "talk/p2p/client/basicportallocator.h"
#include "talk/p2p/client/sessionmanagertask.h"

#include "talk/xmllite/xmlelement.h"
#include "talk/xmpp/constants.h"
#include "talk/xmpp/rostermodule.h"

#ifdef IOS_XMPP_FRAMEWORK
#include "VoiceClientExample/IOSXmppClient.h"
#endif

namespace tuenti {

struct RosterData : talk_base::MessageData {
  RosterData(std::string jid, std::string nick, int available, int show)
    : jid_(jid),
    nick_(nick),
    available_(available),
    show_(show) {}
  std::string jid_;
  std::string nick_;
  int available_;
  int show_;
};

struct XmppMessageData : talk_base::MessageData {
  XmppMessageData(const tuenti::XmppMessage m) : m_(m) {}
  tuenti::XmppMessage m_;
};

struct CallErrorData : talk_base::MessageData {
  CallErrorData(const int error, const int call_id)
    : call_id_(call_id),
    error_(error) {}
  int call_id_;
  int error_;
};

struct CallStateData : talk_base::MessageData{
  CallStateData(const int state, const std::string jid, const int call_id )
    : call_id_(call_id),
    state_(state),
    jid_(jid) {}
  int call_id_;
  int state_;
  std::string jid_;
};

struct XmppStateData : talk_base::MessageData {
  XmppStateData(const buzz::XmppEngine::State &m ): state_(m) {}
  buzz::XmppEngine::State state_;
};

struct XmppSocketCloseState : talk_base::MessageData {
  XmppSocketCloseState(const int &m ): state_(m) {}
  int state_;
};

struct XmppEngineErrorData : talk_base::MessageData {
  XmppEngineErrorData(const buzz::XmppEngine::Error &m ): error_(m) {}
  buzz::XmppEngine::Error error_;
};

struct ClientSignalingData: public talk_base::MessageData {
  ClientSignalingData(uint32 call_id, bool b) :
      call_id_(call_id),
      b_(b) {}
  ClientSignalingData(std::string s) :
      s_(s){}
  ClientSignalingData(std::string s, std::string s2) :
      s_(s),
      s2_(s2){}
  ClientSignalingData(uint32 call_id) :
      call_id_(call_id) {}
  uint32 call_id_;
  std::string s_;
  std::string s2_;
  bool b_;
};

///////////////////////////////////////////////////////////////////////////////
// ClientSignalingThread
///////////////////////////////////////////////////////////////////////////////
#ifdef IOS_XMPP_FRAMEWORK
ClientSignalingThread::ClientSignalingThread(VoiceClientDelegate* voiceClientDelegate)
    : voiceClientDelegate_(voiceClientDelegate),
    auto_accept_(true),
#else
ClientSignalingThread::ClientSignalingThread()
    : auto_accept_(false),
#endif
    presence_out_(NULL),
    ping_task_(NULL),
    keepalive_task_(NULL),
    session_manager_task_(NULL),
    call_(NULL),
    port_allocator_flags_(0),
    port_allocator_filter_(0),
    use_ssl_(false),
    is_caller_(true),
    xmpp_state_(buzz::XmppEngine::STATE_NONE) {
  // int numRelayPorts = 0;
  LOGI("ClientSignalingThread::ClientSignalingThread");
  main_thread_.reset(new talk_base::AutoThread());
  main_thread_.get()->Start();
  signal_thread_ = new talk_base::Thread(&pss_);
  signal_thread_->Start();
#if LOGGING
  // Set debugging to verbose in libjingle if LOGGING on android.
  talk_base::LogMessage::LogToDebug(talk_base::LS_VERBOSE);
#endif
  sp_ssl_identity_.reset(NULL);
  transport_protocol_ = cricket::ICEPROTO_HYBRID;
#if ENABLE_SRTP
  sdes_policy_ = cricket::SEC_ENABLED;
  dtls_policy_ = cricket::SEC_ENABLED;
#else
  dtls_policy_ = cricket::SEC_DISABLED;
  sdes_policy_ = cricket::SEC_DISABLED;
#endif
  sp_network_manager_.reset(new talk_base::BasicNetworkManager());
  my_status_.set_caps_node("http://github.com/lukeweber/webrtc-jingle");
  my_status_.set_version("1.0-SNAPSHOT");
}

ClientSignalingThread::~ClientSignalingThread() {
  LOGI("ClientSignalingThread::~ClientSignalingThread");
  Disconnect();
  main_thread_.reset(NULL);
  delete signal_thread_;
}

void ClientSignalingThread::OnContactAdded(const std::string& jid, const std::string& nick,
    int available, int show) {
  main_thread_->Post(this, MSG_ROSTER_ADD, new RosterData(jid, nick, available, show));
}

void ClientSignalingThread::OnPresenceChanged(const std::string& jid,
    int available, int show) {
  main_thread_->Post(this, MSG_PRESENCE_CHANGED, new RosterData(jid, "", available, show));
}

void ClientSignalingThread::OnSessionState(cricket::Call* call,
    cricket::Session* session, cricket::Session::State state) {
  const char *trackerIdStr = NULL;
#if LOGGING
  LOG(LS_INFO) << "ClientSignalingThread::OnSessionState "
               << call_session_map_debug_[state];
#endif
  assert(talk_base::Thread::Current() == signal_thread_);
  switch (state) {
  default:
    //
    break;
  case cricket::Session::STATE_RECEIVEDINITIATE:
    {
    buzz::Jid jid(session->remote_name());
    LOG(LS_INFO) << "Incoming call from " << jid.Str() << " call_id  "
                 << call->id();
    if (auto_accept_) {
      AcceptCall(call->id());
    }
    trackerIdStr = call->sessions()[0]->call_tracker_id().c_str();
    break;
    }
  case cricket::Session::STATE_RECEIVEDACCEPT:
  case cricket::Session::STATE_SENTACCEPT:
    //Last accept has focus
    call_ = call;
    sp_media_client_->SetFocus(call);
#if LOGGING
    signal_thread_->PostDelayed(1000, this, MSG_PRINT_STATS);
#endif
    break;
  case cricket::Session::STATE_INPROGRESS:
    call->StartSpeakerMonitor(session);
    break;
  }

  std::string jid_str = "";
  //Session has already been terminated, so lets not do this.
  if (!(state == cricket::Session::STATE_RECEIVEDTERMINATE || state == cricket::Session::STATE_DEINIT)) {
    jid_str = session->remote_name();
  }
  main_thread_->Post(this, MSG_CALL_STATE, new CallStateData(state, jid_str, call->id()));
  if(trackerIdStr) {
    SignalCallTrackerId(call->id(), trackerIdStr);
  }
}

void ClientSignalingThread::OnSessionError(cricket::Call* call,
    cricket::Session* session, cricket::Session::Error error) {
#if LOGGING
  LOG(LS_INFO) << "ClientSignalingThread::OnSessionError "
               << call_session_error_map_debug_[error];
#endif
  main_thread_->Post(this, MSG_CALL_ERROR, new CallErrorData(error, call->id()));
}

void ClientSignalingThread::OnXmppError(buzz::XmppEngine::Error error) {
#if LOGGING
  LOG(LS_INFO) << "ClientSignalingThread::OnXmppError: "
               << xmpp_error_map_debug_[error];
#endif
  main_thread_->Post(this, MSG_XMPP_ERROR, new XmppEngineErrorData(error));
}

void ClientSignalingThread::OnXmppSocketClose(int state) {
#if LOGGING
  LOG(LS_INFO) << "ClientSignalingThread::OnXmppSocketClose: "
               << "with state=" << state;
#endif
  main_thread_->Post(this, MSG_XMPP_SOCKET_CLOSE, new XmppSocketCloseState(state));
}

void ClientSignalingThread::OnStateChange(buzz::XmppEngine::State state) {
  assert(talk_base::Thread::Current() == signal_thread_);
  if (state == buzz::XmppEngine::STATE_OPEN)
    OnConnected();
  if (state == buzz::XmppEngine::STATE_CLOSED)
    ResetMedia();

  xmpp_state_ = state;
  main_thread_->Post(this, MSG_XMPP_STATE, new XmppStateData(state));
}

void ClientSignalingThread::OnConnected(){
  assert(talk_base::Thread::Current() == signal_thread_);
  //We logged in, clear the LOGIN_TIMEOUT
  signal_thread_->Clear(this, MSG_LOGIN_TIMEOUT);
  std::string client_unique = sp_pump_->client()->jid().Str();
  talk_base::InitRandom(client_unique.c_str(), client_unique.size());
#if LOGGING
  LOG(LS_INFO) << "ClientSignalingThread::OnConnected with VoiceJid = " << client_unique;
#endif

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

#if XMPP_CHAT_ENABLED
  receive_message_task_ = new ReceiveMessageTask(sp_pump_->client(), buzz::XmppEngine::HL_ALL);
  receive_message_task_->SignalIncomingXmppMessage.connect(this, &ClientSignalingThread::OnIncomingMessage);
  receive_message_task_->Start();
#endif
  sp_media_client_.reset(new cricket::MediaSessionClient(
                                      sp_pump_->client()->jid(),
                                      sp_session_manager_.get()));
  sp_media_client_->SignalCallCreate.connect(this,
      &ClientSignalingThread::OnCallCreate);
  sp_media_client_->SignalCallDestroy.connect(this,
      &ClientSignalingThread::OnCallDestroy);
  sp_media_client_->set_secure(sdes_policy_);
  InitPresence();

#if XMPP_WHITESPACE_KEEPALIVE_ENABLED
  keepalive_task_ = new KeepAliveTask(sp_pump_->client(), signal_thread_,
      XmppKeepAliveInterval);
  keepalive_task_->Start();
#endif
#if XMPP_PING_ENABLED
  assert(talk_base::Thread::Current() == signal_thread_);
  ping_task_ = new buzz::PingTask(sp_pump_->client(), talk_base::Thread::Current(),
      PingInterval, PingTimeout);
  ping_task_->SignalTimeout.connect(this, &ClientSignalingThread::OnPingTimeout);
  ping_task_->Start();
#endif
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
#ifdef XMPP_COMPATIBILITY
  session->set_current_protocol(cricket::PROTOCOL_JINGLE);
#else
  //This duplicates the jingle to also include a gingle version
  //in session. The downside is that there are two stanzas in a
  //iq type=set, which violates xmpp standards and will fail on
  //modern chat servers that enforce these standards. I assume
  //google might require gingle, or at least need it for older
  //clients, but haven't tested.
  session->set_current_protocol(cricket::PROTOCOL_HYBRID);
#endif
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
  Disconnect();
}

void ClientSignalingThread::OnCallStatsUpdate(char *stats) {
  LOGI("ClientSignalingThread::OnCallStatsUpdate");
}

// ================================================================
// THESE ARE THE ONLY FUNCTIONS THAT CAN BE CALLED USING ANY THREAD
// vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
void ClientSignalingThread::Login(const std::string &username,
    const std::string &password, StunConfig* stun_config,
    const std::string &xmpp_host, int xmpp_port, bool use_ssl,
    uint32 port_allocator_filter, bool isGtalk) {
  LOGI("ClientSignalingThread::Login");

  stun_config_ = stun_config;

  buzz::Jid jid = buzz::Jid(username);
  talk_base::InsecureCryptStringImpl pass;
  pass.password() = password;
#if ENABLE_SRTP
  sp_ssl_identity_.reset(talk_base::SSLIdentity::Generate(jid.Str()));
#else
  sp_ssl_identity_.reset(NULL);
#endif
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
  xcs_.set_allow_gtalk_username_custom_domain(isGtalk);
  SetPortAllocatorFilter(port_allocator_filter);
  signal_thread_->Post(this, MSG_LOGIN);
}

void ClientSignalingThread::Disconnect() {
  LOGI("ClientSignalingThread::Disconnect");
  signal_thread_->Post(this, MSG_DISCONNECT);
}


void ClientSignalingThread::Call(std::string remoteJid, std::string call_tracker_id) {
  LOGI("ClientSignalingThread::Call");
  signal_thread_->Post(this, MSG_CALL, new ClientSignalingData(remoteJid, call_tracker_id));
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

void ClientSignalingThread::SendXmppMessage(const tuenti::XmppMessage m) {
  signal_thread_->Post(this, MSG_SEND_XMPP_MESSAGE, new XmppMessageData(m));
}

void ClientSignalingThread::EndCall(uint32 call_id) {
  LOGI("ClientSignalingThread::EndCall %d", call_id);
  signal_thread_->Post(this, MSG_END_CALL, new ClientSignalingData(call_id));
}

void ClientSignalingThread::ReplaceTurn(const std::string &turn) {
  LOGI("ClientSignalingThread::ReplaceTurn");
  signal_thread_->Post(this, MSG_REPLACE_TURN, new ClientSignalingData(turn));
}

// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
// THESE ARE THE ONLY FUNCTIONS THAT CAN BE CALLED USING ANY THREAD
// ================================================================

void ClientSignalingThread::OnMessage(talk_base::Message* message) {
  ClientSignalingData* data;
#if LOGGING
  LOG(LS_INFO) << "ClientSignalingThread::OnMessage: "
               << client_signal_map_debug_[message->message_id];
#endif
  switch (message->message_id) {

  // ------> Events on Signaling Thread <------
  case MSG_LOGIN_TIMEOUT:
    DisconnectS();
    break;
  case MSG_LOGIN:
    LoginS();
    break;
  case MSG_SEND_XMPP_MESSAGE:
    SendXmppMessageS(static_cast<XmppMessageData*>(message->pdata)->m_);
    delete message->pdata;
    break;
  case MSG_DISCONNECT:
    DisconnectS();
    break;
  case MSG_CALL:
    CallS(static_cast<ClientSignalingData*>(message->pdata)->s_,
          static_cast<ClientSignalingData*>(message->pdata)->s2_);
    delete message->pdata;
    break;
  case MSG_MUTE_CALL:
    data = static_cast<ClientSignalingData*>(message->pdata);
    MuteCallS(data->call_id_, data->b_);
    delete message->pdata;
    break;
  case MSG_ACCEPT_CALL:
    AcceptCallS(static_cast<ClientSignalingData*>(message->pdata)->call_id_);
    delete message->pdata;
    break;
  case MSG_DECLINE_CALL:
    data = static_cast<ClientSignalingData*>(message->pdata);
    DeclineCallS(data->call_id_, data->b_);
    delete message->pdata;
    break;
  case MSG_HOLD_CALL:
    data = static_cast<ClientSignalingData*>(message->pdata);
    HoldCallS(data->call_id_, data->b_);
    delete message->pdata;
    break;
  case MSG_END_CALL:
    EndCallS(static_cast<ClientSignalingData*>(message->pdata)->call_id_);
    delete message->pdata;
    break;
  case MSG_PRINT_STATS:
    if (call_){//If there's no call, skip
      PrintStatsS();
      signal_thread_->PostDelayed(1000, this, MSG_PRINT_STATS);
    }
    break;
  case MSG_REPLACE_TURN:
    data = static_cast<ClientSignalingData*>(message->pdata);
    ReplaceTurnS(data->s_);
    delete message->pdata;
    break;
  // ------> Events on Main Thread <------
  case MSG_XMPP_STATE:
    assert(talk_base::Thread::Current() == main_thread_.get());
    SignalXmppStateChange(static_cast<XmppStateData*>(message->pdata)->state_);
    delete message->pdata;
    break;
  case MSG_XMPP_ERROR:
    assert(talk_base::Thread::Current() == main_thread_.get());
    SignalXmppError(static_cast<XmppEngineErrorData*>(message->pdata)->error_);
    delete message->pdata;
    break;
  case MSG_XMPP_SOCKET_CLOSE:
    assert(talk_base::Thread::Current() == main_thread_.get());
    SignalXmppSocketClose(static_cast<XmppSocketCloseState*>(message->pdata)->state_);
    delete message->pdata;
    break;
  case MSG_CALL_ERROR:
    {
    assert(talk_base::Thread::Current() == main_thread_.get());
    CallErrorData* cd = static_cast<CallErrorData*>(message->pdata);
    SignalCallError(cd->error_, cd->call_id_);
    delete message->pdata;
    break;
    }
  case MSG_INCOMING_MESSAGE:
    {
    assert(talk_base::Thread::Current() == main_thread_.get());
    SignalXmppMessage(static_cast<XmppMessageData*>(message->pdata)->m_);
    delete message->pdata;
    break;
    }
  case MSG_ROSTER_REMOVE:
    {
    assert(talk_base::Thread::Current() == main_thread_.get());
    //SignalBuddyListRemove(static_cast<RosterData*>(message->pdata)->jid_.c_str());
    delete message->pdata;
    break;
    }
  case MSG_PRESENCE_CHANGED:
    {
        assert(talk_base::Thread::Current() == main_thread_.get());
        RosterData *rd = static_cast<RosterData*>(message->pdata);
        SignalPresenceChanged(rd->jid_, rd->available_, rd->show_);
        delete message->pdata;
        break;
    }
  case MSG_ROSTER_ADD:
    {
    assert(talk_base::Thread::Current() == main_thread_.get());
    RosterData *rd = static_cast<RosterData*>(message->pdata);
    SignalBuddyListAdd(rd->jid_, rd->nick_, rd->available_, rd->show_);
    delete message->pdata;
    break;
    }
  case MSG_CALL_STATE:
    {
    assert(talk_base::Thread::Current() == main_thread_.get());
    CallStateData *csd = static_cast<CallStateData*>(message->pdata);
    SignalCallStateChange(csd->state_, csd->jid_.c_str(), csd->call_id_);
    delete message->pdata;
    break;
    }
  default:
    //
    break;
  }
}

void ClientSignalingThread::ResetMedia() {
  LOGI("ClientSignalingThread::ResetMedia");
  // Remove everyone from your roster
  if (xcs_.use_tls() == buzz::TLS_REQUIRED) {
    talk_base::CleanupSSL();
  }

#if XMPP_ENABLE_ROSTER
  sp_roster_module_.reset(NULL);
#endif
  sp_media_client_.reset(NULL);

  //Need to delete this after it dies, so that we kill running tasks
  sp_pump_.reset(NULL);
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

  //ICEPROTO_RFC5245 - Requires that all ports share the same passwords
  if (transport_protocol_ == cricket::ICEPROTO_RFC5245){
    port_allocator_flags_ |= cricket::PORTALLOCATOR_ENABLE_SHARED_UFRAG;
  }

  sp_socket_factory_.reset(new talk_base::BasicPacketSocketFactory(signal_thread_));
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
  if (port_allocator_filter_ != cricket::kDefaultPortAllocatorFilter) {
    sp_port_allocator_->set_filter(port_allocator_filter_);
  }
  sp_session_manager_.reset(
      new cricket::SessionManager(sp_port_allocator_.get(), signal_thread_));
  sp_session_manager_->set_secure(dtls_policy_);
  sp_session_manager_->set_identity(sp_ssl_identity_.get());
  sp_session_manager_->set_transport_protocol(transport_protocol_);

  if (xcs_.use_tls() == buzz::TLS_REQUIRED) {
    talk_base::InitializeSSL();
  }

#if XMPP_ENABLE_ROSTER
  //RosterModule depends on engine, Engine is destroyed/created in TxmppPump via client.
  sp_roster_module_.reset();
#endif
#ifdef IOS_XMPP_FRAMEWORK
  sp_pump_.reset(new TXmppPump(this, voiceClientDelegate_));
#else
  sp_pump_.reset(new TXmppPump(this));
#endif
  signal_thread_->PostDelayed(LoginTimeout, this, MSG_LOGIN_TIMEOUT);
  sp_pump_->DoLogin(xcs_);
}

void ClientSignalingThread::SendXmppMessageS(const tuenti::XmppMessage m) {
  assert(talk_base::Thread::Current() == signal_thread_);
  SendMessageTask * smt = new SendMessageTask(sp_pump_.get()->client());
  smt->Send(m);
  smt->Start();
}

void ClientSignalingThread::DisconnectS() {
  LOGI("ClientSignalingThread::DisconnectS");
  assert(talk_base::Thread::Current() == signal_thread_);
  talk_base::CritScope lock(&disconnect_cs_);
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
  sp_session_manager_.reset(NULL);
  sp_socket_factory_.reset(NULL);
}

void ClientSignalingThread::Ping(){
#if XMPP_PING_ENABLED
  if (ping_task_ != NULL) {
    ping_task_->PingNow();
  }
#endif
}

void ClientSignalingThread::CallS(const std::string &remoteJid, const std::string &call_tracker_id) {
  LOGI("ClientSignalingThread::CallS");
  assert(talk_base::Thread::Current() == signal_thread_);
  is_caller_ = true;
  cricket::Call* call;
  cricket::CallOptions options;
  options.is_muc = false;
  bool found = false;

  buzz::Jid callto_jid(remoteJid);
  buzz::Jid found_jid;

#if XMPP_ENABLE_ROSTER
  // TODO: Move this logic to the clients to allow calling all endpoints at the
  // same time, and following the first that answers, and tearing down the rest.
  // now search available presences
  for (unsigned int i = 0; i < sp_roster_module_->GetIncomingPresenceCount(); i++) {
      const buzz::XmppPresence *presence = sp_roster_module_->GetIncomingPresence(i);
      if (presence->available() == buzz::XMPP_PRESENCE_AVAILABLE
              && presence->jid().BareEquals(callto_jid)) {
          found_jid = presence->jid();
          found = true;
          break;
      }
  }
#endif

  // If we have roster disabled, we can pass pass a full jid directly to call.
  if (!found && !callto_jid.IsBare()){
    found_jid = callto_jid;
    found = true;
  }

  if (found) {
    LOGI("Calling friend '%s'", found_jid.Str().c_str());
    call = sp_media_client_->CreateCall();
    call->InitiateSession(found_jid, sp_media_client_->jid(), options, call_tracker_id);
  } else {
#if !XMPP_ENABLE_ROSTER
    LOGE("Can not call a bare jid, enable roster or call a full jid ex@blah.com/resource: '%s'", remoteJid.c_str());
#else
    LOGI("Could not find online friend '%s'", remoteJid.c_str());
#endif
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
    is_caller_ = false;
    cricket::CallOptions options;
    call->AcceptSession(call->sessions()[0], options);
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

void ClientSignalingThread::PrintStatsS() {
  if (call_ == NULL) {
    return;
  }
  const cricket::VoiceMediaInfo& vmi = call_->last_voice_media_info();
  std::ostringstream statsStream;
  bool fireSignal = false;
  if(is_caller_) {
    for (std::vector<cricket::VoiceSenderInfo>::const_iterator it =
         vmi.senders.begin(); it != vmi.senders.end(); ++it) {
      statsStream << "S_ssrc=" << it->ssrc;
      statsStream << "\nS_bytes_sent=" << it->bytes_sent;
      statsStream << "\nS_packets_sent=" << it->packets_sent;
      statsStream << "\nS_packets_lost=" << it->packets_lost;
      statsStream << "\nS_fraction_lost=" << it->fraction_lost;
      statsStream << "\nS_ext_seqnum=" << it->ext_seqnum;
      statsStream << "\nS_rtt_ms=" << it->rtt_ms;
      statsStream << "\nS_jitter_ms=" << it->jitter_ms;
      statsStream << "\nS_audio_level=" << it->audio_level;
      statsStream << "\nS_echo_delay_median_ms=" << it->echo_delay_median_ms;
      statsStream << "\nS_echo_delay_std_ms=" << it->echo_delay_std_ms;
      statsStream << "\nS_echo_return_loss=" << it->echo_return_loss;
      statsStream << "\nS_echo_return_loss_enhancement=" << it->echo_return_loss_enhancement;
      fireSignal = true;
    }
  } else {
    for (std::vector<cricket::VoiceReceiverInfo>::const_iterator it =
         vmi.receivers.begin(); it != vmi.receivers.end(); ++it) {
      statsStream << "\nR_ssrc=" << it->ssrc;
      statsStream << "\nR_bytes_rcvd=" << it->bytes_rcvd;
      statsStream << "\nR_packets_rcvd=" << it->packets_rcvd;
      statsStream << "\nR_packets_lost=" << it->packets_lost;
      statsStream << "\nR_fraction_lost=" << it->fraction_lost;
      statsStream << "\nR_ext_seqnum=" << it->ext_seqnum;
      statsStream << "\nR_jitter_ms=" << it->jitter_ms;
      statsStream << "\nR_jitter_buffer_ms=" << it->jitter_buffer_ms;
      statsStream << "\nR_jitter_buffer_preferred_ms=" << it->jitter_buffer_preferred_ms;
      statsStream << "\nR_delay_estimate_ms=" << it->delay_estimate_ms;
      statsStream << "\nR_audio_level=" << it->audio_level;
      statsStream << "\nR_expand_rate=" << it->expand_rate;
      fireSignal = true;
    }
  }
  if(fireSignal) {
    LOGI("TSTATS: %s", statsStream.str().c_str());
    SignalStatsUpdate(const_cast<const char *>(statsStream.str().c_str()));
  }
}

void ClientSignalingThread::ReplaceTurnS(const std::string turn) {
  LOG(INFO) << "ReplaceTurnS";
  talk_base::SocketAddress turn_socket = talk_base::SocketAddress();
  if (stun_config_ && !turn.empty() && !stun_config_->turn_username.empty() &&
       !stun_config_->turn_password.empty() && turn_socket.FromString(turn)) {
    LOG(INFO) << "ReplaceTurn From: " << stun_config_->ToString();
    stun_config_->turn = std::string(turn);
    LOG(INFO) << "ReplaceTurn To: " << stun_config_->ToString();
    cricket::RelayCredentials credentials(stun_config_->turn_username,
                                          stun_config_->turn_password);
    cricket::RelayServerConfig relay_server(cricket::RELAY_TURN);
    relay_server.ports.push_back(cricket::ProtocolAddress(
        turn_socket, cricket::PROTO_UDP));
    relay_server.credentials = credentials;
    sp_port_allocator_->ClearAllRelays();
    sp_port_allocator_->AddRelay(relay_server);
  } else {
    turn_socket.Clear();
  }
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

  sp_roster_handler_.reset(new tuenti::RosterHandler());
  sp_roster_handler_->SignalContactAdded.connect(this,
    &ClientSignalingThread::OnContactAdded);
  sp_roster_handler_->SignalPresenceChanged.connect(this,
    &ClientSignalingThread::OnPresenceChanged);

#if XMPP_ENABLE_ROSTER
  sp_roster_module_.reset(buzz::XmppRosterModule::Create());
  sp_roster_module_->set_roster_handler(sp_roster_handler_.get());
  sp_roster_module_->RegisterEngine(sp_pump_->client()->engine());
  sp_roster_module_->BroadcastPresence();//Empty presence to get things going.
  sp_roster_module_->RequestRosterUpdate();
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
    buzz::XmlElement* xmlblockpresence = NULL;
#ifdef TUENTI_CUSTOM_BUILD
    //<iq type='set' id='presin4'>
    xmlblockpresence = new buzz::XmlElement(buzz::QN_IQ);
    xmlblockpresence->AddAttr(buzz::QN_TYPE, buzz::STR_SET);
    xmlblockpresence->AddAttr(buzz::QN_ID, buzz::STR_PRESIN4);
    //<query xmlns='jabber:iq:privacy'>
    buzz::XmlElement* xmlblockquery = new buzz::XmlElement(buzz::QN_PRIVACY_QUERY);
    xmlblockquery->AddAttr(buzz::QN_XMLNS, buzz::NS_PRIVACY);
    //<active name='deny-all-presin-and-message'/>
    buzz::XmlElement* xmlblockactive = new buzz::XmlElement(buzz::QN_PRIVACY_ACTIVE);
    xmlblockactive->AddAttr(buzz::QN_NAME, buzz::STR_DENY_ALL_PRESIN_AND_MESSAGE);
    //Adding the elements
    xmlblockquery->AddElement(xmlblockactive);
    xmlblockpresence->AddElement(xmlblockquery);
#else
     xmlblockpresence = new buzz::XmlElement(buzz::QN_IQ);
     xmlblockpresence->AddAttr(buzz::QN_TYPE, buzz::STR_SET);
     xmlblockpresence->AddElement(new buzz::XmlElement(buzz::QN_PRIVACY_QUERY, true));
     buzz::XmlElement* xmlprivacylist = new buzz::XmlElement(buzz::QN_PRIVACY_LIST, true);
     buzz::XmlElement* xmlprivacyitem = new buzz::XmlElement(buzz::QN_PRIVACY_ITEM, true);
     xmlprivacyitem->AddAttr(buzz::QN_ACTION, action);
     xmlprivacyitem->AddElement(new buzz::XmlElement(buzz::QN_PRIVACY_PRESENCE_IN, true));
     xmlprivacylist->AddElement(xmlprivacyitem);
     xmlblockpresence->AddElement(xmlprivacylist);
#endif
    sp_pump_->client()->SendStanza(xmlblockpresence);
  }
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

void ClientSignalingThread::OnIncomingMessage(const tuenti::XmppMessage msg) {
  assert(talk_base::Thread::Current() == signal_thread_);
  main_thread_->Post(this, MSG_INCOMING_MESSAGE, new XmppMessageData(msg));
}

}  // namespace tuenti
