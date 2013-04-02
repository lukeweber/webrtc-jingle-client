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
#import "GCDAsyncSocketMultiDelegate.h"
#import "VoiceClientExample/XmppClientDelegate.h"
#endif

static XmppClientDelegate* xmppClientDelegate_;

VoiceClientDelegate *VoiceClientDelegate::voiceClientDelegateInstance_ = NULL;

VoiceClientDelegate *VoiceClientDelegate::getInstance(){
    if(VoiceClientDelegate::voiceClientDelegateInstance_ == NULL){
        VoiceClientDelegate::voiceClientDelegateInstance_ = new VoiceClientDelegate();
        VoiceClientDelegate::voiceClientDelegateInstance_->Init();
    }
    return VoiceClientDelegate::voiceClientDelegateInstance_;
}

void VoiceClientDelegate::Init(){
    if (voiceClient_ == NULL){
        printf("initing");
        voiceClient_ = new tuenti::VoiceClient();
    }
}

void VoiceClientDelegate::Login(){
    stun_config_.stun = "stun.l.google.com:19302";
    AppDelegate* appDelegate = (AppDelegate*)[UIApplication sharedApplication].delegate;
    voiceClient_->Login([[appDelegate.myJid full] cStringUsingEncoding:NSUTF8StringEncoding], [appDelegate.password cStringUsingEncoding:NSUTF8StringEncoding], &stun_config_, "talk.google.com", 5222, true, 0);
}

void VoiceClientDelegate::Logout(){
    voiceClient_->Disconnect();
}

void VoiceClientDelegate::Call(){
    voiceClient_->Call("userto@gmail.com");
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

void VoiceClientDelegate::OnSignalBuddyListReset() {
}

void VoiceClientDelegate::OnSignalBuddyListRemove(const char *remote_jid) {
}

void VoiceClientDelegate::OnSignalBuddyListAdd(const char *remote_jid, const char *nick) {
}

void VoiceClientDelegate::OnSignalStatsUpdate(const char *stats) {
  AppDelegate *appDelegate = [[UIApplication sharedApplication] delegate];
  ViewController* mainController = (ViewController*) appDelegate.window.rootViewController;
  [mainController statsUpdate:[NSString stringWithUTF8String:stats]];
}

void VoiceClientDelegate::InitXmppClient(talk_base::TaskParent *parent)
{
    AppDelegate* appDelegate = (AppDelegate*) [UIApplication sharedApplication].delegate;
    xmppClientDelegate_ = [[XmppClientDelegate alloc] init];
    client_ = new tictok::IOSXmppClient(parent);
    xmppClientDelegate_.xmppClient = client_;
    xmppClientDelegate_.asyncSocket = [GCDAsyncSocketMultiDelegate instance].socket;
    [[GCDAsyncSocketMultiDelegate instance] addDelegate:xmppClientDelegate_];
    [appDelegate.xmppStream addDelegate:xmppClientDelegate_ delegateQueue:dispatch_get_main_queue()];
}

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
}
