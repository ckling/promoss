//
//  DTDisplayView.m
//  OCDT
//
//  Created by Tom Susel on 11/22/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import "DTDisplayView.h"
#import "Delaunay_Triangulation.h"
#import "Point_dt.h"
#import "Triangle_dt.h"

@implementation DTDisplayView

@synthesize t1 = _t1;
@synthesize t2 = _t2;
@synthesize ajd = _ajd;
@synthesize dx_f = _dx_f;
@synthesize dy_f = _dy_f;
@synthesize dx_map = _dx_map;
@synthesize dy_map = _dy_map;
@synthesize p1 = _p1;
@synthesize p2 = _p2;
@synthesize view_flag = _view_flag;
@synthesize stage = _stage;

- (id)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        // Initialization code
		_mc = 0;
		_topo_dz = 100.0;
		_visible = NO;
		GH = 30;
		CH = 5;		
		_stage = POINT;
		_ajd = [[Delaunay_Triangulation alloc] init];
		
		_dx_f = [[Point_dt alloc] initWithX:5
										  y:frame.size.width - 10];
		_dy_f = [[Point_dt alloc] initWithX:5
										  y:frame.size.height - 10];
		
		_dx_map = [[Point_dt alloc] initWithPoint:_dx_f];
		_dy_map = [[Point_dt alloc] initWithPoint:_dy_f];
		_clients = [[NSMutableArray alloc] init];
		_guards = [[NSMutableArray alloc] init];
		
		self.backgroundColor = [UIColor whiteColor];
    }
    return self;
}

- (id) initWithFrame:(CGRect)frame aj:(Delaunay_Triangulation*)aj{
	if (self = [super initWithFrame:frame]) {
		_mc = 0;
		_topo_dz = 100.0;
		_visible = NO;
		GH = 30;
		CH = 5;		
		_stage = POINT;
		_ajd = [aj retain];
		
		_dx_f = [[Point_dt alloc] initWithX:5
										  y:frame.size.width - 10];
		_dy_f = [[Point_dt alloc] initWithX:5
										  y:frame.size.height - 10];
		
		_dx_map = [[Point_dt alloc] initWithX:aj._bb_max.x
											y:aj._bb_min.x];
		_dy_map = [[Point_dt alloc] initWithX:aj._bb_max.y
											y:aj._bb_min.y];
		_clients = [[NSMutableArray alloc] init];
		_guards = [[NSMutableArray alloc] init];
	}
	return self;
}


//- (void)viewDidLoad{
//	NSString *dir = [[NSBundle mainBundle] resourcePath];
//	NSString *file = [NSString stringWithFormat:@"%@/%@", dir, @"t1-1000.tsin"];
//	_stage = 0;
//	_ajd = [[Delaunay_Triangulation alloc] initWithFilePath:file];
//	_dx_map = [[Point_dt alloc] initWithX:_ajd._bb_min.x
//										y:_ajd._bb_max.x];
//	_dy_map = [[Point_dt alloc] initWithX:_ajd._bb_min.y
//										y:_ajd._bb_max.y];
//	[self setNeedsDisplayInRect:self.bounds];
//}

- (void)dealloc {
	if (_ajd != nil)
		[_ajd release];
	if (_dx_f != nil)
		[_dx_f release];
	if (_dy_f != nil)
		[_dy_f release];
	if (_dx_map != nil)
		[_dx_map release];
	if (_dy_map != nil)
		[_dy_map release];
    [super dealloc];
}

