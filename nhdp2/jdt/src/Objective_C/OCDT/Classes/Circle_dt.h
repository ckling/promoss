//
//  Circle_dt.h
//  OCDT
//
//  Created by Tom Susel on 11/19/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import <Foundation/Foundation.h>

@class Point_dt;

@interface Circle_dt : NSObject {
	Point_dt *c;
	double r;
}

- (id) initWithPoint:(Point_dt*)center
			  radius:(double)radius;
- (id) initWithCircle:(Circle_dt*)circ;

- (NSString*) toString;


@property (nonatomic,readonly) Point_dt *Center;
@property (readonly) double Radius;

@end
