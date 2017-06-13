//
//  Delaunay_Triangulation.m
//  OCDT
//
//  Created by Tom Susel on 11/20/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import "Delaunay_Triangulation.h"
#import "Point_dt.h"
#import "Triangle_dt.h"
#import "dt_defines.h"
#import "DTFileReader.h"

@implementation Delaunay_Triangulation

@synthesize firstP,lastP,firstT,lastT,
			currT,startTriangle,startTriangleHull,
			_bb_min,_bb_max;

@synthesize modeCounter = _modCount;

- (id) init{
	if (self = [super init]){
		nPoints = 0;
		_modCount = 0;
		_modCount2 = 0;
		_bb_min = nil;
		_bb_max = nil;
		//@TODO: @Tom: In the original implementation the vertices are a tree...
		_vertices = [[NSMutableArray alloc] init];
		_triangles = [[NSMutableArray alloc] init];
		allCollinear = YES;
	}
#ifdef DEBUG_MODE
	NSLog(@"Delaunay triangulation initiated\n");
#endif
	return self;
}

- (void)dealloc{
	if (nil != _vertices)
		[_vertices release];
	if (nil != _triangles)
		[_triangles release];
	if (nil != firstP)
		[firstP release];
	if (nil != lastP)
		[lastP release];
	if (nil != firstP)
		[firstT release];
	if (nil != lastP)
		[lastT release];
	if (nil != currT)
		[currT release];
	if (nil != startTriangle)
		[startTriangle release];
	if (nil != _bb_max)
		[_bb_min release];
	if (nil != _bb_max)
		[_bb_max release];
#ifdef DEBUG_MODE
	NSLog(@"Delaunay triangulation deallocated\n");
#endif
	[super dealloc];
}

///**
// * creates an empty Delaunay Triangulation.
// */
//+ (Delaunay_Triangulation*) delaunayTriangulation{
//	Delaunay_Triangulation *dt = [[Delaunay_Triangulation alloc] init];
//	return [dt autorelease];
//}
//
///**
// * creates a Delaunay Triangulation from all the points. Note: duplicated
// * points are ignored.
// */
//+ (Delaunay_Triangulation*) delaunayTriangulationWithPoints:(NSArray*)pointsArray{
//	Delaunay_Triangulation *dt = [[Delaunay_Triangulation alloc] init];
//	
//	for (int i = 0; 
//		 (pointsArray != nil && 
//		  i < [pointsArray count] && 
//		  [pointsArray objectAtIndex:i] != nil); 
//		 i++) {
//		
//		[dt insertPoint:(Point_dt*)[pointsArray objectAtIndex:i]];
//	}
//	return [dt autorelease];			 
//}

/**
 * creates a Delaunay Triangulation from all the points in the suggested
 * tsin file or from a smf file (off like). if the file name is .smf - read
 * it as an smf file as try to read it as .tsin <br>
 * Note: duplicated points are ignored! <br>
 * SMF file has an OFF like format (a face (f) is presented by the indexes
 * of its points - starting from 1 - not from 0): <br>
 * begin <br>
 * v x1 y1 z1 <br>
 * ... <br>
 * v xn yn zn <br>
 * f i11 i12 i13 <br>
 * ... <br>
 * f im1 im2 im3 <br>
 * end <br>
 * <br>
 * The tsin text file has the following (very simple) format <br>
 * vertices# (n) <br>
 * x1 y1 z1 <br>
 * ... <br>
 * xn yn zn <br>
 * 
 * 
 */
- (id) initWithFilePath:(NSString*)file{
	if (self = [super init]){
		nPoints = 0;
		_modCount = 0;
		_modCount2 = 0;
		_bb_min = nil;
		_bb_max = nil;
		//@TODO: @Tom: In the original implementation the vertices are a tree...
		_vertices = [[NSMutableArray alloc] init];
		_triangles = [[NSMutableArray alloc] init];
		allCollinear = YES;
		
		//Get points from file
		NSMutableArray *pointsArray = [Delaunay_Triangulation read_file:file];
		
		//Get
		for (int i = 0; 
			 (pointsArray != nil && 
			  i < [pointsArray count] && 
			  [pointsArray objectAtIndex:i] != nil); 
			 i++) {
			
			[self insertPoint:(Point_dt*)[pointsArray objectAtIndex:i]];
		}
	}
#ifdef DEBUG_MODE
	NSLog(@"Delaunay triangulation initiated\n");
#endif
	return self;
//	return [Delaunay_Triangulation delaunayTriangulationWithPoints:
//			 [Delaunay_Triangulation read_file:file]];
}