- (void)drawRect:(CGRect)rect {
	NSLog (@"Draw rect started");
//    // Drawing code
//	CGContextRef context = UIGraphicsGetCurrentContext();
//	//Triangle test
//	// Drawing with a white stroke color
//	CGContextSetStrokeColorWithColor(context, [UIColor whiteColor].CGColor);
//	// Drawing with a blue fill color
//	CGContextSetFillColorWithColor(context, [UIColor blueColor].CGColor);
//	// Draw them with a 2.0 stroke width so they are a bit more visible.
//	CGContextSetLineWidth(context, 2.0);
//	 
//	CGContextMoveToPoint(context, 20.0, 20.0);
//	
//	CGContextAddLineToPoint(context, 40.0, 20.0);
//	CGContextAddLineToPoint(context, 40.0, 40.0);
//	
//	CGContextClosePath(context);
//	CGContextDrawPath(context, kCGPathFillStroke);
	
	// Drawing code
	CGContextRef context = UIGraphicsGetCurrentContext();
	// Draw them with a 2.0 stroke width so they are a bit more visible.
	CGContextSetLineWidth(context, 3.0);
	
	//Nothing to show
	if (nil == _ajd ||
		[_ajd size] == 0)
		return;
	
	_dx_f = [[Point_dt alloc] initWithX:5.0 y:self.bounds.size.width - 10.0];
	_dy_f = [[Point_dt alloc] initWithX:5.0 y:self.bounds.size.height - 10.0];
	
	Triangle_dt *curr = nil;
	NSEnumerator *enumerator = [_ajd trianglesIterator];
	while (curr = [enumerator nextObject]){
		if (! curr.halfplane)
			[self drawTriangle:context
							 t:curr
							cl:nil];
	}
	enumerator = [_ajd trianglesIterator];
	while (curr = [enumerator nextObject]) {
		if (curr.halfplane)
			[self drawTriangle:context
							 t:curr 
							cl:nil];
	}
	if (_t2 != nil)
		[self drawTriangle:context
						 t:_t2
						cl:[UIColor redColor]];
	if (_t1 != nil &&
		_stage == FIND)
		[self drawTriangle:context
						 t:_t1
						cl:[UIColor greenColor]];
	if (_view_flag == VIEW3)
		[self drawTopo:context];
	
	//debug
	if (_mc < _ajd.modeCounter){
		_mc = _ajd.modeCounter;
		int i = 0;
		Triangle_dt *curr = nil;
		for (NSEnumerator *it2 = [_ajd getLastUpdatedTRiangles];
			 curr = [it2 nextObject];) {
			i++;
			[self drawTriangle:context
							 t:curr
							cl:[UIColor cyanColor]];
		}
		NSLog(@"   MC: %d  number of triangles updated: %d", _mc, i);
	}
		
//	if (_los != nil && 
//		(_stage == SECTION1 | _stage == SECTION2)) {
//		if (_los != nil && _los._tr != null) {
//			it = _los._tr.iterator();
//			while (it.hasNext()) {
//				curr = it.next();
//				if (!curr.isHalfplane())
//					drawTriangle(g, curr, Color.RED);
//			}
//		}
//		Iterator<Point_dt> pit = _los._section.iterator();
//		int i = 0;
//		while (pit.hasNext()) {
//			Point_dt curr_p = pit.next();
//			if (curr_p != null) {
//				drawPoint(g, curr_p, Color.BLUE);
//				System.out.println(i + ") " + curr_p + "  dist _p1: "
//								   + _p1.distance(curr_p));
//				i++;
//			}
//		}
//		drawLine(g, _p1, _p2);
//	}
	/*
	 * if(_stage == GUARD | _stage == CLIENT) { if(_p1!=null)
	 * drawPoint(g,_p1,6,Color.ORANGE); if(_p2!=null) { if(_visible)
	 * drawPoint(g,_p2,6,Color.BLUE); else drawPoint(g,_p2,6, Color.RED); }
	 * }
	 */
//	if (_los == nil)
//		_los = new Visibility(_ajd);
	if (_stage == GUARD | _stage == CLIENT) {
		NSMutableArray *ccc = nil;
		if (_clients != nil)
			ccc = [[NSMutableArray alloc] initWithCapacity:[_clients count]];
		for (int i = 0; i < [ccc count]; i++){
			[ccc insertObject:[NSNumber numberWithInt:0] atIndex:i];
		}
		for (int gr = 0; _guards != nil && gr < [_guards count]; gr++) {
			Point_dt *gg = [_guards objectAtIndex:gr];
			[self drawPoint:context
						 p1:gg 
						  r:8 
						 cl:[UIColor orangeColor]];
			
			for (int c = 0; _clients != nil &&
				 c < [_clients count]; c++) {
				Point_dt *cc = [_clients objectAtIndex:c];
				[self drawPoint:context
							 p1:cc 
							  r:6
							 cl:[UIColor whiteColor]];
				// Color cl = Color.RED;
//				if (_los.los(gg, cc)) {
//					this.drawLine(g, gg, cc);
//					ccc[c]++;
//				}
			}
			if (ccc != nil)
				[ccc release];
		}
		int c1 = 0, c2 = 0;
		for (int i = 0; i < [ccc count]; i++) {
			if ([((NSNumber*)[ccc objectAtIndex:i]) intValue] > 0) {
				c1++;
				c2 += [((NSNumber*)[ccc objectAtIndex:i]) intValue];
			}
		}
		if (c1 > 0)
			NSLog(@"clients:%d visible c:%d ave:%f", [ccc count], c1, (double)c2 / (double)c1);
	}
	NSLog (@"Draw rect ended");
}



