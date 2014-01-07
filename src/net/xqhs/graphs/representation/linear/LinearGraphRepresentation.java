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
package net.xqhs.graphs.representation.linear;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import net.xqhs.graphs.graph.ConnectedNode;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.NodeAlphaComparator;
import net.xqhs.graphs.representation.GraphRepresentationImplementation;
import net.xqhs.graphs.representation.linear.PathElement.PathComparator;

/**
 * Class that allows the representation of a {@link Graph} structure.
 * <p>
 * An instance remains associated with the same {@link Graph} instance for all of its lifecycle.
 * <p>
 * The linearization process works by creating a series of 'paths'. A path is a tree covering part of the graph. Each
 * path attempts to cover as many nodes as possible (ideally all the nodes in a weakly-connected component of the
 * graph). All the edges in the graph are then part of a path, cross between paths, or go from one node in the path to
 * another, without being part of the path. Ideally, most of the edges are on a path and few other edges remain outside
 * paths.
 * <p>
 * The result of the linearization is the set of paths, which are formed of {@link PathElement} instances.
 * <p>
 * The result of a linearization is always the same for the same graph.
 * <p>
 * The class is abstract because it does not provide a viewable representation, but rather a structure which other
 * representations may use (e.g. the linear text representation).
 * 
 * @author Andrei Olaru
 * 
 */
public abstract class LinearGraphRepresentation extends GraphRepresentationImplementation
{
	/**
	 * Compares two {@link Node} structures. First criterion: node with lower in-degree is first; second criterion is
	 * lexical order (using {@link NodeAlphaComparator}).
	 * 
	 * @author Andrei Olaru
	 * 
	 */
	static class NodeInAlphaComparator extends NodeAlphaComparator
	{
		@Override
		public int compare(Node n0, Node n1)
		{
			if(n0 instanceof ConnectedNode && n1 instanceof ConnectedNode
					&& ((ConnectedNode) n0).getInEdges().size() != ((ConnectedNode) n1).getInEdges().size())
				return (int) Math.signum(((ConnectedNode) n0).getInEdges().size()
						- ((ConnectedNode) n1).getInEdges().size());
			return super.compare(n0, n1);
		}
	}
	
	/**
	 * Specifies how paths should relate to the direction of edges on the path. See the documentation of method
	 * <code>setBackwards()</code>.
	 */
	protected boolean			isBackwards	= false;
	/**
	 * The nodes of the graph, as sorted by a {@link NodeInAlphaComparator}.
	 */
	protected List<Node>		sortedNodes	= null;
	/**
	 * The paths in the representation (sorted by a {@link PathComparator}). These represent the output of this
	 * representation, to be used by other non-abstract representations.
	 */
	protected List<PathElement>	paths		= null;
	
	/**
	 * Builds a new {@link LinearGraphRepresentation} for the specified graph.
	 * 
	 * @param theGraph
	 *            : the graph
	 */
	public LinearGraphRepresentation(Graph theGraph)
	{
		super(theGraph);
	}
	
	/**
	 * Sets the instace to be 'backwards'. See the documentation of <code>setBackwards(boolean)</code>.
	 * 
	 * @return the updated instance.
	 */
	public LinearGraphRepresentation setBackwards()
	{
		return setBackwards(true);
	}
	
	/**
	 * Specifies how paths should relate to the direction of edges on the path. If <code>false</code> ('forward'), the
	 * paths follow the direction of edges. If <code>true</code> ('backwards'), the path will follow in the opposite
	 * direction of edges.
	 * <p>
	 * Example of a forward path: a -> b -> c
	 * <p>
	 * Example of a backward path: a <- b <- c (with the equivalent forward path c -> b -> a).
	 * <p>
	 * The choice of this setting depends on the purpose of the representation. For instance, if the edges mean
	 * 'flows-to', the representation should be 'forward'. If the edges mean 'has-parent', a 'backward' representation
	 * may be more suitable.
	 * 
	 * @param back
	 *            : specifies whether the representation should be 'backward'.
	 * 
	 * @return the updated instance.
	 */
	public LinearGraphRepresentation setBackwards(boolean back)
	{
		isBackwards = back;
		return this;
	}
	
	@Override
	protected String setDefaultName(String name)
	{
		return super.setDefaultName(name) + "Lin";
	}
	
	/**
	 * Processing the graph actually relies on building the paths, after obtaining a sorted list of the nodes in the
	 * graph (using {@link NodeInAlphaComparator}).
	 */
	@Override
	protected void processGraph()
	{
		super.processGraph();
		
		sortedNodes = new LinkedList<Node>(theGraph.getNodes());
		Collections.sort(sortedNodes, new NodeInAlphaComparator());
		lf("sorted nodes: " + sortedNodes);
		
		buildPaths();
	}
	
