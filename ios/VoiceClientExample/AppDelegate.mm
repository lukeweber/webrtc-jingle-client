//
//  AppDelegate.m
//  VoiceClientExample
//
//  Created by Luke Weber on 12/17/12.
//  Copyright (c) 2012 Luke Weber. All rights reserved.
//

#import "AppDelegate.h"
#import "VoiceClientDelegate.h"
#import "XmppClientDelegate.h"
#import "DDLog.h"
#import "DDTTYLogger.h"

#import <CFNetwork/CFNetwork.h>

// Log levels: off, error, warn, info, verbose
//#if DEBUG
//static const int ddLogLevel = LOG_LEVEL_VERBOSE;
//#else
//static const int ddLogLevel = LOG_LEVEL_INFO;
//#endif

@interface AppDelegate()
{
    BOOL allowSelfSignedCertificates;
	BOOL allowSSLHostNameMismatch;

	BOOL isXmppConnected;
    XmppClientDelegate* xmppClientDelegate;
}

@end

@implementation AppDelegate

@synthesize window = _window;
@synthesize xmppStream;
@synthesize xmppReconnect;
@synthesize xmppRoster;
@synthesize xmppRosterStorage;
@synthesize xmppvCardTempModule;
@synthesize xmppvCardAvatarModule;
@synthesize xmppCapabilities;
@synthesize xmppCapabilitiesStorage;
@synthesize xmppvCardStorage;
@synthesize myJid;
@synthesize password;
@synthesize reconnectAfterClosed;

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    // Override point for customization after application launch.
    [DDLog addLogger:[DDTTYLogger sharedInstance]];
    // Setup the XMPP stream

//	[self setupStream];
//  [self connect];
    return YES;
}

- (void)applicationWillResignActive:(UIApplication *)application
{
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}

- (void)applicationWillTerminate:(UIApplication *)application
{
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}

- (BOOL)connect
{
	if (![self.xmppStream isDisconnected]) {
		return YES;
	}
    
	NSString *myJID = @"";
	NSString *myPassword = @"";

	//
	// If you don't want to use the Settings view to set the JID,
	// uncomment the section below to hard code a JID and password.
	//
	// myJID = @"user@gmail.com/xmppframework";
	// myPassword = @"";

	if (myJID == nil || myPassword == nil) {
		return NO;
	}

    self.myJid = [XMPPJID jidWithString:myJID];
	self.password = myPassword;

    [self.xmppStream setMyJID:self.myJid];

    xmppClientDelegate = [[XmppClientDelegate alloc] init];
    [xmppClientDelegate activate:self.xmppStream];
    [xmppClientDelegate getVoiceClientDelegate]->Login();
    
	NSError *error = nil;
	if (![self.xmppStream connect:&error])
	{
		UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:@"Error connecting"
		                                                    message:@"See console for error details."
		                                                   delegate:nil
		                                          cancelButtonTitle:@"Ok"
		                                          otherButtonTitles:nil];
		[alertView show];

//		DDLogError(@"Error connecting: %@", error);

		return NO;
	}
	return YES;
}

-(void)reconnect
{
    xmppClientDelegate = [[XmppClientDelegate alloc] init];
    [xmppClientDelegate activate:self.xmppStream];
    [xmppClientDelegate getVoiceClientDelegate]->Login();
    [self.xmppReconnect manualStart];
}

- (void)disconnect
{
	[self goOffline];
	[self.xmppStream disconnect];
}

#pragma mark Core Data

- (NSManagedObjectContext *)managedObjectContext_roster
{
	return [self.xmppRosterStorage mainThreadManagedObjectContext];
}

- (NSManagedObjectContext *)managedObjectContext_capabilities
{
	return [self.xmppCapabilitiesStorage mainThreadManagedObjectContext];
}

#pragma mark end

