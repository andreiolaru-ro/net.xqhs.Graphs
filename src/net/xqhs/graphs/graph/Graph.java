package net.xqhs.graphs.graph;

import java.util.Collection;

public interface Graph
{
	public Graph addNode(Node node);
	
	public Graph addEdge(Edge edge);
	
	public Graph removeNode(Node node);
	
	public Graph removeEdge(Edge edge);
	
	public int n();
	
	public int m();
	
	public int size();
	
	public boolean contains(Edge e);
	
	public boolean contains(Node node);
	
	public Collection<Node> getNodesNamed(String name);
	
	public Collection<Node> getNodes();
	
	public Collection<Edge> getEdges();
	
	
	
}
