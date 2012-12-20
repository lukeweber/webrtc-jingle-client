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



@end