/**
 * the number of (different) vertices in this triangulation.
 * 
 * @return the number of vertices in the triangulation (duplicates are
 *         ignore - set size).
 */
- (int) size{
	if (_vertices == nil)
	{
		return 0;
	}
	return [_vertices count];
}

/**
 * @return the number of triangles in the triangulation. <br />
 * Note: includes infinife faces!!.
 */
- (int) triangleSize{
	[self initTriangles];
	return [self size];
}


/**
 * insert the point to this Delaunay Triangulation. Note: if p is null or
 * already exist in this triangulation p is ignored.
 * 
 * @param p
 *            new vertex to be inserted the triangulation.
 */
- (void) insertPoint:(Point_dt*)p{
	if ([_vertices containsObject:p])
		return;
	_modCount++;

	[self updateBoundingBox:p];
	[_vertices addObject:p];
	
	Triangle_dt *t = [self insertPointSimple:p];
	if (t == nil) // 
		return;
	Triangle_dt *tt = t;
	self.currT = t; // recall the last point for - fast (last) update iterator.
	do {
		[self flip:tt mc:_modCount];
		tt = [tt canext];
	} while (tt != t && ![tt halfplane]);
}


/**
 * returns an iterator object involved in the last update. 
 * @return iterator to all triangles involved in the last update of the
 *         triangulation NOTE: works ONLY if the are triangles (it there is
 *         only a half plane - returns an empty iterator
 */
- (NSEnumerator*) getLastUpdatedTRiangles{ //?
	NSEnumerator *toReturn;
	NSMutableArray *tmp = [[NSMutableArray alloc] init];
	if ([self triangleSize] > 1) {
		Triangle_dt *t = currT;
		[self allTriangles:t
					 front:tmp
						mc:_modCount];
	}
	toReturn = [tmp objectEnumerator];
	//@TODO: @Tom: Check if the retain is necessary
	[tmp release];
	return toReturn;
}


- (void) allTriangles:(Triangle_dt*)curr front:(NSMutableArray*)front mc:(int)mc{
	if (curr != nil && 
		curr._mc == mc && 
		![front containsObject:curr]) {

		[front addObject:curr];
		[self allTriangles:curr.abnext
					 front:front
						mc:mc];
		[self allTriangles:curr.bcnext
					 front:front
						mc:mc];
		[self allTriangles:curr.canext
					 front:front
						mc:mc];
	}
}

- (Triangle_dt*) insertPointSimple:(Point_dt*)p{
	nPoints++;
	if (!allCollinear) {
		Triangle_dt *t = [self find:startTriangle
								  p:p];
		if (t.halfplane)
			self.startTriangle = [self extendOutside:t p:p];
		else
			self.startTriangle = [self extendInside:t p:p];
		return startTriangle;
	}
	
	if (nPoints == 1) {
		self.firstP = p;
		return nil;
	}
	
	if (nPoints == 2) {
		[self startTriangulation:firstP
							  p2:p];
		return nil;
	}
	
	switch ([p pointLineTest:firstP
					  pointB:lastP]){
		case LEFT: 
			self.startTriangle = [self extendOutside:firstT.abnext
											  p:p];
			allCollinear = false;
			break;
		case RIGHT:
			self.startTriangle = [self extendOutside:firstT
											  p:p];
			
			allCollinear = false;
			break;
		case ONSEGMENT: 
			[self insertCollinear:p 
							  res:ONSEGMENT];
			break;
		case INFRONTOFA:
			[self insertCollinear:p
							  res:INFRONTOFA];
			break;
		case BEHINDB:
			[self insertCollinear:p
							  res:BEHINDB];
			break;
	}
	return nil;
}

