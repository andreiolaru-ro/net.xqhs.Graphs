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

import java.io.Serializable;
import net.xqhs.graphs.representation.AbstractVisualizableGraphComponent;

/**
 * A simple implementation on the {@link Edge} interface, also inheriting functions from
 * {@link AbstractVisualizableGraphComponent}.
 * 
 * @author Andrei Olaru
 * 
 */
public class SimpleEdge extends AbstractVisualizableGraphComponent implements Edge
{
	/**
	 * Unique serial id
	 */
	private static final long serialVersionUID = 3564476601107009433L;

	/**
	 * The label of the edge.
	 */
	protected String	label	= null; // FIXME: support null labels throughout the source
	/**
	 * The source of the edge.
	 */
	protected Node		from	= null;
	
	/**
	 * The destination of the edge.
	 */
	protected Node		to		= null;
	
	/**
	 * Constructs a new edge.
	 * 
	 * @param fromNode
	 *            : the source {@link Node}; the edge is added to the node's outEdges list.
	 * @param toNode
	 *            : the destination {@link Node}; the edge is added to the node's inEdges list.
	 * @param edgeLabel
	 *            : the label of the edge.
	 */
	public SimpleEdge(Node fromNode, Node toNode, String edgeLabel)
	{
		this.from = fromNode;
		this.to = toNode;
		this.label = edgeLabel;
	}
	
	@Override
	public String getLabel()
	{
		return label;
	}

	@Override
	public Edge setLabel(String label)
	{
		this.label = label;
		return this;
	}

	@Override
	public Node getFrom()
	{
		return from;
	}
	
	@Override
	public Node getTo()
	{
		return to;
	}
	
	@Override
	public String toString()
	{
		return from + toStringShort() + to;
	}
	
	@Override
	public String toStringShort()
	{
		return toStringShort(false);
	}
	
	@Override
	public String toStringShort(boolean isBackward)
	{
		return (isBackward ? "<" : "") + (this.label != null ? ("-" + this.label + "-") : "-")
				+ (isBackward ? "" : ">");
	}
}
