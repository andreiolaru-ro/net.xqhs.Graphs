package net.xqhs.graphs.graph;

import java.util.Comparator;

import net.xqhs.graphs.pattern.NodeP;

public class NodeAlphaComparator implements Comparator<Node>
{
	@Override
	public int compare(Node n0, Node n1)
	{
		if(n0 instanceof NodeP && ((NodeP) n0).isGeneric() && n0 instanceof NodeP && ((NodeP) n0).isGeneric()
				&& n0.getLabel().equals(n1.getLabel()))
			return ((NodeP) n0).genericIndex() - ((NodeP) n1).genericIndex();
		return n0.getLabel().compareTo(n1.getLabel());
	}
}