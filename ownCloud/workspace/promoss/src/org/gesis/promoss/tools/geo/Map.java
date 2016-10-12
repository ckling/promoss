package org.gesis.promoss.tools.geo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Map {

	private String output = "";
	private int size = 3;
	private BufferedWriter bw = null;
	//colours of areas, index corresponds to id
	private String[] colours = null;

	//	public static void main(String[] args) {
	//		Map map = new Map(0,0,0);
	//		double[] lat = {1,2,3};
	//		double[] lon = {-81,72,-111};
	//		map.addPolygon(lat,lon,0.2,1);
	//	}

	public Map (double latCenter, double lonCenter, int level,int areas) {

		addText("<!DOCTYPE html>"+
				"<html>"+
				"    <head>"+
				"    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">"+
				"    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0\">"+
				"    <meta name=\"apple-mobile-web-app-capable\" content=\"yes\">"+
				"        <title>Map</title>"+
				"<script src=\"http://openlayers.org/api/2.11/OpenLayers.js\" type=\"text/javascript\"></script>"+
				"<script src=\"http://maps.googleapis.com/maps/api/js?sensor=true\" type=\"text/javascript\"></script>"+
				"        <script type=\"text/javascript\">"+
				"var map;" +
				"var selectState = [];"+
				""+
				"function init() {"+
				"    map = new OpenLayers.Map(\"map\",{projection:\"EPSG:3857\"});"+
				"map.addControl(new OpenLayers.Control.LayerSwitcher());"+
				""+
				"var gphy = new OpenLayers.Layer.Google(\"Google Physical\",{type: google.maps.MapTypeId.TERRAIN});"+
				"var gmap = new OpenLayers.Layer.Google(\"Google Streets\", {numZoomLevels: 20});"+
				"var ghyb = new OpenLayers.Layer.Google(\"Google Hybrid\",{type: google.maps.MapTypeId.HYBRID, numZoomLevels: 20});"+
				"var gsat = new OpenLayers.Layer.Google(\"Google Satellite\",{type: google.maps.MapTypeId.SATELLITE, numZoomLevels: 22});"+
				"var osm = new OpenLayers.Layer.OSM();"+
				"var blank = new OpenLayers.Layer.XYZ(\"BlankMap\","+
				"[\"http://localhost/map/white.png\","+
				" \"http://localhost/map/white.png\","+
				" \"http://localhost/map/white.png\"]);" +
				""+
				"    var toMercator = OpenLayers.Projection.transforms['EPSG:4326']['EPSG:3857'];"+
				"    var center = toMercator({x:-0.05,y:51.5});"+
				"    "+
				"    var features = [];"+
				"    var layers = [];"+
				"    var selectors = [];"+
				"    var multiPolygon = [];"+
				"    map.addControls(selectors);    "+
				"    map.addLayers([gsat,gphy, gmap, ghyb,osm,blank]);"+
				"    map.addLayers(layers);"+
				"    map.setCenter(new OpenLayers.LonLat(center.x,center.y), 0);"+
				"var layers = [];" +
				"var selectors = [];" +
				"var toMercator = OpenLayers.Projection.transforms['EPSG:4326']['EPSG:3857'];" +
				"\n\n\n" +
				"");

		for (int i=0;i<areas;i++) {
			addText("layers.push(new OpenLayers.Layer.Vector(\"Topic "+i+"\",{visibility: true,displayInLayerSwitcher:false})); ");
		}

	}



	public String getMap(String[][] words, String[][] probabilities) {

		try {
			addText("map.addLayers(layers);" +
					"}\n\n" +
					"function selectTopic(id) {" +
					"if (selectState.length > 0) {"+
					"var layers = map.getLayersByName(/Topic(.+)/);"+
					"for (var i = 0; i<layers.length;i++) {layers[i].setVisibility(selectState[i]);}"+
					"}"+
					"selectState = [];" +
					"" +
					"" +
					"if (id != null) {"+
					" var layers = map.getLayersByName(/Topic(.+)/);"+
					" for (var i = 0; i<layers.length;i++) {"+
					" selectState[i] = layers[i].getVisibility();"+
					" layers[i].setVisibility(false);}"+
					" var layers = map.getLayersByName(\"Topic \"+id);"+
					" for (var i = 0; i<layers.length;i++) {layers[i].setVisibility(true);}"+
					" }"+
					"}" +
					"" +
					"function selectTopicChange(id) {"+
					""+
					"var layers = map.getLayersByName(\"Topic \"+id);" +
					"" +
					"for (var i = 0; i<layers.length;i++) {" +
					"layers[i].setVisibility(!layers[i].getVisibility());" +
					"}" +
					"" +
					"document.getElementById(\"Topic \"+id).classList.toggle(\"active\"+id);" +
					"" +
					"}" +
					"" +
					"</script>" +
					"" +
					"<style type=\"text/css\">" +
					"html{height:100%;width:100%;margin:0;padding:0;}"+
					" body{font-size:12pt;}");
			for (int i=0;i<colours.length;i++) {
				addText(".active"+i+"{background-color:#"+colours[i]+";} ");
			}
			addText("div.box{padding:5px;float:left;}"+
					"div.outerBox{float:left;width:25%;}"+
					"div.words{float:left;font-family:monospace;padding-left: 10px;font-size:8pt;}"+
					"a{color: #222222;text-decoration: none;line-height:30px}"+
					"a.topicNames:hover{background-color:#CCFFCC;}"+
					"a.topicCheck{border: 1px solid #ccc;padding:2px 9px;margin:0px 7px;}"+
					"div.map{float:left;width: 75%;height: 100%;}"+
					"div.topicWords {width:100%;clear:left}"+
					"div.headline{font-size:12pt;background-color:#CCDDFF;text-align:center;margin:0;width:100%;border: 1px solid #ccc;}"+
					"</style>" +
					""+
					"    </head>"+
					"    <body onload=\"init()\" style=\"margin:0;padding:0;height:100%;width:100%\">"+
					"    "+
					"        "+
					"        <div id=\"map\" class=\"map\"></div>"+
					"        "+
					"        <div class=\"outerBox\"><div class=\"box topicCheck\">"+
					"        ");
			for (int i = 0; i < words.length; i++) {

				addText("<a class=\"topicCheck active"+i+"\" href=\"#\" id=\"Topic "+i+"\" onClick=\"selectTopicChange("+i+")\">&nbsp;</a> ");

				addText("<a href=\"#\" class=\"topicNames topicCheck\" onMouseOver=\"selectTopic("+i+");" +
						"document.getElementById('thead').innerHTML ='Topic "+i+"';" +
						"document.getElementById('twords').innerHTML = '");

				for (int j = 0; j < words[i].length && words[i][j]!=null; j++) {
					addText(words[i][j].replace("\\", "\\\\").replace("\"", "\\\"") + " <br />");
				}

				addText("';document.getElementById('pwords').innerHTML = '");		
				for (int j = 0; j < probabilities[i].length && probabilities[i][j]!=null; j++) {
					addText(probabilities[i][j]+ "<br />");
				}
				addText("'\" onMouseOut=\"selectTopic()\">Topic "+i+"</a><span style=\"font-size:8pt\">");

				for (int j = 0; j < Math.min(words[i].length,3) && words[i][j]!=null && Double.valueOf(probabilities[i][j]) > 0.001 ; j++) {
					addText(words[i][j].replace("\\", "\\\\").replace("\"", "\\\"") + " ");
				}
				addText("</span><br />");

			}

			addText("   " +
					"</div>"+
					"<div class=\"topicWords\">"+
					"<div id=\"thead\" class=\"headline\">Topic</div>"+
					"<div>"+
					"<div class=\"words\" id =\"twords\">"+
					"select topic..."+
					"</div>"+
					"<div class=\"words\" id =\"pwords\">"+
					"</div>"+
					"</div>"+
					"</div>"+
					"</div>"+
					"" +
					" </body>"+
					"</html>");

			if (bw != null) {
				bw.close();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return output;

	}

	public void setColours(String[] colours) {
		this.colours = colours;
	}

	public void size (int size) {
		this.size = size;
	}

	public void image (String image) {
		addText("		 var image = new google.maps.MarkerImage('"+image+"',"+
				// This marker is 20 pixels wide by 32 pixels tall.
				"		      new google.maps.Size("+size+", "+size+"),"+
				// The origin for this image is 0,0.
				"new google.maps.Point(0,0),"+
				// The anchor for this image is the base of the flagpole at 0,32.
				"new google.maps.Point(0, 0));\n");
	}
	
	public void addPolygon(double[] lat, double[] lon,double opacity, int id) {
		 addPolygon(lat, lon, opacity, id, 0);
	}
	
	public void addPolygon(double[] lat, double[] lon,double opacity, int id, int k) {

		int fix = 0;

		//fix date line bug
//		for (int i = 0;i<lon.length-1;i++) {
//			int next = (i+1);
//			double diff =  lon[i]-lon[next];
//			if (diff > 180) {
//				lon[next]+=360;
//				fix = -1;
//			}
//			else if (diff < -180) {
//				lon[next]-=360;
//				fix = 1;
//			}
//		}

		if (fix != 0) {			
			//check, if now everything is on one part of the earth, otherwise changes were bad
			if (Math.abs(lon[lon.length-1]-lon[0]) > 180) {
				//dont fix anymore
				fix = 0;
				//undo changes
				for (int i = 1;i<lon.length;i++) {
					if (lon[i]>180) { lon[i]-=360; }
					if (lon[i]<-180) { lon[i]+=360; }
				}
			}
		}

		if (fix != 0) {			
			//copy polygon on left / right side of the map
			double[] lon2 = new double[lon.length];
			System.arraycopy(lon, 0, lon2, 0, lon.length);
			for (int i = 0;i<lon2.length;i++) {
				lon2[i]+= fix * 360;
			}
			addPolygon(lat,lon2,opacity,id);
		}

		String rgb = colours[id];

		addText("var site_points=[];");

		
		//double x = (Math.log(Math.exp(k)-(0.95/c))/k;
		//      geom_point = new OpenLayers.Geometry.Point(new_lonlat.lon, new_lonlat.lat);
       // points.push(geom_point);
		for (int i = 0;i<lat.length;i++) {
			addText("var point = toMercator(new OpenLayers.Geometry.Point("+lon[i]+","+lat[i]+"));" +
					"site_points.push(point);\n");
		}

		addText(
				"linear_ring = new OpenLayers.Geometry.LinearRing(site_points);" +
						"var polygon = new OpenLayers.Geometry.Polygon(linear_ring);" +
						"var features=new OpenLayers.Feature.Vector(polygon,{},{strokeColor : '#"+rgb+"',fillColor : '#"+rgb+"',fillOpacity : "+opacity+",strokeWidth:"+k+"});" +
						"layers["+id+"].addFeatures(features);" +
				"\n\n");
	}
	
	public void addLine(double[] lat, double[] lon,double opacity, int id, int width) {

//		int fix = 0;

		//fix date line bug
//		for (int i = 0;i<lon.length-1;i++) {
//			int next = (i+1);
//			double diff =  lon[i]-lon[next];
//			if (diff > 180) {
//				lon[next]+=360;
//				fix = -1;
//			}
//			else if (diff < -180) {
//				lon[next]-=360;
//				fix = 1;
//			}
//		}

		String rgb = colours[id];

		addText("var site_points=[];");

		
		//double x = (Math.log(Math.exp(k)-(0.95/c))/k;
		//      geom_point = new OpenLayers.Geometry.Point(new_lonlat.lon, new_lonlat.lat);
       // points.push(geom_point);
		for (int i = 0;i<lat.length;i++) {
			addText("var point = toMercator(new OpenLayers.Geometry.Point("+lon[i]+","+lat[i]+"));" +
					"site_points.push(point);\n");
		}

		addText(
				"line_string = new OpenLayers.Geometry.LineString(site_points);" +
						"var features=new OpenLayers.Feature.Vector(line_string,{},{'strokeColor': '#"+rgb+"',strokeOpacity : "+opacity+",'strokeWidth':"+width+"});" +
						"layers["+id+"].addFeatures(features);" +
				"\n\n");
	}

	
	public void addPoint(double lat, double lon,double opacity, int id, int width) {

//		int fix = 0;

		//fix date line bug
//		for (int i = 0;i<lon.length-1;i++) {
//			int next = (i+1);
//			double diff =  lon[i]-lon[next];
//			if (diff > 180) {
//				lon[next]+=360;
//				fix = -1;
//			}
//			else if (diff < -180) {
//				lon[next]-=360;
//				fix = 1;
//			}
//		}

		String rgb = colours[id];

		
		//double x = (Math.log(Math.exp(k)-(0.95/c))/k;
		//      geom_point = new OpenLayers.Geometry.Point(new_lonlat.lon, new_lonlat.lat);
       // points.push(geom_point);
			addText("var point = toMercator(new OpenLayers.Geometry.Point("+lon+","+lat+"));");


		addText(
						"var features=new OpenLayers.Feature.Vector(point,{},{'fillColor': '#"+rgb+"',fillOpacity : "+opacity+",'pointRadius':"+width+",'strokeWidth':0});" +
						"layers["+id+"].addFeatures(features);" +
				"\n\n");
	}



	public void setFile (String path)  {
		try {
			File outputFile = new File(path);
			FileWriter fw;
			fw = new FileWriter(outputFile);
			bw = new BufferedWriter(fw); 
			addText(output);
			output = "";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	private void addText(String text) {

		if (bw != null) {

			try {

				bw.write(text);
				bw.flush();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {

			output += text;

		}
	}

}
