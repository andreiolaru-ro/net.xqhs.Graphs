package net.xqhs.graphs.nlp;

import edu.stanford.nlp.ling.IndexedWord;
import net.xqhs.graphs.graph.Node;

public class FunctionWord implements NodeWithIndex
{
	public FunctionWord(String tag, String label, int index)
	{
		super();
		this.tag = tag;
		this.label = label;
		this.index = index;
	}

	public FunctionWord(String tag, IndexedWord iw)
	{
		super();
		this.tag = tag;
		this.label = iw.word();
		this.index = iw.index();

	}

	private String	tag;
	private String	label;
	private int		index;

	/**
	 * @return the tag
	 */
	public String getTag()
	{
		return tag;
	}

	/**
	 * @param tag
	 *            the tag to set
	 */
	public void setTag(String tag)
	{
		this.tag = tag;
	}

	/**
	 * @return the label
	 */
	@Override
	public String getLabel()
	{
		return label;
	}

	/**
	 * @param label
	 *            the label to set
	 * @return
	 */
	@Override
	public Node setLabel(String label)
	{
		this.label = label;
		return this;
	}

	/**
	 * @return the index
	 */
	@Override
	public int getWordIndex()
	{
		return index;
	}

	/**
	 * @param index
	 *            the index to set
	 */
	@Override
	public void setWordIndex(int index)
	{
		this.index = index;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "\"" + tag + " - " + label + " [" + index + "]" + "\"";
	}
	
	/**
	 * @return string to be used when writing to a represenation.
	 */
	String toStringStorage()
	{
		return tag + "/" + label + "/" + index;
	}

	public boolean equals(NLNodeP other)
	{
		return this.getLabel().equals(other.getLabel()) && this.index == other.getWordIndex();
	}

	public boolean equalsByIndex(NLNodeP other)
	{
		return this.index == other.getWordIndex();
	}
}
