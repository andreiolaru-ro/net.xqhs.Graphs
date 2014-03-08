/*******************************************************************************
 * Copyright (C) 2013 Andrei Olaru.
 * 
 * This file is part of net.xqhs.Graphs.
 * 
 * net.xqhs.Graphs is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * net.xqhs.Graphs is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with net.xqhs.Graphs.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.graphs.pattern;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleEdge;
import net.xqhs.graphs.graph.SimpleGraph;

/**
 * Graph patterns are graphs that allow nodes with unspecified labels (marked with question marks) and edges labeled
 * with regular expressions.
 * <p>
 * The class inherits from {@link SimpleGraph}, to which it is identical with the exception that it provides additional
 * support for {@link NodeP} and {@link EdgeP} instances.
 * <p>
 * 
 * @author Andrei Olaru
 */
public class GraphPattern extends SimpleGraph
{
	/**
	 * Creates an empty graph pattern.
	 */
	public GraphPattern()
	{
		super();
	}
	
	/**
	 * Get the maximum possible <i>k</i> for matches of this pattern. Effectively, the number of edges in the pattern.
	 * 
	 * @return the maximum value of k for a match.
	 */
	public int maxK()
	{
		return m();
	}
	
	/**
	 * Adds a node to the graph, but offers support for indexing {@link NodeP} instances (see
	 * <code>addNode(NodeP, boolean)</code>).
	 * <p>
	 * Generic nodes will be (re)indexed.
	 */
	@Override
	public GraphPattern addNode(Node node)
	{
		return this.addNode(node, true);
	}
	
	/**
	 * Adds a node to the graph, also indexing generic {@link NodeP} instances if required.
	 * <p>
	 * <b>Warning:</b> while the method allows not indexing the added generic nodes (by setting <code>doindex</code> to
	 * <code>false</code>), this is strongly discouraged and should be used with caution.
	 * 
	 * @param node
	 *            - the node to be added
	 * @param doindex
	 *            - if set to <code>true</code>, and if the node is a generic {@link NodeP}, the node will be
	 *            (re)indexed according to the pre-existing nodes in the graph
	 * @return the graph itself
	 */
	public GraphPattern addNode(Node node, boolean doindex)
	{
		if(doindex && (node instanceof NodeP) && ((NodeP) node).isGeneric())
		{
			int maxIdx = 0;
			for(Node n : this.nodes.keySet())
				if((n instanceof NodeP) && (((NodeP) n).isGeneric()) && (maxIdx <= ((NodeP) n).genericIndex()))
					maxIdx = ((NodeP) n).genericIndex();
			if(maxIdx >= 0)
				((NodeP) node).labelIndex = maxIdx + 1;
		}
		super.addNode(node);
		return this;
	}
	
	@Override
	public GraphPattern readFrom(InputStream input)
	{
		super.readFrom(input);
		
		Set<GraphComponent> additions = new HashSet<GraphComponent>();
		Set<GraphComponent> removals = new HashSet<GraphComponent>();
		Map<Node, Node> replacements = new HashMap<Node, Node>();
		for(Node node : nodes.keySet())
			if(node.getLabel().startsWith(NodeP.NODEP_LABEL + NodeP.NODEP_INDEX_MARK))
			{
				Node newNode = new NodeP();
				removals.add(node);
				additions.add(newNode);
				replacements.put(node, newNode);
			}
		for(Edge edge : edges)
		{
			Node from = replacements.get(edge.getFrom());
			Node to = replacements.get(edge.getTo());
			if((from != null) || (to != null))
			{
				removals.add(edge);
				// it is safe. super.readFrom() uses SimpleEdge, so it is ok to replace it with another SimpleEdge.
				additions.add(new SimpleEdge(from != null ? from : edge.getFrom(), to != null ? to : edge.getTo(), edge
						.getLabel()));
			}
		}
		for(GraphComponent comp : removals)
			remove(comp);
		for(GraphComponent comp : additions)
			add(comp);
		
		return this;
	}
}
