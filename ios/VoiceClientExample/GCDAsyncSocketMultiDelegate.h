//
//  GCDAsyncSocketMultiDelegate.h
//  Viny
//
//  Created by Hai Le Gia on 3/1/13.
//  Copyright (c) 2013 Hai Le Gia. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "GCDAsyncSocket.h"

@interface GCDAsyncSocketMultiDelegate : NSObject <GCDAsyncSocketDelegate>

@property (nonatomic, strong) NSMutableArray* delegates;
@property (nonatomic, assign) GCDAsyncSocket* socket;

+ (GCDAsyncSocketMultiDelegate*) instance;

-(void)addDelegate:(id)delegate;
@end
