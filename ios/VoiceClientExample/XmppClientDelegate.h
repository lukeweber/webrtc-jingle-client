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

namespace tictok {
    class IOSXmppClient;
}

@interface XmppClientDelegate : NSObject <GCDAsyncSocketDelegate, XMPPStreamDelegate>

@property (nonatomic, assign) GCDAsyncSocket* asyncSocket;
@property (nonatomic, assign) tictok::IOSXmppClient* xmppClient;

//XmppOutputHandler, call only from XMPPClient::Private
-(void)writeOutput:(const char *) bytes withLenght:(size_t) len;
-(void)startTLS:(const std::string &) domainname;
-(void)closeConnection;

@end
