//
//  DTFileReader.h
//  OCDT
//
//  Created by Tom Susel on 11/21/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import <Foundation/Foundation.h>


@interface DTFileReader : NSObject {
	NSFileHandle		*_fileHandle;
	NSMutableString		*_buffer;	
}

- (id) initWithFilePath:(NSString*)file;

//Returns the string of the next line not including linebreaks
// or nil for end of line
- (NSString*) readLine;

@end

// private methods
@interface DTFileReader (Private)
-(NSString*) getNextLineFromBuffer;
-(void) readDataToBuffer;
@end