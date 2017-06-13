//
//  DTTest.h
//  OCDT
//
//  Created by Tom Susel on 11/21/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import <Foundation/Foundation.h>


@class Delaunay_Triangulation, Point_dt;

@interface DTTest : NSObject {

}

+ (void) runReadWriteTest;
+ (void) runSimpleFindUsageTest;
+ (void) runSimpleTriangulationTest;

@end

@interface DTTest (Private)
+ (void) findAndPrint:(Delaunay_Triangulation*)triangulation
				point:(Point_dt*)point;
@end