- (void) insertCollinear:(Point_dt*)p res:(int)res{
	Triangle_dt *t, *tp, *u;
	
	switch (res) {
		case INFRONTOFA:
			t = [[Triangle_dt alloc] initWithPointA:firstP
											 pointB:p];
			tp = [[Triangle_dt alloc] initWithPointA:p
											  pointB:firstP];
			t.abnext = tp;
			tp.abnext = t;
			t.bcnext = tp;
			tp.canext = t;
			t.canext = firstT;
			firstT.bcnext = t;
			tp.bcnext = firstT.abnext;
			firstT.abnext.canext = tp;
			self.firstT = t;
			self.firstP = p;
			[t release];
			[tp release];
			break;
		case BEHINDB:
			t = [[Triangle_dt alloc] initWithPointA:p
											 pointB:lastP];
			tp = [[Triangle_dt alloc] initWithPointA:lastP
											  pointB:p];
			t.abnext = tp;
			tp.abnext = t;
			t.bcnext = lastT;
			lastT.canext = t;
			t.canext = tp;
			tp.bcnext = t;
			tp.canext = lastT.abnext;
			lastT.abnext.bcnext = tp;
			self.lastT = t;
			self.lastP = p;
			[t release];
			[tp release];
			break;
		case ONSEGMENT: 
			u = firstT;
			while ([p isGreaterThan:u.a])
				u = u.canext;
			t = [[Triangle_dt alloc] initWithPointA:p
											 pointB:u.b];
			tp = [[Triangle_dt alloc] initWithPointA:u.b
											  pointB:p];
			u.b = p;
			u.abnext.a = p;
			t.abnext = tp;
			tp.abnext = t;
			t.bcnext = u.bcnext;
			u.bcnext.canext = t;
			t.canext = u;
			u.bcnext = t;
			tp.canext = u.abnext.canext;
			u.abnext.canext.bcnext = tp;
			tp.bcnext = u.abnext;
			u.abnext.canext = tp;
			if (firstT == u) {
				self.firstT = t;
			}
			[t release];
			[tp release];
			break;
	}
}


- (void) startTriangulation:(Point_dt*)p1 p2:(Point_dt*)p2{
	
	Point_dt *ps, *pb;
	if ([p1 isLess:p2]) {
		ps = p1;
		pb = p2;
	} else {
		ps = p2;
		pb = p1;
	}
	Triangle_dt *firstTriangle = [[Triangle_dt alloc] initWithPointA:pb
															  pointB:ps];
	self.firstT = firstTriangle;
	[firstTriangle release];
	self.lastT = firstT;
	Triangle_dt *t = [[Triangle_dt alloc] initWithPointA:ps
												  pointB:pb];

	firstT.abnext = t;
	t.abnext = firstT;
	firstT.bcnext = t;
	t.canext = firstT;
	firstT.canext = t;
	t.bcnext = firstT;
	self.firstP = firstT.b;
	self.lastP = lastT.a;
	self.startTriangleHull = firstT;
	[t release];
}

- (Triangle_dt*) extendInside:(Triangle_dt*)t p:(Point_dt*)p{
	
	Triangle_dt *h1, *h2;
	h1 = [self treatDegeneracyInside:t
								   p:p];
	if (h1 != nil)
		return h1;
	
	h1 = [[Triangle_dt alloc] initWithPointA:t.c
									  pointB:t.a
									  pointC:p];
	h2 = [[Triangle_dt alloc] initWithPointA:t.b
									  pointB:t.c
									  pointC:p];
	t.c = p;
	[t circumcircle];
	h1.abnext = t.canext;
	h1.bcnext = t;
	h1.canext = h2;
	h2.abnext = t.bcnext;
	h2.bcnext = h1;
	h2.canext = t;
	[h1.abnext switchNeighbors:t
						   New:h1];
	[h2.abnext switchNeighbors:t
						   New:h2];
	t.bcnext = h2;
	t.canext = h1;
	
	[h1 release];
	[h2 release];
	return t;
}

- (Triangle_dt*) treatDegeneracyInside:(Triangle_dt*)t p:(Point_dt*)p{	
	if (t.abnext.halfplane
		&& [p pointLineTest:t.b
					 pointB:t.a] == ONSEGMENT)
		return [self extendOutside:t.abnext
								 p:p];
	if (t.bcnext.halfplane
		&& [p pointLineTest:t.c
					 pointB:t.b] == ONSEGMENT)
		return [self extendOutside:t.bcnext
								 p:p];
	if (t.canext.halfplane
		&& [p pointLineTest:t.a
					 pointB:t.c] == ONSEGMENT)
		return [self extendOutside:t.canext
								 p:p];
	return nil;
}