- (void)setupStream
{
	NSAssert(self.xmppStream == nil, @"Method setupStream invoked multiple times");

	// Setup xmpp stream
	//
	// The XMPPStream is the base class for all activity.
	// Everything else plugs into the xmppStream, such as modules/extensions and delegates.

	self.xmppStream = [[XMPPStream alloc] init];

#if !TARGET_IPHONE_SIMULATOR
	{
		// Want xmpp to run in the background?
		//
		// P.S. - The simulator doesn't support backgrounding yet.
		//        When you try to set the associated property on the simulator, it simply fails.
		//        And when you background an app on the simulator,
		//        it just queues network traffic til the app is foregrounded again.
		//        We are patiently waiting for a fix from Apple.
		//        If you do enableBackgroundingOnSocket on the simulator,
		//        you will simply see an error message from the xmpp stack when it fails to set the property.

		self.xmppStream.enableBackgroundingOnSocket = YES;
	}
#endif

	// Setup reconnect
	//
	// The XMPPReconnect module monitors for "accidental disconnections" and
	// automatically reconnects the stream for you.
	// There's a bunch more information in the XMPPReconnect header file.

	self.xmppReconnect = [[XMPPReconnect alloc] init];

	// Setup roster
	//
	// The XMPPRoster handles the xmpp protocol stuff related to the roster.
	// The storage for the roster is abstracted.
	// So you can use any storage mechanism you want.
	// You can store it all in memory, or use core data and store it on disk, or use core data with an in-memory store,
	// or setup your own using raw SQLite, or create your own storage mechanism.
	// You can do it however you like! It's your application.
	// But you do need to provide the roster with some storage facility.

	self.xmppRosterStorage = [[XMPPRosterCoreDataStorage alloc] init];
    //	xmppRosterStorage = [[XMPPRosterCoreDataStorage alloc] initWithInMemoryStore];

	self.xmppRoster = [[XMPPRoster alloc] initWithRosterStorage:self.xmppRosterStorage];

	self.xmppRoster.autoFetchRoster = YES;
	self.xmppRoster.autoAcceptKnownPresenceSubscriptionRequests = YES;

	// Setup vCard support
	//
	// The vCard Avatar module works in conjuction with the standard vCard Temp module to download user avatars.
	// The XMPPRoster will automatically integrate with XMPPvCardAvatarModule to cache roster photos in the roster.

	self.xmppvCardStorage = [XMPPvCardCoreDataStorage sharedInstance];
	self.xmppvCardTempModule = [[XMPPvCardTempModule alloc] initWithvCardStorage:self.xmppvCardStorage];

	self.xmppvCardAvatarModule = [[XMPPvCardAvatarModule alloc] initWithvCardTempModule:self.xmppvCardTempModule];

	// Setup capabilities
	//
	// The XMPPCapabilities module handles all the complex hashing of the caps protocol (XEP-0115).
	// Basically, when other clients broadcast their presence on the network
	// they include information about what capabilities their client supports (audio, video, file transfer, etc).
	// But as you can imagine, this list starts to get pretty big.
	// This is where the hashing stuff comes into play.
	// Most people running the same version of the same client are going to have the same list of capabilities.
	// So the protocol defines a standardized way to hash the list of capabilities.
	// Clients then broadcast the tiny hash instead of the big list.
	// The XMPPCapabilities protocol automatically handles figuring out what these hashes mean,
	// and also persistently storing the hashes so lookups aren't needed in the future.
	//
	// Similarly to the roster, the storage of the module is abstracted.
	// You are strongly encouraged to persist caps information across sessions.
	//
	// The XMPPCapabilitiesCoreDataStorage is an ideal solution.
	// It can also be shared amongst multiple streams to further reduce hash lookups.

	self.xmppCapabilitiesStorage = [XMPPCapabilitiesCoreDataStorage sharedInstance];
    self.xmppCapabilities = [[XMPPCapabilities alloc] initWithCapabilitiesStorage:self.xmppCapabilitiesStorage];

    self.xmppCapabilities.autoFetchHashedCapabilities = YES;
    self.xmppCapabilities.autoFetchNonHashedCapabilities = NO;

    
	// Activate xmpp modules

	[self.xmppReconnect         activate:self.xmppStream];
	[self.xmppRoster            activate:self.xmppStream];
	[self.xmppvCardTempModule   activate:self.xmppStream];
	[self.xmppvCardAvatarModule activate:self.xmppStream];
	[self.xmppCapabilities      activate:self.xmppStream];
	// Add ourself as a delegate to anything we may be interested in

	[self.xmppStream addDelegate:self delegateQueue:dispatch_get_main_queue()];
	[self.xmppRoster addDelegate:self delegateQueue:dispatch_get_main_queue()];

	// Optional:
	//
	// Replace me with the proper domain and port.
	// The example below is setup for a typical google talk account.
	//
	// If you don't supply a hostName, then it will be automatically resolved using the JID (below).
	// For example, if you supply a JID like 'user@quack.com/rsrc'
	// then the xmpp framework will follow the xmpp specification, and do a SRV lookup for quack.com.
	//
	// If you don't specify a hostPort, then the default (5222) will be used.

    [self.xmppStream setHostName:@"talk.google.com"];
//    [self.xmppStream setHostPort:5222];


	// You may need to alter these settings depending on the server you're connecting to
	allowSelfSignedCertificates = NO;
	allowSSLHostNameMismatch = NO;
}

- (void)dealloc
{
	[self teardownStream];
}

