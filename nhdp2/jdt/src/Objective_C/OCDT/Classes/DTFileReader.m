//
//  DTFileReader.m
//  OCDT
//
//  Created by Tom Susel on 11/21/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import "DTFileReader.h"


@implementation DTFileReader

#define BUFFER_SIZE 128

- (id) initWithFilePath:(NSString*)file{
	if (self = [super init]){
		_fileHandle = [[NSFileHandle fileHandleForReadingAtPath:file] retain];
		_buffer = [[NSMutableString alloc] initWithCapacity:BUFFER_SIZE];
		[_buffer setString:@""];
		[self readDataToBuffer];
	}
#ifdef DEBUG_MODE
	NSLog(@"DTFileReader initiated %@\n", file);
#endif
	return self;
}

- (void)dealloc{
	[_fileHandle closeFile];
	[_fileHandle release];
	[_buffer release];
#ifdef DEBUG_MODE
	NSLog(@"DTFileReader deallocated\n",);
#endif
	[super dealloc];
}

- (NSString*) readLine{
	NSString *toReturn;
	
	toReturn = [self getNextLineFromBuffer];	
	
	//Populate the buffer
	if (toReturn == nil){
		[self readDataToBuffer];
		toReturn = [self getNextLineFromBuffer];
		if (toReturn == nil){
			//End of line reached
			return nil;
		}
		
	}	
	
	return toReturn;	
}


@end

//Private methods
@implementation DTFileReader (Private)

// Returns the next line from the buffer or nil if there is no more lines
-(NSString*) getNextLineFromBuffer{
	if ([_buffer length] <= 0)
		return nil;
	
	//Get range of line
	NSRange endOfLineRange = [_buffer rangeOfString:@"\n"];
	
	if (endOfLineRange.location == NSNotFound)
		return nil;
	
	//Get the line from the buffer
	NSString *toReturn = [_buffer substringToIndex:endOfLineRange.location];
	
	//Remove the line from the buffer
	[_buffer deleteCharactersInRange:NSMakeRange(0, endOfLineRange.location+1)];
	
	return toReturn;
}

-(void) readDataToBuffer{
	NSData *dataToAppend = [_fileHandle readDataOfLength:BUFFER_SIZE];
	
	//Check if end of file was reachedd
	if ((dataToAppend == nil) ||
		([dataToAppend length] == 0))
		return;
	
	NSString *toAppend = [[NSString alloc] initWithData:dataToAppend
											   encoding:NSASCIIStringEncoding];
	[_buffer appendString:toAppend];
	[toAppend release];
}

@end