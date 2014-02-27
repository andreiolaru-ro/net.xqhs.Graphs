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
package net.xqhs.graphs.graph;

import java.util.Collection;

/**
 * An interface representing a graph structure, using {@link Node} and {@link Edge} components.
 * <p>
 * Supports actions like adding and removing nodes and edges, getting the graph's size and lists of edges and nodes,
 * filtering nodes by name, and testing if a node or edge belong to the graph.
 * <p>
 * Warning: depending on implementation, a {@link Graph} instance may not check if all edges have sources and
 * destinations inside the graph.
 * 
 * @author Andrei Olaru
 */
public interface Graph
{
	/**
	 * @param node
	 *            - the {@link Node} to add
	 * @return the graph itself, for chaining calls
	 */
	public Graph addNode(Node node);
	
	/**
	 * @param edge
	 *            - the {@link Edge} to add
	 * @return the graph itself, for chained calls
	 */
	public Graph addEdge(Edge edge);
	
	/**
	 * @param node
	 *            - the {@link Node} to remove
	 * @return the graph itself, for chained calls
	 */
	public Graph removeNode(Node node);
	
	/**
	 * @param edge
	 *            - the {@link Edge} to remove
	 * @return the graph itself, for chained calls
	 */
	public Graph removeEdge(Edge edge);
	
	/**
	 * @return the number of nodes in the graph
	 */
	public int n();
	
	/**
	 * @return the number of edges in the graph
	 */
	public int m();
	
	/**
	 * @return the size of the graph, in number of nodes
	 */
	public int size();
	
	/**
	 * @return the list of {@link Node} instances in the graph
	 */
	public Collection<Node> getNodes();
	
	/**
	 * @return the list of {@link Edge} instances in the graph
	 */
	public Collection<Edge> getEdges();
	
	/**
	 * @param node
	 *            - the {@link Node} to search for
	 * @return <code>true</code> if the node is contained in the graph
	 */
	public boolean contains(Node node);
	
	/**
	 * @param e
	 *            - the {@link Edge} to search for
	 * @return <code>true</code> if the edge is contained in the graph
	 */
	public boolean contains(Edge e);
	
	/**
	 * @param name
	 *            - the name to search for
	 * @return a {@link Collection} of {@link Node} instances with the required label
	 */
	public Collection<Node> getNodesNamed(String name);
	
}
