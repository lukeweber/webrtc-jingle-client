//
//  VoiceClientDelegate.h
//  webrtcjingle
//
//  Created by Luke Weber on 12/17/12.
//
//

#ifndef webrtcjingle_VoiceClientDelegate_h
#define webrtcjingle_VoiceClientDelegate_h

#include "client/voiceclient.h"
#ifdef XMPP_FRAMEWORK
namespace tuenti {
    class TXmppPump;
};
namespace tictok {
    class IOSXmppClient;
};
#endif

class VoiceClientDelegate {
    
public:
    static VoiceClientDelegate *getInstance();
    void Init();
    void Login();
    void Logout();
    void Call();
    void OnSignalCallStateChange(int state, const char *remote_jid, int call_id);
    void OnSignalCallTrackingId(int call_id, const char *call_tracker_id);
    void OnSignalAudioPlayout();
    void OnSignalCallError(int error, int call_id);
    void OnSignalXmppError(int error);
    void OnSignalXmppSocketClose(int state);
    void OnSignalXmppStateChange(int state);
    void OnSignalBuddyListReset();
    void OnSignalBuddyListRemove(const char *remote_jid);
    
    void OnSignalBuddyListAdd(const char *remote_jid, const char *nick);
    void OnSignalStatsUpdate(const char *stats);
#ifdef XMPP_FRAMEWORK
    talk_base::Thread* GetSignalThread()
    {
        return voiceClient_->GetSignalThread();
    }
    
    tictok::IOSXmppClient* GetClient()
    {
        return client_;
    }
    
    void InitXmppClient(talk_base::TaskParent* parent);
    void WriteOutput(const char* bytes, size_t len);
    void StartTls(const std::string& domain);
    void CloseConnection();
#endif
private:
    static VoiceClientDelegate * voiceClientDelegateInstance_;
    tuenti::VoiceClient *voiceClient_;
    tuenti::StunConfig stun_config_;
#ifdef XMPP_FRAMEWORK
    tictok::IOSXmppClient* client_;
#endif
};

#endif
