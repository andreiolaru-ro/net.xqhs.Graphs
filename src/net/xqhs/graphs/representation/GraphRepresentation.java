package net.xqhs.graphs.representation;

import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.util.logging.Unit;
import net.xqhs.util.logging.UnitConfigData;

/**
 * Abstract class for all classes that produce a representation (graphical, textual, etc) for a {@link SimpleGraph} instance.
 * 
 * <p>
 * It is possible that a {@link GraphRepresentation} class uses other, "sub-" {@link GraphRepresentation} instances for
 * the representation, forming a tree that is defined by its root representation (the one called from the exterior).
 * 
 * <p>
 * A {@link GraphRepresentation} is based on {@link RepresentationElement} instances, each instance associated with a
 * specific {@link GraphComponent}. Representation elements should be updateable.
 * 
 * @author Andrei Olaru
 * 
 */
public abstract class GraphRepresentation extends Unit
{
	/**
	 * Configures the graph representation with the {@link SimpleGraph} to represent, the root of the representation hierarchy
	 * (if any), and an indication whether to process the graph immediately or not.
	 * 
	 * @author Andrei Olaru
	 * 
	 */
	public static class GraphConfig extends UnitConfigData
	{
		SimpleGraph				graph				= null;
		GraphRepresentation	rootRepresentation	= null;
		boolean				doProcess			= true;
		
		public GraphConfig(SimpleGraph thegraph)
		{
			super();
			if(thegraph == null)
				throw new IllegalArgumentException("the graph cannot be null");
			this.graph = thegraph;
		}
		
		@Override
		public GraphConfig setName(String unitName)
		{
			if(DEFAULT_UNIT_NAME.equals(unitName))
			{
				if(this.graph.getUnitName() == null)
					return (GraphConfig) super.setName(setDefaultName("Graph"
							+ new Integer(graph.hashCode()).toString().substring(0, 5)));
				return (GraphConfig) super.setName(setDefaultName(this.graph.getUnitName()));
			}
			return (GraphConfig) super.setName(unitName);
		}
		
		/**
		 * This can be overridden by other representations to produce the correct suffix.
		 * 
		 * @param name
		 *            : the name of the graph's unit.
		 * @return the name of the representation
		 */
		@SuppressWarnings("static-method")
		protected String setDefaultName(String name)
		{
			return name + "-R";
		}
	}
	
	SimpleGraph					theGraph			= null;
	RepresentationElement	theRepresentation	= null;
	
	public GraphRepresentation(GraphConfig conf)
	{
		super(conf);
		
		if(!(config instanceof GraphConfig))
			throw new IllegalArgumentException("null graph configuration");
		GraphConfig gconfig = (GraphConfig) config;
		
		this.theGraph = gconfig.graph;
		
		if(gconfig.rootRepresentation == null)
			gconfig.rootRepresentation = this;
		
		if(gconfig.doProcess)
			processGraph();
	}
	
	public void update()
	{
		processGraph();
	}
	
	abstract void processGraph();
	
	public abstract RepresentationElement getRepresentation();
	
	public abstract Object displayRepresentation();
}
