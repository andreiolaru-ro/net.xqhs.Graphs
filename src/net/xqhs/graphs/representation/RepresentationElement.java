package net.xqhs.graphs.representation;

import net.xqhs.util.config.Config;

public abstract class RepresentationElement
{
	public static class RepElementConfig extends Config
	{
		GraphRepresentation				rootRepresentation;
		GraphComponentImplementation	representedComponent	= null;
		
		public RepElementConfig(GraphRepresentation root, GraphComponentImplementation component)
		{
			this.rootRepresentation = root;
			this.representedComponent = component;
		}
	}
	
	protected RepElementConfig	config;
	
	public RepresentationElement(RepElementConfig conf)
	{
		this.config = conf;
	}
	
	public GraphRepresentation getRootRepresentation()
	{
		return config.rootRepresentation;
	}
}