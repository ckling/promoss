//
//  DTSettingsViewController.h
//  OCDT
//
//  Created by Tom Susel on 11/28/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import <UIKit/UIKit.h>


@interface DTSettingsViewController : UIViewController <UITableViewDelegate, UITableViewDataSource>{
	UITableView *_tableView;
	NSIndexPath *_previousViewIndex;
	NSIndexPath *_previousInputIndex;
}

- (IBAction) clearButtonPushed:(id)sender;

@property (nonatomic,retain) IBOutlet UITableView *tableView;

typedef enum{
	DT_SECTION_INPUT = 0,
	DT_SECTION_VIEW,
	DT_SECTION_COUNT
}DT_SETTINGS_SECTIONS;

typedef enum{
	DT_INPUT_POINT_CELL = 0,
	DT_INPUT_100_RAND_PS_CELL,
//	DT_INPUT_GUARD_30M,
//	DT_INPUT_CLIENT_5M,
	DT_INPUT_CELLS_COUNT
}DT_INPUT_CELLS;

typedef enum{
	DT_VIEW_LINES_CELL,
	DT_VIEW_TRIANGLES_CELL,
	DT_VIEW_TOPO_CELL,
	DT_VIEW_FIND_CELL,
//	DT_VIEW_SECTION_CELL,
	DT_VIEW_INFO_CELL,
//	DT_VIEW_CH_CELL,
	DT_VIEW_CELLS_COUNT
}DT_VIEW_CELLS;

@end
