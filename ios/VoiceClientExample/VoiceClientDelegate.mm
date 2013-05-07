//
//  VoiceClientDelegate.cpp
//  webrtcjingle
//
//  Created by Luke Weber on 12/17/12.
//
//
#include "VoiceClientDelegate.h"
#include "client/voiceclient.h"

#import "AppDelegate.h"
#import "ViewController.h"

#ifdef IOS_XMPP_FRAMEWORK
#import "XmppClientDelegate.h"
#import "IOSXmppClient.h"
#import "AppDelegate.h"
#import "VoiceClientExample/XmppClientDelegate.h"
#endif

VoiceClientDelegate *VoiceClientDelegate::voiceClientDelegateInstance_ = NULL;

VoiceClientDelegate *VoiceClientDelegate::getInstance(){
    if(VoiceClientDelegate::voiceClientDelegateInstance_ == NULL){
        VoiceClientDelegate::voiceClientDelegateInstance_ = new VoiceClientDelegate();
        VoiceClientDelegate::voiceClientDelegateInstance_->Init();
    }
    return VoiceClientDelegate::voiceClientDelegateInstance_;
}

#ifdef IOS_XMPP_FRAMEWORK
VoiceClientDelegate *VoiceClientDelegate::Create(XmppClientDelegatePtr xmppClientDelegate)
{
    VoiceClientDelegate* result = new VoiceClientDelegate();
    result->xmppClientDelegate_ = xmppClientDelegate;
    result->Init();
    return result;
}
#endif

VoiceClientDelegate::~VoiceClientDelegate()
{
    delete voiceClient_;
    voiceClient_ = NULL;
}

void VoiceClientDelegate::Init(){
    if (voiceClient_ == NULL){
        printf("initing");
#ifdef IOS_XMPP_FRAMEWORK
        voiceClient_ = new tuenti::VoiceClient(this);
#else
        voiceClient_ = new tuenti::VoiceClient();
#endif
    }
}

void VoiceClientDelegate::Login(){
    stun_config_.stun = "stun.l.google.com:19302";
    AppDelegate* appDelegate = (AppDelegate*)[UIApplication sharedApplication].delegate;
    voiceClient_->Login([[appDelegate.myJid full] cStringUsingEncoding:NSUTF8StringEncoding], [appDelegate.password cStringUsingEncoding:NSUTF8StringEncoding], &stun_config_, "talk.google.com", 5222, true, 0, true/*isGtalk*/);
}

void VoiceClientDelegate::Logout(){
    voiceClient_->Disconnect();
}

void VoiceClientDelegate::Call(const char *remote_jid){
    voiceClient_->Call(remote_jid);
}

void VoiceClientDelegate::OnSignalCallStateChange(int state, const char *remote_jid, int call_id) {
}

void VoiceClientDelegate::OnSignalCallTrackingId(int call_id, const char *call_tracker_id) {
    printf("------- Call Tracker Id %s for call_id %d", call_tracker_id, call_id);
}

void VoiceClientDelegate::OnSignalAudioPlayout() {
}

void VoiceClientDelegate::OnSignalCallError(int error, int call_id) {
}

void VoiceClientDelegate::OnSignalXmppError(int error) {
}

void VoiceClientDelegate::OnSignalXmppSocketClose(int state) {
}

void VoiceClientDelegate::OnSignalXmppStateChange(int state) {
}

void VoiceClientDelegate::OnPresenceChanged(const std::string& jid, int available, int show) {
}

void VoiceClientDelegate::OnSignalBuddyListRemove(const std::string& jid) {
}

void VoiceClientDelegate::OnSignalBuddyListAdd(const std::string& jid, const std::string& nick,
		int available, int show) {
}

void VoiceClientDelegate::OnSignalStatsUpdate(const char *stats) {
  AppDelegate *appDelegate = [[UIApplication sharedApplication] delegate];
  ViewController* mainController = (ViewController*) appDelegate.window.rootViewController;
  [mainController statsUpdate:[NSString stringWithUTF8String:stats]];
}

#ifdef IOS_XMPP_FRAMEWORK
talk_base::Thread* VoiceClientDelegate::GetSignalThread()
{
    return voiceClient_->GetSignalThread();
}
#endif

void VoiceClientDelegate::WriteOutput(const char *bytes, size_t len)
{
    [xmppClientDelegate_ writeOutput:bytes withLenght:len];
}

void VoiceClientDelegate::StartTls(const std::string& domain) {
#if defined(FEATURE_ENABLE_SSL)
    [xmppClientDelegate_ startTLS:domain];
#endif
}

void VoiceClientDelegate::CloseConnection() {
    [xmppClientDelegate_ closeConnection];
    client_->ConnectionClosed(0);
    xmppClientDelegate_ = nil;
    client_ = NULL;
}
