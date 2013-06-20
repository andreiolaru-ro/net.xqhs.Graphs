package net.xqhs.graphs.pattern;

import net.xqhs.graphs.graph.SimpleNode;

public class NodeP extends SimpleNode
{
	public static final String	NODEP_LABEL			= "?";
	public static final String	NODEP_INDEX_MARK	= "#";
	
	boolean						generic				= false;
	int							labelIndex			= 0;		// must be greater than 0 for generic nodes;
																
	public NodeP()
	{
		super(NODEP_LABEL);
		generic = true;
	}
	
	/**
	 * WARNING: use this with grate caution;
	 * 
	 * @param genericIndex
	 *            : be absolutely certain this is not the same index with other nodes in the graph
	 */
	public NodeP(int genericIndex)
	{
		super(NODEP_LABEL);
		generic = true;
		labelIndex = genericIndex;
	}
	
	public NodeP(String label)
	{
		super(label);
	}
	
	public boolean isGeneric()
	{
		return generic;
	}
	
	public int genericIndex()
	{
		return labelIndex;
	}
	
	@Override
	public String toString()
	{
		return super.toString() + (labelIndex > 0 ? NODEP_INDEX_MARK + labelIndex : "");
	}
}