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

import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleEdge;

/**
 * The {@link EdgeP} is an edge that is part of a {@link GraphPattern} and may be generic (used in graph matching to one
 * or a series of more edges).
 * <p>
 * Currently, the generic aspect is not implemented. //FIXME Implement the generic aspect for edges.
 * 
 * @author Andrei Olaru
 * 
 */
public class EdgeP extends SimpleEdge
{
	/**
	 * Indicates that the edge is generic.
	 */
	boolean	generic	= false;
	
	/**
	 * A constructor that replicates the one in {@link SimpleEdge}.
	 * 
	 * @param fromNode
	 *            : the source {@link Node}; the edge is added to the node's outEdges list
	 * @param toNode
	 *            : the destination {@link Node}; the edge is added to the node's inEdges list
	 * @param edgeLabel
	 *            : the label of the edge
	 */
	public EdgeP(NodeP fromNode, NodeP toNode, String edgeLabel)
	{ // this constructor exists because it is required, as the superclass has no 0-argument constructors
		super(fromNode, toNode, edgeLabel);
	}
	
	/**
	 * @return <code>true</code> if the edge is generic.
	 */
	public boolean isGeneric()
	{
		return generic;
	}
}