- (Triangle_dt*) extendOutside:(Triangle_dt*)t p:(Point_dt*)p{	
	if ([p pointLineTest:t.a
				  pointB:t.b] == ONSEGMENT) { 
		
		Triangle_dt *dg = [[Triangle_dt alloc] initWithPointA:t.a
													   pointB:t.b
													   pointC:p];
		Triangle_dt *hp = [[Triangle_dt alloc] initWithPointA:p
													   pointB:t.b];

		t.b = p;
		dg.abnext = t.abnext;
		[dg.abnext switchNeighbors:t
							   New:dg];
		dg.bcnext = hp;
		hp.abnext = dg;
		dg.canext = t;
		t.abnext = dg;
		hp.bcnext = t.bcnext;
		hp.bcnext.canext = hp;
		hp.canext = t;
		t.bcnext = hp;
		[hp release];
		return [dg autorelease];
	}
	Triangle_dt *ccT = [self extendcounterclock:t
											  p:p];
						
	Triangle_dt *cT = [self extendclock:t
									  p:p];
	ccT.bcnext = cT;
	cT.canext = ccT;
	self.startTriangleHull = cT;
	
	return cT.abnext;
}

- (Triangle_dt*) extendcounterclock:(Triangle_dt*)t p:(Point_dt*)p{	
	t.halfplane = NO;
	t.c = p;
	[t circumcircle];
	
	Triangle_dt *tca = t.canext;
	
	if ([p pointLineTest:tca.a
				  pointB:tca.b] >= RIGHT) {
		Triangle_dt *nT = [[Triangle_dt alloc] initWithPointA:t.a
													   pointB:p];
		nT.abnext = t;
		t.canext = nT;
		nT.canext = tca;
		tca.bcnext = nT;
		return [nT autorelease];
	}
	return [self extendcounterclock:tca
								  p:p];
}

- (Triangle_dt*) extendclock:(Triangle_dt*)t p:(Point_dt*)p{
	t.halfplane = NO;
	t.c = p;
	[t circumcircle];
	
	Triangle_dt *tbc = t.bcnext;
	
	if ([p pointLineTest:tbc.a
				  pointB:tbc.b] >= RIGHT) {
		Triangle_dt *nT = [[Triangle_dt alloc] initWithPointA:p
													   pointB:t.b];
		
		nT.abnext = t;
		t.bcnext = nT;
		nT.bcnext = tbc;
		tbc.canext = nT;
		return [nT autorelease];
	}
	return [self extendclock:tbc
						   p:p];
}

- (void) flip:(Triangle_dt*)t mc:(int)mc{
	Triangle_dt *u = t.abnext;
	Triangle_dt *v = nil;
	t._mc = mc;
	
	if (u.halfplane || ![u circumcircle_contains:t.c])
		return;
	
	if (t.a == u.a) {
		v = [[Triangle_dt alloc] initWithPointA:u.b
										 pointB:t.b
										 pointC:t.c];
		v.abnext = u.bcnext;
		t.abnext = u.abnext;
	} else if (t.a == u.b) {
		v = [[Triangle_dt alloc] initWithPointA:u.c
										 pointB:t.b
										 pointC:t.c];
		v.abnext = u.canext;
		t.abnext = u.bcnext;
	} else if (t.a == u.c) {
		v = [[Triangle_dt alloc] initWithPointA:u.a
										 pointB:t.b
										 pointC:t.c];
		v.abnext = u.abnext;
		t.abnext = u.canext;
	} else {
		NSAssert(NO, @"Error in flip.");
	}
	
	v._mc = mc;
	v.bcnext = t.bcnext;
	[v.abnext switchNeighbors:u
						  New:v];

	[v.bcnext switchNeighbors:t
						  New:v];
	 
	t.bcnext = v;
	v.canext = t;
	t.b = v.a;
	[t.abnext switchNeighbors:u
						  New:t];
	[t circumcircle];
	
	self.currT = v;
	[self flip:t
			mc:mc];
	[self flip:v
			mc:mc];
	if (nil != v)
		[v release];
}

