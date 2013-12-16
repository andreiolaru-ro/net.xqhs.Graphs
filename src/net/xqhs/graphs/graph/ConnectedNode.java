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
 * Interface that extends {@link Node} with functions related to the edges adjacent to this node: incoming and outgoing
 * edges, nodes at the end of said edges, and filtering of edges based on adjacent nodes.
 * 
 * 
 * @author Andrei Olaru
 */
public interface ConnectedNode extends Node
{
	/**
	 * @return the {@link Collection} of outgoing edges
	 */
	public Collection<Edge> getOutEdges();
	
	/**
	 * @return the {@link Collection} of incoming edges
	 */
	public Collection<Edge> getInEdges();
	
	/**
	 * @return the {@link Collection} of nodes that are at the destination of outgoing edges (no duplicates)
	 */
	public Collection<Node> getOutNodes();
	
	/**
	 * @return the {@link Collection} of nodes that are at the source of incoming edges (no duplicates)
	 */
	public Collection<Node> getInNodes();
	
	/**
	 * @param outNode
	 *            - the {@link Node} that will serve for filtering.
	 * @return a {@link Collection} of outgoing edges with said node as destination (and <code>this</code> as source)
	 */
	public Collection<Edge> getEdgesTo(Node outNode);
	
	/**
	 * @param inNode
	 *            - the {@link Node} that will serve for filtering.
	 * @return a {@link Collection} of incoming edges with said node as source (and <code>this</code> as destination)
	 */
	public Collection<Edge> getEdgesFrom(Node inNode);
	
}
