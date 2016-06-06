package jgibblda;

public class Option {

	public boolean est = true;
	public boolean estc = false;
	public boolean inf = false;
	public String dir = "";
	public String dfile = "";
	public String modelName = "";
	public double alpha = -1.0;
	public double beta = -1.0;
	public double zeta = 1.0;
	public int K = 25;
	public int R = 10;
	public int J = 500;
	public int niters = 5000;
	public int savestep = 100;
	public int twords = 100;
	public int rtopics = 5;
	public boolean withrawdata = false;
	public String wordMapFileName = "wordmap.txt";
	
	public Option(String[] input) {
		
		for (int i=0; i < input.length; i++) {
			
			if (input[i].equals("-est")) {
				 est=true;
				 estc= false;
				 inf = false;
			}
			
			if (input[i].equals("-estc")) {
				 estc= true;
				 est = false;
				 inf=false;
			}
			
			if (input[i].equals("-inf")) {
				 est=false;
				 estc= false;
				 inf = true;
			}
			
			if (input[i].equals("-modelName")) 
				modelName = input[++i];
			
			if (input[i].equals("-dfile")) 
				dfile = input[++i];
			
			if (input[i].equals("-dir")) 
				dir = input[++i];
			
			if (input[i].equals("-K")) 
				K = Integer.valueOf(input[++i]);
			
			if (input[i].equals("-R")) 
				R = Integer.valueOf(input[++i]);		
			
			if (input[i].equals("-J")) 
				J = Integer.valueOf(input[++i]);
			
			if (input[i].equals("-twords")) 
				twords = Integer.valueOf(input[++i]);
					
			if (input[i].equals("-savestep")) 
				savestep = Integer.valueOf(input[++i]);
			
			if (input[i].equals("-niters")) 
				niters = Integer.valueOf(input[++i]);
			
			if (input[i].equals("-alpha")) 
				alpha = Double.valueOf(input[++i]);
			
			if (input[i].equals("-beta")) 
				beta = Double.valueOf(input[++i]);
									
			if (input[i].equals("-zeta")) 
				zeta = Double.valueOf(input[++i]);
			
			
		}
		
	}
	
}