-(void) drawTopo:(CGContextRef)context{
	Triangle_dt *curr = nil;
	NSEnumerator *enumerator = [_ajd trianglesIterator];
	CGContextSetStrokeColorWithColor(context, [UIColor redColor].CGColor);
	while (curr = [enumerator nextObject]){
		if (!curr.halfplane)
			[self drawTriangleTopoLines:context
									  t:curr
									 dz:_topo_dz
									 cl:nil];
	}
}

-(void) drawTriangleTopoLines:(CGContextRef)context
							t:(Triangle_dt*)t
						   dz:(double)dz
						   cl:(UIColor*)cl{

	if (t.a.z < 0 | t.b.z < 0 | t.c.z < 0)
		return;
	
	NSMutableArray *p12 = [DTDisplayView computePoints:t.a
													p2:t.b
													dz:dz];
	NSMutableArray *p23 = [DTDisplayView computePoints:t.b
													p2:t.c
													dz:dz];	
	NSMutableArray *p31 = [DTDisplayView computePoints:t.c
													p2:t.a
													dz:dz];
	
	int i12 = 0, i23 = 0, i31 = 0;
	BOOL cont = YES;
	while (cont) {
		cont = NO;
		if (i12 < [p12 count] && i23 < [p23 count] && 
			((Point_dt*)[p12 objectAtIndex:i12]).z == 
			((Point_dt*)[p23 objectAtIndex:i23]).z) {
			CGContextSetStrokeColorWithColor(context, [UIColor yellowColor].CGColor);
			if (((int)(((Point_dt*)[p12 objectAtIndex:i12]).z) % 200) > 100)
				CGContextSetStrokeColorWithColor(context, [UIColor redColor].CGColor);
			[self drawLine:context 
						p1:[p12 objectAtIndex:i12]
						p2:[p23 objectAtIndex:i23] 
				  moveToP1:YES];
			i12++;
			i23++;
			cont = YES;
		}
		if (i23 < [p23 count] && i31 < [p31 count] && 
			((Point_dt*)[p23 objectAtIndex:i23]).z ==
			((Point_dt*)[p31 objectAtIndex:i31]).z) {
			CGContextSetStrokeColorWithColor(context, [UIColor yellowColor].CGColor);
			if (
				((int)((Point_dt*)[p23 objectAtIndex:i23]).z) % 200 > 100)
				CGContextSetStrokeColorWithColor(context, [UIColor redColor].CGColor);
			[self drawLine:context 
						p1:[p23 objectAtIndex:i23]
						p2:[p31 objectAtIndex:i31] 
				  moveToP1:YES];
			i23++;
			i31++;
			cont = YES;
		}
		if (i12 < [p12 count] && i31 < [p31 count] &&
			((Point_dt*)[p12 objectAtIndex:i12]).z ==
			((Point_dt*)[p31 objectAtIndex:i31]).z) {
			CGContextSetStrokeColorWithColor(context, [UIColor yellowColor].CGColor);
			if (
				((int)((Point_dt*)[p12 objectAtIndex:i12]).z) % 200 > 100)
				CGContextSetStrokeColorWithColor(context, [UIColor redColor].CGColor);
			[self drawLine:context 
						p1:[p12 objectAtIndex:i12]
						p2:[p31 objectAtIndex:i31] 
				  moveToP1:YES];
			i12++;
			i31++;
			cont = YES;
		}
	}
	CGContextStrokePath(context);
}


