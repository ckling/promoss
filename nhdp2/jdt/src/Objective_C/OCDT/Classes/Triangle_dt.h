//
//  Triangle_dt.h
//  OCDT
//
//  Created by Tom Susel on 11/19/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import <Foundation/Foundation.h>

@class Point_dt,Circle_dt;

//static NSInteger _counter = 0;
//static NSInteger _c2 = 0;

@interface Triangle_dt : NSObject {
	Point_dt *a;
	Point_dt *b;
	Point_dt *c;
	Triangle_dt *abnext;
	Triangle_dt *bcnext;
	Triangle_dt *canext;
	Circle_dt *circum;
	int _mc;
	BOOL halfplane;
	BOOL _mark;
}

- (id) initWithPointA:(Point_dt*)A
			   pointB:(Point_dt*)B
			   pointC:(Point_dt*)C;
//+ (Triangle_dt*) triangleWithPointA:(Point_dt*)A 
//							 pointB:(Point_dt*)B
//							 pointC:(Point_dt*)C;
- (id)	initWithPointA:(Point_dt*)A 
			   pointB:(Point_dt*)B;

- (void) switchNeighbors:(Triangle_dt*)Old
					 New:(Triangle_dt*)New;
- (Triangle_dt*) neighbor:(Point_dt*)p;
 - (Circle_dt*) circumcircle;
- (BOOL) circumcircle_contains:(Point_dt*)p;
- (NSString*) toString;
- (BOOL) contains:(Point_dt*)p;
- (double) z_value:(Point_dt*)q;
- (double) z:(double)x y:(double)y;
- (Point_dt*) z:(Point_dt*)q;

- (BOOL) isEqualToTriangle:(Triangle_dt*)other;

@property (assign) BOOL halfplane;
@property (nonatomic,retain) Point_dt *a;
@property (nonatomic,retain) Point_dt *b;
@property (nonatomic,retain) Point_dt *c;
@property (nonatomic,retain) Triangle_dt *abnext;
@property (nonatomic,retain) Triangle_dt *bcnext;
@property (nonatomic,retain) Triangle_dt *canext;
@property (nonatomic,retain) Circle_dt *circum;
@property (assign) int _mc;
@property (assign) BOOL _mark;

@end