//@TODO: @Tom: Check if this function works
/**
 * write all the vertices of this triangulation to a text file of the
 * following format <br>
 * #vertices (n) <br>
 * x1 y1 z1 <br>
 * ... <br>
 * xn yn zn <br>
 */
- (void) write_tsin:(NSString*)tsinFile{
	//Create the file in the file system
	[[NSFileManager defaultManager] createFileAtPath:tsinFile
											contents:[NSData data]	//Zero length data
										  attributes:nil];			//Default attributes
	
	NSFileHandle *fw = [NSFileHandle fileHandleForWritingAtPath:tsinFile];
	
	// prints the tsin file header:
	int len = [_vertices count];
	
	
	[fw writeData:
	 [[NSString stringWithFormat:@"%@\n", 
	   [[NSNumber numberWithInt:len] stringValue]] dataUsingEncoding:NSASCIIStringEncoding]];
	
	[_vertices sortUsingSelector:@selector(compare:)];
	NSEnumerator *enumerator = [_vertices objectEnumerator];
	Point_dt *currPoint;
	
	while ((currPoint = [enumerator nextObject])) {
		[fw writeData:
		 [[NSString stringWithFormat:@"%@\n",
		  [currPoint toFile]] dataUsingEncoding:NSASCIIStringEncoding]];
	}
	 
	[fw closeFile];
}

/**
 * this method write the triangulation as an SMF file (OFF like format)
 * 
 * 
 * @param smfFile
 *            - file name
 * @throws Exception
 */
- (void) write_smf:(NSString*)smfFile{
	int len = [_vertices count];
	
	[_vertices sortUsingSelector:@selector(compare:)];	
	NSMutableArray *ans = [[NSMutableArray alloc] initWithArray:_vertices];
	 
//	NSEnumerator *enumerator = [_vertices objectEnumerator];
//	//Comparator<Point_dt> comp = Point_dt.getComparator();
//	
//	for (int i = 0; i < len; i++) {
//		[ans addObject:[enumerator nextObject]];
//	}
//	
	//Create the actual file
	[[NSFileManager defaultManager] createFileAtPath:smfFile
											contents:[NSData data]	//Zero length data
										  attributes:nil];			//Default attributes
	NSFileHandle *fw = [NSFileHandle fileHandleForWritingAtPath:smfFile];
	// prints the tsin file header:
	[fw writeData:[@"begin\n" dataUsingEncoding:NSASCIIStringEncoding]];
	
	for (int i = 0; i < len; i++) {
		[fw writeData:
		 [[NSString stringWithFormat:@"v %@\n", 
		   [((Point_dt*)[ans objectAtIndex:i]) toFile]] dataUsingEncoding:NSASCIIStringEncoding]];
	}
	int t = 0, i1 = -1, i2 = -1, i3 = -1;
	
	NSEnumerator *enumerator = [self trianglesIterator];
	Triangle_dt *curr;
	
	//@TODO: @Tom - should the triangles be sorted here?
	while ((curr = [enumerator nextObject])) {
		t++;
		if (!curr.halfplane) {
			i1 = [ans indexOfObject:curr.a];
			i2 = [ans indexOfObject:curr.b];
			i3 = [ans indexOfObject:curr.c];
			if (i1 == NSNotFound ||
				i2 == NSNotFound || 
				i3 == NSNotFound)
				NSAssert(NO, @"wrong triangulation inner bug - cant write as an SMF file!");
			[fw writeData:
			 [[NSString stringWithFormat:@"f %d  %d %d\n",(i1 + 1),(i2 + 1),(i3 + 1)]
			  dataUsingEncoding:NSASCIIStringEncoding]];
		}		
	}
	[fw writeData:[@"end" dataUsingEncoding:NSASCIIStringEncoding]];
	[fw closeFile];
	
	[ans release];
}

/**
 * compute the number of vertices in the convex hull. <br />
 * NOTE: has a 'bug-like' behavor: <br />
 * in cases of colinear - not on a asix parallel rectangle,
 * colinear points are reported
 * 
 * @return the number of vertices in the convex hull.
 */
