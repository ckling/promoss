package org.gesis.promoss.tools.geo;

import java.io.File;

import org.gesis.promoss.tools.math.BasicMath;
import org.gesis.promoss.tools.text.Text;
import org.json.*;

/**
 * @author Christoph Carl Kling
 * released under GPL 3.0
 * 
 *
 */
public class GeoJSON {

	public static void main(String[] args){
		double[][] test = {{0.1,0.3,0.6},{0.1,0.2,0.7},{0.3,0.1,0.6}};
		saveTopicMap(test, "/home/c/ownCloud/statistics/notebooks/topicmodels/test/cluster_0_geo", "/home/c/ownCloud/statistics/notebooks/topicmodels/test/geoJSON");
	}

	public static void saveTopicMap(double[][] topicProbabilities, String clusterFile, String geoJSONFolder) {

		String outFileName = geoJSONFolder+"/topic_";
		
		int K = topicProbabilities[0].length;
		String[] colours = getColours(K);


		//check if folder exists / create it
		File file = new File(geoJSONFolder);
		if (!file.exists()) file.mkdir();


		for (int k=0;k<K;k++) {
			
			String outFileTopic = outFileName + k + ".geojson";
			
			Text outFile = new Text();
			outFile.write(outFileTopic, "", false);

			JSONObject featureCollection = new JSONObject();
			featureCollection.put("type", "FeatureCollection");

			JSONArray features = new JSONArray();

			Text text = new Text();
			String line = "";
			int l = 0;
			while ((line = text.readLine(clusterFile))!=null) {

				JSONObject feature = new JSONObject();
				feature.put("type", "Feature");

				JSONObject geometry = new JSONObject();
				geometry.put("type", "Point");

				String[] lineSplit = line.split(" ");

				double lat = Double.valueOf(lineSplit[0]);
				double lon = Double.valueOf(lineSplit[1]);

				JSONArray coordinates = new JSONArray();	
				coordinates.put(0, lat);
				coordinates.put(1, lon);
				geometry.put("coordinates", coordinates);
				feature.put("geometry", geometry);


				JSONObject properties = new JSONObject();
				properties.put("color", "none");
				properties.put("fillColor", colours[k]);
				properties.put("fillOpacity", 1.0-topicProbabilities[l][k]);

				properties.put("topicProbability", topicProbabilities[l][k]);


				feature.put("properties",properties);
				
				features.put(feature);
				
				l++;
			}
			text.close();


			featureCollection.put("features", features);

			//System.out.println(featureCollection.toString());
			outFile.write(outFileTopic, featureCollection.toString(), true);

		}

	}

	public static String[] getColours(int K) {
		String[] rgb = new String[K];
		for (int k=0; k<K; k++) {
			int red = 0,green = 0,blue = 0;

			if (k%3==0) {
				red = 255;
				green =  (int) Math.floor(255.0 * ((double) k / (double)K));
				blue = 0;
			}
			else if (k%3==1) {
				red = 0;
				green =  255;
				blue =  (int) Math.floor(255.0 * ((double) k / (double)K));
			}
			if (k%3==2) {
				red = (int) Math.floor(255.0 * ((double) k / (double)K));
				green =  0;
				blue =  255;
			}

			rgb[k] = BasicMath.convertNumber(red, 16, 2) + 
					BasicMath.convertNumber(green, 16, 2) +
					BasicMath.convertNumber(blue, 16, 2);
			//rgb[k]="ff0000";
		}
		return(rgb);
	}


}
