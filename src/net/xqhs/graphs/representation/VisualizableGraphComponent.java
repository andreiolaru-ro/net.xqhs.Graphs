package net.xqhs.graphs.representation;

import java.util.Collection;

public interface VisualizableGraphComponent
{
	public void addRepresentation(RepresentationElement repr);
	
	public Collection<RepresentationElement> getRepresentations();
	
	public RepresentationElement getFirstRepresentationForPlatform(GraphRepresentation representation);
	
	public Collection<RepresentationElement> getRepresentationsForPlatform(GraphRepresentation representation);
}
