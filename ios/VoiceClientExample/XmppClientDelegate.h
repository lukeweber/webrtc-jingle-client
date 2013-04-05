//
//  XmppClientDelegate.h
//  Viny
//
//  Created by Hai Le Gia on 3/1/13.
//  Copyright (c) 2013 Hai Le Gia. All rights reserved.
//

#import <Foundation/Foundation.h>
#include <string>
#import "GCDAsyncSocket.h"
#import "XMPPStream.h"
#import "XMPPModule.h"
#import "VoiceClientDelegate.h"

@interface XmppClientDelegate : XMPPModule

@property (nonatomic, assign) GCDAsyncSocket* asyncSocket;

- (id)init;
- (id)initWithDispatchQueue:(dispatch_queue_t)queue;

//XmppOutputHandler, call only from XMPPClient::Private
-(VoiceClientDelegate*) getVoiceClientDelegate;
-(void)writeOutput:(const char *) bytes withLenght:(size_t) len;
-(void)startTLS:(const std::string &) domainname;
-(void)closeConnection;

@end
