package org.gesis.promoss.tools.geo;

import java.math.BigDecimal;

import org.gesis.promoss.tools.math.BasicMath;


public class Coordinates {
	
	//dist in kilometers
	public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
	    double earthRadius = 3958.75;
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLng = Math.toRadians(lng2-lng1);
	    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
	               Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
	               Math.sin(dLng/2) * Math.sin(dLng/2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    double dist = earthRadius * c;

	    return dist;
	    }
	
	public static double distFrom(double[] p1, double[] p2) {

			//convert to spherical coordinates if cartesian coordinates are given
			if (p1.length == 3) {
				p1=org.gesis.promoss.tools.geo.Coordinates.toSpherical(p1[0],p1[1],p1[2]);
			}
			if (p2.length == 3) {
				p2=org.gesis.promoss.tools.geo.Coordinates.toSpherical(p2[0],p2[1],p2[2]);
			}
			
			return distFrom(p1[0],p1[1],p2[0],p2[1]);
	}
	
	public static BigDecimal[] toCartBig(double lat, double lon) {
		BigDecimal[] coords = new BigDecimal[3];
		
		lat = 90 - lat;
		BigDecimal latBig = BasicMath.DegtoRad(new BigDecimal(lat));
		BigDecimal lonBig = BasicMath.DegtoRad(new BigDecimal(lon));
		
		coords[0] = BasicMath.cos(latBig).multiply(BasicMath.cos(lonBig));
		coords[1] = BasicMath.cos(latBig).multiply(BasicMath.sin(lonBig));
		coords[2] = BasicMath.sin(latBig);
		
		return(coords);
	}
	
	public static BigDecimal[][] toCartBig(double[] lats, double[] lons) {

		BigDecimal[] x= new BigDecimal[lats.length];
		BigDecimal[] y = new BigDecimal[lats.length];
		BigDecimal[] z = new BigDecimal[lats.length];
		

		for (int i = 0; i < lats.length; i++) {
			
			BigDecimal[] coords = toCartBig(lats[i],lons[i]);
			
			x[i] = coords[0];
			y[i] = coords[1];
			z[i] = coords[2];
					
		}
		
		BigDecimal[][] result = {x,y,z};
		
		return result;
		
	}
	
	public static double[] toCart(double[] latlon) {
		return toCart(latlon[0],latlon[1]);
	}
	
	public static double[] toCart(double lat, double lon) {
		double[] coords = new double[3];
		
//		lat = 90 - lat;
//		lat = BasicMath.deg2rad(lat);
//		lon = BasicMath.deg2rad(lon);
//		
//		coords[0] = Math.cos(lat) * Math.cos(lon);
//		coords[1] = Math.cos(lat) * Math.sin(lon);
//		coords[2] = Math.sin(lat);
		
		lat +=90;		
		lon += 180;
		lat = BasicMath.deg2rad(lat);
		lon = BasicMath.deg2rad(lon);
		
		coords[0] = Math.sin(lat) * Math.cos(lon);
		coords[1] = Math.sin(lat) * Math.sin(lon);
		coords[2] = Math.cos(lat);
				
		return(coords);
	}
	
	public static double[][] toCart(Double[] lats, Double[] lons) {

		double[] x= new double[lats.length];
		double[] y = new double[lats.length];
		double[] z = new double[lats.length];
		

		for (int i = 0; i < lats.length; i++) {
			
			double[] coords = toCart(lats[i],lons[i]);
			
			x[i] = coords[0];
			y[i] = coords[1];
			z[i] = coords[2];
					
		}
		
		double[][] result = {x,y,z};
		
		return result;
		
	}
	
	public static double[][] toCart(double[] lats, double[] lons) {

		double[] x= new double[lats.length];
		double[] y = new double[lats.length];
		double[] z = new double[lats.length];
		

		for (int i = 0; i < lats.length; i++) {
			
			double[] coords = toCart(lats[i],lons[i]);
			
			x[i] = coords[0];
			y[i] = coords[1];
			z[i] = coords[2];
					
		}
		
		double[][] result = {x,y,z};
		
		return result;
		
	}
	
	public static double[] toSpherical(double[] xyz){ 
				return toSpherical(xyz[0],xyz[1],xyz[2]);
	}
	
	public static double[][] toSpherical(double[] x, double[] y, double[] z){    
			
		double[] lat = new double[x.length];
		double[] lon = new double[x.length];
		
		for (int i = 0; i < x.length; i++) {
			double[] latlon = toSpherical(x[i],y[i],z[i]);
			
			lat[i] = latlon[0];
			lon[i] = latlon[1];
			
		}
		
		double[][] result = {lat,lon};
		return (result);
	}
	
	public static double[] toSpherical(double x, double y, double z){    
		
		double[] latlon = new double[2];
		
//			latlon[0] = BasicMath.RadtoDeg(Math.asin(z));   
//			latlon[0] = 90 - latlon[0];
//			latlon[1] = BasicMath.RadtoDeg(Math.atan2(y,x));
		
	
		latlon[0] = Math.acos(z);
		latlon[1] = Math.atan2(y, x);
		latlon[0] = BasicMath.rad2deg(latlon[0]);
		latlon[1] = BasicMath.rad2deg(latlon[1]);
		latlon[0] -=90;		
		if (latlon[1]<0) {
			latlon[1] +=180;
		}
		else {
			latlon[1] -=180;
		}
		
		return (latlon);
	}
		
	public static double[] mean(double[] lats, double[] lons) {

		double[][] toc = toCart(lats,lons);
		double[] x = toc[0];
		double[] y = toc[1];
		double[] z = toc[2];
		
		double mx = org.gesis.promoss.tools.math.BasicMath.mean(x);
		double my = org.gesis.promoss.tools.math.BasicMath.mean(y);
		double mz = org.gesis.promoss.tools.math.BasicMath.mean(z);

		double mlat = Math.atan2(my, mx);
		double hyp = Math.sqrt(mx*mx + my*my);
		double mlon = Math.atan2(mz, hyp);
		
		double[] result = {mlat,mlon};
		
	    return result;
	    }
	
	public static double[][] sdv(double[] lats, double[] lons, double[] mean) {
		
		double[][] result = {{},{}};
		
	    return result;
	    }
	
	/*
	 * 
	 * Given the values for the first location in the list:
	Lat1, lon1, years1, months1 and days1
	Convert lat/lon to Cartesian coordinates for first location.
	X1 = cos(lat1) * cos(lon1)
	Y1 = cos(lat1) * sin(lon1)
	Z1 = sin(lat1)
	Compute number of days for first location.
	D1= (years1 * 365.25) + (months1 * 30.4375) + days1
	if d1 = 0 then let d1 = 1 for all locations.
	Repeat steps 1-3 for all remaining locations in the list.
	Compute combined total days for all locations.
	Totdays = d1 + d2 + ... + dn
	Compute weighted average x, y and z coordinates.
	x = ((x1 * d1) + (x2 * d2) + ... + (xn * dn)) / totdays
	y = ((y1 * d1) + (y2 * d2) + ... + (yn * dn)) / totdays
	z = ((z1 * d1) + (z2 * d2) + ... + (zn * dn)) / totdays
	Convert average x, y, z coordinate to latitude and longitude.
	Lon = atan2(y, x)
	Hyp = sqrt(x * x + y * y)
	Lat = atan2(z, hyp)
	 * 
	 */
	
	
}
