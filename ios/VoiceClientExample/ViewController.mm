//
//  ViewController.m
//  VoiceClientExample
//
//  Created by Luke Weber on 12/17/12.
//  Copyright (c) 2012 Luke Weber. All rights reserved.
//

#import "ViewController.h"
#import "VoiceClientDelegate.h"

@implementation ViewController

//@synthesize loginButton = loginButton_;

- (void)viewDidLoad
{
    [super viewDidLoad];
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
   VoiceClientDelegate* vc = VoiceClientDelegate::getInstance();
   (void)vc;
}

- (IBAction)login:(id)sender{
    printf("logging in");
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
//    statsLabel_.text = stats;
//  [statsLabel_ setText:stats];
  //NFHACK If this makes the text update it is because we are running main stuff on the UI thread
  //[[NSRunLoop mainRunLoop] runUntilDate:[NSDate dateWithTimeIntervalSinceNow:0.5]];
}

- (void)dealloc {
  [statsLabel_ release];
  [super dealloc];
}

- (void)viewDidUnload {
  [statsLabel_ release];
  statsLabel_ = nil;
  [super viewDidUnload];
}
@end

