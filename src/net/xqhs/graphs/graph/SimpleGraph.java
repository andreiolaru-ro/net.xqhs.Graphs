package net.xqhs.graphs.graph;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

import net.xqhs.graphs.graph.GraphPattern.NodeP;
import net.xqhs.graphs.representation.LinearGraphRepresentation;
import net.xqhs.util.logging.Unit;
import net.xqhs.util.logging.UnitComponent;
import net.xqhs.util.logging.UnitConfigData;

/**
 * <p>
 * Represents a directed graph, using {@link Node} and {@link Edge} elements.
 * 
 * <p>
 * Functions that modify the graph return the graph itself, so that chained calls are possible.
 * 
 * <p>
 * This class should only be used as a data structure. Visualization should happen elsewhere (for instance, in
 * {@link LinearGraphRepresentation}.
 * 
 * <p>
 * Currently only supports adding of new nodes and edges.
 * 
 * <p>
 * Warning: if a graph contains the edge, id does not necessarily contain any of the nodes of the edge. It may be that
 * the nodes have not been added to the graph. This is because this graph may be a subgraph of a larger graph.
 * 
 * @author Andrei Olaru
 * 
 */
public class SimpleGraph extends Unit implements Graph
{
	protected Set<Node>	nodes	= null;
	protected Set<Edge>	edges	= null;
	
	/**
	 * Generates an empty graph.
	 */
	public SimpleGraph()
	{
		this(null);
	}
	
	public SimpleGraph(UnitConfigData unitConfig)
	{
		super(unitConfig);
		nodes = new HashSet<>();
		edges = new HashSet<>();
	}
	
	@Override
	public String getUnitName()
	{
		return super.getUnitName();
	}
	
	@Override
	public SimpleGraph addNode(Node node)
	{
		if(node == null)
			throw new IllegalArgumentException("null nodes not allowed");
		if(!nodes.add(node))
			lw("node [" + node.toString() + "] already present.");
		return this;
	}
	
	/**
	 * Warning: the function will not add the nodes to the graph, only the edge between them. Nodes must be added
	 * separately.
	 * 
	 * @param edge
	 *            : the edge to add
	 * @return the updated graph
	 */
	public SimpleGraph addEdge(Edge edge)
	{
		if(edge == null)
			throw new IllegalArgumentException("null edges not allowed");
		if(!edges.add(edge))
			lw("edge [" + edge.toString() + "] already present.");
		return this;
	}
	
	public SimpleGraph removeNode(Node node)
	{
		if(!nodes.remove(node))
			lw("node[" + node + "] not contained");
		return this;
	}
	
	public SimpleGraph removeEdge(Edge edge)
	{
		if(!edges.remove(edge))
			lw("edge [" + edge + "] not contained");
		return this;
	}
	
	public int n()
	{
		return nodes.size();
	}
	
	public int m()
	{
		return edges.size();
	}
	
	public int size()
	{
		return n();
	}
	
	public boolean contains(Edge e)
	{
		return edges.contains(e);
	}
	
	public boolean contains(Node node)
	{
		return nodes.contains(node);
	}
	
	public Collection<Node> getNodesNamed(String name)
	{
		Collection<Node> ret = new HashSet<>();
		for(Node node : nodes)
			if(node.getLabel().equals(name))
				ret.add(node);
		return ret;
	}
	
	public Collection<Node> getNodes()
	{
		return nodes;
	}
	
	public Collection<Edge> getEdges()
	{
		return edges;
	}
	
	/**
	 * Checks if the inEdges and outEdges lists of the nodes are coherent with the edges list of the graph
	 * 
	 * @return true if the graph is coherent with respect to the above principle.
	 */
	@SuppressWarnings("static-method")
	public boolean isCoherent()
	{
		// TODO
		return true;
	}
	
	/**
	 * Simple Dijskstra algorithm to compute the distance between one node and all others.
	 * 
	 * @param node
	 *            : the source node.
	 * @return the distances to the other nodes.
	 */
	protected Map<Node, Integer> computeDistancesFromUndirected(Node node)
	{
		if(!nodes.contains(node))
			throw new IllegalArgumentException("node " + node + " is not in graph");
		if(!(node instanceof ConnectedNode))
			throw new IllegalArgumentException("node " + node + " is not a ConnectedNode");
		Map<Node, Integer> dists = new HashMap<>();
		Queue<ConnectedNode> grayNodes = new LinkedList<>();
		Set<ConnectedNode> blackNodes = new HashSet<>();
		grayNodes.add((ConnectedNode) node);
		dists.put(node, new Integer(0));
		
		while(!grayNodes.isEmpty())
		{
			ConnectedNode cNode = grayNodes.poll();
			int dist = dists.get(cNode).intValue();
			blackNodes.add(cNode);
			
			for(Edge e : cNode.getOutEdges())
				if(!blackNodes.contains(e.getTo()))
				{
					if(!grayNodes.contains(e.getTo()))
					{
						if(!(e.getTo() instanceof ConnectedNode))
							throw new IllegalArgumentException("node " + e.getTo() + " is not a ConnectedNode");
						grayNodes.add((ConnectedNode) e.getTo());
					}
					if(!dists.containsKey(e.getTo()) || (dists.get(e.getTo()).intValue() > (dist + 1)))
						dists.put(e.getTo(), new Integer(dist + 1));
				}
			for(Edge e : cNode.getInEdges())
				if(!blackNodes.contains(e.getFrom()))
				{
					if(!grayNodes.contains(e.getFrom()))
					{
						if(!(e.getFrom() instanceof ConnectedNode))
							throw new IllegalArgumentException("node " + e.getFrom()+ " is not a ConnectedNode");
						grayNodes.add((ConnectedNode) e.getFrom());
					}
					if(!dists.containsKey(e.getFrom()) || (dists.get(e.getFrom()).intValue() > (dist + 1)))
						dists.put(e.getFrom(), new Integer(dist + 1));
				}
		}
		
		return dists;
	}
	
