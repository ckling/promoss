//
//  Point_dt.m
//  OCDT
//
//  Created by Tom Susel on 11/19/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import "Point_dt.h"
#import "dt_defines.h"

@implementation Point_dt

@synthesize x = _x;
@synthesize y = _y;
@synthesize z = _z;


- (id) initWithX:(double)x
			   y:(double)y
			   z:(double)z{
	if (self = [super init]){
		_x = x;
		_y = y;
		_z = z;
	}
#ifdef DEBUG_MODE
	//NSLog(@"Point initiated %@\n", [self toString]);
#endif
	return self;
}

- (void)dealloc{
#ifdef DEBUG_MODE
	//NSLog(@"Point deallocated %@\n", [self toString]);
#endif
	[super dealloc];
}

/**
 * Default Constructor. <br />
 * constructs a 3D point at (0,0,0).
 */
- (id) initEmptyPoint{
	if (self = [super init]){
		_x = 0;
		_y = 0;
		_z = 0;
	}
	return self;
}
//
///** 
// * constructs a 3D point 
// */
//+ (Point_dt*) pointWithX:(double)x
//					   y:(double)y
//					   z:(double)z{
//	Point_dt *point = [[Point_dt alloc] initWithX:x
//												y:y
//												z:z];
//	return [point autorelease];
//}
//
/** constructs a 3D point with a z value of 0. */
- (id) initWithX:(double)x
			   y:(double)y{
	if (self = [super init]){
		_x = x;
		_y = y;
		_z = 0;
	}
	return self;
}

/** simple copy constructor */
- (id) initWithPoint:(Point_dt*)point{
	if (self = [super init]){
		_x = point.x;
		_y = point.y;
		_z = point.z;
	}
	return self;
}

- (double) distance2:(Point_dt*)point{
	return (point.x - _x) * (point.x - _x) + (point.y - _y) * (point.y - _y);
}

- (double) distance2:(double)px
				  py:(double)py{
	return (px - _x) * (px - _x) + (py - _y) * (py - _y);
}

- (BOOL) isLess:(Point_dt*)point{
	return (_x < point.x) || ((_x == point.x) && (_y < point.y));
}

- (BOOL) isGreaterThan:(Point_dt*)point{
	return (_x > point.x) || ((_x == point.x) && (_y > point.y));
}

/**
 * return true iff this point [x,y] coordinates are the same as p [x,y]
 * coordinates. (the z value is ignored).
 */
- (BOOL) isEqualToPoint:(Point_dt*)point{
	if (self == point)
		return YES;
	return (_x == point.x) && (_y == point.y);
}

/** return a String in the [x,y,z] format */
- (NSString*) toString{
	return [NSString stringWithFormat:@" Pt[%f,%f,%f]",_x,_y,_z];
}

/** @return the L2 distanse NOTE: 2D only!!! */
- (double) distance:(Point_dt*)point{
	double temp = pow(point.x - _x, 2) + pow(point.y - _y, 2);
	return sqrt(temp);
}

/** @return the L2 distanse NOTE: 2D only!!! */
- (double) distance3D:(Point_dt*)point{
	double temp = pow(point.x - _x, 2) + pow(point.y - _y, 2)
	+ pow(point.z - _z, 2);
	return sqrt(temp);
}

/** return a String: x y z (used by the save to file - write_tsin method). */
- (NSString*) toFile{
	return [NSString stringWithFormat:@"%f %f %f", _x, _y, _z];
}


- (NSString*) toFileXY{
	return [NSString stringWithFormat:@"%f %f", _x, _y];
}

// pointLineTest
// ===============
// simple geometry to make things easy!
/** ïïïïïa----+----bïïïïïï */

/**
 * tests the relation between this point (as a 2D [x,y] point) and a 2D
 * segment a,b (the Z values are ignored), returns one of the following:
 * LEFT, RIGHT, INFRONTOFA, BEHINDB, ONSEGMENT
 * 
 * @param a
 *            the first point of the segment.
 * @param b
 *            the second point of the segment.
 * @return the value (flag) of the relation between this point and the a,b
 *         line-segment.
 */
- (NSInteger) pointLineTest:(Point_dt*)a
					 pointB:(Point_dt*)b{
	double dx = b.x - a.x;
	double dy = b.y - a.y;
	double res = dy * (_x - a.x) - dx * (_y - a.y);
	
	if (res < 0)
		return LEFT;
	if (res > 0)
		return RIGHT;
	
	if (dx > 0) {
		if (_x < a.x)
			return INFRONTOFA;
		if (b.x < _x)
			return BEHINDB;
		return ONSEGMENT;
	}
	if (dx < 0) {
		if (_x > a.x)
			return INFRONTOFA;
		if (b.x > _x)
			return BEHINDB;
		return ONSEGMENT;
	}
	if (dy > 0) {
		if (_y < a.y)
			return INFRONTOFA;
		if (b.y < _y)
			return BEHINDB;
		return ONSEGMENT;
	}
	if (dy < 0) {
		if (_y > a.y)
			return INFRONTOFA;
		if (b.y > _y)
			return BEHINDB;
		return ONSEGMENT;
	}
	NSLog(@"Error, pointLineTest with a=b\n");
	return ERROR;
}

