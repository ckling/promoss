//
//  DTTestViewController.m
//  OCDT
//
//  Created by Tom Susel on 11/21/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import "DTDataViewController.h"
#import "DTTest.h"
#import "dt_defines.h"

@implementation DTDataViewController

@synthesize tableView = _tableView;
@synthesize textField = _textField;


 // The designated initializer.  Override if you create the controller programmatically and want to perform customization that is not appropriate for viewDidLoad.
- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    if (self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil]) {
        // Custom initialization
		_fileName = nil;
		_previousSelection = nil;
    }
    return self;
}

// Implement viewDidLoad to do additional setup after loading the view, typically from a nib.
- (void)viewDidLoad {
    [super viewDidLoad];
	
	_tableView.backgroundColor = [UIColor clearColor];
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
	self.tableView = nil;
	self.textField = nil;
	if (_previousSelection != nil)
		[_previousSelection release];
	if (_fileName != nil)
		[_fileName release];
	
    [super dealloc];
}

- (IBAction) saveFile:(id)sender{
	NSString *fileName = _textField.text;
	//Input check
	if (!([[fileName lowercaseString] hasSuffix:@".tsin"] ||
		[[fileName lowercaseString] hasSuffix:@".smf"])){
		UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:@"Error"
															message:@"Data file can only be with tsin/smf suffix"
														   delegate:nil
												  cancelButtonTitle:@"Ok"
												  otherButtonTitles:nil];
		[alertView show];
		[alertView release];
		return;
	}
	
	//Resign first responder
	[_textField resignFirstResponder];
	
	[[NSNotificationCenter defaultCenter] postNotificationName:dtSaveFileNotification
														object:nil
													  userInfo:[NSDictionary dictionaryWithObject:fileName forKey:@"fileName" ]];
	
	//Refresh table
	[_tableView reloadData];
}

- (IBAction) loadFile:(id)sender{	
	NSArray *filesArray = [DTDataViewController getFilesList];
	NSString *fileName = [filesArray objectAtIndex:_previousSelection.row];
	[[NSNotificationCenter defaultCenter] postNotificationName:dtLoadFileNotification
														object:nil
													  userInfo:[NSDictionary dictionaryWithObject:fileName forKey:@"fileName" ]];
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
	NSArray *filesList = [DTDataViewController getFilesList];
	cell.textLabel.text = (NSString*) [filesList objectAtIndex:indexPath.row];
	
	return cell;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section{
	//Get list of files
	NSArray *filesList = [DTDataViewController getFilesList];
	//Return list size
	return [filesList count];
}

																
#pragma mark -
#pragma mark Tableview delegate
#pragma mark -

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath{
	[_tableView deselectRowAtIndexPath:indexPath animated:YES];	
	//Remove V from previous selection
	UITableViewCell *previousCell = [_tableView cellForRowAtIndexPath:_previousSelection];
	if (previousCell != nil)
		previousCell.accessoryType = UITableViewCellAccessoryNone;
	
	//Add V to current selection
	UITableViewCell *selectedCell = [_tableView cellForRowAtIndexPath:indexPath];
	if (selectedCell != nil)
		selectedCell.accessoryType = UITableViewCellAccessoryCheckmark;
	_previousSelection = indexPath;
}

@end

@implementation DTDataViewController (Private)
+ (NSArray*) getFilesList{
	NSString *dir = [[NSBundle mainBundle] resourcePath];
	NSArray *contents = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:dir
																			error:NULL];
	
	NSMutableArray *toReturn = [[NSMutableArray alloc] initWithCapacity:[contents count]];
	for (NSString *fileName in contents){
		//Use only tsin / smf files
		if ([[fileName lowercaseString] hasSuffix:@".tsin"] || 
			[[fileName lowercaseString] hasSuffix:@".smf"]){
			[toReturn addObject:fileName];
		}
	}
	
	return [toReturn autorelease];
}

#pragma mark -
#pragma mark textField delegate
#pragma mark -

- (BOOL)textFieldShouldReturn:(UITextField *)textField{
	[_textField resignFirstResponder];
	return YES;
}
@end