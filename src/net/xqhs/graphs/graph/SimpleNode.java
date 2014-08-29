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
 * A simple implementation of the {@link Node} interface, also inheriting functionality from
 * {@link AbstractVisualizableGraphComponent}.
 * 
 * @author Andrei Olaru
 * 
 */
public class SimpleNode extends AbstractVisualizableGraphComponent implements Node, Serializable
{
	/**
	 * Unique serial id.
	 */
	private static final long serialVersionUID = 9073121395731771182L;

	/**
	 * The label of the node
	 */
	protected String	label	= null;
	
	/**
	 * Constructs a new node with the specified label and empty edge adjacency lists - representing an unconnected node.
	 * 
	 * @param nodeLabel
	 *            - the label of the node
	 */
	public SimpleNode(String nodeLabel)
	{
		this.label = nodeLabel;
	}
	
	@Override
	public String getLabel()
	{
		return label;
	}
	
	@Override
	public String toString()
	{
		return this.label;
	}

	@Override
	public Node setLabel(String label)
	{
		this.label = label;
		return this;
	}

	// @Override
	// public boolean equals(Object obj)
	// {
	// if(!(obj instanceof SimpleNode))
	// return false;
	// return label.equals(((SimpleNode) obj).label);
	// }
	//
	// @Override
	// public int hashCode()
	// {
	// return label.hashCode();
	// }
}
