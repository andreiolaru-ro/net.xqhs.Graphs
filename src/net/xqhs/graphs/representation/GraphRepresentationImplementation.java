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
 * <i>root representation</i>. In this tree, each representation has a <code>parentRepresentation</code>. If this
 * instance is the root of the tree (or it is not part of such a tree), the member is <code>null</code>.
 * <p>
 * A {@link GraphRepresentationImplementation} is based on {@link RepresentationElement} instances, each instance
 * associated with a specific {@link VisualizableGraphComponent}. Representation elements should be updateable,
 * potentially leading to an update of the whole representation.
 * 
 * @author Andrei Olaru
 * 
 */
public abstract class GraphRepresentationImplementation extends Unit implements GraphRepresentation
{
	/**
	 * The represented {@link Graph}.
	 */
	protected Graph					theGraph				= null;
	/**
	 * The parent representation in the tree of representations this is part of (if any such tree exists).
	 */
	protected GraphRepresentation	parentRepresentation	= null;
	/**
	 * The root representation element of this representation, from which all other elements in this representation can
	 * be found.
	 */
	protected RepresentationElement	theRepresentation		= null;
	
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
	
	/**
	 * This constructor creates the link with the {@link Graph} instance that this representation will be bound to
	 * throughout its lifecycle.
	 * 
	 * @param graph
	 *            : the represented graph.
	 */
	public GraphRepresentationImplementation(Graph graph)
	{
		super();
		if(graph == null)
			throw new IllegalArgumentException("the graph cannot be null");
		theGraph = graph;
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
	public GraphRepresentationImplementation setParentRepresentation(GraphRepresentation parent)
	{
		if(lockedR())
			return null;
		if(parent != null)
			parentRepresentation = parent;
		return this;
	}
	
	@Override
	public GraphRepresentation getRootRepresentation()
	{
		GraphRepresentationImplementation root = this;
		while(root.parentRepresentation != null)
			root = (GraphRepresentationImplementation) root.parentRepresentation;
		return root;
	}
	
	@Override
	public GraphRepresentation update()
	{
		processGraph();
		return this;
	}
	
	/**
	 * Begins processing of the graph in order to create a representation.
	 * <p>
	 * It is assumed that all configuration of this instance is completed when this method is called.
	 * <p>
	 * It is important for inheriting classes to call <code>super.processGraph()</code> because this ensures that the
	 * instance is 'locked' (see {@link Unit}).
	 */
	protected void processGraph()
	{
		ensureLocked();
	}
	
	@Override
	public abstract RepresentationElement getRepresentation();
	
	@Override
	public abstract Object displayRepresentation();
}