- (int) CH_size{
	int ans = 0;
	NSEnumerator *it = [self CH_vertices_Iterator];
	Point_dt *curr;
	while ((curr = [it nextObject])){
		ans++;
	}
	//@TODO: @Tom: Verify that this gives the correct result (not -1)
	//@TODO: @This can be much faster
	return ans;
}

- (void) write_CH:(NSString*)tsinFile{
	//Create the file in the file system
	[[NSFileManager defaultManager] createFileAtPath:tsinFile
											contents:[NSData data]	//Zero length data
										  attributes:nil];			//Default attributes
	
	NSFileHandle *fw = [NSFileHandle fileHandleForWritingAtPath:tsinFile];

	// prints the tsin file header:
	[fw writeData:
	 [[NSString stringWithFormat:@"%d\n",[self CH_size]] dataUsingEncoding:NSASCIIStringEncoding]];
	 
	 NSEnumerator *it = [self CH_vertices_Iterator];
	 Point_dt *curr;
	 while ((curr = [it nextObject])){		
		 [fw writeData:
		  [[curr toFileXY] dataUsingEncoding:NSASCIIStringEncoding]];
	}
	
	[fw closeFile];
}

+ (NSMutableArray*) read_file:(NSString*)file{
	if ([file hasSuffix:@".smf"]
		|| [file hasSuffix:@".SMF"])
		return [self read_smf:file];
	else
		return [self read_tsin:file];
}

+ (NSMutableArray*) read_tsin:(NSString*)tsinFile{	
	DTFileReader *fileReader = [[DTFileReader alloc] initWithFilePath:tsinFile];
	
	NSString *s = [fileReader readLine];
	
	while ([s characterAtIndex:0] == '/')
		s = [fileReader readLine];
	
	//StringTokenizer st = new StringTokenizer(s);
	NSArray *verticesArray = [s componentsSeparatedByString:@" "];
	
	int numOfVer = [((NSString*)[verticesArray objectAtIndex:0]) intValue];
	
	NSMutableArray *ans = [[NSMutableArray alloc] initWithCapacity:numOfVer];
	
	// ** reading the file verteces - insert them to the triangulation **
	for (int i = 0; i < numOfVer; i++) {
		s = [fileReader readLine];
		verticesArray = [s componentsSeparatedByString:@" "];
		double d1 = [((NSString*) [verticesArray objectAtIndex:0]) doubleValue];
		double d2 = [((NSString*) [verticesArray objectAtIndex:1]) doubleValue];
		double d3 = [((NSString*) [verticesArray objectAtIndex:2]) doubleValue];
		Point_dt *point = [[Point_dt alloc] initWithX:(int)d1
													y:(int)d2
	  												z:d3];
		[ans insertObject:point
				  atIndex:i];
		[point release];
	}
	[fileReader release];
	return [ans autorelease];
}

/*
 * SMF file has an OFF like format (a face (f) is presented by the indexes
 * of its points - starting from 1, and not from 0): 
 * begin 
 * v x1 y1 z1
 * ... 
 * v xn yn zn 
 * f i11 i12 i13 
 * ... 
 * f im1 im2 im3 
 * end 
 */
+ (NSMutableArray*) read_smf:(NSString*)smfFile{
	return [self read_smf:smfFile
					   dx:1
					   dy:1
					   dz:1
					 minX:0
					 minY:0
					 minZ:0];
}



+ (NSMutableArray*) read_smf:(NSString*)smfFile 
						  dx:(double)dx
						  dy:(double)dy
						  dz:(double)dz
						minX:(double)minX
						minY:(double)minY
						minZ:(double)minZ{
	DTFileReader *is = [[DTFileReader alloc] initWithFilePath:smfFile];

	NSString *s = [is readLine];
	
	while ([s characterAtIndex:0] != 'v')
		s = [is readLine];
	
	NSMutableArray *vec = [[NSMutableArray alloc] init];

	while (s != nil && 
		   [s characterAtIndex:0] == 'v') {
		NSArray *verticesArray = [s componentsSeparatedByString:@" "];
		double d1 = [((NSString*)[verticesArray objectAtIndex:1]) doubleValue] * dx + minX;
		double d2 = [((NSString*)[verticesArray objectAtIndex:2]) doubleValue] * dy + minY;
		double d3 = [((NSString*)[verticesArray objectAtIndex:3]) doubleValue] * dz + minZ;
		Point_dt *point = [[Point_dt alloc] initWithX:(int)d1
													y:(int)d2 
													z:d3];
		[vec addObject:point];
		[point release];
		
		s = [is readLine];
	}

	[is release];
	return [vec autorelease];
}

