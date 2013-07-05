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
#ifndef CLIENT_CLIENTSIGNALINGTHREAD_H_
#define CLIENT_CLIENTSIGNALINGTHREAD_H_

#include <string>
#include <map>

#include "talk/base/signalthread.h"
#include "talk/base/constructormagic.h"
#include "talk/base/thread.h"
#include "talk/base/sigslot.h"
#include "talk/p2p/base/session.h"  // Needed for enum cricket::Session::State
#include "talk/media/base/mediachannel.h"  // Needed for enum cricket::ReceiveDataParams
#include "talk/p2p/base/basicpacketsocketfactory.h"
#include "talk/base/criticalsection.h"
#include "talk/base/scoped_ptr.h"
#include "talk/base/physicalsocketserver.h"
#include "talk/xmpp/pingtask.h"
#include "talk/xmpp/rostermodule.h"
#include "talk/xmpp/xmppengine.h"

#include "client/client_defines.h"
#include "client/xmppmessage.h"
#include "client/sendmessagetask.h"
#include "client/receivemessagetask.h"
#include "client/rosterhandler.h"
#include "client/keepalivetask.h"
#include "client/status.h"
#include "client/txmpppump.h"  // Needed for TXmppPumpNotify

#ifdef IOS_XMPP_FRAMEWORK
#include "VoiceClientExample/VoiceClientDelegate.h"
#endif

namespace talk_base {
class BasicNetworkManager;
}
namespace cricket {
class DataEngine;
class BasicPortAllocator;
class Session;
class SessionManager;
class SessionManagerTask;
class Call;
class MediaSessionClient;
class MediaEngineInterface;
}
namespace buzz {
class Status;
class Jid;
class XmppClient;
class PresenceOutTask;
}

namespace tuenti {

#ifndef IOS_XMPP_FRAMEWORK
struct StunConfig {
  std::string stun;
  std::string turn;
  std::string turn_username;
  std::string turn_password;
  std::string ToString() {
    std::stringstream stream;
    stream << "[stun=(" << stun << "),";
    stream << "turn=(" << turn << ")]";
    return stream.str();
  }
};
#endif
class TXmppPump;
class VoiceClientNotify;

enum ClientSignals {
  // From Main to Worker
  // ST_MSG_WORKER_DONE is defined in SignalThread.h
  MSG_LOGIN = talk_base::SignalThread::ST_MSG_FIRST_AVAILABLE,
  MSG_LOGIN_TIMEOUT,
  MSG_DISCONNECT,  // Logout
  MSG_CALL,
  MSG_ACCEPT_CALL,
  MSG_SEND_XMPP_MESSAGE,
  MSG_HOLD_CALL,
  MSG_DECLINE_CALL,
  MSG_MUTE_CALL,
  MSG_END_CALL,
  MSG_PRINT_STATS,
  MSG_REPLACE_TURN,

