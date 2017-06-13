//
//  FirstViewController.m
//  OCDT
//
//  Created by Tom Susel on 11/19/09.
//  Copyright __MyCompanyName__ 2009. All rights reserved.
//

#import "DTDisplayViewController.h"
#import "DTDisplayView.h"
#import "DTSettingsViewController.h"
#import "Delaunay_Triangulation.h"
#import "Point_dt.h"
#import "dt_defines.h"

@implementation DTDisplayViewController

@synthesize scrollView;


// The designated initializer. Override to perform setup that is required before the view is loaded.
- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    if (self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil]) {
        // Custom initialization
		
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
    [super viewDidLoad];
	[scrollView setContentSize:CGSizeMake(1000.0, 1000.0)];
	_displayView = [[DTDisplayView alloc] initWithFrame:CGRectMake(0.0, 0.0, 1000.0, 1000.0)];
	[scrollView addSubview:_displayView];
	
	//Register for notifications
	[[NSNotificationCenter defaultCenter] addObserver:self
											 selector:@selector(recievedLoadFileNotification:)
												 name:dtLoadFileNotification
											   object:nil];
	[[NSNotificationCenter defaultCenter] addObserver:self
											 selector:@selector(recievedSaveFileNotification:)
												 name:dtSaveFileNotification
											   object:nil];
	[[NSNotificationCenter defaultCenter] addObserver:self
											 selector:@selector(recievedClearDisplayNotification:)
												 name:dtClearDisplayNotification
											   object:nil];
	[[NSNotificationCenter defaultCenter] addObserver:self
											 selector:@selector(recievedSettingsChangedNotification:)
												 name:dtSettingsChangedNotification
											   object:nil];
}

- (void)viewWillAppear:(BOOL)animated{
	[scrollView setZoomScale:0.3 animated:YES];	
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
	//Unregister from notifications
	[[NSNotificationCenter defaultCenter] removeObserver:self
													name:dtLoadFileNotification
												  object:nil];
	[[NSNotificationCenter defaultCenter] removeObserver:self
													name:dtSaveFileNotification
												  object:nil];	
	[[NSNotificationCenter defaultCenter] removeObserver:self
													name:dtClearDisplayNotification
												  object:nil];	
	[[NSNotificationCenter defaultCenter] removeObserver:self
													name:dtSettingsChangedNotification
												  object:nil];	
	
	self.scrollView = nil;
	[_displayView release];
    [super dealloc];
}

#pragma mark -
#pragma mark Scrollview delegate
#pragma mark -

- (UIView *)viewForZoomingInScrollView:(UIScrollView *)scrollView {
	return _displayView;
}

#pragma mark -
#pragma mark notifications
#pragma mark -

- (void) recievedLoadFileNotification:(NSNotification*)aNotification{
	NSLog(@"File load started");
	NSString *fileName = [[aNotification userInfo] objectForKey:@"fileName"];
	[_displayView openTextFile:fileName];
	NSLog(@"File load ended");
}

- (void) recievedSaveFileNotification:(NSNotification*)aNotification{
	NSString *fileName = [[aNotification userInfo] objectForKey:@"fileName"];
	[_displayView saveTextFile:fileName];
}

- (void) recievedClearDisplayNotification:(NSNotification*)aNotification{
	[_displayView clearDisplay];
}

- (void) recievedSettingsChangedNotification:(NSNotification*)aNotification{
	int section = [[[aNotification userInfo] objectForKey:@"section"] intValue];
	int row = [[[aNotification userInfo] objectForKey:@"row"] intValue];
	switch (section) {
		case DT_SECTION_VIEW:
			switch (row) {
//				case DT_VIEW_CH_CELL:
//					[_displayView.ajd CH_vertices_Iterator];
//					break;
				case DT_VIEW_FIND_CELL:
					_displayView.stage = FIND;
					break;
				case DT_VIEW_INFO_CELL:
				{
					NSString *message = [[NSString alloc] initWithFormat:@"# Vertices: %d\n# Triangles: %d\nMin BB: %@\n Max BB: %@"
										 ,((nil == _displayView.ajd) ? (-1) : [_displayView.ajd size])
										 ,((nil == _displayView.ajd) ? (-1) : [_displayView.ajd triangleSize])
										 ,((nil == _displayView.ajd._bb_min) ? @"" : [_displayView.ajd._bb_min toString])
										 ,((nil == _displayView.ajd._bb_min) ? @"" : [_displayView.ajd._bb_max toString])];
					UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:@"Info"
																		message:message
																	   delegate:nil
															  cancelButtonTitle:@"Ok"
															  otherButtonTitles:nil];
					[alertView show];
					[alertView release];
					[message release];
				}
					break;
				case DT_VIEW_LINES_CELL:
					_displayView.view_flag = VIEW1;
					[_displayView setNeedsDisplayInRect:_displayView.bounds];
					break;
//				case DT_VIEW_SECTION_CELL:
//					_displayView.stage = SECTION1;
					break;
				case DT_VIEW_TOPO_CELL:
					_displayView.view_flag = VIEW3;
					[_displayView setNeedsDisplayInRect:_displayView.bounds];
					break;
				case DT_VIEW_TRIANGLES_CELL:
					_displayView.view_flag = VIEW2;
					[_displayView setNeedsDisplayInRect:_displayView.bounds];
					break;
				default:
					break;
			}
			break;
		case DT_SECTION_INPUT:
		{
			switch (row) {
				case DT_INPUT_100_RAND_PS_CELL:
				{
					double x0 = 10, 
							y0 = 60, 
							dx = _displayView.bounds.size.width - x0 - 10,
							dy = _displayView.bounds.size.height - y0 - 10;
					for (int i=0; i < 100; i++){
						double x = ((double)arc4random() / ARC4RANDOM_MAX) * dx + x0;
						double y = ((double)arc4random() / ARC4RANDOM_MAX) * dy + y0;
						Point_dt *q = [[Point_dt alloc] initWithX:x
																y:y];
						Point_dt *p = [_displayView screen2world:q];
						[q release];
						[_displayView.ajd insertPoint:p];
						[p release];						
					}
					[_displayView setNeedsDisplayInRect:_displayView.bounds];
				}
					break;
//				case DT_INPUT_CLIENT_5M:
//					_displayView.stage = CLIENT;
//					break;
//				case DT_INPUT_GUARD_30M:
//					_displayView.stage = GUARD;
					break;
				case DT_INPUT_POINT_CELL:
					_displayView.stage = POINT;
				default:
					break;
			}
		}
			break;
		default:
			break;
	}
}

@end
