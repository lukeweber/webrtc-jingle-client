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

@synthesize asyncSocket=_asyncSocket;
@synthesize xmppClient=_xmppClient;

-(void)writeOutput:(const char *) bytes withLenght:(size_t) len
{
#if DEBUG
    NSString* s = [NSString stringWithCString:bytes encoding:NSUTF8StringEncoding];
    NSLog(@"SEND: %@", s);
#endif
    NSData* data = [NSData dataWithBytes:(void *)bytes length:len];
    [_asyncSocket writeData:data withTimeout:-1 tag:TAG_XMPP_WRITE_STREAM];
}

-(void)startTLS:(const std::string &) domainname
{
    [_asyncSocket startTLS:nil];
}

-(void)closeConnection
{
    [_asyncSocket disconnectAfterReadingAndWriting];
}

-(tictok::IOSXmppClient*) getClient
{
    return _xmppClient;
}

#pragma mark XMPPStreamDelegate
-(void)xmppStream:(XMPPStream *)sender socketDidConnect:(GCDAsyncSocket *)socket
{
    self.asyncSocket = socket;
}

- (void)xmppStreamDidAuthenticate:(XMPPStream *)sender
{
    XMPPJID* myJid = sender.myJID;
    _xmppClient->ConnectionConnected([[myJid full] cStringUsingEncoding:NSUTF8StringEncoding]);
}

-(BOOL)xmppStream:(XMPPStream *)sender didReceiveIQ:(XMPPIQ *)iq
{
    if ([iq elementForName:@"jingle"] || [iq elementForName:@"gingle"])
    {
        NSData* data = [[iq XMLString] dataUsingEncoding:NSUTF8StringEncoding];
        _xmppClient->HandleInput((char*) [data bytes], [data length]);
        return YES;
    }
    return NO;
}

-(void)xmppStreamDidDisconnect:(XMPPStream *)sender withError:(NSError *)error
{
    self.asyncSocket = nil;
    _xmppClient->ConnectionClosed(error.code);
}
#pragma mark -
@end
