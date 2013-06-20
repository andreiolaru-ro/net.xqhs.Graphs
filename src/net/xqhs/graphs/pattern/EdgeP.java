package net.xqhs.graphs.pattern;

import net.xqhs.graphs.graph.SimpleEdge;

public class EdgeP extends SimpleEdge
{
	boolean	generic	= false;
	
	public EdgeP(NodeP fromNode, NodeP toNode, String edgeLabel)
	{ // TODO: why does this constructor exist?
		super(fromNode, toNode, edgeLabel);
	}
}