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
    voiceClient_->Login("username@gmail.com","password",
                        &stun_config_, "talk.google.com", 5222, true, 0);
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
