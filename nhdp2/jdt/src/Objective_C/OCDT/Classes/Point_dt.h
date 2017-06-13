//
//  Point_dt.h
//  OCDT
//
//  Created by Tom Susel on 11/19/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import <Foundation/Foundation.h>

/**
 * This class represents a 3D point, with some simple geometric methods
 * (pointLineTest).
 */
@interface Point_dt : NSObject {
	double _x;
	double _y;
	double _z;
}

typedef enum{
	ONSEGMENT = 0,
	LEFT,		//1
	RIGHT,		//2
	INFRONTOFA,	//3
	BEHINDB,	//4
	ERROR		//5
} POINT_DT_LOCATION;

- (id) initWithX:(double)x
			   y:(double)y
			   z:(double)z;

- (id) initEmptyPoint;
//+ (Point_dt*) pointWithX:(double)x
//					  y:(double)y
//					  z:(double)z;
- (id) initWithX:(double)x
			   y:(double)y;
- (id) initWithPoint:(Point_dt*)point;

- (double) distance2:(Point_dt*)point;
- (double) distance2:(double)px
				  py:(double)py;
- (BOOL) isLess:(Point_dt*)point;
- (BOOL) isGreaterThan:(Point_dt*)point;
- (BOOL) isEqualToPoint:(Point_dt*)point;
- (NSString*) toString;
- (double) distance:(Point_dt*)point;
- (double) distance3D:(Point_dt*)point;
- (NSString*) toFile;
- (NSString*) toFileXY;

- (NSInteger) pointLineTest:(Point_dt*)a
					 pointB:(Point_dt*)b;
- (BOOL) areCollinear:(Point_dt*)a 
			   pointB:(Point_dt*)b;
- (Point_dt*) circumcenter:(Point_dt*)a 
					pointB:(Point_dt*)b;

//+ (int) compare:(id)o1 object2:(id)o2 flag:(int)_flag;

- (NSComparisonResult) compare:(id)otherPoint;
- (BOOL) isEqual:(id)object;

@property (assign) double x;
@property (assign) double y;
@property (assign) double z;

@end
