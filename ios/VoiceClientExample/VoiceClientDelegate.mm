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
        voiceClient_->Init();
    }
}

void VoiceClientDelegate::Login(){
    stun_config_.stun = "stun.l.google.com:19302";
    stun_config_.turn = "95.131.170.1:3478";
    stun_config_.turn_username = "";
//    voiceClient_->Login("59989700@xmpp1.tuenti.com","xF6TAwAAAABE1ypF9qQ83cW785uQG1WWMZOAkZOM0FA", &stun_config_, "xmpp1.tuenti.com", 5222, false);
    voiceClient_->Login("nicktuentitesting@gmail.com","20testing", &stun_config_, "talk.google.com", 5222, true);
}

void VoiceClientDelegate::Logout(){
    voiceClient_->Disconnect();
}

void VoiceClientDelegate::Call(){
    //voiceClient_->Call("1058@xmpp1.tuenti.com");
    voiceClient_->Call("nicktuentitesting2@gmail.com");
}

void VoiceClientDelegate::OnSignalCallStateChange(int state, const char *remote_jid, int call_id) {

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
    printf("state %i\n", state);
    if (buzz::XmppEngine::STATE_NONE == state){
        //Login after this event.
    }
    
    if (buzz::XmppEngine::STATE_OPEN == state){
        printf("Calling\n");
        //voiceClient_->Call("luke.weber@gmail.com");
    }
}

void VoiceClientDelegate::OnSignalBuddyListReset() {
}

void VoiceClientDelegate::OnSignalBuddyListRemove(const char *remote_jid) {
}

void VoiceClientDelegate::OnSignalBuddyListAdd(const char *remote_jid, const char *nick) {
}

void VoiceClientDelegate::OnSignalStatsUpdate(const char *stats) {
/*    AppDelegate *appDelegate = [[UIApplication sharedApplication] delegate];
    UIViewController *vc = appDelegate->window_.root
    UIView *topView = appDelegate.viewController.view;*/
  
/*  UIStoryboard *mainStoryboard = [UIStoryboard storyboardWithName:@"MainStoryboard"
                                                         bundle: nil];

  ViewController *controller = (ViewController*)[mainStoryboard
                    instantiateViewControllerWithIdentifier: @"<Controller ID>"];*/
  AppDelegate *appDelegate = [[UIApplication sharedApplication] delegate];
  ViewController* mainController = (ViewController*) appDelegate.window.rootViewController;
  [mainController statsUpdate:[NSString stringWithUTF8String:stats]];

}
