package org.gesis.promoss.tools.text;


public class Document {

	//Unique ID of the document
	protected int ID;
	//Set of words and their frequencies
	protected Object words;
	//Arbitrary metdata for the document
	protected Object metadata ;
	
	Document (int ID, Object words, Object metadata) {
		this.ID = ID;
		this.words = words;
		this.metadata = metadata;
	}
	
	public int getID() { return ID; }
	public Object getWords() { return words; }
	public Object getMetadata() { return metadata; }

	
}