  // From Worker to Main
  MSG_XMPP_STATE,
  MSG_XMPP_ERROR,
  MSG_XMPP_SOCKET_CLOSE,
  MSG_CALL_STATE,
  MSG_CALL_ERROR,
  MSG_CONNECTION_ERROR,
  MSG_INCOMING_MESSAGE,
  MSG_ROSTER_ADD,
  MSG_ROSTER_REMOVE,
  MSG_PRESENCE_CHANGED
};

#if LOGGING
struct ClientSignalingMap : std::map<unsigned int, std::string>
{
  ClientSignalingMap()
  {
  this->operator[]( MSG_LOGIN ) = "MSG_LOGIN";
  this->operator[]( MSG_LOGIN_TIMEOUT ) = "MSG_LOGIN_TIMEOUT";
  this->operator[]( MSG_DISCONNECT ) = "MSG_DISCONNECT";
  this->operator[]( MSG_CALL ) = "MSG_CALL";
  this->operator[]( MSG_ACCEPT_CALL ) = "MSG_ACCEPT_CALL";
  this->operator[]( MSG_SEND_XMPP_MESSAGE ) = "MSG_SEND_XMPP_MESSAGE";
  this->operator[]( MSG_HOLD_CALL ) = "MSG_HOLD_CALL";
  this->operator[]( MSG_DECLINE_CALL ) = "MSG_DECLINE_CALL";
  this->operator[]( MSG_MUTE_CALL ) = "MSG_MUTE_CALL";
  this->operator[]( MSG_END_CALL ) = "MSG_END_CALL";
  this->operator[]( MSG_PRINT_STATS ) = "MSG_PRINT_STATS";
  this->operator[]( MSG_REPLACE_TURN ) = "MSG_REPLACE_TURN";
  this->operator[]( MSG_XMPP_STATE ) = "MSG_XMPP_STATE";
  this->operator[]( MSG_XMPP_ERROR ) = "MSG_XMPP_ERROR";
  this->operator[]( MSG_XMPP_SOCKET_CLOSE ) = "MSG_XMPP_SOCKET_CLOSE";
  this->operator[]( MSG_CALL_STATE ) = "MSG_CALL_STATE";
  this->operator[]( MSG_CALL_ERROR ) = "MSG_CALL_ERROR";
  this->operator[]( MSG_CONNECTION_ERROR ) = "MSG_CONNECTION_ERROR";
  this->operator[]( MSG_INCOMING_MESSAGE ) = "MSG_INCOMING_MESSAGE";
  this->operator[]( MSG_ROSTER_ADD ) = "MSG_ROSTER_ADD";
  this->operator[]( MSG_ROSTER_REMOVE ) = "MSG_ROSTER_REMOVE";
  this->operator[]( MSG_PRESENCE_CHANGED ) = "MSG_PRESENCE_CHANGED";
  };
  ~ClientSignalingMap(){};
};
struct XmppEngineErrorMap : std::map<unsigned int, std::string>
{
  XmppEngineErrorMap()
  {
  this->operator[]( buzz::XmppEngine::ERROR_NONE ) = "ERROR_NONE - No error";
  this->operator[]( buzz::XmppEngine::ERROR_XML )
      = "ERROR_XML - Malformed XML or encoding error";
  this->operator[]( buzz::XmppEngine::ERROR_STREAM )
      = "ERROR_STREAM - XMPP stream error - see GetStreamError()";
  this->operator[]( buzz::XmppEngine::ERROR_VERSION )
      = "ERROR_VERSION - XMPP version error";
  this->operator[]( buzz::XmppEngine::ERROR_UNAUTHORIZED )
      = "ERROR_UNAUTHORIZED -  User is not authorized (rejected credentials)";
  this->operator[]( buzz::XmppEngine::ERROR_TLS )
      = "ERROR_TLS - TLS could not be negotiated";
  this->operator[]( buzz::XmppEngine::ERROR_AUTH )
      = "ERROR_AUTH - Authentication could not be negotiated";
  this->operator[]( buzz::XmppEngine::ERROR_BIND )
      = "ERROR_BIND - Resource or session binding could not be negotiated";
  this->operator[]( buzz::XmppEngine::ERROR_CONNECTION_CLOSED )
      = "ERROR_CONNECTION_CLOSED - Connection closed by output handler.";
  this->operator[]( buzz::XmppEngine::ERROR_DOCUMENT_CLOSED )
      = "ERROR_DOCUMENT_CLOSED - Closed by </stream:stream>";
  this->operator[]( buzz::XmppEngine::ERROR_SOCKET )
      = "ERROR_SOCKET - Socket error";
  this->operator[]( buzz::XmppEngine::ERROR_NETWORK_TIMEOUT )
      = "ERROR_NETWORK_TIMEOUT - Some sort of timeout (eg., we never got the roster)";
  this->operator[]( buzz::XmppEngine::ERROR_MISSING_USERNAME )
      = "ERROR_MISSING_USERNAME - User has a Google Account but no nickname";  };
  ~XmppEngineErrorMap(){};
};
struct CallSessionMap : std::map<unsigned int, std::string>
{
  CallSessionMap()
  {
  this->operator[]( cricket::Session::STATE_INIT ) = "STATE_INIT";
  this->operator[]( cricket::Session::STATE_SENTINITIATE )
      = "STATE_SENTINITIATE - sent initiate, waiting for Accept or Reject";
  this->operator[]( cricket::Session::STATE_RECEIVEDINITIATE )
      = "STATE_RECEIVEDINITIATE - received an initiate. Call Accept or Reject";
  this->operator[]( cricket::Session::STATE_RECEIVEDINITIATE_ACK )
      = "STATE_RECEIVEDINITIATE_ACK - received an initiate ack. Client is alive.";
  this->operator[]( cricket::Session::STATE_SENTPRACCEPT )
      = "STATE_SENTPRACCEPT - sent provisional Accept";
  this->operator[]( cricket::Session::STATE_SENTACCEPT )
      = "STATE_SENTACCEPT - sent accept. begin connecting transport";
  this->operator[]( cricket::Session::STATE_RECEIVEDPRACCEPT )
      = "STATE_RECEIVEDPRACCEPT - received provisional Accept, waiting for Accept";
  this->operator[]( cricket::Session::STATE_RECEIVEDACCEPT )
      = "STATE_RECEIVEDACCEPT - received accept. begin connecting transport";
  this->operator[]( cricket::Session::STATE_SENTMODIFY )
      = "STATE_SENTMODIFY - sent modify, waiting for Accept or Reject";
  this->operator[]( cricket::Session::STATE_RECEIVEDMODIFY )
      = "STATE_RECEIVEDMODIFY - received modify, call Accept or Reject";
  this->operator[]( cricket::Session::STATE_SENTBUSY )
      = "STATE_SENTBUSY - sent busy after receiving initiate";
  this->operator[]( cricket::Session::STATE_SENTREJECT )
      = "STATE_SENTREJECT - sent reject after receiving initiate";
  this->operator[]( cricket::Session::STATE_RECEIVEDBUSY )
      = "STATE_RECEIVEDBUSY - received busy after sending initiate";
  this->operator[]( cricket::Session::STATE_RECEIVEDREJECT )
      = "STATE_RECEIVEDREJECT - received reject after sending initiate";
  this->operator[]( cricket::Session::STATE_SENTREDIRECT )
      = "STATE_SENTREDIRECT - sent direct after receiving initiate";
  this->operator[]( cricket::Session::STATE_SENTTERMINATE )
      = "STATE_SENTTERMINATE - sent terminate (any time / either side)";
  this->operator[]( cricket::Session::STATE_RECEIVEDTERMINATE )
      = "STATE_RECEIVEDTERMINATE - received terminate (any time / either side)";
  this->operator[]( cricket::Session::STATE_INPROGRESS )
      = "STATE_INPROGRESS - session accepted and in progress";
  this->operator[]( cricket::Session::STATE_DEINIT )
      = "STATE_DEINIT - session is being destroyed";
  };
  ~CallSessionMap(){};
};
struct CallSessionErrorMap : std::map<unsigned int, std::string>
{
  CallSessionErrorMap()
  {
  this->operator[]( cricket::Session::ERROR_NONE ) = "ERROR_NONE - No error";
  this->operator[]( cricket::Session::ERROR_TIME )
      = "ERROR_TIME - no response to signaling";
  this->operator[]( cricket::Session::ERROR_RESPONSE )
      = "ERROR_RESPONSE - error during signaling";
  this->operator[]( cricket::Session::ERROR_NETWORK )
      = "ERROR_NETWORK - network error, could not allocate network resources";
  this->operator[]( cricket::Session::ERROR_CONTENT )
      = "ERROR_CONTENT -  channel errors in SetLocalContent/SetRemoteContent";
  this->operator[]( cricket::Session::ERROR_TRANSPORT )
      = "ERROR_TRANSPORT - transport error of some kind";
  this->operator[]( cricket::Session::ERROR_ACK_TIME )
      = "ERROR_ACK_TIME - no ack response to signaling, client not available";
  }
  ~CallSessionErrorMap(){};
};
#endif


//TODO: No longer accurate.
///////////////////////////////////////////////////////////////////////////////
// ClientSignalingThread - Derived from Base class SignalThread for worker
// threads.  The main thread should call
//  Start() to begin work, and then follow one of these models:
//   Normal: Wait for SignalWorkDone, and then call Release to destroy.
//   Cancellation: Call Release(true), to abort the worker thread.
//   Fire-and-forget: Call Release(false), which allows the thread to run to
//    completion, and then self-destruct without further notification.
//   Periodic tasks: Wait for SignalWorkDone, then eventually call Start()
//    again to repeat the task. When the instance isn't needed anymore,
//    call Release. DoWork, OnWorkStart and OnWorkStop are called again,
//    on a new thread.
//  The subclass should override DoWork() to perform the background task.  By
//   periodically calling ContinueWork(), it can check for cancellation.
//   OnWorkStart and OnWorkDone can be overridden to do pre- or post-work
//   tasks in the context of the main thread.
///////////////////////////////////////////////////////////////////////////////

class ClientSignalingThread
    : public talk_base::MessageHandler,
      public sigslot::has_slots<>,
      public TXmppPumpNotify {
 public:
#ifdef IOS_XMPP_FRAMEWORK
  ClientSignalingThread(VoiceClientDelegate* voiceClientDelegate);
#else
  ClientSignalingThread();
#endif
  virtual ~ClientSignalingThread();
  // Public Library Callbacks
  void OnSessionState(cricket::Call* call, cricket::Session* session,
                      cricket::Session::State state);
  void OnSessionError(cricket::Call* call, cricket::Session* session,
                      cricket::Session::Error error);
  void OnContactAdded(const std::string& jid, const std::string& nick, int available, int show);
  void OnPresenceChanged(const std::string& jid, int available, int show);
  // OnStateChange Needed by TXmppPumpNotify maybe better in another class
  void OnStateChange(buzz::XmppEngine::State state);
  void OnXmppSocketClose(int state);
  void OnXmppError(buzz::XmppEngine::Error error);
  void OnRequestSignaling();
  void OnSessionCreate(cricket::Session* session, bool initiate);
  void OnCallCreate(cricket::Call* call);
  void OnCallDestroy(cricket::Call* call);
  void OnMediaEngineTerminate();
  void OnPingTimeout();
  void OnAudioPlayout();
  void OnCallStatsUpdate(char *statsString);
  void Ping();
  // These are signal thread entry points that will be farmed
  // out to the worker equivilent functions
  void Login(const std::string &username, const std::string &password,
             StunConfig* stun_config, const std::string &xmpp_host,
             int xmpp_port, bool use_ssl, uint32 port_allocator_filter,
			 bool is_gtalk);
  void Disconnect();
  void Call(std::string remoteJid, std::string call_tracker_id);
  void AcceptCall(uint32 call_id);
  void DeclineCall(uint32 call_id, bool busy);
  void EndCall(uint32 call_id);
  void MuteCall(uint32 call_id, bool mute);
  void HoldCall(uint32 call_id, bool hold);
  void SendXmppMessage(const tuenti::XmppMessage m);
  void ReplaceTurn(const std::string &turn);
#if IOS_XMPP_FRAMEWORK
  talk_base::Thread* GetSignalThread() {
    return signal_thread_;
  }
#endif
  // signals
  sigslot::signal3<int, const char *, int> SignalCallStateChange;
  sigslot::signal2<int, const char *> SignalCallTrackerId;
  sigslot::signal2<int, int> SignalCallError;

  sigslot::signal1<int> SignalXmppError;
  sigslot::signal1<int> SignalXmppSocketClose;
  sigslot::signal1<int> SignalXmppStateChange;
  sigslot::signal1<const XmppMessage> SignalXmppMessage;

  sigslot::signal0<> SignalAudioPlayout;

  sigslot::signal1<const std::string&> SignalBuddyListRemove;
  sigslot::signal3<const std::string&, int, int> SignalPresenceChanged;
  sigslot::signal4<const std::string&, const std::string&, int, int> SignalBuddyListAdd;

  sigslot::signal1<const char *> SignalStatsUpdate;


 protected:
  virtual void OnMessage(talk_base::Message* message);

 private:
  // Worker methods
  void LoginS();
  void DisconnectS();
  void CallS(const std::string &remoteJid, const std::string &call_tracker_id);
  void MuteCallS(uint32 call_id, bool mute);
  void HoldCallS(uint32 call_id, bool hold);
  void AcceptCallS(uint32 call_id);
  void DeclineCallS(uint32 call_id, bool busy);
  void EndCallS(uint32 call_id);
  cricket::Call* GetCall(uint32 call_id);
  bool EndAllCalls();
  void PrintStatsS();
  void ReplaceTurnS(const std::string turn);
  void SendXmppMessageS(const tuenti::XmppMessage m);
  void OnConnected();
  void PresenceInPrivacy(const std::string &action);
  void OnIncomingMessage(const tuenti::XmppMessage msg);
  void OnIncomingMessageS(const tuenti::XmppMessage msg);

  // These should live inside of the TXmppPump
  void ResetMedia();
  void InitPresence();

  void SetPortAllocatorFilter(uint32 filter) { port_allocator_filter_ = filter; };

  std::string turn_username_;
  std::string turn_password_;

  talk_base::scoped_ptr<cricket::MediaSessionClient> sp_media_client_;
  talk_base::scoped_ptr<cricket::BasicPortAllocator> sp_port_allocator_;
  talk_base::scoped_ptr<talk_base::BasicPacketSocketFactory> sp_socket_factory_;
  talk_base::scoped_ptr<cricket::SessionManager> sp_session_manager_;
  talk_base::scoped_ptr<tuenti::TXmppPump> sp_pump_;
  talk_base::scoped_ptr<buzz::XmppRosterModule> sp_roster_module_;
  talk_base::scoped_ptr<tuenti::RosterHandler> sp_roster_handler_;
  talk_base::scoped_ptr<talk_base::BasicNetworkManager> sp_network_manager_;
  talk_base::scoped_ptr<talk_base::SSLIdentity> sp_ssl_identity_;

  talk_base::Thread *signal_thread_;
  talk_base::scoped_ptr<talk_base::AutoThread> main_thread_;
#ifdef IOS_XMPP_FRAMEWORK
  VoiceClientDelegate* voiceClientDelegate_;
#endif
  bool auto_accept_;
  buzz::PresenceOutTask* presence_out_;
  buzz::PingTask* ping_task_;
  KeepAliveTask * keepalive_task_;
  ReceiveMessageTask *receive_message_task_;
  cricket::SessionManagerTask *session_manager_task_;
  cricket::Call* call_;
  cricket::SecurePolicy sdes_policy_;
  cricket::SecurePolicy dtls_policy_;
  cricket::TransportProtocol transport_protocol_;
  uint32 port_allocator_flags_;
  uint32 port_allocator_filter_;
  bool use_ssl_;
  bool is_caller_;
  buzz::XmppEngine::State xmpp_state_;
  StunConfig *stun_config_;
  cricket::DataEngine *data_engine_;
  // use default constructors
  buzz::Status my_status_;
  buzz::XmppClientSettings xcs_;
  talk_base::PhysicalSocketServer pss_;
  talk_base::CriticalSection disconnect_cs_;
#if LOGGING
  ClientSignalingMap client_signal_map_debug_;
  XmppEngineErrorMap xmpp_error_map_debug_;
  CallSessionMap call_session_map_debug_;
  CallSessionErrorMap call_session_error_map_debug_;
#endif
  DISALLOW_COPY_AND_ASSIGN(ClientSignalingThread);
};

///////////////////////////////////////////////////////////////////////////////
}  // namespace tuenti

#endif  // CLIENT_CLIENTSIGNALINGTHREAD_H_
