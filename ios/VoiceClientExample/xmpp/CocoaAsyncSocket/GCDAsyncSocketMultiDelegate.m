//
//  GCDAsyncSocketMultiDelegate.m
//  Viny
//
//  Created by Hai Le Gia on 3/1/13.
//  Copyright (c) 2013 Hai Le Gia. All rights reserved.
//

#import "GCDAsyncSocketMultiDelegate.h"

@implementation GCDAsyncSocketMultiDelegate

static GCDAsyncSocketMultiDelegate* _instance;

+(void)initialize
{
    _instance = [[GCDAsyncSocketMultiDelegate alloc] init];
}

+(GCDAsyncSocketMultiDelegate*) instance
{
    return _instance;
}

-(id) init
{
    self.delegates = [[NSMutableArray alloc] init];
    return self;
}

-(void)addDelegate:(id)delegate
{
    [self.delegates addObject:delegate];
}

- (dispatch_queue_t)newSocketQueueForConnectionFromAddress:(NSData *)address onSocket:(GCDAsyncSocket *)sock
{
    __block dispatch_queue_t result = nil;
    [_delegates enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
        if ([obj respondsToSelector:@selector(newSocketQueueForConnectionFromAddress:onSocket:)])
        {
            if (!result)
            {
                result = [obj newSocketQueueForConnectionFromAddress:address onSocket:sock];
            }
            else
            {
                [obj newSocketQueueForConnectionFromAddress:address onSocket:sock];
            }
        }
        *stop = NO;
    }];
    return result;
}

- (void)socket:(GCDAsyncSocket *)sock didAcceptNewSocket:(GCDAsyncSocket *)newSocket
{
    [_delegates enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
        if ([obj respondsToSelector:@selector(socket:didAcceptNewSocket:)])
        {
            [obj socket:sock didAcceptNewSocket:newSocket];
            *stop = NO;
        }
    }];
}

- (void)socket:(GCDAsyncSocket *)sock didConnectToHost:(NSString *)host port:(uint16_t)port
{
    [_delegates enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
        if ([obj respondsToSelector:@selector(socket:didConnectToHost:port:)])
        {
            [obj socket:sock didConnectToHost:host port:port];
            *stop = NO;
        }
    }];
}

- (void)socket:(GCDAsyncSocket *)sock didReadData:(NSData *)data withTag:(long)tag
{
    [_delegates enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
        if ([obj respondsToSelector:@selector(socket:didReadData:withTag:)])
        {
            [obj socket:sock didReadData:data withTag:tag];
        }
        *stop = NO;
    }];
}

- (void)socket:(GCDAsyncSocket *)sock didReadPartialDataOfLength:(NSUInteger)partialLength tag:(long)tag
{
    [_delegates enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
        if ([obj respondsToSelector:@selector(socket:didReadPartialDataOfLength:tag:)])
        {
            [obj socket:sock didReadPartialDataOfLength:partialLength tag:tag];
        }
        *stop = NO;
    }];
}

- (void)socket:(GCDAsyncSocket *)sock didWriteDataWithTag:(long)tag
{
    [_delegates enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
        if ([obj respondsToSelector:@selector(socket:didWriteDataWithTag:)])
        {
            [obj socket:sock didWriteDataWithTag:tag];
        }
        *stop = NO;
    }];
}

- (void)socket:(GCDAsyncSocket *)sock didWritePartialDataOfLength:(NSUInteger)partialLength tag:(long)tag
{
    [_delegates enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
        if ([obj respondsToSelector:@selector(socket:didWritePartialDataOfLength:tag:)])
        {
            [obj socket:sock didWritePartialDataOfLength:partialLength tag:tag];
        }
        *stop = NO;
    }];
}


- (NSTimeInterval)socket:(GCDAsyncSocket *)sock shouldTimeoutReadWithTag:(long)tag
                 elapsed:(NSTimeInterval)elapsed
               bytesDone:(NSUInteger)length
{
    [_delegates enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
        NSTimeInterval result = 0;
        if ([obj respondsToSelector:@selector(socket:shouldTimeoutReadWithTag:elapsed:bytesDone:)])
        {
            if (result > 0)
            {
                [obj socket:sock shouldTimeoutReadWithTag:tag elapsed:elapsed bytesDone:length];
            }
            else
            {
                result = [obj socket:sock shouldTimeoutReadWithTag:tag elapsed:elapsed bytesDone:length];
            }
        }
        *stop = NO;
    }];
}

- (NSTimeInterval)socket:(GCDAsyncSocket *)sock shouldTimeoutWriteWithTag:(long)tag
                 elapsed:(NSTimeInterval)elapsed
               bytesDone:(NSUInteger)length
{
    [_delegates enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
        NSTimeInterval result = 0;
        if ([obj respondsToSelector:@selector(socket:shouldTimeoutWriteWithTag:elapsed:bytesDone:)])
        {
            if (result > 0)
            {
                [obj socket:sock shouldTimeoutWriteWithTag:tag elapsed:elapsed bytesDone:length];
            }
            else
            {
                result = [obj socket:sock shouldTimeoutWriteWithTag:tag elapsed:elapsed bytesDone:length];
            }
        }
        *stop = NO;
    }];
}

- (void)socketDidCloseReadStream:(GCDAsyncSocket *)sock
{
    [_delegates enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
        if ([obj respondsToSelector:@selector(socketDidCloseReadStream:)])
        {
            [obj socketDidCloseReadStream:sock];
        }
        *stop = NO;
    }];
}

- (void)socketDidDisconnect:(GCDAsyncSocket *)sock withError:(NSError *)err
{
    [_delegates enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
        if ([obj respondsToSelector:@selector(socketDidDisconnect:withError:)])
        {
            [obj socketDidDisconnect:sock withError:err];
        }
        *stop = NO;
    }];
}

- (void)socketDidSecure:(GCDAsyncSocket *)sock
{
    [_delegates enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
        if ([obj respondsToSelector:@selector(socketDidSecure:)])
        {
            [obj socketDidSecure:sock];
        }
        *stop = NO;
    }];
}

@end