/**
 * finds the triangle the query point falls in, note if out-side of this
 * triangulation a half plane triangle will be returned (see contains), the
 * search has expected time of O(n^0.5), and it starts form a fixed triangle
 * (this.startTriangle),
 * 
 * @param p
 *            query point
 * @return the triangle that point p is in.
 */
- (Triangle_dt*) find:(Point_dt*)p{
	return [self find:self.startTriangle
					p:p];
}

/**
 * finds the triangle the query point falls in, note if out-side of this
 * triangulation a half plane triangle will be returned (see contains). the
 * search starts from the the start triangle
 * 
 * @param p
 *            query point
 * @param start
 *            the triangle the search starts at.
 * @return the triangle that point p is in..
 */
- (Triangle_dt*) find:(Point_dt*)p 
				start:(Triangle_dt*)start{
	if (start == nil)
		start = self.startTriangle;
	Triangle_dt *T = [self find:start
							  p:p];
	return T;
}


- (Triangle_dt*) find:(Triangle_dt*)curr 
					p:(Point_dt*)p{
	if (p == nil)
		return nil; 
	Triangle_dt *next_t;
	if (curr.halfplane) {
		next_t = [self findnext2:p
							   v:curr];
		if (next_t == nil ||
			next_t.halfplane)
			return curr;
		curr = next_t;
	}
	while (YES) {
		next_t = [self findnext1:p
							   v:curr];
		if (next_t == nil)
			return curr;
		if (next_t.halfplane)
			return next_t;
//		NSLog(@"Point is not in triangle - %@",[curr toString]);
		curr = next_t;
	}
}


/*
 * assumes v is NOT an halfplane!
 * returns the next triangle for find.
 */
- (Triangle_dt*) findnext1:(Point_dt*)p v:(Triangle_dt*)v{
	if ([p pointLineTest:v.a pointB:v.b] == RIGHT &&
		!v.abnext.halfplane)
		return v.abnext;
	if ([p pointLineTest:v.b pointB:v.c] == RIGHT &&
		!v.bcnext.halfplane)
		return v.bcnext;
	if ([p pointLineTest:v.c pointB:v.a] == RIGHT &&
		!v.canext.halfplane)
		return v.canext;
	if ([p pointLineTest:v.a pointB:v.b] == RIGHT)
		return v.abnext;
	if ([p pointLineTest:v.b pointB:v.c] == RIGHT)
		return v.bcnext;
	if ([p pointLineTest:v.c pointB:v.a] == RIGHT)
		return v.canext;
	return nil;
}

/** assumes v is an halfplane! - returns another (none halfplane) triangle */
- (Triangle_dt*) findnext2:(Point_dt*)p v:(Triangle_dt*)v{
	if (v.abnext != nil &&
		!v.abnext.halfplane)
		return v.abnext;
	if (v.bcnext != nil && 
		!v.bcnext.halfplane)
		return v.bcnext;
	if (v.canext != nil && 
		!v.canext.halfplane)
		return v.canext;
	return nil;
}

/**
 * 
 * @param p
 *            query point
 * @return true iff p is within this triangulation (in its 2D convex hull).
 */

- (BOOL) contains:(Point_dt*)p{
	Triangle_dt *tt = [self find:p];
	if (tt == nil)
		return NO;
	return !tt.halfplane;
}

/**
 * 
 * @param x
 *            - X cordination of the query point
 * @param y
 *            - Y cordination of the query point
 * @return true iff (x,y) falls inside this triangulation (in its 2D convex
 *         hull).
 */
- (BOOL) contains:(double)x y:(double)y{
	Point_dt *point = [[Point_dt alloc] initWithX:x
												y:y];
	BOOL res = [self contains:point];
	[point release];
	
	return res;

}

/**
 * 
 * @param q
 *            Query point
 * @return the q point with updated Z value (z value is as given the
 *         triangulation).
 */
- (Point_dt*) z:(Point_dt*)q{
	Triangle_dt *t = [self find:q];
	return [t z:q];
}