	/**
	 * Building paths has two phases.
	 * <p>
	 * <b>Phase 1</b>
	 * <p>
	 * First, the paths are decided (it is decide what edges are on the paths and in what order). The first node on the
	 * path is the first node in the sorted list of nodes not explored so far (white node). Then, children of the node
	 * (at the end of outgoing edges if forward or incoming edges if backward, anyway edges that belong to the graph)
	 * are inspected: "new" children (white) are added as new, child, path elements (nodes outside the graph at the end
	 * of edges inside the graph are added as "other children"), and others are added as "other children": edges that
	 * point back in the current path from root - circular; edges that point to nodes already somewhere else in the path
	 * - other branches. If the paths reaches a node through with a longer distance (depth) than before, the node is
	 * shifted on the current path.
	 * <p>
	 * Then, paths are 'measured' and <code>forwardLength</code> information is computed (for each path element, the
	 * longest distance to a leaf).
	 * <p>
	 * In the third phase, forward lengths are used to switch elements from paths with lower forward lengths to paths
	 * with higher ones, in order to be sure that longer paths come first. All paths are then sorted using
	 * {@link PathComparator}.
	 * 
	 */
	protected void buildPaths()
	{
		Queue<PathElement> grayNodes = new LinkedList<PathElement>();
		Queue<PathElement> blackNodes = new LinkedList<PathElement>();
		
		while(blackNodes.size() < sortedNodes.size())
		{
			for(Node node : sortedNodes)
			{
				boolean found = false;
				for(PathElement el : blackNodes)
					if(el.node == node)
						found = true;
				if(!found)
				{
					grayNodes.add(new PathElement(node, 0, null));
					break;
				}
			}
			
			while(!grayNodes.isEmpty())
			{
				PathElement el = grayNodes.poll();
				lf("taking element " + el);
				// expand
				List<Node> childSet = new LinkedList<Node>(isBackwards ? el.node.getInNodes() : el.node.getOutNodes());
				Collections.sort(childSet, new NodeInAlphaComparator());
				for(Node n1 : childSet)
				{
					boolean towardsoutside = false;
					// FIXME: only works with single edges between nodes; is that OK?
					if(!isBackwards && !theGraph.contains(el.node.getEdgesTo(n1).iterator().next()))
						continue; // the edge is not in the graph displayed.
					// FIXME: only works with single edges between nodes; is that OK?
					if(isBackwards && !theGraph.contains(el.node.getEdgesFrom(n1).iterator().next()))
						continue; // the edge is not in the graph displayed.
					if(!theGraph.contains(n1))
						// the edge is in the graph, but the other node is not
						towardsoutside = true;
					
					PathElement el1 = null;
					boolean wasinblacknodes = false;
					for(PathElement eli : grayNodes)
						if(eli.node == n1)
							el1 = eli;
					for(PathElement eli : blackNodes)
						if(eli.node == n1)
						{
							el1 = eli;
							wasinblacknodes = true;
							lf("(element " + el1 + " was black)");
						}
					if(el1 == null)
					{ // new node, add new PathElement
						el1 = new PathElement(n1, el.depth + 1, el);
						if(!towardsoutside)
						{
							lf("new gray node added: " + el1 + " of " + el);
							grayNodes.add(el1);
							el.children.add(el1);
						}
						else
						{
							el.otherChildren.add(el1);
						}
					}
					else if(el.pathContains(el1))
					{ // cycle detected -> not good / no add
						lf("cycle detected for " + el1);
						if(!el.otherChildren.contains(el1))
							el.otherChildren.add(el1);
					}
					else if(el.depth + 1 > el1.depth)
					{ // new, longer path detected -> update
						if(el1.parent != null)
						{
							el1.parent.children.remove(el1);
							el1.parent.otherChildren.add(el1);
						}
						el1.depth = el.depth + 1;
						el1.parent = el;
						if(el.otherChildren.contains(el1))
							el.otherChildren.remove(el1);
						el.children.add(el1);
						if(wasinblacknodes)
						{
							blackNodes.remove(el1);
							grayNodes.add(el1);
						}
						lf("element reinserted: " + el1);
					}
					else
					{ // new distance would not be longer, leave alone
						lf("element not reinserted");
						if(!el.otherChildren.contains(el1))
							el.otherChildren.add(el1);
					}
				}
				blackNodes.add(el);
			}
			li("build paths done");
			
			for(PathElement el : blackNodes)
				if(el.children.isEmpty())
				{ // leaf
					el.forwardLength = 0;
					PathElement eli = el, elp;
					while(eli.parent != null)
					{
						elp = eli.parent;
						if(elp.forwardLength < eli.forwardLength + 1)
							// update
							elp.forwardLength = eli.forwardLength + 1;
						eli = elp;
					}
				}
		}
		li("measure paths done");
		lf("path_element : [children] / [otherchildren]");
		
		for(PathElement el : blackNodes)
		{
			Set<PathElement> marked = new HashSet<PathElement>();
			for(PathElement oth : el.otherChildren)
				if(!oth.pathContains(el) && (oth.parent != null) && (el.forwardLength > oth.parent.forwardLength))
				// 1) if the other child is already having the element as ancestor, then it is already on the main path
				// and it is a forward link
				// 2) if the parent of the other child is null, then it is already root of the main path (and it is a
				// backlink)
				// 3) switch if the other child is outside of the main path, and this way it would be closer to the main
				// path
				{ // switch
					lf("switching " + oth.toString() + ((oth.parent != null) ? (" from " + oth.parent.toString()) : "")
							+ " to " + el.toString());
					if(oth.parent != null)
					{
						oth.parent.children.remove(oth);
						oth.parent.otherChildren.add(oth);
					}
					oth.parent = el;
					oth.depth = el.depth + 1; // FIXME distance should be propagated
					marked.add(oth);
				}
			for(PathElement oth : marked)
			{
				el.otherChildren.remove(oth);
				el.children.add(oth);
			}
			Collections.sort(el.children, new PathComparator());
			Collections.sort(el.otherChildren, new PathComparator());
			lf(el.toString() + ": " + el.children.toString() + " / " + el.otherChildren.toString());
		}
		paths = new LinkedList<PathElement>(blackNodes);
		Collections.sort(paths, new PathComparator());
		
		li("sort paths done");
		
		li("[node_name ( distance_from_root : parent_or_dash_if_root / number_of_children_or_dot_if_none +number_of_other_children)]"
				+ "/ path_length_from_node)]");
		li(paths.toString());
	}
}
