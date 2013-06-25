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
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.util.logging.Unit;

/**
 * Abstract class for all classes that produce a representation (graphical, textual, etc) for a {@link Graph} instance.
 * <p>
 * It is possible that a {@link GraphRepresentationImplementation} class uses other, "sub-"
 * {@link GraphRepresentationImplementation} instances for the representation, forming a tree that is defined by its
 * <i>root representation</i> (the one called from the exterior).
 * <p>
 * A {@link GraphRepresentationImplementation} is based on {@link RepresentationElement} instances, each instance
 * associated with a specific {@link AbstractVisualizableGraphComponent}. Representation elements should be updateable.
 * 
 * 
 * @author Andrei Olaru
 * 
 */
public abstract class GraphRepresentationImplementation extends Unit implements GraphRepresentation
{
	protected Graph					theGraph			= null;
	protected GraphRepresentation	rootRepresentation	= null;
	protected RepresentationElement	theRepresentation	= null;
	
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
	
	public GraphRepresentationImplementation(Graph thegraph)
	{
		super();
		if(thegraph == null)
			throw new IllegalArgumentException("the graph cannot be null");
		this.theGraph = thegraph;
	}
	
	@Override
	public GraphRepresentationImplementation setUnitName(String unitName)
	{
		if(DEFAULT_UNIT_NAME.equals(unitName) && (this.theGraph instanceof SimpleGraph))
		{
			String graphName = ((SimpleGraph) this.theGraph).getUnitName();
			if(graphName == null)
				return (GraphRepresentationImplementation) super.setUnitName(setDefaultName("Graph"
						+ new Integer(theGraph.hashCode()).toString().substring(0, 5)));
			return (GraphRepresentationImplementation) super.setUnitName(setDefaultName(graphName));
		}
		return (GraphRepresentationImplementation) super.setUnitName(unitName);
	}
	
	@Override
	public GraphRepresentationImplementation setRootRepresentation(GraphRepresentation root)
	{
		locked();
		if(root == null)
			rootRepresentation = this;
		else
			rootRepresentation = root;
		return this;
	}
	
	@Override
	public void update()
	{
		processGraph();
	}
	
	protected void processGraph()
	{
		ensureLocked();
	}
	
	@Override
	public abstract RepresentationElement getRepresentation();
	
	@Override
	public abstract Object displayRepresentation();
}
