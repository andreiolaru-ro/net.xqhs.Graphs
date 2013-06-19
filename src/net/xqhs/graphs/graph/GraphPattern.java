package net.xqhs.graphs.graph;

import java.util.ArrayList;
import java.util.Collection;

import net.xqhs.util.logging.UnitConfigData;

public class GraphPattern extends Graph
{
	public static class NodeP extends Node
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
	
	public static class EdgeP extends Edge
	{
		boolean	generic	= false;
		
		public EdgeP(NodeP fromNode, NodeP toNode, String edgeLabel)
		{ // TODO: why does this constructor exist?
			super(fromNode, toNode, edgeLabel);
		}
	}
	
	public GraphPattern()
	{
		this(null);
	}
	
	public GraphPattern(UnitConfigData unitConfig)
	{
		super(unitConfig);
	}
	
	/**
	 * Only {@link NodeP} instances can be added to a {@link GraphPattern}. This makes it easier to work with the nodes
	 * in the pattern, because conversion won't fail.
	 */
	@Override
	public GraphPattern addNode(Node node)
	{
		if(!(node instanceof NodeP))
			throw new IllegalArgumentException("cannot add Node instances to a pattern");
		return this.addNode((NodeP) node, true);
	}
	
	public GraphPattern addNode(NodeP node, boolean doindex)
	{
		// Map<String, Integer> labelNs = new HashMap<String, Integer>();
		if(doindex)
		{
			int maxIdx = 0;
			NodeP lastEquiv = null;
			for(Node n : this.nodes)
				if((n.label.equals(node.label)) && (maxIdx <= ((NodeP) n).labelIndex))
				{
					maxIdx = ((NodeP) n).labelIndex;
					lastEquiv = (NodeP) n;
				}
			if((lastEquiv != null) && (maxIdx == 0))
			{
				lastEquiv.labelIndex++;
				maxIdx = lastEquiv.labelIndex;
			}
			if(maxIdx > 0)
				node.labelIndex = maxIdx + 1;
		}
		super.addNode(node);
		return this;
	}
	
	public Collection<NodeP> getNodesP()
	{
		Collection<NodeP> ret = new ArrayList<>();
		for(Node n : getNodes())
			ret.add((NodeP) n);
		return ret;
	}
}
