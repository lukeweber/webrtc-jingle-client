//
//  ViewController.h
//  VoiceClientExample
//
//  Created by Luke Weber on 12/17/12.
//  Copyright (c) 2012 Luke Weber. All rights reserved.
//

#import <UIKit/UIKit.h>
@class ButtonController;
@interface ViewController : UIViewController {
//    UIButton *loginButton_;
}

//@property (nonatomic, retain) UIButton *loginButton;
- (IBAction)init:(id)sender;
- (IBAction)login:(id)sender;
- (IBAction)logout:(id)sender;
- (IBAction)call:(id)sender;
@end
