//
//  DTTestViewController.h
//  OCDT
//
//  Created by Tom Susel on 11/21/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import <UIKit/UIKit.h>


@interface DTDataViewController : UIViewController <UITableViewDelegate, UITableViewDataSource, UITextFieldDelegate>{
	UITableView *_tableView;
	UITextField *_textField;
	NSString *_fileName;
	NSIndexPath *_previousSelection;
}

- (IBAction) saveFile:(id)sender;
- (IBAction) loadFile:(id)sender;

@property (nonatomic,retain) IBOutlet UITableView *tableView;
@property (nonatomic,retain) IBOutlet	UITextField *textField;

@end

@interface DTDataViewController (Private)
+ (NSArray*) getFilesList;
@end