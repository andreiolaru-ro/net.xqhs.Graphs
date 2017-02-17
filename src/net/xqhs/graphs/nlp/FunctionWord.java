package net.xqhs.graphs.nlp;

import edu.stanford.nlp.ling.IndexedWord;

public class FunctionWord implements Nodeish {
	public FunctionWord(String tag, String label, int index) {
		super();
		this.tag = tag;
		this.label = label;
		this.index = index;
	}

	public FunctionWord(String tag, IndexedWord iw) {
		super();
		this.tag = tag;
		this.label = iw.word();
		this.index = iw.index();

	}

	private String tag;
	private String label;
	private int index;

	/**
	 * @return the tag
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * @param tag
	 *            the tag to set
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label
	 *            the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @param index
	 *            the index to set
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return tag + " - " + label + " [" + index + "]";
	}

}