+ (NSMutableArray*) computePoints:(Point_dt*)p1
						p2:(Point_dt*)p2
						dz:(double)dz{
	NSMutableArray *ans = nil;
	
	double z1 = (p1.z > p2.z) ? p2.z : p1.z; // Get minimum
	double z2 = (p1.z > p2.z) ? p1.z : p2.z; // Get maximum
	if (z1 == z2)
		return ans;
	double zz1 = ((int) (z1 / dz)) * dz;
	if (zz1 < z1)
		zz1 += dz;
	double zz2 = ((int) (z2 / dz)) * dz;
	int len = (int) ((zz2 - zz1) / dz) + 1, i = 0;
	
	ans = [[NSMutableArray alloc] initWithCapacity:len];
	
	double DZ = p2.z - p1.z, DX = p2.x - p1.x, DY = p2.y - p1.y;
	for (double z = zz1; z <= zz2; z += dz) {
		double scale = (z - p1.z) / DZ;
		double x = p1.x + DX * scale;
		double y = p1.y + DY * scale;
		Point_dt *point = [[Point_dt alloc] initWithX:x 
													y:y 
													z:z];
		[ans insertObject:point
				  atIndex:i];
		[point release];
		i++;
	}
	return [ans autorelease];
}


- (void) drawTriangle:(CGContextRef)context
					t:(Triangle_dt*)t
				   cl:(UIColor*)cl{
	if (_view_flag == VIEW1 || t.halfplane){
		if (cl != nil)
			CGContextSetStrokeColorWithColor(context, cl.CGColor);

		if (t.halfplane){
			if (cl == nil)
				CGContextSetStrokeColorWithColor(context, [UIColor blueColor].CGColor);
			[self drawLine:context
						p1:t.a 
						p2:t.b
				  moveToP1:YES];
			CGContextStrokePath(context);
		}
		else {
			if (cl == nil){
				CGContextSetStrokeColorWithColor(context, [UIColor darkGrayColor].CGColor);
			}
			[self drawLine:context
						p1:t.a 
						p2:t.b 
				  moveToP1:YES];
			[self drawLine:context
						p1:t.b 
						p2:t.c
				  moveToP1:NO];
			//Close the rectangle path
			CGContextClosePath(context);
			CGContextStrokePath(context);
		}	
	 }
	else {
		// //////////////////////////////////////////////////////////////////
		double maxZ = _ajd._bb_max.z;
		double minZ = _ajd._bb_min.z;
		double z = (t.a.z + t.b.z + t.c.z) / 3.0;
		double dz = maxZ - minZ;
		int co;
		if ((dz == 0) && (cl == nil)){
			CGContextSetStrokeColorWithColor(context, [UIColor blackColor].CGColor);
			CGContextSetFillColorWithColor(context, [UIColor lightGrayColor].CGColor);
		}
		else {			
			co = 30 + (int) (220 * ((z - minZ) / dz));
		
			if (cl == nil)
				cl = [UIColor colorWithRed:((float)co)/255.0 
									 green:((float)co)/255.0 
									  blue:((float)co)/255.0  
									 alpha:1.0];
			CGContextSetStrokeColorWithColor(context, cl.CGColor);
			CGContextSetFillColorWithColor(context, cl.CGColor);		
		}
//		int[] xx = new int[3], yy = new int[3];
		// double f = 0;
		// double dx_map = _dx_map.y()- _dx_map.x();
		// double dy_map = _dy_map.y()- _dy_map.x();
		
		// f = (t.p1().x() -_dx_map.x())/dx_map;		
		[self drawLine:context
					p1:t.a
					p2:t.b 
			  moveToP1:YES];
		[self drawLine:context
					p1:t.b
					p2:t.c 
			  moveToP1:NO];
		CGContextClosePath(context);
		CGContextFillPath(context);
		
		// ////////////////////////////////////
	}
	
	
}

- (void) drawLine:(CGContextRef)context
			   p1:(Point_dt*)p1
			   p2:(Point_dt*)p2
		 moveToP1:(BOOL)moveToP1{
	
	Point_dt *t2 = [self world2screen:p2];
	
	 if (moveToP1){
		Point_dt *t1 = [self world2screen:p1];
		CGContextMoveToPoint(context, t1.x, t1.y);
	 }
	CGContextAddLineToPoint(context, t2.x, t2.y);	
}

