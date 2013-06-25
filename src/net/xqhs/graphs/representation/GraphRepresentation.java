package net.xqhs.graphs.representation;


public interface GraphRepresentation
{
	public GraphRepresentationImplementation setRootRepresentation(GraphRepresentation root);
	
	public void update();
	
	public RepresentationElement getRepresentation();
	
	public Object displayRepresentation();
}
