//
//  Triangle_dt.m
//  OCDT
//
//  Created by Tom Susel on 11/19/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import "Triangle_dt.h"
#import "dt_defines.h"
#import "Point_dt.h"
#import "Circle_dt.h"


@implementation Triangle_dt


@synthesize halfplane, a, b, c, abnext, bcnext, canext, circum, _mc, _mark;

- (id) initWithPointA:(Point_dt*)A
			   pointB:(Point_dt*)B
			   pointC:(Point_dt*)C{
	if (self = [super init]){
		NSInteger res = [C pointLineTest:A
								  pointB:B];
		if ( (res <= LEFT) ||
			(res == INFRONTOFA) ||
			(res == BEHINDB))
		{
			self.a = A;
			self.b = B;
			self.c = C;
		}
		else {
			NSLog(@"Warning, ajTriangle(A,B,C) expects points in counterclockwise order.\n%@%@%@\n", A,B,C);
			self.a = A;
			self.b = C;
			self.c = B;
		}
		[self circumcircle];
	}
#ifdef DEBUG_MODE
	//NSLog(@"Triangle initiated %@\n", [self toString]);
#endif
	return self;
}

- (void)dealloc{
	[a release];
	[b release];
	[c release];
	[abnext release];
	[bcnext release];
	[canext release];
	[circum release];
#ifdef DEBUG_MODE
	//NSLog(@"Triangle deallocated %@\n", [self toString]);
#endif
	[super dealloc];
}


/** constructs a triangle form 3 point - store it in counterclockwised order.*/
//+ (Triangle_dt*) triangleWithPointA:(Point_dt*)A 
//  							 pointB:(Point_dt*)B							
//							 pointC:(Point_dt*)C{
//	
//	Triangle_dt *triangle;
//	
//	NSInteger res = [C pointLineTest:A
//							  pointB:B];
//	if ( (res <= LEFT) ||
//		(res == INFRONTOFA) ||
//		(res == BEHINDB))
//	{
//		triangle = [[Triangle_dt alloc]
//					 initWithPointA:A
//					 pointB:B 
//					 pointC:C];
//	}
//	else {
//		NSLog(@"Warning, ajTriangle(A,B,C) expects points in counterclockwise order.\n%@%@%@\n", A,B,C);
//		triangle = [[Triangle_dt alloc]
//					initWithPointA:A
//					pointB:C 
//					pointC:B];
//	}
//	[triangle circumcircle];
//	return [triangle autorelease];
//}
//
/**
 * creates a half plane using the segment (A,B).
 * @param A
 * @param B
 */
- (id)	initWithPointA:(Point_dt*)A 
				pointB:(Point_dt*)B{
	if (self = [super init]){
		Point_dt *C = nil;
		
		NSInteger res = [C pointLineTest:A
								  pointB:B];
		if ( (res <= LEFT) ||
			(res == INFRONTOFA) ||
			(res == BEHINDB))
		{
			self.a = A;
			self.b = B;
			self.c = C;
		}
		else {
			NSLog(@"Warning, ajTriangle(A,B,C) expects points in counterclockwise order.\n%@%@%@\n", A,B,C);
			self.a = A;
			self.b = C;
			self.c = B;
		}
		[self circumcircle];
		//The essence of 
		self.halfplane = YES;
	}

	return self;
}

- (void) switchNeighbors:(Triangle_dt*)Old
					 New:(Triangle_dt*)New{
	if ( abnext==Old ) self.abnext=New;
    else if ( bcnext==Old ) self.bcnext=New;
    else if ( canext==Old ) self.canext=New;
    else NSLog(@"Error, switchneighbors can't find Old.\n" );	
}

- (Triangle_dt*) neighbor:(Point_dt*)p{
	if ( [a isEqualToPoint:p] ) return canext;
    if ( [b isEqualToPoint:p] ) return abnext;
    if ( [c isEqualToPoint:p] ) return bcnext;
    NSLog( @"Error, neighbors can't find p: %@\n", p );
    return nil;
}


- (Circle_dt*) circumcircle{
	double u = ((a.x-b.x)*(a.x+b.x) + (a.y-b.y)*(a.y+b.y)) / 2.0f;
    double v = ((b.x-c.x)*(b.x+c.x) + (b.y-c.y)*(b.y+c.y)) / 2.0f;
    double den = (a.x-b.x)*(b.y-c.y) - (b.x-c.x)*(a.y-b.y);
    if ( den==0 ){ // oops, degenerate case
		Circle_dt *circle = [[Circle_dt alloc] initWithPoint:a
													  radius:DBL_MAX];
		self.circum = circle;
		[circle release];
	}
    else {
		Point_dt *cen =  [[Point_dt alloc] initWithX:((u*(b.y-c.y) - v*(a.y-b.y)) / den)
												   y:((v*(a.x-b.x) - u*(b.x-c.x)) / den)];
		Circle_dt *circle = [[Circle_dt alloc] initWithPoint:cen
													  radius:[cen distance2:a]];
		[cen release];
		self.circum = circle;
		[circle release];		
    }
    return circum;
}

- (BOOL) circumcircle_contains:(Point_dt*)p{
	return (circum.Radius > [circum.Center distance2:p]);
}

- (NSString*) toString{
	if (!halfplane)
		return [NSString stringWithFormat:@"%@%@%@", 
				[a toString], 
				[b toString],
				[c toString]];
	else {
		return [NSString stringWithFormat:@"%@%@", 
				[a toString], 
				[b toString]];
	}
}

