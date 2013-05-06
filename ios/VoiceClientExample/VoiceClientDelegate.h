//
//  VoiceClientDelegate.h
//  webrtcjingle
//
//  Created by Luke Weber on 12/17/12.
//
//

#ifndef webrtcjingle_VoiceClientDelegate_h
#define webrtcjingle_VoiceClientDelegate_h

#include "client/client_defines.h"

#ifdef IOS_XMPP_FRAMEWORK
#ifdef __OBJC__
@class XmppClientDelegate;
typedef XmppClientDelegate * XmppClientDelegatePtr;
#endif
namespace tuenti {
    class TXmppPump;
    class VoiceClient;
};
namespace tictok {
    class IOSXmppClient;
};
namespace talk_base
{
    class Thread;
};
#endif

class VoiceClientDelegate {

public:
  static VoiceClientDelegate *getInstance();
#ifdef IOS_XMPP_FRAMEWORK
#ifdef __OBJC__
    static VoiceClientDelegate *Create(XmppClientDelegatePtr xmppClientDelegate);
#endif
#endif
  ~VoiceClientDelegate();
  void Init();
  void Login();
  void Logout();
  void Call(const char *remote_jid);
  void OnSignalCallStateChange(int state, const char *remote_jid, int call_id);
  void OnSignalCallTrackingId(int call_id, const char *call_tracker_id);
  void OnSignalAudioPlayout();
  void OnSignalCallError(int error, int call_id);
  void OnSignalXmppError(int error);
  void OnSignalXmppSocketClose(int state);
  void OnSignalXmppStateChange(int state);
  void OnPresenceChanged(const std::string& jid, int available, int show);
  void OnSignalBuddyListRemove(const std::string& jid);
  void OnSignalBuddyListAdd(const std::string& jid, const std::string& nick,
		int available, int show);
  void OnSignalStatsUpdate(const char *stats);
#ifdef IOS_XMPP_FRAMEWORK
    talk_base::Thread* GetSignalThread();

    tictok::IOSXmppClient* GetClient()
    {
        return client_;
    }
    void SetClient(tictok::IOSXmppClient* client)
    {
        client_ = client;
    }
    void WriteOutput(const char* bytes, size_t len);
    void StartTls(const std::string& domain);
    void CloseConnection();
#endif
private:
    static VoiceClientDelegate * voiceClientDelegateInstance_;
    tuenti::VoiceClient *voiceClient_;
    tuenti::StunConfig stun_config_;
#ifdef IOS_XMPP_FRAMEWORK
    tictok::IOSXmppClient* client_;
#ifdef __OBJC__
    XmppClientDelegatePtr xmppClientDelegate_;
#endif
#endif
};

#endif // webrtcjingle_VoiceClientDelegate_h