- (BOOL) areCollinear:(Point_dt*)a 
			   pointB:(Point_dt*)b{
	double dx = b.x - a.x;
	double dy = b.y - a.y;
	double res = dy * (_x - a.x) - dx * (_y - a.y);
	return res == 0;
}

/*
 * public ajSegment Bisector( ajPoint b) { double sx = (x+b.x)/2; double sy
 * = (y+b.y)/2; double dx = b.x-x; double dy = b.y-y; ajPoint p1 = new
 * ajPoint(sx-dy,sy+dx); ajPoint p2 = new ajPoint(sx+dy,sy-dx); return new
 * ajSegment( p1,p2 ); }
 */
- (Point_dt*) circumcenter:(Point_dt*)a 
					pointB:(Point_dt*)b{
	double u = ((a.x - b.x) * (a.x + b.x) + (a.y - b.y) * (a.y + b.y)) / 2.0f;
	double v = ((b.x - _x) * (b.x + _x) + (b.y - _y) * (b.y + _y)) / 2.0f;
	double den = (a.x - b.x) * (b.y - _y) - (b.x - _x) * (a.y - b.y);
	if (den == 0) // oops
		NSLog(@"circumcenter, degenerate case\n");
	Point_dt *point = [[Point_dt alloc] initWithX:((u * (b.y - _y) - v * (a.y - b.y)) / den)
												y:((v * (a.x - b.x) - u * (b.x - _x)) / den)];
	return [point autorelease];
}

- (NSComparisonResult) compare:(id)otherPoint{
	if (otherPoint != nil &&
		[otherPoint isKindOfClass:[Point_dt class]])
	{
		if (self.x > ((Point_dt*)otherPoint).x)
			return NSOrderedDescending;
		if (self.x < ((Point_dt*)otherPoint).x)
			return NSOrderedAscending;
		// self.x == otherPoint.x
		if (self.y > ((Point_dt*)otherPoint).y)
			return NSOrderedDescending;
		if (self.y < ((Point_dt*)otherPoint).y)
			return NSOrderedAscending;
	}
	return NSOrderedSame;
}

/** compare between two points. */
//+ (int) compare:(id)o1 object2:(id)o2 flag:(int)_flag{
//	int ans = 0;
//	if (o1 != nil && o2 != nil && 
//		[o1 isKindOfClass:[Point_dt class]] &&
//		[o2 isKindOfClass:[Point_dt class]]) {
//			
//		Point_dt *d1 = (Point_dt*) o1;
//		Point_dt *d2 = (Point_dt*) o2;
//			
//		if (_flag == 0) {
//			if (d1.x > d2.x)
//				return 1;
//			if (d1.x < d2.x)
//				return -1;
//			// x1 == x2
//			if (d1.y > d2.y)
//				return 1;
//			if (d1.y < d2.y)
//				return -1;
//		} else if (_flag == 1) {
//			if (d1.x > d2.x)
//				return -1;
//			if (d1.x < d2.x)
//				return 1;
//			// x1 == x2
//			if (d1.y > d2.y)
//				return -1;
//			if (d1.y < d2.y)
//				return 1;
//		} else if (_flag == 2) {
//			if (d1.y > d2.y)
//				return 1;
//			if (d1.y < d2.y)
//				return -1;
//			// y1 == y2
//			if (d1.x > d2.x)
//				return 1;
//			if (d1.x < d2.x)
//				return -1;
//			
//		} else if (_flag == 3) {
//			if (d1.y > d2.y)
//				return -1;
//			if (d1.y < d2.y)
//				return 1;
//			// y1 == y2
//			if (d1.x > d2.x)
//				return -1;
//			if (d1.x < d2.x)
//				return 1;
//		}
//	} else {
//		if (o1 == nil && o2 == nil)
//			return 0;
//		if (o1 == nil && o2 != nil)
//			return 1;
//		if (o1 != nil && o2 == nil)
//			return -1;
//	}
//	return ans;
//}


- (BOOL) isEqual:(id)other{
	if (self == other)
		return YES;
	if (!other || ![other isKindOfClass:[self class]])
        return NO;
	return [self isEqualToPoint:other];
}

- (NSUInteger)hash{
	int prime = 31;
	NSUInteger result = 1;
	result = prime * result + [[NSNumber numberWithDouble:_x] hash];
	result = prime * result + [[NSNumber numberWithDouble:_y] hash];
	result = prime * result + [[NSNumber numberWithDouble:_z] hash];
	return result;
}

@end
