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

import net.xqhs.util.config.Config;

/**
 * This abstract class should be extended by any class implementing the representation of a graph component (more
 * precisely of a {@link VisualizableGraphComponent} instance.
 * <p>
 * Each representation relates to the component it represents, as well as to the parent representation.
 * 
 * @author Andrei Olaru
 * 
 */
public abstract class RepresentationElement extends Config
{
	/**
	 * The parent representation.
	 */
	GraphRepresentation			parentRepresentation	= null;
	/**
	 * The represented graph component.
	 */
	VisualizableGraphComponent	representedComponent	= null;
	
	/**
	 * Creates a new representation element, initializing the references to the parent representation and to the
	 * represented component.
	 * 
	 * @param parent
	 *            : the parent representation.
	 * @param component
	 *            : the represented component.
	 */
	public RepresentationElement(GraphRepresentation parent, VisualizableGraphComponent component)
	{
		this.parentRepresentation = parent;
		this.representedComponent = component;
	}
	
	/**
	 * @return the parent representation.
	 */
	public GraphRepresentation getParentRepresentation()
	{
		return parentRepresentation;
	}
	
	/**
	 * This method gets the root representation of the multi-level representation (if any, otherwise just the parent
	 * representation).
	 * 
	 * @return the root representation.
	 */
	public GraphRepresentation getRootRepresentation()
	{
		return parentRepresentation.getRootRepresentation();
	}
	
	/**
	 * @return the graph component represented by this element.
	 */
	public VisualizableGraphComponent getRepresentedComponent()
	{
		return representedComponent;
	}
}