/**
 * determinates if this triangle contains the point p.
 * @param p the query point
 * @return true iff p is not null and is inside this triangle (Note: on boundary is considered inside!!).
 */
- (BOOL) contains:(Point_dt*)p{
	BOOL ans = NO;
	if(halfplane | p == nil) return NO;
	
    if((p.x==a.x& p.y==a.y) | (p.x==b.x& p.y==b.y)| (p.x==c.x& p.y==c.y)) return true;
    int a12 = [p pointLineTest:a
						pointB:b];
    int a23 = [p pointLineTest:b
						pointB:c];
    int a31 = [p pointLineTest:c
						pointB:a];
    
    if ((a12 == LEFT && a23 == LEFT && a31 == LEFT ) ||
		(a12 == RIGHT && a23 == RIGHT && a31 == RIGHT ) ||	
		(a12 == ONSEGMENT ||a23 == ONSEGMENT ||  a31 == ONSEGMENT))
		ans = YES;
	
	return ans;
}

/**
 * compute the Z value for the X,Y values of q. <br />
 * assume this triangle represent a plane --> q does NOT need to be contained
 * in this triangle.
 * 
 * @param q query point (its Z value is ignored).
 * @return the Z value of this plane implies by this triangle 3 points.
 */
- (double) z_value:(Point_dt*)q{
	NSAssert1((!(q==nil || halfplane)),@"*** ERR wrong parameters, can't approximate the z value ..***: %@", [q toString]);

  	/* incase the query point is on one of the points */
  	if(q.x==a.x & q.y==a.y) return a.z;
  	if(q.x==b.x & q.y==b.y) return b.z;
  	if(q.x==c.x & q.y==c.y) return c.z;
  	
	/* 
  	 *  plane: aX + bY + c = Z:
  	 *  2D line: y= mX + k
  	 *  
  	 */
  	double X=0,x0 = q.x, x1 = a.x, x2=b.x, x3=c.x;
  	double Y=0,y0 = q.y, y1 = a.y, y2=b.y, y3=c.y;
  	double Z=0, m01=0,k01=0,m23=0,k23=0;
	
  	// 0 - regular, 1-horisintal , 2-vertical.
  	int flag01 = 0;
  	if(x0!=x1) {
  		m01 = (y0-y1)/(x0-x1);
  		k01 = y0 - m01*x0;
  		if(m01 ==0) flag01 = 1;
  	}
  	else { // 2-vertical.
  		flag01 = 2;//x01 = x0
  	}
  	int flag23 = 0;
  	if(x2!=x3) {
  		m23 = (y2-y3)/(x2-x3);
  		k23 = y2 - m23*x2;
  		if(m23 ==0) flag23 = 1;
  	}
  	else { // 2-vertical.
  		flag23 = 2;//x01 = x0
  	}
  	
  	if(flag01 ==2 ) {
  		X = x0;
  		Y = m23*X + k23;
  	}
  	else {
  		if(flag23==2) {
  			X = x2;
  	  		Y = m01*X + k01;
  		}
  		else {  // regular case 
  			X=(k23-k01)/(m01-m23);
  			Y = m01*X+k01;
  			
  		}
  	}
  	double r = 0;
  	if(flag23==2) {r=(y2-Y)/(y2-y3);} else {r=(x2-X)/(x2-x3);}
  	Z = b.z + (c.z-b.z)*r;
  	if(flag01==2) {r=(y1-y0)/(y1-Y);} else {r=(x1-x0)/(x1-X);}
  	double qZ = a.z + (Z-a.z)*r;
  	return qZ;
}

/**
 * compute the Z value for the X,Y values of q.
 * assume this triangle represent a plane --> q does NOT need to be contained
 * in this triangle.
 *   
 * @param x  x-coordinate of the query point.
 * @param y  y-coordinate of the query point.
 * @return z (height) value approximation given by the triangle it falls in.
 * 
 */
- (double) z:(double)x y:(double)y{
	Point_dt *point = [[Point_dt alloc] initWithX:x
												y:y];
	double res = [self z_value:point];
	[point release];
	
	return res;
}

/**
 * compute the Z value for the X,Y values of q.
 * assume this triangle represent a plane --> q does NOT need to be contained
 * in this triangle.
 *   
 * @param q query point (its Z value is ignored).
 * @return q with updated Z value.
 * 
 */
- (Point_dt*) z:(Point_dt*)q{
  	double z = [self z_value:q];
	Point_dt *point = [[Point_dt alloc] initWithX:q.x
												y:q.y
												z:z];
	return [point autorelease];
}

- (BOOL) isEqualToTriangle:(Triangle_dt*)other{
	
	if (self == other)
		return YES;
	
	return ([self.a isEqualToPoint:other.a] &&
			[self.b isEqualToPoint:other.b] &&
			[self.c isEqualToPoint:other.c]);
}

- (BOOL) isEqual:(id)other{
	if (self == other)
		return YES;
	if (!other || !
		[other isKindOfClass:[self class]])
        return NO;
	return [self isEqualToTriangle:other];
}

- (NSUInteger)hash{
	int prime = 31;
	NSUInteger result = 1;
	
	result = prime * result + [a hash];
	result = prime * result + [b hash];
	result = prime * result + [c hash];
	
	return result;
}

@end
