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
//#include "tuenti/clientsignalingthread.h"

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
//class ClientSignalingThread;
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

namespace tuenti {
  class ClientSignalingThread;

/*struct RosterItem {
    buzz::Jid jid;
    buzz::Status::Show show;
    std::string status;
};*/


class VoiceClientNotify {

public:

    virtual ~VoiceClientNotify() {
    }

    /* Called when the connection state changes */
    virtual void OnStateChange(buzz::XmppEngine::State) = 0;

    /* Called when the call state changes */
    virtual void OnCallStateChange(cricket::Session* session, cricket::Session::State state) = 0;
};

class VoiceClient: public sigslot::has_slots<>, talk_base::MessageHandler {
public:

// initialization
    VoiceClient(VoiceClientNotify *notify);
    ~VoiceClient();
    void Destroy(int delay);//Deletes self after deleting threads

// passthru functions
    void Login(std::string &username, std::string &password, std::string &server, bool use_ssl);
    void Disconnect();
    void Call(std::string &remoteJid);
    void EndCall();
    void AcceptCall();
    void DeclineCall();

private:


// signaling thread functions initialization
    void InitializeS();
    void DestroyS();
// signaling thread functions other
    void OnMessage(talk_base::Message *msg);

    //typedef std::map<std::string, RosterItem> RosterMap;

    VoiceClientNotify *notify_;
    talk_base::Thread *signal_thread_;
    tuenti::ClientSignalingThread *client_signaling_thread_;

    buzz::Status my_status_;
};

}// namespace tuenti
#endif
