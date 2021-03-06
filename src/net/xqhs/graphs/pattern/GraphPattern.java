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
import java.util.BitSet;
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
 *
 * @author Andrei Olaru
 */
public class GraphPattern extends SimpleGraph
{
	/**
	 * Keeps track of the maximum index of a generic node that has been added to this pattern.
	 */
	protected int	maxIndex		= 0;
	/**
	 * Bit set storing which generic indexes are currently in use in this graph.
	 */
	BitSet			genericIndexes	= null;

	/**
	 * Creates an empty graph pattern.
	 */
	public GraphPattern()
	{
		super();
		genericIndexes = new BitSet();
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
	 * Adds a node to the graph, also re-indexing generic {@link NodeP} instances if required in the second argument, if
	 * the node is not indexed (index is not strictly positive) or if index already exists.
	 * <p>
	 * If a generic node is added, that has an index which already exists in the graph, a warning will be logged and the
	 * node <b>will be</b> re-indexed.
	 *
	 * @param node
	 *            - the node to be added (any {@link GraphComponent} is accepted, but special action is taken only in
	 *            case of {@link NodeP} instances, otherwise the call is deferred to
	 *            {@link SimpleGraph#add(GraphComponent)}).
	 * @param reindex
	 *            - if set to <code>true</code>, and if the node is a generic {@link NodeP}, the node will be re-indexed
	 *            according to the pre-existing nodes in the graph.
	 * @return the graph itself
	 */
	public GraphPattern addNode(GraphComponent node, boolean reindex)
	{
		if((node instanceof NodeP) && ((NodeP) node).isGeneric())
		{
			NodeP nodeP = (NodeP) node;
			boolean mustIndex = reindex;

			if(nodeP.labelIndex <= 0)
				mustIndex = true;
			else if(genericIndexes.get(nodeP.labelIndex))
			{ // index is already in use
				mustIndex = true;
				lw("Index [] is already in use; will re-index.", new Integer(nodeP.labelIndex));
			}

			if(mustIndex)
			{ // re-index
				maxIndex++;
				nodeP.labelIndex = maxIndex;
			}
			else if(nodeP.labelIndex > maxIndex)
			{
				maxIndex = nodeP.labelIndex;
			}

			// add index
			genericIndexes.set(nodeP.labelIndex);
		}

		super.add(node);
		return this;
	}

	/**
	 * Adds a component to the graph, but considers nodes in a special way:
	 * <ul>
	 * <li>if a {@link NodeP} instance is added, the call is deferred to {@link #addNode(GraphComponent, boolean)}, not
	 * forcing re-indexing.
	 * <li>if a {@link Node} instance is added that is not a generic {@link NodeP}, labels having as prefix
	 * {@link NodeP#NODEP_LABEL} are not allowed.
	 * <p>
	 * Generic nodes will only be re-indexed if their generic index is not strictly positive or if the index already
	 * exists (in which case a warning will be logged). Otherwise, the existing index will be used.
	 */
	@Override
	public GraphPattern add(GraphComponent component)
	{
		if((component instanceof NodeP) && ((NodeP) component).isGeneric())
			addNode(component, false);
		else if((component instanceof Node)
				&& (((Node) component).getLabel().substring(0, NodeP.NODEP_LABEL.length()).equals(NodeP.NODEP_LABEL)))
			throw new IllegalArgumentException("Node labels starting with " + NodeP.NODEP_LABEL + " are not allowed");
		else
			super.add(component);
		return this;
	}

	@Override
	public GraphPattern remove(GraphComponent component)
	{
		if((component instanceof NodeP) && ((NodeP) component).isGeneric())
			genericIndexes.clear(((NodeP) component).genericIndex());

		super.remove(component);
		return this;
	}

	/**
	 * Retrieves the generic node with the specified index.
	 *
	 * @param index
	 *            - the index to search. Must be strictly positive, otherwise an exception is thrown.
	 * @return the generic node with the index, or <code>null</code> if the index is not in use.
	 */
	public NodeP getGenericNodeWithIndex(int index)
	{
		if(index <= 0)
			throw new IllegalArgumentException("Index must be strictly positive");
		if((index > maxIndex) || !genericIndexes.get(index))
			return null;
		for(Node node : nodes.keySet())
			if((node instanceof NodeP) && (((NodeP) node).genericIndex() == index))
				return (NodeP) node;
		throw new IllegalStateException("Generic node should be present but not found");
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
