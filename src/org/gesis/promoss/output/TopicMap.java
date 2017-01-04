package org.gesis.promoss.output;

import org.gesis.promoss.tools.geo.Map;
import org.gesis.promoss.tools.text.Load;

public class TopicMap {

	public static void main(String[] args) {
		
		int cells = 10000;
		
		Load load = new Load();
		double[][] clusters = load.readFileDouble2("/home/c/work/topicmodels/maryam8/cluster_desc/cluster_0");
		String[][] topicDescriptions = load.readFileString2("/home/c/work/topicmodels/maryam8/output_HMD/100/topktopics", ",");
		
		double[][] latLonDocs = load.readFileDouble2("/home/c/work/topicmodels/maryam8/meta.txt");
		
		
		double minLat;
		double maxLat;
		double minLon;
		double maxLon;
		
		int K = topicDescriptions.length;
		
		int topWords = Math.min(topicDescriptions[0].length, 10);
		
		String[][] topicWords = new String[K][topWords];
		String[][] topicProbs = new String[K][topWords];

		
		for (int i=0;i<K;i++) {
			for (int j=0;j<topWords;j++) {
			topicWords[i][j] = topicDescriptions[i*2][j];
			topicProbs[i][j] = topicDescriptions[(i+1)*2][j];
			}
		}
		
		Map map = new Map(K);
		
		
		System.out.println(map.getMap(topicWords, topicProbs));
		
	}
	
	
}
