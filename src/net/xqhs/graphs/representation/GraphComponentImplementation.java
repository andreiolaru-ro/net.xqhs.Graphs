package net.xqhs.graphs.representation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class GraphComponentImplementation implements VisualizableGraphComponent
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
