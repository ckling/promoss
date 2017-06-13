//
//  DTSettingsViewController.m
//  OCDT
//
//  Created by Tom Susel on 11/28/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import "DTSettingsViewController.h"
#import "dt_defines.h"

@implementation DTSettingsViewController
@synthesize tableView = _tableView;


 // The designated initializer.  Override if you create the controller programmatically and want to perform customization that is not appropriate for viewDidLoad.
- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    if (self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil]) {
        // Custom initialization
		_previousViewIndex = nil;
		_previousInputIndex = nil;
    }
    return self;
}


/*
// Implement loadView to create a view hierarchy programmatically, without using a nib.
- (void)loadView {
}
*/


// Implement viewDidLoad to do additional setup after loading the view, typically from a nib.
- (void)viewDidLoad {
	_tableView.backgroundColor = [UIColor clearColor];
    [super viewDidLoad];
}


/*
// Override to allow orientations other than the default portrait orientation.
- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
    // Return YES for supported orientations
    return (interfaceOrientation == UIInterfaceOrientationPortrait);
}
*/

- (void)didReceiveMemoryWarning {
	// Releases the view if it doesn't have a superview.
    [super didReceiveMemoryWarning];
	
	// Release any cached data, images, etc that aren't in use.
}

- (void)viewDidUnload {
	// Release any retained subviews of the main view.
	// e.g. self.myOutlet = nil;
}


- (void)dealloc {
	_tableView.delegate = nil;
	[_tableView release];
	if (nil != _previousViewIndex)
		[_previousViewIndex release];
	if (nil != _previousInputIndex)
		[_previousInputIndex release];	
		
    [super dealloc];
}

- (IBAction) clearButtonPushed:(id)sender{
	[[NSNotificationCenter defaultCenter] postNotificationName:dtClearDisplayNotification
														object:nil];
}


#pragma mark -
#pragma mark Tableview datasource
#pragma mark -

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath{
	UITableViewCell *cell = [_tableView dequeueReusableCellWithIdentifier:@"DTRegularCell"];
	//Create cell
	if (cell == nil){
		cell = [[[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault
									   reuseIdentifier:@"DTRegularCell"] autorelease];
		cell.selectionStyle = UITableViewCellSelectionStyleNone;
	}
	
	//Configure cell
	switch (indexPath.section) {
		case DT_SECTION_INPUT:
		{
			switch (indexPath.row) {
				case DT_INPUT_100_RAND_PS_CELL:
					cell.textLabel.text = @"100 Random points";
					break;
//				case DT_INPUT_CLIENT_5M:
//					cell.textLabel.text = @"Client 5m";
//					break;
//				case DT_INPUT_GUARD_30M:
//					cell.textLabel.text = @"Guard 30m";
					break;
				case DT_INPUT_POINT_CELL:
					cell.textLabel.text = @"Point";
					break;
				default:
					break;
			}
		}
			break;
		case DT_SECTION_VIEW:
		{
			switch (indexPath.row) {
//				case DT_VIEW_CH_CELL:
//					cell.textLabel.text = @"CH";
					break;
				case DT_VIEW_FIND_CELL:
					cell.textLabel.text = @"Find";
					break;
				case DT_VIEW_INFO_CELL:
					cell.textLabel.text = @"Info";
					break;
				case DT_VIEW_LINES_CELL:
					cell.textLabel.text = @"Lines";
					break;
				case DT_VIEW_TOPO_CELL:
					cell.textLabel.text = @"Topo";
					break;
				case DT_VIEW_TRIANGLES_CELL:
					cell.textLabel.text = @"Triangles";
					break;
//				case DT_VIEW_SECTION_CELL:
//					cell.textLabel.text = @"Section";
					break;
				default:
					break;
			}
		}
			break;
		default:
			break;
	}
	return cell;
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView{
	return DT_SECTION_COUNT;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section{
	switch (section) {
		case DT_SECTION_INPUT:
			return DT_INPUT_CELLS_COUNT;
			break;
		case DT_SECTION_VIEW:
			return DT_VIEW_CELLS_COUNT;
			break;
		default:
			return 0;
			break;
	}
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section{
	switch (section) {
		case DT_SECTION_INPUT:
			return @"Input";
			break;
		case DT_SECTION_VIEW:
			return @"View";
			break;
		default:
			return @"";
			break;
	}
}


#pragma mark -
#pragma mark Tableview delegate
#pragma mark -

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath{
	[_tableView deselectRowAtIndexPath:indexPath animated:YES];	
	switch (indexPath.section) {
		case DT_SECTION_INPUT:
		{
			//Remove V from previous selection
			UITableViewCell *previousCell = [_tableView cellForRowAtIndexPath:_previousInputIndex];
			if (previousCell != nil)
				previousCell.accessoryType = UITableViewCellAccessoryNone;
			//Updat previous index
			_previousInputIndex = indexPath;
		}
			break;
		case DT_SECTION_VIEW:
		{
			//Remove V from previous selection
			UITableViewCell *previousCell = [_tableView cellForRowAtIndexPath:_previousViewIndex];
			if (previousCell != nil)
				previousCell.accessoryType = UITableViewCellAccessoryNone;
			//Updat previous index
			_previousViewIndex = indexPath;
		}
			break;
		default:
			break;
	}
	//Add V to current selection
	UITableViewCell *selectedCell = [_tableView cellForRowAtIndexPath:indexPath];
	if (selectedCell != nil)
		selectedCell.accessoryType = UITableViewCellAccessoryCheckmark;
	
	//Post settings notification
	NSDictionary *userInfo = [[NSDictionary alloc] initWithObjects:[NSArray arrayWithObjects:[NSNumber numberWithInt:indexPath.section],[NSNumber numberWithInt:indexPath.row],nil]
														   forKeys:[NSArray arrayWithObjects:@"section",@"row",nil]];
	[[NSNotificationCenter defaultCenter] postNotificationName:dtSettingsChangedNotification
														object:nil
													  userInfo:userInfo];
	[userInfo release];
	
}

@end