//
//  DTDisplayView.h
//  OCDT
//
//  Created by Tom Susel on 11/22/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import <UIKit/UIKit.h>

static long serialVersionUID = 1L;

typedef enum{
	POINT = 1, 
	FIND = 2, 
	VIEW1 = 3, 
	VIEW2 = 4,
	VIEW3 = 5, 
	VIEW4 = 6, 
	SECTION1 = 7, 
	SECTION2 = 8, 
	GUARD = 9,
	CLIENT = 10	
}DT_GUI_OPTIONS;

@class Delaunay_Triangulation, Triangle_dt, Point_dt;

@interface DTDisplayView : UIView {
@private
	int _stage, _view_flag, _mc;
	Triangle_dt *_t1, *_t2; // tmp triangle for find testing for selection
	Delaunay_Triangulation *_ajd;
	double _topo_dz, GH, CH;
	
@protected
	NSMutableArray *_clients, *_guards;
	Point_dt *_dx_f, *_dy_f, *_dx_map, *_dy_map, *_p1, *_p2;
	BOOL _visible;
	//Visibility _loss;
}

- (id) initWithFrame:(CGRect)frame aj:(Delaunay_Triangulation*)aj;

-(void) drawTopo:(CGContextRef)context;
- (void) drawTriangle:(CGContextRef)context
					t:(Triangle_dt*)t
				   cl:(UIColor*)cl;
-(void) drawTriangleTopoLines:(CGContextRef)context
							t:(Triangle_dt*)t
						   dz:(double)dz
						   cl:(UIColor*)cl;
+ (NSMutableArray*) computePoints:(Point_dt*)p1
							   p2:(Point_dt*)p2
							   dz:(double)dz;
- (void) drawLine:(CGContextRef)context
			   p1:(Point_dt*)p1
			   p2:(Point_dt*)p2
		 moveToP1:(BOOL)moveToP1;
- (void) drawPoint:(CGContextRef)context
				p1:(Point_dt*)p1
				cl:(UIColor*)cl;	
- (void) drawPoint:(CGContextRef)context 
				p1:(Point_dt*)p1
				 r:(int)r
				cl:(UIColor*)cl;

- (void) openTextFile:(NSString*)fileName;
- (void) saveTextFile:(NSString*)fileName;

- (void) clearDisplay;

@property (nonatomic, retain) Triangle_dt *t1;
@property (nonatomic, retain) Triangle_dt *t2;
@property (nonatomic, retain) Delaunay_Triangulation *ajd;
@property (nonatomic, retain) Point_dt *dx_f;
@property (nonatomic, retain) Point_dt *dy_f;
@property (nonatomic, retain) Point_dt *dx_map;
@property (nonatomic, retain) Point_dt *dy_map;
@property (nonatomic, retain) Point_dt *p1;
@property (nonatomic, retain) Point_dt *p2;
@property (assign) int view_flag;
@property (assign) int stage;

@end

@interface DTDisplayView (Private)
- (Point_dt*) screen2world:(Point_dt*)p;
- (Point_dt*) screen2world_fixed:(Point_dt*)p;
- (Point_dt*) world2screen:(Point_dt*)p;
+ (double) transform:(Point_dt*)range
				   x:(double)x
		   new_range:(Point_dt*)new_range;
+ (double) transformY:(Point_dt*)range
					x:(double)x
			new_range:(Point_dt*)new_range;
+ (double) transformY_fixed:(Point_dt*)range
					x:(double)x
			new_range:(Point_dt*)new_range;
@end