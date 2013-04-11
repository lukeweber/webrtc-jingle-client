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
  void OnSignalCallTrackingId(int call_id, const char *call_tracker_id);
  void OnSignalAudioPlayout();
  void OnSignalCallError(int error, int call_id);
  void OnSignalXmppError(int error);
  void OnSignalXmppSocketClose(int state);
  void OnSignalXmppStateChange(int state);
  void OnPresenceChanged(const std::string& jid, int available, int show);
  void OnSignalBuddyListReset();
  void OnSignalBuddyListRemove(const std::string& jid);
  void OnSignalBuddyListAdd(const std::string& jid, const std::string& nick,
		int available, int show);
  void OnSignalStatsUpdate(const char *stats);

private:
    static VoiceClientDelegate * voiceClientDelegateInstance_;
    tuenti::VoiceClient *voiceClient_;
    tuenti::StunConfig stun_config_;

};

#endif