- (void) drawPoint:(CGContextRef)context
				p1:(Point_dt*)p1
				cl:(UIColor*)cl{
	[self drawPoint:context
				 p1:p1
				  r:4 
				 cl:cl];
}

- (void) drawPoint:(CGContextRef)context 
				p1:(Point_dt*)p1
				 r:(int)r
				cl:(UIColor*)cl{
	//Set the stroke color
	CGContextSetStrokeColorWithColor(context, cl.CGColor);
	//Set the fill color
	CGContextSetFillColorWithColor(context, cl.CGColor);
	//Create circle
	Point_dt *t1 = [self world2screen:p1];
	CGRect ellipseRect = CGRectMake(t1.x - r / 2.0, t1.y - r / 2, r, r);
	CGContextAddEllipseInRect(context, ellipseRect);
	CGContextFillEllipseInRect(context, ellipseRect);
}

#pragma mark -
#pragma mark File system
#pragma mark -

- (void) openTextFile:(NSString*)fileName{
	NSString *dir = [[NSBundle mainBundle] resourcePath];
	NSString *file = [NSString stringWithFormat:@"%@/%@", dir, fileName];
	_stage = 0;
	[_clients removeAllObjects];
	[_guards removeAllObjects];
	self.ajd = [[Delaunay_Triangulation alloc] initWithFilePath:file];
	self.dx_map = [[Point_dt alloc] initWithX:_ajd._bb_min.x
											y:_ajd._bb_max.x];
	self.dy_map = [[Point_dt alloc] initWithX:_ajd._bb_min.y
											y:_ajd._bb_max.y];
	[self setNeedsDisplayInRect:self.bounds];	
}

- (void) saveTextFile:(NSString*)fileName{
	NSString *dir = [[NSBundle mainBundle] resourcePath];
	NSString *file = [NSString stringWithFormat:@"%@/%@", dir, fileName];

	if ([[fileName lowercaseString] hasSuffix:@".tsin"])
		[_ajd write_tsin:file];
	else if ([[fileName lowercaseString] hasSuffix:@".smf"]) {
		[_ajd write_smf:file];
	}
}

#pragma mark -
#pragma mark Settings
#pragma mark -

- (void) clearDisplay{
	self.ajd = [[Delaunay_Triangulation alloc] init];
	self.dx_map = [[Point_dt alloc] initWithPoint:_dx_f];
	self.dy_map = [[Point_dt alloc] initWithPoint:_dy_f];
	[_clients release];
	_clients = nil;
	[_guards release];
	_guards = nil;
	_mc = 0;
	[self setNeedsDisplayInRect:self.bounds];
}

@end

@implementation DTDisplayView (Private)

- (Point_dt*) screen2world:(Point_dt*)p{
	double x = [DTDisplayView transform:_dx_f
							 x:p.x
					 new_range:_dx_map];
	double y = [DTDisplayView transformY:_dy_f
							  x:p.y
					  new_range:_dy_map];
	Point_dt *point = [[Point_dt alloc] initWithX:x y:y];
	return [point autorelease];
}

- (Point_dt*) screen2world_fixed:(Point_dt*)p{
	double x = [DTDisplayView transform:_dx_f
									  x:p.x
							  new_range:_dx_map];
	double y = [DTDisplayView transformY_fixed:_dy_f
									   x:p.y
							   new_range:_dy_map];
	Point_dt *point = [[Point_dt alloc] initWithX:x y:y];
	return [point autorelease];
}

- (Point_dt*) world2screen:(Point_dt*)p{
	double x = [DTDisplayView transform:_dx_map
							 x:p.x
					 new_range:_dx_f];
	double y = [DTDisplayView transform:_dy_map
							 x:p.y
					 new_range:_dy_f];

	Point_dt *point = [[Point_dt alloc] initWithX:x y:y];
	return [point autorelease];
}

+ (double) transform:(Point_dt*)range
				   x:(double)x
		   new_range:(Point_dt*)new_range{
	double dx1 = range.y - range.x;
	double dx2 = new_range.y - new_range.x;
	
	double scale = (x - range.x) / dx1;
	double ans = new_range.x + dx2 * scale;
	return ans;
}

