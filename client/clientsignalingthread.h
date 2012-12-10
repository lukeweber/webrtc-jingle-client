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
#include "client/txmpppump.h"  // Needed for TXmppPumpNotify
#include "talk/p2p/base/session.h"  // Needed for enum cricket::Session::State
#include "talk/media/base/mediachannel.h"  // Needed for enum cricket::ReceiveDataParams
#include "talk/base/basicpacketsocketfactory.h"
#include "talk/base/scoped_ptr.h"
#include "talk/xmpp/pingtask.h"

#include "client/status.h"
#include "client/txmpppump.h"  // Needed for TXmppPumpNotify
#include "client/voiceclient.h"

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
class PresencePushTask;
class PresenceOutTask;
}

struct RosterItem {
  buzz::Jid jid;
  buzz::Status::Show show;
  std::string status;
  std::string nick;
};
namespace tuenti {
class TXmppPump;
class VoiceClientNotify;

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

class ClientSignalingThread: public talk_base::SignalThread,
    public TXmppPumpNotify {
 public:
  ClientSignalingThread(
      talk_base::Thread *signal_thread);
  virtual ~ClientSignalingThread();
  // Public Library Callbacks
  void OnSessionState(cricket::Call* call, cricket::Session* session,
                      cricket::Session::State state);
  void OnSessionError(cricket::Call* call, cricket::Session* session,
                      cricket::Session::Error error);
  void OnStatusUpdate(const buzz::Status& status);
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
  // These are signal thread entry points that will be farmed
  // out to the worker equivilent functions
  void Login(const std::string &username, const std::string &password,
             StunConfig* stun_config, const std::string &xmpp_host,
             int xmpp_port, bool use_ssl);
  void Disconnect();
  void Call(std::string remoteJid);
  void AcceptCall(uint32 call_id);
  void DeclineCall(uint32 call_id, bool busy);
  void EndCall(uint32 call_id);
  void MuteCall(uint32 call_id, bool mute);
  void HoldCall(uint32 call_id, bool hold);
  void Destroy();

  // signals
  sigslot::signal3<int, const char *, int> SignalCallStateChange;
  sigslot::signal2<int, int> SignalCallError;

  sigslot::signal1<int> SignalXmppError;
  sigslot::signal1<int> SignalXmppSocketClose;
  sigslot::signal1<int> SignalXmppStateChange;

  sigslot::signal0<> SignalAudioPlayout;
  sigslot::signal0<> SignalBuddyListReset;
  sigslot::signal1<const char *> SignalBuddyListRemove;
  sigslot::signal2<const char *, const char *> SignalBuddyListAdd;

 protected:
  virtual void OnMessage(talk_base::Message* message);
  // Context: Worker Thread.
  virtual void DoWork();

 private:
  // Worker methods
  void LoginS();
  void DisconnectS();
  void CallS(const std::string &remoteJid);
  void MuteCallS(uint32 call_id, bool mute);
  void HoldCallS(uint32 call_id, bool hold);
  void AcceptCallS(uint32 call_id);
  void DeclineCallS(uint32 call_id, bool busy);
  void EndCallS(uint32 call_id);
  cricket::Call* GetCall(uint32 call_id);
  bool EndAllCalls();
  void OnKeepAliveS();
  void ScheduleKeepAlive();
  void PresenceInPrivacy(const std::string &action);

  // These should live inside of the TXmppPump
  void InitMedia();
  void ResetMedia();
  void InitPresence();
  void InitPing();

  // data
  typedef std::map<std::string, RosterItem> RosterMap;
  typedef std::map<std::string, int> BuddyListMap;

  std::string turn_username_;
  std::string turn_password_;

  talk_base::scoped_ptr<cricket::MediaSessionClient> sp_media_client_;
  talk_base::scoped_ptr<cricket::BasicPortAllocator> sp_port_allocator_;
  talk_base::scoped_ptr<talk_base::BasicPacketSocketFactory> sp_socket_factory_;
  talk_base::scoped_ptr<cricket::SessionManager> sp_session_manager_;
  talk_base::scoped_ptr<tuenti::TXmppPump> sp_pump_;
  talk_base::scoped_ptr<talk_base::BasicNetworkManager> sp_network_manager_;

  talk_base::Thread *signal_thread_;
  RosterMap *roster_;
  BuddyListMap *buddy_list_;
  buzz::PresencePushTask* presence_push_;
  buzz::PresenceOutTask* presence_out_;
  buzz::PingTask* ping_;
  cricket::SessionManagerTask *session_manager_task_;
  cricket::Call* call_;
  uint32 port_allocator_flags_;
  bool use_ssl_;
  bool auto_accept_;
  buzz::XmppEngine::State xmpp_state_;
  StunConfig *stun_config_;
  cricket::DataEngine *data_engine_;
  // use default constructors
  buzz::Status my_status_;
  buzz::XmppClientSettings xcs_;
  DISALLOW_COPY_AND_ASSIGN(ClientSignalingThread);
};

///////////////////////////////////////////////////////////////////////////////
}  // namespace tuenti

#endif  // CLIENT_CLIENTSIGNALINGTHREAD_H_
