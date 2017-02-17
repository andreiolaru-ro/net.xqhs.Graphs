package net.xqhs.graphs.nlp;

import java.util.ArrayList;
import java.util.HashMap;

import net.xqhs.graphs.pattern.NodeP;
import edu.stanford.nlp.ling.IndexedWord;

public class NLNodeP extends NodeP implements Nodeish {

	/**
	 *
	 */
	private static final long serialVersionUID = 6632471420865157148L;
	/**
	 *
	 */

	private String lemma;
	private String pos;

	public String getPos() {
		return pos;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

	private int wordIndex;

	public int getWordIndex() {
		return wordIndex;
	}

	public void setWordIndex(int index) {
		this.wordIndex = index;
	}

	HashMap<String, ArrayList<FunctionWord>> attributes;

	public HashMap<String, ArrayList<FunctionWord>> getAttributes() {
		return attributes;
	}

	public ArrayList<FunctionWord> getAttributes(String s) {
		if (attributes.containsKey(s)) {
			return attributes.get(s);
		}
		return null;
	}

	public void addAttribute(FunctionWord attribute) {
		ArrayList<FunctionWord> atList = attributes.get(attribute.getTag());
		if (atList == null) {
			atList = new ArrayList<FunctionWord>();
			atList.add(attribute);
			attributes.put(attribute.getTag(), atList);
			System.out.println("Added attribute " + attribute);
		} else {
			atList.add(attribute);
		}
	}

	public String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public NLNodeP(IndexedWord w) {
		super(w.word());

		this.setWordIndex(w.index());
		this.setLemma(w.lemma());
		this.pos = w.tag();
		this.attributes = new HashMap<String, ArrayList<FunctionWord>>();
	}

	/**
	 * Constructor for generic node
	 */
	public NLNodeP() {
		super();
		this.wordIndex = Integer.MAX_VALUE - this.genericIndex();
		this.attributes = new HashMap<String, ArrayList<FunctionWord>>();
	}

	public NLNodeP(NLNodeP other) {
		super(other.label);
		this.wordIndex = other.wordIndex;
		this.lemma = other.lemma;
		this.pos = other.pos;
		this.attributes = new HashMap<String, ArrayList<FunctionWord>>();

	}

	public boolean equals(NLNodeP other) {
		if (other.label.equals(this.label) && other.wordIndex == this.wordIndex)
			return true;
		return false;
	}

	public NLNodeP identity() {
		return this;
	}

}
