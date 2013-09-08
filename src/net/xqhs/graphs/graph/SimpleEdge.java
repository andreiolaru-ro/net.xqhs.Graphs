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
	 * <p>
	 * WARNING: if the nodes are instances of {@link ConnectedNode}, this also changes the from and to nodes, by adding
	 * the newly constructed {@link Edge} to their respective out and in lists.
	 * 
	 * @param fromNode
	 *            : the source {@link Node}; the edge is added to the node's outEdges list
	 * @param toNode
	 *            : the destination {@link Node}; the edge is added to the node's inEdges list
	 * @param edgeLabel
	 *            : the label of the edge
	 */
	public SimpleEdge(Node fromNode, Node toNode, String edgeLabel)
	{
		this.from = fromNode;
		this.to = toNode;
		this.label = edgeLabel;
		if(this.to instanceof ConnectedNode && this.to != null)
			((ConnectedNode) this.to).getInEdges().add(this);
		if(this.from instanceof ConnectedNode && this.from != null)
			((ConnectedNode) this.from).getOutEdges().add(this);
	}
	
	/**
	 * Removes the edge from the list of outgoing edges in the source node.
	 * 
	 * @return the edge itself.
	 */
	public SimpleEdge unlinkFrom()
	{
		if(this.from instanceof ConnectedNode && this.from != null)
			((ConnectedNode) this.from).getOutEdges().remove(this);
		return this;
	}
	
	/**
	 * Removes the edge from the list of incoming edges in the destination node.
	 * 
	 * @return the edge itself.
	 */
	public SimpleEdge unlinkTo()
	{
		if(this.to instanceof ConnectedNode && this.to != null)
			((ConnectedNode) this.to).getInEdges().remove(this);
		return this;
	}
	
	@Override
	public String getLabel()
	{
		return label;
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
