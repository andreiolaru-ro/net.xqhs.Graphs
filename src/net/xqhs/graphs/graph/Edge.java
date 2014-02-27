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

/**
 * Interface for a simple, labeled, directed edge, characterized by its source and destination nodes.
 * 
 * @author Andrei Olaru
 */
public interface Edge extends GraphComponent
{
	/**
	 * If in need of a readable rendition of the edge's features, use the <code>toString</code> functions.
	 * 
	 * @return the label of the edge
	 */
	public String getLabel();
	
	/**
	 * @return the source {@link Node}
	 */
	public Node getFrom();
	
	/**
	 * @return the destination {@link Node}
	 */
	public Node getTo();
	
	/**
	 * Constructs a full representation of the edge, including its two adjacent nodes.
	 * 
	 * @return a {@link String} representation of the edge
	 */
	@Override
	String toString();
	
	/**
	 * Constructs a short representation of the edge, including only information about label.
	 * 
	 * @return a short {@link String} representation of the edge
	 */
	String toStringShort();
	
	/**
	 * Constructs a short representation of the edge, including only information about label and direction. The
	 * direction depends on the general direction of the representation.
	 * 
	 * @param isBackward
	 *            - mentions that the edge is in opposite direction with respect to the representation and should be
	 *            represented accordingly
	 * 
	 * @return a short {@link String} representation of the edge
	 */
	String toStringShort(boolean isBackward);
}
