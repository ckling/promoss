package ckling.inference;

class InferenceThread extends Thread {	
	
	private PracticalInference pi;
	private int id;
	private int rhot;
	
	public InferenceThread (PracticalInference pi, int id, int rhot) {
		super ("Inference thread "+id);
		this.pi = pi;
		this.rhot = rhot;

		start();
	}
	
	public void run() {
			pi.inferenceDoc(id);
	}

	
}