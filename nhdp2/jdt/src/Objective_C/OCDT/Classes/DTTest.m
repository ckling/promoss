//
//  DTTest.m
//  OCDT
//
//  Created by Tom Susel on 11/21/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import "DTTest.h"
#import "Delaunay_Triangulation.h"
#import "Point_dt.h"
#import "Triangle_dt.h"

@implementation DTTest

+ (void) runReadWriteTest{
	NSString *dir = [[NSBundle mainBundle] resourcePath];
	NSString *file = [NSString stringWithFormat:@"%@/%@", dir, @"t1-1000.tsin"];
	
	Delaunay_Triangulation *dt = [[Delaunay_Triangulation alloc] initWithFilePath:file];
	[dt write_smf:[NSString stringWithFormat:@"%@%@", file, @"_test.smf"]];
	[dt release];
}

+ (void) runSimpleFindUsageTest{
	Delaunay_Triangulation *dt = [[Delaunay_Triangulation alloc] init];
	
	Point_dt *pointA = [[Point_dt alloc] initWithX:0
												 y:0];
	Point_dt *pointB = [[Point_dt alloc] initWithX:2
												y:2];
	Point_dt *pointC = [[Point_dt alloc] initWithX:4
												y:0];
	
	[dt insertPoint:pointA];
	[dt insertPoint:pointB];
	[dt insertPoint:pointC];
	
	[pointA release];
	[pointB release];
	[pointC release];
	
	Point_dt *pointX = [[Point_dt alloc] initWithX:1.3
												y:1];
	Point_dt *pointY = [[Point_dt alloc] initWithX:4
												 y:2];
	
	[self findAndPrint:dt
				 point:pointX];
	[self findAndPrint:dt
				 point:pointY];
	
	[pointX release];
	[pointY release];
	
	[dt release];
}

+ (void) runSimpleTriangulationTest{
	Delaunay_Triangulation *dt = [[Delaunay_Triangulation alloc] init];
	
	Point_dt *pointA = [[Point_dt alloc] initWithX:0
												 y:1];
	Point_dt *pointB = [[Point_dt alloc] initWithX:2
												 y:0];
	Point_dt *pointC = [[Point_dt alloc] initWithX:2
												 y:2];
	Point_dt *pointD = [[Point_dt alloc] initWithX:4
												 y:1];
	
	[dt insertPoint:pointA];
	[dt insertPoint:pointB];
	[dt insertPoint:pointC];
	[dt insertPoint:pointD];
	
	NSEnumerator *enumerator = [dt trianglesIterator];
	Triangle_dt *curr;
	
	while ((curr = [enumerator nextObject])) {
		if (!curr.halfplane) {
			NSLog(@"%@, %@, %@",[curr.a toString], [curr.b toString], [curr.c toString]);
		}
	}
	
	[dt release];
}

@end

@implementation DTTest (Private)
+ (void) findAndPrint:(Delaunay_Triangulation*)triangulation
				point:(Point_dt*)point{
	
	Triangle_dt *triangle = [triangulation find:point];
	if (triangle.halfplane){
		NSLog(@"Point %@ is not in the triangle.", [point toString]);
	} else {
		NSLog(@"Point %@ is in the triangle.", [point toString]);
	}
}
@end