//
//  Delaunay_Triangulation.h
//  OCDT
//
//  Created by Tom Susel on 11/20/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import <Foundation/Foundation.h>

/**
 * 
 * This class represents a Delaunay Triangulation. The class was written for a
 * large scale triangulation (1000 - 200,000 vertices). The application main use is 3D surface (terrain) presentation. 
 * <br>
 * The class main properties are the following:<br>
 * - fast point location. (O(n^0.5)), practical runtime is often very fast. <br>
 * - handles degenerate cases and none general position input (ignores duplicate points). <br>
 * - save & load from\to text file in TSIN format. <br>
 * - 3D support: including z value approximation. <br>
 * - standard java (1.5 generic) iterators for the vertices and triangles. <br>
 * - smart iterator to only the updated triangles - for terrain simplification <br>
 * <br>
 *  
 * Testing (done in early 2005): Platform java 1.5.02 windows XP (SP2), AMD laptop 1.6G sempron CPU
 * 512MB RAM. Constructing a triangulation of 100,000 vertices takes ~ 10
 * seconds. point location of 100,000 points on a triangulation of 100,000
 * vertices takes ~ 5 seconds.
 * 
 * Note: constructing a triangulation with 200,000 vertices and more requires
 * extending java heap size (otherwise an exception will be thrown).<br>
 * 
 * Bugs: if U find a bug or U have an idea as for how to improve the code,
 * please send me an email to: benmo@ariel.ac.il
 * 
 * @author Boaz Ben Moshe 5/11/05 <br>
 * The project uses some ideas presented in the VoroGuide project, written by Klasse f?r Kreise (1996-1997), 
 * For the original applet see: http://www.pi6.fernuni-hagen.de/GeomLab/VoroGlide/ . <br>
 */
@class Point_dt, Triangle_dt;

@interface Delaunay_Triangulation : NSObject {
	// the first and last points (used only for first step construction)
	Point_dt *firstP;
	Point_dt *lastP;
	
	// for degenerate case!
	BOOL allCollinear;
	
	// the first and last triangles (used only for first step construction)
	Triangle_dt *firstT, *lastT, *currT;
	
	// the triangle the fond (search start from
	Triangle_dt *startTriangle;
	
	// the triangle the convex hull starts from
	Triangle_dt *startTriangleHull;
	
	int nPoints; // numbr of points
	// additional data 4/8/05 used by the iterators
	NSMutableArray *_vertices;
	NSMutableArray *_triangles;
	
	int _modCount, _modCount2;
	
	// the Bounding Box, {{x0,y0,z0} , {x1,y1,z1}}
	Point_dt *_bb_min, *_bb_max;	
}

//+ (Delaunay_Triangulation*) delaunayTriangulation;
//+ (Delaunay_Triangulation*) delaunayTriangulationWithPoints:(NSArray*)pointsArray;
- (id) initWithFilePath:(NSString*)file;

- (int) size;
- (int) triangleSize;

- (void) insertPoint:(Point_dt*)p;
- (NSEnumerator*) getLastUpdatedTRiangles; //?
- (void) allTriangles:(Triangle_dt*)curr front:(NSMutableArray*)front mc:(int)mc;
- (Triangle_dt*) insertPointSimple:(Point_dt*)p;
- (void) insertCollinear:(Point_dt*)p res:(int)res;
- (void) startTriangulation:(Point_dt*)p1 p2:(Point_dt*)p2;

- (Triangle_dt*) extendInside:(Triangle_dt*)t p:(Point_dt*)p;
- (Triangle_dt*) treatDegeneracyInside:(Triangle_dt*)t p:(Point_dt*)p;
- (Triangle_dt*) extendOutside:(Triangle_dt*)t p:(Point_dt*)p;
- (Triangle_dt*) extendcounterclock:(Triangle_dt*)t p:(Point_dt*)p;
- (Triangle_dt*) extendclock:(Triangle_dt*)t p:(Point_dt*)p;
- (void) flip:(Triangle_dt*)t mc:(int)mc;

- (void) write_tsin:(NSString*)tsinFile;
- (void) write_smf:(NSString*)smfFile;

- (int) CH_size;
- (void) write_CH:(NSString*)tsinFile;
+ (NSMutableArray*) read_file:(NSString*)file;
+ (NSMutableArray*) read_tsin:(NSString*)tsinFile;
+ (NSMutableArray*) read_smf:(NSString*)smfFile;
+ (NSMutableArray*) read_smf:(NSString*)smfFile 
						  dx:(double)dx
						  dy:(double)dy
						  dz:(double)dz
						minX:(double)minX
						minY:(double)minY
						minZ:(double)minZ;
- (Triangle_dt*) find:(Point_dt*)p;
- (Triangle_dt*) find:(Point_dt*)p start:(Triangle_dt*)start;
- (Triangle_dt*) find:(Triangle_dt*)curr p:(Point_dt*)p;
- (Triangle_dt*) findnext1:(Point_dt*)p v:(Triangle_dt*)v;
- (Triangle_dt*) findnext2:(Point_dt*)p v:(Triangle_dt*)v;
- (BOOL) contains:(Point_dt*)p;
- (BOOL) contains:(double)x y:(double)y;
- (Point_dt*) z:(Point_dt*)q;
- (double) z:(double)x y:(double)y;
- (void) updateBoundingBox:(Point_dt*)p;
- (Point_dt*) minBoundingBox;
- (Point_dt*) maxBoundingBox;
- (NSEnumerator*) trianglesIterator;
- (NSEnumerator*) CH_vertices_Iterator;
- (NSEnumerator*) verticesIterator;
- (void) initTriangles;

@property (readonly) int modeCounter;
@property (nonatomic,retain) Point_dt *firstP;
@property (nonatomic,retain) Point_dt *lastP;
@property (nonatomic,retain) Triangle_dt *firstT;
@property (nonatomic,retain) Triangle_dt *lastT;
@property (nonatomic,retain) Triangle_dt *currT;
@property (nonatomic,retain) Triangle_dt *startTriangle;
@property (nonatomic,retain) Triangle_dt *startTriangleHull;
@property (nonatomic,retain) Point_dt *_bb_min;
@property (nonatomic,retain) Point_dt *_bb_max;

@end
