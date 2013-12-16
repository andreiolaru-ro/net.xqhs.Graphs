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

import java.util.Collection;

/**
 * This interface should be implemented by all classes that are associated with specific graph components and that are
 * meant to be represented by means of {@link GraphRepresentation} objects.
 * <p>
 * The methods relate to managing the links between the component, the representation element, and the representation
 * root.
 * <p>
 * Normally, an implementation of this interface keeps a list of existing representation elements and whole-graph
 * representations for the component, that can be updated when the component changes.
 * 
 * @author Andrei Olaru
 * 
 */
public interface VisualizableGraphComponent
{
	/**
	 * Adds a new representation for this component (as a {@link RepresentationElement}) to the list of active
	 * representations.
	 * 
	 * @param repr
	 *            : the element representing this component.
	 */
	public void addRepresentation(RepresentationElement repr);
	
	/**
	 * Gets a collection of the elements representing this component (in various representations).
	 * 
	 * @return a collection of the representing elements.
	 */
	public Collection<RepresentationElement> getRepresentations();
	
	/**
	 * Gets a collection of elements representing this component, filtered to match only the one(s) that belong to a
	 * particular (multi-level) representation.
	 * 
	 * @param representation
	 *            : the root representation to which the returned elements belong.
	 * @return the elements representing this component and belonging to the specified representation.
	 */
	public Collection<RepresentationElement> getRepresentationsForRoot(GraphRepresentation representation);
	
	/**
	 * Gets the first (and potentially only) element representing this component that is part of the specified
	 * (multi-level) representation.
	 * 
	 * @param representation
	 *            : the root representation to which the returned element should belong.
	 * @return the required representing element.
	 */
	public RepresentationElement getFirstRepresentationForRoot(GraphRepresentation representation);
}
