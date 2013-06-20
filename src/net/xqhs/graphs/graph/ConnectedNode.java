package net.xqhs.graphs.graph;

import java.util.Set;

public interface ConnectedNode extends Node
{
	public Set<Edge> getOutEdges();
	
	public Set<Edge> getInEdges();
	
	public Set<Edge> getEdgesTo(Node outNode);
	
	public Set<Edge> getEdgesFrom(Node inNode);
	
	public Set<Node> getOutNodes();
	
	public Set<Node> getInNodes();
	
}
