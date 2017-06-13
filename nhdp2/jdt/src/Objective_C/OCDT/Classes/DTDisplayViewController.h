//
//  FirstViewController.h
//  OCDT
//
//  Created by Tom Susel on 11/19/09.
//  Copyright __MyCompanyName__ 2009. All rights reserved.
//

#import <UIKit/UIKit.h>

@class DTDisplayView;

@interface DTDisplayViewController : UIViewController <UIScrollViewDelegate>{
	//Display
	UIScrollView	*scrollView;
	DTDisplayView	*_displayView;
}

@property (nonatomic,retain) IBOutlet UIScrollView *scrollView;

//Notifications
- (void) recievedLoadFileNotification:(NSNotification*)aNotification;
- (void) recievedSaveFileNotification:(NSNotification*)aNotification;
- (void) recievedClearDisplayNotification:(NSNotification*)aNotification;
- (void) recievedSettingsChangedNotification:(NSNotification*)aNotification;
@end
