package net.xqhs.graphs.nlp;

import java.util.ArrayList;
import java.util.stream.Collectors;

import net.xqhs.graphs.pattern.NodeP;
import edu.stanford.nlp.ling.IndexedWord;

public class NLNodeP extends NodeP implements NLNode {

	/**
	 *
	 */
	private static final long serialVersionUID = 6632471420865157148L;
	/**
	 *
	 */

	private String lemma;
	private String pos;

	@Override
	public String getPos() {
		return pos;
	}

	@Override
	public void setPos(String pos) {
		this.pos = pos;
	}

	private int wordIndex;

	@Override
	public int getWordIndex() {
		return wordIndex;
	}

	@Override
	public void setWordIndex(int index) {
		this.wordIndex = index;
	}

	ArrayList<FunctionWord> attributes;

	@Override
	public ArrayList<FunctionWord> getAttributes() {
		return attributes;
	}

	@Override
	public ArrayList<FunctionWord> getAttributes(String s) {
		// System.out
		// .println("Determiners of word "
		// + this.getLabel()
		// + " : "
		// + attributes.stream().filter(a -> a.getTag().equals(s))
		// .collect(Collectors.toList()) + " out of "
		// + attributes);
		return (ArrayList<FunctionWord>) attributes.stream()
				.filter(a -> a.getTag().equals(s)).collect(Collectors.toList());

	}

	// public void addAttribute(FunctionWord attribute) {
	// ArrayList<FunctionWord> atList = attributes.get(attribute.getTag());
	// if (atList == null) {
	// atList = new ArrayList<FunctionWord>();
	// atList.add(attribute);
	// attributes.put(attribute.getTag(), atList);
	// System.out.println("Added attribute " + attribute);
	// } else {
	// atList.add(attribute);
	// }
	// }

	@Override
	public String getLemma() {
		return lemma;
	}

	@Override
	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public NLNodeP(IndexedWord w) {
		super(w.word());

		this.setWordIndex(w.index());
		this.setLemma(w.lemma());
		this.pos = w.tag();
		this.attributes = new ArrayList<FunctionWord>();
	}

	/**
	 * Constructor for generic node
	 */
	public NLNodeP() {
		super();
		this.wordIndex = Integer.MAX_VALUE - this.genericIndex();
		this.attributes = new ArrayList<FunctionWord>();
	}

	public NLNodeP(NLNodeP other) {
		super(other.label);
		this.wordIndex = other.wordIndex;
		this.lemma = other.lemma;
		this.pos = other.pos;
		this.attributes = new ArrayList<FunctionWord>();

	}

	public boolean equals(NLNodeP other) {
		if (other.label.equals(this.label) && other.wordIndex == this.wordIndex)
			return true;
		return false;
	}

	public NLNodeP identity() {
		return this;
	}

	@Override
	public String toString() {
		if (isGeneric())
			return this.label + this.labelIndex;
		return this.label + this.getWordIndex();
	}

}
