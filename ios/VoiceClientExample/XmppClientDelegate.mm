//
//  XmppClientDelegate.m
//  Viny
//
//  Created by Hai Le Gia on 3/1/13.
//  Copyright (c) 2013 Hai Le Gia. All rights reserved.
//

#import "XmppClientDelegate.h"
#import "IOSXmppClient.h"
#import "AppDelegate.h"
#import "XMPPJID.h"

#define TAG_XMPP_WRITE_STREAM       201

@implementation XmppClientDelegate
{
    VoiceClientDelegate* voiceClientDelegate;
}

@synthesize asyncSocket=_asyncSocket;

- (id)init
{
    return [self initWithDispatchQueue:nil];
}

- (id)initWithDispatchQueue:(dispatch_queue_t)queue
{
    self = [super initWithDispatchQueue:queue];
    if (self)
    {
        NSLog(@"CREATE VOICE CLIENT DELEGATAE");
        voiceClientDelegate = VoiceClientDelegate::Create(self);
    }
    return self;
}

-(void)dealloc
{
    NSLog(@"REMOVE VOICE CLIENT DELEGATAE");
//    delete voiceClientDelegate;
    voiceClientDelegate = nil;
}

- (VoiceClientDelegate*) getVoiceClientDelegate
{
    return voiceClientDelegate;
}

-(void)writeOutput:(const char *) bytes withLenght:(size_t) len
{
    NSString* s = [NSString stringWithCString:bytes encoding:NSUTF8StringEncoding];
    NSLog(@"SEND: %@", s);
    NSData* data = [NSData dataWithBytes:(void *)bytes length:len];
    [_asyncSocket writeData:data withTimeout:-1 tag:TAG_XMPP_WRITE_STREAM];
}

-(void)startTLS:(const std::string &) domainname
{
    [_asyncSocket startTLS:nil];
}

-(void)closeConnection
{
//    [_asyncSocket disconnectAfterReadingAndWriting];
    AppDelegate* appDelegate = (AppDelegate*) [UIApplication sharedApplication].delegate;
    [appDelegate.xmppStream disconnect];
    appDelegate.reconnectAfterClosed = YES;
}

#pragma mark XMPPStreamDelegate
-(void)xmppStream:(XMPPStream *)sender socketDidConnect:(GCDAsyncSocket *)socket
{
    self.asyncSocket = socket;
}

- (void)xmppStreamDidAuthenticate:(XMPPStream *)sender
{
    XMPPJID* myJid = sender.myJID;
    NSString* startStream = [NSString stringWithFormat:@"<stream:stream from='gmail.com' to='%@' version='1.0' xml:lang='en' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams'>", [myJid full]];
    tictok::IOSXmppClient* xmppClient = voiceClientDelegate->GetClient();
    xmppClient->HandleInput((char*) [startStream cStringUsingEncoding:NSUTF8StringEncoding], startStream.length);
    xmppClient->ConnectionConnected([[myJid full] cStringUsingEncoding:NSUTF8StringEncoding]);
    
}

-(BOOL)xmppStream:(XMPPStream *)sender didReceiveIQ:(XMPPIQ *)iq
{
    /*
     * I need to comment this "if" because in jingle session, they will set a password for P2P authentication and
     * those stanzas are just iq set, so we need to pass all iq stanzas to webrtc.
     */
//    if ([iq elementForName:@"jingle"] || [iq elementForName:@"session"])
//    {
        NSLog(@"RECEIVE: %@", [iq compactXMLString]);
        NSData* data = [[iq XMLString] dataUsingEncoding:NSUTF8StringEncoding];
        tictok::IOSXmppClient* xmppClient = voiceClientDelegate->GetClient();
        if (xmppClient)
        {
            xmppClient->HandleInput((char*) [data bytes], [data length]);
        }
        return YES;
//    }
    return NO;
}

-(void)xmppStreamDidDisconnect:(XMPPStream *)sender withError:(NSError *)error
{
    self.asyncSocket = nil;
    tictok::IOSXmppClient* xmppClient = voiceClientDelegate->GetClient();
    if (xmppClient != NULL && !xmppClient->IsDone())
    {
        xmppClient->ConnectionClosed(error.code);
    }
}
#pragma mark -
@end