	@Override
	public String toString()
	{
		String ret = "";
		ret += "G[" + n() + ", " + m() + "] ";
		List<Node> list = new ArrayList<>(nodes);
		Collections.sort(list, new NodeAlphaComparator());
		int first = 0;
		ret += list.toString();
		// for(Node node : list)
		// ret += "\n" + node.getLabel() + ": " + node.inEdges + "  :  " + node.outEdges;
		for(Edge e : edges)
			ret += "\n" + e.toString();
		return ret;
	}
	
	public String toDot()
	{
		String ret = "digraph G {\n";
		for(Edge edge : edges)
		{
			String fromNode = edge.getFrom().toString();
			String toNode = edge.getTo().toString();
			// if(fromNode.contains(" "))
			// fromNode = fromNode.replace(' ', '_');
			// if(toNode.contains(" "))
			// toNode = toNode.replace(' ', '_');
			ret += "\t";
			ret += "\"" + fromNode + "\"";
			ret += " -> ";
			ret += "\"" + toNode + "\"";
			if(edge.getLabel() != null)
				ret += " [" + "label=\"" + edge.getLabel() + "\"]";
			ret += ";\n";
		}
		for(Node node : nodes)
		{
			if(node instanceof NodeP && ((NodeP) node).isGeneric()) // fairly redundant as all NodeP instances are
																	// generic
				ret += "\t\"" + node.toString() + "\" [label=\"" + node.getLabel() + "\"];\n";
			// if(node.getLabel().contains(" "))
			// ret += "\t" + node.getLabel().replace(' ', '_') + " [label=\"" + node.getLabel() + "\"];\n";
		}
		ret += "}";
		return ret;
	}
	
	public static SimpleGraph readFrom(InputStream input)
	{
		return readFrom(input, null);
	}
	
	public static SimpleGraph readFrom(InputStream input, UnitConfigData unitConfig)
	{
		SimpleGraph g = new SimpleGraph(unitConfig);
		UnitComponent log = new UnitComponent(new UnitConfigData().setLink(g.getUnitName()));
		try (Scanner scan = new Scanner(input))
		{
			while(scan.hasNextLine())
			{
				String line = scan.nextLine();
				String edgeReads[] = line.split(";");
				for(String edgeRead : edgeReads)
				{
					log.lf("new edge: " + edgeRead);
					
					String[] parts1 = edgeRead.split("-", 2);
					if(parts1.length < 2)
					{
						log.le("input corrupted");
						continue;
					}
					String node1name = parts1[0].trim();
					String node2name = null;
					String edgeName = null;
					boolean bidirectional = false;
					String[] parts2 = parts1[1].split(">");
					if((parts2.length < 1) || (parts2.length > 2))
					{
						log.le("input corrupted");
						continue;
					}
					
					Node node1 = null;
					Node node2 = null;
					
					if(parts2.length == 2)
					{
						edgeName = parts2[0].trim();
						node2name = parts2[1].trim();
					}
					else
					{
						bidirectional = true;
						parts2 = parts1[1].split("-");
						if(parts2.length == 2)
						{
							edgeName = parts2[0].trim();
							node2name = parts2[1].trim();
						}
						else
							node2name = parts2[0].trim();
					}
					if((edgeName != null) && (edgeName.length() == 0))
						edgeName = null;
					// log.trace("[" + parts1.toString() + "] [" + parts2.toString() + "]");
					log.lf("[" + node1name + "] [" + node2name + "] [" + edgeName + "]");
					
					if(g.getNodesNamed(node1name).isEmpty())
					{
						node1 = new SimpleNode(node1name);
						g.addNode(node1);
					}
					else
						node1 = g.getNodesNamed(node1name).iterator().next();
					
					if(g.getNodesNamed(node2name).isEmpty())
					{
						node2 = new SimpleNode(node2name);
						g.addNode(node2);
					}
					else
						node2 = g.getNodesNamed(node2name).iterator().next();
					
					g.addEdge(new SimpleEdge(node1, node2, edgeName));
					if(bidirectional)
						g.addEdge(new SimpleEdge(node2, node1, edgeName));
				}
			}
			
			return g;
		}
	}
}