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

import java.util.HashSet;
import java.util.Set;

import net.xqhs.graphs.representation.AbstractVisualizableGraphComponent;

/**
 * A simple implementation of the {@link Node} and {@link ConnectedNode} interfaces, also inheriting functionality from
 * {@link AbstractVisualizableGraphComponent}.
 * 
 * @author Andrei Olaru
 * 
 */
public class SimpleNode extends AbstractVisualizableGraphComponent implements ConnectedNode
{
	/**
	 * The label of the node
	 */
	protected String	label		= null;
	/**
	 * The set of outgoing edges. It will be updated when adjacent edges are added.
	 */
	protected Set<Edge>	outEdges	= null;
	/**
	 * The set of incoming edges. It will be updated when adjacent edges are added.
	 */
	protected Set<Edge>	inEdges		= null;
	
	/**
	 * Constructs a new node with the specified label and empty edge adjacency lists - representing an unconnected node.
	 * 
	 * @param nodeLabel
	 *            - the label of the node
	 */
	public SimpleNode(String nodeLabel)
	{
		this.label = nodeLabel;
		this.outEdges = new HashSet<>();
		this.inEdges = new HashSet<>();
	}
	
	@Override
	public Set<Node> getOutNodes()
	{
		Set<Node> ret = new HashSet<>();
		for(Edge e : outEdges)
			ret.add(e.getTo());
		return ret;
	}
	
	@Override
	public Set<Node> getInNodes()
	{
		Set<Node> ret = new HashSet<>();
		for(Edge e : inEdges)
			ret.add(e.getFrom());
		return ret;
	}
	
	@Override
	public String getLabel()
	{
		return label;
	}
	
	@Override
	public Set<Edge> getOutEdges()
	{
		return outEdges;
	}
	
	@Override
	public Set<Edge> getInEdges()
	{
		return inEdges;
	}
	
	@Override
	public Set<Edge> getEdgesTo(Node outNode)
	{
		Set<Edge> ret = new HashSet<>();
		for(Edge e : outEdges)
			if(e.getTo() == outNode)
				ret.add(e);
		return ret;
	}
	
	@Override
	public Set<Edge> getEdgesFrom(Node inNode)
	{
		Set<Edge> ret = new HashSet<>();
		for(Edge e : inEdges)
			if(e.getFrom() == inNode)
				ret.add(e);
		return ret;
	}
	
	@Override
	public String toString()
	{
		return this.label;
	}
}