- (void)teardownStream
{
	[self.xmppStream removeDelegate:self];
	[self.xmppRoster removeDelegate:self];

	[self.xmppReconnect         deactivate];
	[self.xmppRoster            deactivate];
	[self.xmppvCardTempModule   deactivate];
	[self.xmppvCardAvatarModule deactivate];
	[self.xmppCapabilities      deactivate];

	[self.xmppStream disconnect];

	self.xmppStream = nil;
	self.xmppReconnect = nil;
    self.xmppRoster = nil;
	self.xmppRosterStorage = nil;
	self.xmppvCardStorage = nil;
    self.xmppvCardTempModule = nil;
	self.xmppvCardAvatarModule = nil;
	self.xmppCapabilities = nil;
	self.xmppCapabilitiesStorage = nil;
}

- (void)goOnline
{
//	XMPPPresence *presence = [XMPPPresence presence]; // type="available" is implicit
//	[self.xmppStream sendElement:presence];
}

- (void)goOffline
{
	XMPPPresence *presence = [XMPPPresence presenceWithType:@"unavailable"];
	[self.xmppStream sendElement:presence];
}

- (void)xmppStream:(XMPPStream *)sender socketDidConnect:(GCDAsyncSocket *)socket
{
//	DDLogVerbose(@"%@: %@", THIS_FILE, THIS_METHOD);
    NSLog(@"SOCKET DID CONNECT");
}

- (void)xmppStream:(XMPPStream *)sender willSecureWithSettings:(NSMutableDictionary *)settings
{
//	DDLogVerbose(@"%@: %@", THIS_FILE, THIS_METHOD);

	if (allowSelfSignedCertificates)
	{
		[settings setObject:[NSNumber numberWithBool:YES] forKey:(NSString *)kCFStreamSSLAllowsAnyRoot];
	}

	if (allowSSLHostNameMismatch)
	{
		[settings setObject:[NSNull null] forKey:(NSString *)kCFStreamSSLPeerName];
	}
	else
	{
		// Google does things incorrectly (does not conform to RFC).
		// Because so many people ask questions about this (assume xmpp framework is broken),
		// I've explicitly added code that shows how other xmpp clients "do the right thing"
		// when connecting to a google server (gmail, or google apps for domains).

		NSString *expectedCertName = nil;

		NSString *serverDomain = self.xmppStream.hostName;
		NSString *virtualDomain = [self.xmppStream.myJID domain];

		if ([serverDomain isEqualToString:@"talk.google.com"])
		{
			if ([virtualDomain isEqualToString:@"gmail.com"])
			{
				expectedCertName = virtualDomain;
			}
			else
			{
				expectedCertName = serverDomain;
			}
		}
		else if (serverDomain == nil)
		{
			expectedCertName = virtualDomain;
		}
		else
		{
			expectedCertName = serverDomain;
		}

		if (expectedCertName)
		{
			[settings setObject:expectedCertName forKey:(NSString *)kCFStreamSSLPeerName];
		}
	}
}

- (void)xmppStreamDidSecure:(XMPPStream *)sender
{
//	DDLogVerbose(@"%@: %@", THIS_FILE, THIS_METHOD);
}

- (void)xmppStreamDidConnect:(XMPPStream *)sender
{
//	DDLogVerbose(@"%@: %@", THIS_FILE, THIS_METHOD);

	isXmppConnected = YES;
//    NSString* str = [sender.rootElement compactXMLString];
//    const char* strC = [str cStringUsingEncoding:NSUTF8StringEncoding];
//    printf("%s", strC);
	NSError *error = nil;

	if (![[self xmppStream] authenticateWithPassword:self.password error:&error])
	{
//		DDLogError(@"Error authenticating: %@", error);
	}
}

- (void)xmppStreamDidAuthenticate:(XMPPStream *)sender
{
//	DDLogVerbose(@"%@: %@", THIS_FILE, THIS_METHOD);
//	[self goOnline];
    NSLog(@"SOCKET DID AUTHENTICATE");
}

- (void)xmppStream:(XMPPStream *)sender didNotAuthenticate:(NSXMLElement *)error
{
//	DDLogVerbose(@"%@: %@", THIS_FILE, THIS_METHOD);
}

- (BOOL)xmppStream:(XMPPStream *)sender didReceiveIQ:(XMPPIQ *)iq
{
//	DDLogVerbose(@"%@: %@", THIS_FILE, THIS_METHOD);

	return NO;
}

