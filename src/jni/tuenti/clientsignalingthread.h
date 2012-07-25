#ifndef TUENTI_CLIENTSIGNALINGTHREAD_H_
#define TUENTI_CLIENTSIGNALINGTHREAD_H_

#include <string>
#include <map>

#include "talk/base/signalthread.h"
#include "talk/base/constructormagic.h"
#include "talk/base/thread.h"
#include "talk/base/sigslot.h"
#include "talk/examples/login/xmpppump.h"//Needed for XmppPumpNotify
#include "talk/p2p/base/session.h"//Needed for enum cricket::Session::State
#include "talk/session/phone/mediachannel.h"//Needed for enum cricket::ReceiveDataParams
#include "tuenti/status.h"

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
class XmppPump;

struct RosterItem {
    buzz::Jid jid;
    buzz::Status::Show show;
    std::string status;
};
namespace tuenti {

class VoiceClientNotify;

///////////////////////////////////////////////////////////////////////////////
// ClientSignalingThread - Derived from Base class SignalThread for worker threads.  The main thread should call
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

class ClientSignalingThread: public talk_base::SignalThread, public XmppPumpNotify {

public:

    ClientSignalingThread(VoiceClientNotify *notifier, talk_base::Thread *signal_thread);
    //Public Library Callbacks
    void OnSessionState(cricket::Call* call, cricket::Session* session, cricket::Session::State state);
    void OnStatusUpdate(const buzz::Status& status);
    void OnStateChange(buzz::XmppEngine::State state); //Needed by XmppPumpNotify maybe better in another class
    void OnDataReceived(cricket::Call*, const cricket::ReceiveDataParams& params, const std::string& data);
    void OnRequestSignaling();
    void OnSessionCreate(cricket::Session* session, bool initiate);
    void OnCallCreate(cricket::Call* call);
    void OnCallDestroy(cricket::Call* call);
    void OnMediaEngineTerminate();
    void OnPingTimeout();
    //These are signal thread entry points that will be farmed out to the worker equivilent functions
    void Login(const std::string &username, const std::string &password, const std::string &xmpp_host, int xmpp_port,
            bool use_ssl, const std::string &stun_host, int stun_port);
    void Disconnect();
    void Call(std::string &remoteJid);
    void AcceptCall();
    void DeclineCall();
    void EndCall();
    void Destroy();

protected:

    virtual ~ClientSignalingThread();
    virtual void OnMessage(talk_base::Message* message);
    // Context: Worker Thread.
    virtual void DoWork();

private:

    //Worker methods
    void LoginW();
    void DisconnectW();
    void CallW(const std::string &remoteJid);
    void AcceptCallW();
    void DeclineCallW();
    void EndCallW();

    //These should live inside of the XmppPump
    void InitMedia();
    void InitPresence();
    void InitPing();

    //data
    typedef std::map<std::string, RosterItem> RosterMap;

    VoiceClientNotify *notify_;
    talk_base::Thread *signal_thread_;
    XmppPump *pump_;
    talk_base::BasicNetworkManager *network_manager_;
    cricket::DataEngine *data_engine_;
    cricket::BasicPortAllocator *port_allocator_;
    cricket::SessionManager *session_manager_;
    cricket::SessionManagerTask* session_manager_task_;
    cricket::MediaSessionClient* media_client_;
    cricket::MediaEngineInterface* media_engine_;
    buzz::XmppClientSettings xcs_;

    cricket::Session *session_;
    cricket::Call* call_;
    bool incoming_call_;
    bool auto_accept_;

    buzz::Status my_status_;DISALLOW_COPY_AND_ASSIGN(ClientSignalingThread);
    buzz::PresencePushTask* presence_push_;
    buzz::PresenceOutTask* presence_out_;
    buzz::PingTask* ping_;
    RosterMap *roster_;
};

///////////////////////////////////////////////////////////////////////////////

}// namespace tuenti

#endif  // TUENTI_CLIENTSIGNALINGTHREAD_H_
