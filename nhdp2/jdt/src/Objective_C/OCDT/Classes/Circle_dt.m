//
//  Circle_dt.m
//  OCDT
//
//  Created by Tom Susel on 11/19/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import "Circle_dt.h"
#import "Point_dt.h"

@implementation Circle_dt

@synthesize Center = c;
@synthesize Radius = r;

- (id) initWithPoint:(Point_dt*)center
			  radius:(double)radius{
	if (self = [super init]){
		c = [center retain];
		r = radius;
	}
#ifdef DEBUG_MODE
	NSLog(@"Circle initiated %@\n", [self toString]);
#endif
	return self;
}

- (void)dealloc{
	[c release];
#ifdef DEBUG_MODE
	NSLog(@"Circle deallocated %@\n", [self toString]);
#endif
	[super dealloc];
}

- (id) initWithCircle:(Circle_dt*)circ{
	if (self = [super init]){
		c = [circ.Center retain];
		r = circ.Radius;
	}
	return self;
}

- (NSString*) toString{
	return [NSString stringWithFormat:@" Circle[%@|%f|%f]",
			[c toString], 
			self.Radius, 
			round(sqrt(self.Radius))
			];
}


@end
