#ifndef TUENTI_VOICECLIENT_H_
#define TUENTI_VOICECLIENT_H_

#include <jni.h>
#include <string.h>

#include "talk/p2p/base/session.h"
#include "talk/session/phone/mediachannel.h"
#include "talk/session/phone/mediamessages.h"
#include "talk/session/phone/mediasessionclient.h"
#include "talk/xmpp/xmppclient.h"
#include "talk/examples/login/xmpppump.h"


#include "tuenti/status.h"

namespace buzz {
class PresencePushTask;
class PresenceOutTask;
class DiscoInfoQueryTask;
class Status;
class IqTask;
class XmlElement;
}

namespace talk_base {
class Thread;
class SignalThread;
class NetworkManager;
}

namespace cricket {
class PortAllocator;
class MediaEngineInterface;
class MediaSessionClient;
class Call;
class SessionManagerTask;
struct CallOptions;
struct MediaStreams;
struct StreamParams;
}

struct RosterItem {
    buzz::Jid jid;
    buzz::Status::Show show;
    std::string status;
};

class VoiceClientNotify {

public:

    virtual ~VoiceClientNotify() {
    }

    /* Called when the connection state changes */
    virtual void OnStateChange(buzz::XmppEngine::State) = 0;

    /* Called when the call state changes */
    virtual void OnCallStateChange(cricket::Session* session, cricket::Session::State state) = 0;
};

class VoiceClient: public sigslot::has_slots<>, talk_base::MessageHandler, XmppPumpNotify {
public:

// initialization
    VoiceClient(VoiceClientNotify *notify);
    ~VoiceClient();

// state functions
    void Login(std::string &username, std::string &password, std::string &server, bool use_ssl);
    void Disconnect();
    void Call(std::string &remoteJid);
    void EndCall();
    void AcceptCall();
    void DeclineCall();
    void Destruct();

private:




// callback functions
    void OnRequestSignaling();
    void OnSessionCreate(cricket::Session* session, bool initiate);
    void OnCallCreate(cricket::Call* call);
    void OnCallDestroy(cricket::Call* call);
    void OnMediaEngineTerminate();
    void OnSessionState(cricket::Call* call, cricket::Session* session,
            cricket::Session::State state);
    void OnStatusUpdate(const buzz::Status& status);
    void OnDataReceived(cricket::Call*, const cricket::ReceiveDataParams& params,
            const std::string& data);
    void OnMessage(talk_base::Message *msg);
    void OnStateChange(buzz::XmppEngine::State state);


// main thread functions - These should probably be ui thread functions only
    void OnMessageM(talk_base::Message *msg);
    void OnStateChangeM(buzz::XmppEngine::State state);

// signaling thread functions initialization
    void InitializeS();
    void DestroyS();
    void InitMediaS();
    void InitPresenceS();
// signaling thread functions other
    void LoginS();
    void DisconnectS();
    void CallS(const std::string &remoteJid);
    void EndCallS();
    void AcceptCallS();
    void DeclineCallS();
    void OnStateChangeS(buzz::XmppEngine::State state);
    void OnMessageS(talk_base::Message *msg);

    typedef std::map<std::string, RosterItem> RosterMap;

    VoiceClientNotify *notify_;
    talk_base::scoped_ptr<talk_base::Thread> main_thread_;
    talk_base::scoped_ptr<XmppPump> pump_;
    buzz::XmppClientSettings xcs_;
    talk_base::Thread* worker_thread_;
    //talk_base::SignalThread* signaling_thread_;
    talk_base::Thread* signaling_thread_;
    talk_base::NetworkManager* network_manager_;
    cricket::PortAllocator* port_allocator_;
    cricket::SessionManager* session_manager_;
    cricket::SessionManagerTask* session_manager_task_;
    talk_base::scoped_ptr<cricket::MediaEngineInterface> media_engine_;
    cricket::DataEngineInterface* data_engine_;
    cricket::MediaSessionClient* media_client_;

    cricket::Call* call_;
    cricket::Session *session_;
    bool incoming_call_;
    bool auto_accept_;
    bool use_ssl_;
    bool releasing_;

    buzz::Status my_status_;
    buzz::PresencePushTask* presence_push_;
    buzz::PresenceOutTask* presence_out_;
    RosterMap* roster_;
    uint32 portallocator_flags_;

    cricket::SignalingProtocol initial_protocol_;
    cricket::SecureMediaPolicy secure_policy_;
};

#endif
