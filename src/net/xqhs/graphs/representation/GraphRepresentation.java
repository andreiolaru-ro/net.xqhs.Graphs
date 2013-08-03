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
package net.xqhs.graphs.representation;

import net.xqhs.graphs.graph.Graph;

/**
 * This is the interface that should be implemented by any class offering a representation for a {@link Graph} instance.
 * <p>
 * A representation is supposed to be associated with the same {@link Graph} instance throughout its entire lifetime.
 * This is why the associated {@link Graph} instance should be set in the constructor of the {@link GraphRepresentation}
 * instance.
 * <p>
 * It is possible to use an instance of this interface also in a tree of representations, in which each representation
 * has a parent.
 * <p>
 * The representation is based on {@link RepresentationElement} instances, each being usually assigned to a graph
 * element (instance of {@link VisualizableGraphComponent}). The elements of the representation are linked so that they
 * form a tree, rooted by the element returned by <code>getRepresentation</code>.
 * 
 * @author Andrei Olaru
 * 
 */
public interface GraphRepresentation
{
	/**
	 * Instructs the representation to update, since some elements of the graph may have changed.
	 * <p>
	 * FIXME: this is inefficient as it will cause the whole representation to update - there is no indication on what
	 * part of the graph has changed.
	 * 
	 * @return the instance itself.
	 */
	public GraphRepresentation update();
	
	/**
	 * Set the parent representation in a multi-level graph representation.
	 * <p>
	 * The argument may be <code>null</code>, to indicate this representation does not belong to a larger one, or is the
	 * root of a larger representation.
	 * 
	 * @param parent
	 *            : the parent representation.
	 * @return the instance itself.
	 */
	public GraphRepresentation setParentRepresentation(GraphRepresentation parent);
	
	/**
	 * Gets the root representation of the tree of representation this instance is part of. The result may be the
	 * instance itself, if there is no higher-level representation.
	 * 
	 * @return the root {@link GraphRepresentation}.
	 */
	public GraphRepresentation getRootRepresentation();
	
	/**
	 * Gets the root element of the representation.
	 * 
	 * @return the root {@link RepresentationElement} instance.
	 */
	public RepresentationElement getRepresentation();
	
	/**
	 * Depending on the nature of the representation, this method returns a way to visualize the representation. For
	 * instance, for textual representations, the returned value is of type {@link String}.
	 * 
	 * @return the representation of the graph.
	 */
	public Object displayRepresentation();
}
