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
import java.util.HashSet;
import java.util.Set;

/**
 * THis class implements the basic representing element management required by the {@link VisualizableGraphComponent}
 * interface. It does so by maintaining a {@link Set} of {@link RepresentationElement} instances.
 * 
 * @author Andrei Olaru
 * 
 */
public abstract class AbstractVisualizableGraphComponent implements VisualizableGraphComponent
{
	/**
	 * The set of elements representing this component.
	 */
	protected Set<RepresentationElement>	representations	= new HashSet<RepresentationElement>();
	
	@Override
	public void addRepresentation(RepresentationElement repr)
	{
		representations.add(repr);
	}
	
	@Override
	public Collection<RepresentationElement> getRepresentations()
	{
		return representations;
	}
	
	@Override
	public RepresentationElement getFirstRepresentationForRoot(GraphRepresentation root)
	{
		Collection<RepresentationElement> filtered = getRepresentationsForRoot(root);
		if(filtered.isEmpty())
			return null;
		return filtered.iterator().next();
	}
	
	@Override
	public Collection<RepresentationElement> getRepresentationsForRoot(GraphRepresentation root)
	{
		Collection<RepresentationElement> ret = new HashSet<RepresentationElement>();
		for(RepresentationElement repr : representations)
			if(repr.getParentRepresentation() == root)
				ret.add(repr);
		return ret;
	}
}
