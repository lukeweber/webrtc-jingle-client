//
//  ViewController.m
//  VoiceClientExample
//
//  Created by Luke Weber on 12/17/12.
//  Copyright (c) 2012 Luke Weber. All rights reserved.
//

#import "ViewController.h"
#import "VoiceClientDelegate.h"
#import "AppDelegate.h"

@interface ViewController()
{
    AppDelegate* appDelegate;
}
@end

@implementation ViewController

//@synthesize loginButton = loginButton_;

- (void)viewDidLoad
{
    [super viewDidLoad];
    appDelegate = (AppDelegate*) [UIApplication sharedApplication].delegate;
	// Do any additional setup after loading the view, typically from a nib.
    [self statsUpdate:@"Sender:\nunknown stats\nReceiver:\nunknown stats\n"];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)init:(id)sender{
   printf("init");
   [appDelegate setupStream];
   VoiceClientDelegate* vc = VoiceClientDelegate::getInstance();
   (void)vc;
}

- (IBAction)login:(id)sender{
    printf("logging in");
    [appDelegate connect];
    VoiceClientDelegate* vc = VoiceClientDelegate::getInstance();
    vc->Login();
}

- (IBAction)call:(id)sender{
    printf("calling");
    VoiceClientDelegate* vc = VoiceClientDelegate::getInstance();
    vc->Call();
}

- (IBAction)logout:(id)sender{
    printf("logout");
    VoiceClientDelegate* vc = VoiceClientDelegate::getInstance();
    vc->Logout();
}

- (void)statsUpdate:(NSString *)stats {
  dispatch_async(dispatch_get_main_queue(), ^{
    unsigned numberOfLines, index, stringLength = [stats length];
    for (index = 0, numberOfLines = 0; index < stringLength; numberOfLines++)
      index = NSMaxRange([stats lineRangeForRange:NSMakeRange(index, 0)]);
    [self->statsLabel_ setNumberOfLines:numberOfLines];
    [self->statsLabel_ setText:stats];
  });
}

- (void)dealloc {
  
}

- (void)viewDidUnload {
  statsLabel_ = nil;
  [super viewDidUnload];
}
@end

