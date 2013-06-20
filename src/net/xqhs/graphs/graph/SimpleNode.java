package net.xqhs.graphs.graph;

import java.util.HashSet;
import java.util.Set;

public class SimpleNode extends GraphComponent implements ConnectedNode
{
	protected String	label		= null;
	protected Set<Edge>	outEdges	= null;
	protected Set<Edge>	inEdges		= null;
	
	public SimpleNode(String nodeLabel)
	{
		this.label = nodeLabel;
		this.outEdges = new HashSet<>();
		this.inEdges = new HashSet<>();
	}
	
	public Set<Node> getOutNodes()
	{
		Set<Node> ret = new HashSet<>();
		for(Edge e : outEdges)
			ret.add(e.getTo());
		return ret;
	}
	
	public Set<Node> getInNodes()
	{
		Set<Node> ret = new HashSet<>();
		for(Edge e : inEdges)
			ret.add(e.getFrom());
		return ret;
	}
	
	public String getLabel()
	{
		return label;
	}
	
	public Set<Edge> getOutEdges()
	{
		return outEdges;
	}
	
	public Set<Edge> getInEdges()
	{
		return inEdges;
	}
	
	public Set<Edge> getEdgesTo(Node outNode)
	{
		Set<Edge> ret = new HashSet<>();
		for(Edge e : outEdges)
			if(e.getTo() == outNode)
				ret.add(e);
		return ret;
	}
	
	public Set<Edge> getEdgesFrom(Node inNode)
	{
		Set<Edge> ret = new HashSet<>();
		for(Edge e : inEdges)
			if(e.getFrom() == inNode)
				ret.add(e);
		return ret;
	}
	
	@Override
	public String toString()
	{
		return this.label;
	}
}