/**
 * 
 * @param x
 *            - X cordination of the query point
 * @param y
 *            - Y cordination of the query point
 * @return the q point with updated Z value (z value is as given the
 *         triangulation).
 */
- (double) z:(double)x y:(double)y{
	Point_dt *q = [[Point_dt alloc] initWithX:x
											y:y];
	Triangle_dt *t = [self find:q];
	double res = [t z_value:q];
	[q release];
	
	return res;
}


- (void) updateBoundingBox:(Point_dt*)p{
	
	double x = p.x, y = p.y, z = p.z;
	if (_bb_min == nil) {
		self._bb_min = [[Point_dt alloc] initWithPoint:p];
		self._bb_max = [[Point_dt alloc] initWithPoint:p];
	} else {
		if (x < _bb_min.x)
			_bb_min.x = x;
		else if (x > _bb_max.x)
			_bb_max.x = x;
		if (y < _bb_min.y)
			_bb_min.y = y;
		else if (y > _bb_max.y)
			_bb_max.y = y;
		if (z < _bb_min.z)
			_bb_min.z = z;
		else if (z > _bb_max.z)
			_bb_max.z = z;
	}
}

/**
 * return the min point of the bounding box of this triangulation
 * {{x0,y0,z0}}
 */
- (Point_dt*) minBoundingBox{
	return _bb_min;
}

/**
 * return the max point of the bounding box of this triangulation
 * {{x1,y1,z1}}
 */
- (Point_dt*) maxBoundingBox{
	return _bb_max;
}

/**
 * computes the current set (vector) of all triangles and 
 * return an iterator to them.
 * 
 * @return an iterator to the current set of all triangles. 
 */
- (NSEnumerator*) trianglesIterator{
	if ([self size] <= 2){
		[_triangles removeAllObjects];
	}
	[self initTriangles];
	return [_triangles objectEnumerator];
}


/**
 * returns an iterator to the set of all the points on the XY-convex hull
 * @return iterator to the set of all the points on the XY-convex hull.
 */
- (NSEnumerator*) CH_vertices_Iterator{
	NSMutableArray *ans = [[NSMutableArray alloc] init];
	Triangle_dt *curr = self.startTriangleHull;
	BOOL cont = YES;
	double x0 = _bb_min.x, x1 = _bb_max.x;
	double y0 = _bb_min.y, y1 = _bb_max.y;
	BOOL sx, sy;
	while (cont) {
		sx = curr.a.x == x0 || curr.a.x == x1;
		sy = curr.a.y == y0 || curr.a.y == y1;
		if ((sx & sy) | (!sx & !sy)) {
			[ans addObject:curr.a];
		}
		if (curr.bcnext != nil && 
			curr.bcnext.halfplane)
			curr = curr.bcnext;
		if (curr == [self startTriangleHull])
			cont = YES;
	}
	NSEnumerator *toReturn = [ans objectEnumerator];
	[ans release];
	return toReturn;
}

/**
 * returns an iterator to the set of points compusing this triangulation.
 * @return iterator to the set of points compusing this triangulation.
 */
- (NSEnumerator*) verticesIterator{
	return [_vertices objectEnumerator];
}

- (void) initTriangles{
	if (_modCount == _modCount2)
		return;
	if ([self size] > 2) {
		_modCount2 = _modCount;
		NSMutableArray *front = [[NSMutableArray alloc] init];
		[_triangles removeAllObjects];
		[front addObject:self.startTriangle];
		while ([front count] > 0) {
			Triangle_dt *t = [front objectAtIndex:0];
			[front removeObjectAtIndex:0];
			if (t._mark == NO) {
				t._mark = YES;
				[_triangles addObject:t];
				if (t.abnext != nil && 
					!t.abnext._mark) {
					[front addObject:t.abnext];
				}
				if (t.bcnext != nil && 
					!t.bcnext._mark) {
					[front addObject:t.bcnext];
				}
				if (t.canext != nil && 
					!t.canext._mark) {
					[front addObject:t.canext];
				}
			}
		}
		// _triNum = _triangles.size();
		for (int i = 0; i < [_triangles count]; i++) {
			((Triangle_dt*)[_triangles objectAtIndex:i])._mark = NO;
		}
		
		[front release];
	}

}

@end