//
//  AppDelegate.h
//  VoiceClientExample
//
//  Created by Luke Weber on 12/17/12.
//  Copyright (c) 2012 Luke Weber. All rights reserved.
//
#import <UIKit/UIKit.h>
#import "xmppframework/XMPPFramework.h"

@interface AppDelegate : UIResponder <UIApplicationDelegate>

@property (nonatomic, strong) UIWindow *window;

@property (nonatomic, strong) XMPPStream *xmppStream;
@property (nonatomic, strong) XMPPReconnect *xmppReconnect;
@property (nonatomic, strong) XMPPRoster *xmppRoster;
@property (nonatomic, strong) XMPPRosterCoreDataStorage *xmppRosterStorage;
@property (nonatomic, strong) XMPPvCardTempModule *xmppvCardTempModule;
@property (nonatomic, strong) XMPPvCardAvatarModule *xmppvCardAvatarModule;
@property (nonatomic, strong) XMPPCapabilities *xmppCapabilities;
@property (nonatomic, strong) XMPPCapabilitiesCoreDataStorage *xmppCapabilitiesStorage;
@property (nonatomic, strong) XMPPvCardCoreDataStorage* xmppvCardStorage;
@property BOOL reconnectAfterClosed;
@property (nonatomic, strong) XMPPJID* myJid;
@property (nonatomic, strong) NSString* password;

- (NSManagedObjectContext *)managedObjectContext_roster;
- (NSManagedObjectContext *)managedObjectContext_capabilities;

- (void)setupStream;
- (void)teardownStream;
- (void)goOnline;
- (void)goOffline;

- (BOOL)connect;
- (void)disconnect;

#pragma mark VoiceClientDelegate section
-(void)call:(NSString*) jid;
#pragma mark -
@end

