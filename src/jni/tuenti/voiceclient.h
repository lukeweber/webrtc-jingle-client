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

namespace tuenti {

class ClientSignalingThread;

class VoiceClientNotify {

public:

    virtual ~VoiceClientNotify() {
    }

    /* Called when the connection state changes */
    virtual void OnXmppStateChange(buzz::XmppEngine::State) = 0;

    /* Called when the call state changes */
    virtual void OnCallStateChange(cricket::Session* session, cricket::Session::State state) = 0;

    /* Called when there is a xmpp error */
    virtual void OnXmppError(buzz::XmppEngine::Error) = 0;
};

class VoiceClient: public sigslot::has_slots<>, talk_base::MessageHandler {
public:

    // initialization
    VoiceClient(VoiceClientNotify *notify);
    ~VoiceClient();
    void Destroy(int delay);//Deletes self after deleting threads

    // passthru functions
    void Login(const std::string &username, const std::string &password, const std::string &xmpp_host, int xmpp_port,
            bool use_ssl, const std::string &stun_host, int stun_port);
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

    VoiceClientNotify *notify_;
    talk_base::Thread *signal_thread_;
    tuenti::ClientSignalingThread *client_signaling_thread_;
};

}// namespace tuenti
#endif
