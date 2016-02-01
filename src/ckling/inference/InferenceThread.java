package ckling.inference;

class InferenceThread extends Thread {	
	
	private PracticalInference pi;
	public int id;
	private int start;
	private int end;
	
	public InferenceThread (PracticalInference pi, int start, int end, int id) {
		super ("Inference thread "+id);
		this.pi = pi;
		this.start = start;
		this.end = end;
		start();
	}
	
	public void run() {
		for (int m=start; m<end; m++) {
			pi.inferenceDoc(m);
		}
	}

	
}