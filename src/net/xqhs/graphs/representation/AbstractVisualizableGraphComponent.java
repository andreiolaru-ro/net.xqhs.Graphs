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

public abstract class AbstractVisualizableGraphComponent implements VisualizableGraphComponent
{
	protected Set<RepresentationElement>	representations	= new HashSet<>();
	
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
	public RepresentationElement getFirstRepresentationForPlatform(GraphRepresentation representation)
	{
		Collection<RepresentationElement> filtered = getRepresentationsForPlatform(representation);
		if(filtered.isEmpty())
			return null;
		return filtered.iterator().next();
	}
	
	@Override
	public Collection<RepresentationElement> getRepresentationsForPlatform(GraphRepresentation representation)
	{
		Collection<RepresentationElement> ret = new HashSet<>();
		for(RepresentationElement repr : representations)
			if(repr.getRootRepresentation() == representation)
				ret.add(repr);
		return ret;
	}
}