- (void)xmppStream:(XMPPStream *)sender didReceiveMessage:(XMPPMessage *)message
{
//	DDLogVerbose(@"%@: %@", THIS_FILE, THIS_METHOD);

	// A simple example of inbound message handling.

	if ([message isChatMessageWithBody])
	{
		XMPPUserCoreDataStorageObject *user = [self.xmppRosterStorage userForJID:[message from]
		                                                         xmppStream:self.xmppStream
		                                               managedObjectContext:[self managedObjectContext_roster]];

		NSString *body = [[message elementForName:@"body"] stringValue];
		NSString *displayName = [user displayName];

		if ([[UIApplication sharedApplication] applicationState] == UIApplicationStateActive)
		{
			UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:displayName
                                                                message:body
                                                               delegate:nil
                                                      cancelButtonTitle:@"Ok"
                                                      otherButtonTitles:nil];
			[alertView show];
		}
		else
		{
			// We are not active, so use a local notification instead
			UILocalNotification *localNotification = [[UILocalNotification alloc] init];
			localNotification.alertAction = @"Ok";
			localNotification.alertBody = [NSString stringWithFormat:@"From: %@\n\n%@",displayName,body];

			[[UIApplication sharedApplication] presentLocalNotificationNow:localNotification];
		}
	}
}

- (void)xmppStream:(XMPPStream *)sender didReceivePresence:(XMPPPresence *)presence
{
//	DDLogVerbose(@"%@: %@ - %@", THIS_FILE, THIS_METHOD, [presence fromStr]);
}

- (void)xmppStream:(XMPPStream *)sender didReceiveError:(id)error
{
//	DDLogVerbose(@"%@: %@", THIS_FILE, THIS_METHOD);
}

- (void)xmppStreamDidDisconnect:(XMPPStream *)sender withError:(NSError *)error
{
//	DDLogVerbose(@"%@: %@", THIS_FILE, THIS_METHOD);

	if (!isXmppConnected)
	{
//		DDLogError(@"Unable to connect to server. Check xmppStream.hostName");
	}
    [xmppClientDelegate deactivate];
    xmppClientDelegate = nil;
    if (self.reconnectAfterClosed)
    {
        [NSTimer scheduledTimerWithTimeInterval:2 target:self selector:@selector(reconnect) userInfo:nil repeats:NO];
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark XMPPRosterDelegate
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (void)xmppRoster:(XMPPRoster *)sender didReceiveBuddyRequest:(XMPPPresence *)presence
{
//	DDLogVerbose(@"%@: %@", THIS_FILE, THIS_METHOD);

	XMPPUserCoreDataStorageObject *user = [self.xmppRosterStorage userForJID:[presence from]
	                                                         xmppStream:self.xmppStream
	                                               managedObjectContext:[self managedObjectContext_roster]];

	NSString *displayName = [user displayName];
	NSString *jidStrBare = [presence fromStr];
	NSString *body = nil;

	if (![displayName isEqualToString:jidStrBare])
	{
		body = [NSString stringWithFormat:@"Buddy request from %@ <%@>", displayName, jidStrBare];
	}
	else
	{
		body = [NSString stringWithFormat:@"Buddy request from %@", displayName];
	}


	if ([[UIApplication sharedApplication] applicationState] == UIApplicationStateActive)
	{
		UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:displayName
		                                                    message:body
		                                                   delegate:nil
		                                          cancelButtonTitle:@"Not implemented"
		                                          otherButtonTitles:nil];
		[alertView show];
	}
	else
	{
		// We are not active, so use a local notification instead
		UILocalNotification *localNotification = [[UILocalNotification alloc] init];
		localNotification.alertAction = @"Not implemented";
		localNotification.alertBody = body;

		[[UIApplication sharedApplication] presentLocalNotificationNow:localNotification];
	}

}

#pragma mark VoiceClientDelegate section
-(void)call: (NSString*) jid
{
    if (xmppClientDelegate)
    {
        XMPPUserCoreDataStorageObject* user = [self getUserWithJid: jid];
        NSArray* resources = [user allResources];
        int size = resources.count;
        for (int i = 0; i < size; i++)
         {
           XMPPResourceCoreDataStorageObject* resource = [resources objectAtIndex:i];
           NSString* fullJid = [[resource jid] full];
           [xmppClientDelegate getVoiceClientDelegate]->Call([fullJid cStringUsingEncoding:NSUTF8StringEncoding]);
         }
    }
}

-(XMPPUserCoreDataStorageObject*) getUserWithJid:(NSString*) jid
{
    NSManagedObjectContext *moc = [self managedObjectContext_roster];
    
    NSEntityDescription *entity = [NSEntityDescription entityForName:@"XMPPUserCoreDataStorageObject"
                                              inManagedObjectContext:moc];
    NSFetchRequest *fetchRequest = [[NSFetchRequest alloc] init];
    [fetchRequest setEntity:entity];
    [fetchRequest setFetchBatchSize:1];
    [fetchRequest setPredicate:[NSPredicate predicateWithFormat:@"jidStr == %@", jid]];
    
    NSArray* users = [moc executeFetchRequest:fetchRequest error:nil];
    if (users && users.count > 0) {
        return [users objectAtIndex:0];
    }
    return nil;
}
#pragma mark -

@end