+ (double) transformY_fixed:(Point_dt*)range
					x:(double)x
			new_range:(Point_dt*)new_range{
	double dy1 = range.y - range.x;
	double dy2 = new_range.y - new_range.x;
	
	double scale = (x - range.x) / dy1;
	double ans = new_range.x + dy2 * scale;
	return ans;
}

+ (double) transformY:(Point_dt*)range
				   x:(double)x
		   new_range:(Point_dt*)new_range{
	double dy1 = range.y - range.x;
	double dy2 = new_range.y - new_range.x;
	
	double scale = (x - range.x) / dy1;
	double ans = new_range.y - dy2 * scale;
	return ans;
}

#pragma mark -
#pragma mark Touches
#pragma mark -

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event{
	if ([touches count] != 1){
		//Pinching should not perform any manipulation
		return;
	}
	
	UITouch *touch = [touches anyObject];
	
	CGPoint touchPoint = [touch locationInView:self];
	int xx = touchPoint.x;
	int yy = touchPoint.y;
	// System.out.println("_stage: "+_stage+"  selected: "+len);
	switch (_stage) {
		case (0): {
			NSLog(@"[%d,%d]",xx,yy);
			break;
		}
		case (POINT): {
			Point_dt *q = [[Point_dt alloc] initWithX:xx y:yy];
//			Point_dt *p = [self screen2world:q];
//			[q release];
			[_ajd insertPoint:q];
			[q release];
			[self setNeedsDisplayInRect:self.bounds];
			break;
		}
		case (FIND): {
			NSLog(@"Find started");
			Point_dt *q = [[Point_dt alloc] initWithX:xx y:yy];
			Point_dt *p = [self screen2world_fixed:q];
			[q release];
			_t1 = [_ajd find:p];
			[self setNeedsDisplayInRect:self.bounds];
			NSLog(@"Find ended");
			break;
		}
		case (SECTION1): {
			Point_dt *q = [[Point_dt alloc] initWithX:xx
													y:yy];
			_p1 = [self screen2world:q];
			[q release];
			// _p1 = new Point_dt(99792.03,1073355.0,30.0);
			
			// _t1 = _ajd.find(_p1);
			_stage = SECTION2;
			break;
		}
		case (SECTION2): {
			Point_dt *q = [[Point_dt alloc] initWithX:xx
													y:yy];
			_p2 = [self screen2world:q];
			// _p2 = new Point_dt(149587.055,1040477.0,5.0);
			
			// _t2 = _ajd.find(_p2);
//			_los = new Visibility(_ajd);
// 			_los.computeSection(_p1, _p2);
			[q release];
			[self setNeedsDisplayInRect:self.bounds];
			_stage = SECTION1;
			break;
			
		}
		case (GUARD): {
			Point_dt *q = [[Point_dt alloc] initWithX:xx y:yy];
			_p1 = [self screen2world:q];
			[q release];
			if (_guards == nil)
				_guards = [[NSMutableArray alloc] init];
			Point_dt *toAdd = [[Point_dt alloc] initWithX:_p1.x
														y:_p1.y
														z:GH];
			[_guards addObject:toAdd];
			[toAdd release];
			/*
			 * if(_p2!=null) { _los = new Visibility(_ajd);
			 * _los.computeSection(_p1,_p2); _visible =
			 * _los.isVisible(30,5); }
			 */
			[self setNeedsDisplayInRect:self.bounds];
			break;
		}
		case (CLIENT): {
			Point_dt *q = [[Point_dt alloc] initWithX:xx y:yy];
			_p2 = [self screen2world:q];
			if (_clients == nil)
				_clients = [[NSMutableArray alloc] init];
			Point_dt *toAdd = [[Point_dt alloc] initWithX:_p2.x
														y:_p2.y
														z:CH];
			[_clients addObject:toAdd];
			[toAdd release];
			/*
			 * if(_p1!=null) { _los = new Visibility(_ajd);
			 * _los.computeSection(_p1,_p2); _visible =
			 * _los.isVisible(30,5); }
			 */
			[self setNeedsDisplayInRect:self.bounds];
			break;
		}			
			// //////////////
	}
}

@end
