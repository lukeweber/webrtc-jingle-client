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

class VoiceClientDelegate {
    
public:
    static VoiceClientDelegate *getInstance();
    void Init();
    void Login();
    void Logout();
    void Call();
    void OnSignalCallStateChange(int state, const char *remote_jid, int call_id);
    void OnSignalAudioPlayout();
    void OnSignalCallError(int error, int call_id);
    void OnSignalXmppError(int error);
    void OnSignalXmppSocketClose(int state);
    void OnSignalXmppStateChange(int state);
    void OnSignalBuddyListReset();
    void OnSignalBuddyListRemove(const char *remote_jid);
    
    void OnSignalBuddyListAdd(const char *remote_jid, const char *nick);
private:
    static VoiceClientDelegate * voiceClientDelegateInstance_;
    tuenti::VoiceClient *voiceClient_;
    tuenti::StunConfig stun_config_;

};

#endif
