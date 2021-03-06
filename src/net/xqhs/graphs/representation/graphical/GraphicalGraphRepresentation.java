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
package net.xqhs.graphs.representation.graphical;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.xqhs.graphical.GCanvas;
import net.xqhs.graphical.GConnector;
import net.xqhs.graphical.GContainer;
import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.representation.VisualizableGraphComponent;
import net.xqhs.graphs.representation.graphical.GraphicalRepresentationElement.EdgeType;
import net.xqhs.graphs.representation.graphical.GraphicalRepresentationElement.Type;
import net.xqhs.graphs.representation.linear.LinearGraphRepresentation;
import net.xqhs.graphs.representation.linear.PathElement;

/**
 * Graphical representation for a {@link Graph} that relies on {@link LinearGraphRepresentation} to create a tree of
 * paths and then displays the tree of paths.
 * <p>
 * It relies on {@link GCanvas} and the <code>Gel</code> framework for the graphical display.
 * <p>
 * Currently not supporting links in the same path very well.
 * 
 * @author Andrei Olaru
 */
public class GraphicalGraphRepresentation extends LinearGraphRepresentation
{
	/**
	 * The canvas in which the graph will be displayed.
	 */
	GCanvas	canvas;
	/**
	 * Top left corner of the representation, in the canvas.
	 */
	Point2D	topleft;
	/**
	 * Bottom right corner of the representation, in the canvas.
	 */
	Point2D	bottomright;
	
	/**
	 * Creates a new representation, based on the specified graph.
	 * 
	 * @param theGraph
	 *            - the {@link Graph} instance to be represented.
	 */
	public GraphicalGraphRepresentation(Graph theGraph)
	{
		super(theGraph);
	}
	
	/**
	 * Sets a new {@link GCanvas} as a canvas for the representation, and sets the representation to be represented in a
	 * 200 units square centered in the origin of the canvas.
	 */
	@Override
	public GraphicalGraphRepresentation makeDefaults()
	{
		super.makeDefaults();
		setCanvas(new GCanvas());
		topleft = new Point2D.Float(-100, -100);
		bottomright = new Point2D.Float(100, 100);
		return this;
	}
	
	/**
	 * Configures the representation to use the specified canvas. By default, a new canvas is used.
	 * 
	 * @param representationCanvas
	 *            - the {@link GCanvas} instance. If the argument is <code>null</code>, the call will be ignored.
	 * @return the instance itself.
	 */
	public GraphicalGraphRepresentation setCanvas(GCanvas representationCanvas)
	{
		if(representationCanvas != null)
			canvas = representationCanvas;
		return this;
	}
	
	/**
	 * Sets the origin of the rectangle for this representation, on the GCanvas.
	 * 
	 * @param origin
	 *            - the origin (top left corner - the minimum x and y in the representation).
	 * @return the instance itself.
	 */
	public GraphicalGraphRepresentation setOrigin(Point2D origin)
	{
		this.topleft = origin;
		return this;
	}
	
	/**
	 * Sets the size of the representation, by means of the bottom right corner.
	 * 
	 * @param bottomRight
	 *            - the bottom right corner - the maximum x and y in the representation).
	 * @return the instance itself.
	 */
	public GraphicalGraphRepresentation setBottomRight(Point2D bottomRight)
	{
		this.bottomright = bottomRight;
		return this;
	}
	
	@Override
	protected String setDefaultName(String name)
	{
		return super.setDefaultName(name) + "X";
	}
	
	/**
	 * Calculates the graph paths and creates the layout.
	 */
	@Override
	protected void processGraph()
	{
		super.processGraph(); // calculates paths
		
		Set<PathElement> blackNodes = new HashSet<PathElement>();
		// root element
		GraphicalRepresentationElement representation = new GraphicalRepresentationElement(this, null,
				Type.ELEMENT_CONTAINER);
		
		// add not yet added elements, together with their children.
		for(PathElement el : paths)
			if(!blackNodes.contains(el))
				representation.connected.add(representChildren(el, blackNodes));
		
		this.theRepresentation = representation;
		
		doLayout();
		lf("get ready for magnetics");
		canvas.startMagnetics();
	}
	
	protected GraphicalRepresentationElement representChildren(PathElement el, Set<PathElement> blackNodes)
	{
		// FIXME: check casts to VisualizableGraphComponent throughout the method
		GraphicalRepresentationElement repr = new GraphicalRepresentationElement(this,
				(VisualizableGraphComponent) el.getNode(), Type.NODE);
		blackNodes.add(el);
		
		List<PathElement> others = new LinkedList<PathElement>(el.getOtherChildren());
		
		representOthers(others, blackNodes, EdgeType.BACKLINK, el, repr);
		
		boolean first = true;
		for(PathElement child : el.getChildren())
		{
			for(Edge edge : (isBackwards ? theGraph.getInEdges(el.getNode()) : theGraph.getOutEdges(el.getNode())))
				if((isBackwards && (edge.getFrom() == child.getNode()))
						|| (!isBackwards && (edge.getTo() == child.getNode())))
				{
					GraphicalRepresentationElement childRepr = representChildren(child, blackNodes);
					GraphicalRepresentationElement edgeRepr = new GraphicalRepresentationElement(this,
							(VisualizableGraphComponent) edge, Type.EDGE);
					edgeRepr.setEdge(EdgeType.CHILDLINK, repr, childRepr);
					
					repr.connected.add(edgeRepr);
					((VisualizableGraphComponent) child.getNode()).addRepresentation(childRepr);
					((VisualizableGraphComponent) edge).addRepresentation(edgeRepr);
					first = false;
				}
			
			representOthers(others, blackNodes, first ? EdgeType.FORELINK : EdgeType.SIDELINK, el, repr);
			
		}
		representOthers(others, blackNodes, EdgeType.EXTLINK, el, repr);
		
		return repr;
	}
	
	protected void representOthers(List<PathElement> others, Set<PathElement> blackNodes, EdgeType edgeType,
			PathElement parent, GraphicalRepresentationElement parentRepr)
	{
		// FIXME: check casts to VisualizableGraphComponent throughout the method
		while(!others.isEmpty() && ((edgeType == EdgeType.EXTLINK) || blackNodes.contains(others.get(0))))
		{
			PathElement other = others.get(0);
			EdgeType actualEdgeType = edgeType;
			GraphicalRepresentationElement otherRepr = (GraphicalRepresentationElement) ((VisualizableGraphComponent) other
					.getNode()).getFirstRepresentationForRoot(parentRepr.getParentRepresentation());
			if(otherRepr == null)
			{
				others.remove(other);
				continue;
				// FIXME this may happen for some SIDELINKS. decide.
			}
			
			if((actualEdgeType == EdgeType.BACKLINK) && !parent.pathContains(other))
				actualEdgeType = EdgeType.SIDELINK;
			for(Edge edge : (isBackwards ? theGraph.getInEdges(parent.getNode()) : theGraph.getOutEdges(parent
					.getNode())))
				if((isBackwards && (edge.getFrom() == other.getNode()))
						|| (!isBackwards && (edge.getTo() == other.getNode())))
				{
					GraphicalRepresentationElement edgeRepr = new GraphicalRepresentationElement(this,
							(VisualizableGraphComponent) edge, Type.EDGE);
					edgeRepr.setEdge(actualEdgeType, parentRepr, otherRepr);
					
					parentRepr.connected.add(edgeRepr);
					((VisualizableGraphComponent) edge).addRepresentation(edgeRepr);
				}
			others.remove(other);
		}
	}
	
	protected void doLayout()
	{
		float w = (float) (bottomright.getX() - topleft.getX());
		float h = (float) (bottomright.getY() - topleft.getY());
		
		Point measurement = measureLayout((GraphicalRepresentationElement) theRepresentation);
		int wc = measurement.x;
		int hc = measurement.y;
		
		GraphicalRepresentationElement repr = (GraphicalRepresentationElement) theRepresentation;
		((GContainer) repr.gelement).setSize(w, h).setMoveTo(
				new Point2D.Double(topleft.getX() + w / 2, topleft.getY() + h / 2));
		
		doLayout((GraphicalRepresentationElement) theRepresentation, new Point(1, 0), w / (wc + 2), h / (hc + 1), repr);
		
	}
	
	protected void doLayout(GraphicalRepresentationElement repr, Point cPos, float wFactor, float hFactor,
			GraphicalRepresentationElement container)
	{
		repr.gelement.setCanvas(canvas);
		if(repr.glabel != null)
			repr.glabel.setCanvas(canvas);
		if(repr.type == Type.NODE)
			((GContainer) container.gelement).addReferencingElement(repr.gelement);
		
		li("layout for: [" + repr.label + "] at [" + cPos + "] having size [" + repr.subSize + "] : "
				+ (repr.edgeType == null ? "-" : repr.edgeType));
		switch(repr.type)
		{
		case EDGE:
			if(repr.edgeType == EdgeType.CHILDLINK)
				doLayout(repr.connected.get(1), new Point(cPos), wFactor, hFactor, container);
			((GConnector) repr.gelement).setFrom(repr.connected.get(0).gelement).setTo(repr.connected.get(1).gelement);
			break;
		case NODE:
			repr.positionInGrid(new Point(cPos), wFactor, hFactor);
			// add 1 to x to avoid forelinks passing through the node itself
			cPos.setLocation(cPos.x, cPos.y + 1);
			for(GraphicalRepresentationElement sub : repr.connected)
			{
				doLayout(sub, new Point(cPos), wFactor, hFactor, container);
				if(sub.edgeType == EdgeType.CHILDLINK)
					cPos.setLocation(cPos.x + sub.subSize.x, cPos.y);
			}
			break;
		case ELEMENT_CONTAINER:
			cPos.setLocation(cPos.x, cPos.y + 1);
			for(GraphicalRepresentationElement sub : repr.connected)
			{
				doLayout(sub, new Point(cPos), wFactor, hFactor, container);
				cPos.setLocation(cPos.x + sub.subSize.x, cPos.y);
			}
		}
	}
	
	protected Point measureLayout(GraphicalRepresentationElement repr)
	{
		int height = 0;
		int width = 0;
		switch(repr.type)
		{
		case ELEMENT_CONTAINER:
			for(GraphicalRepresentationElement sub : repr.connected)
			{
				Point result = measureLayout(sub);
				if(result.y > height)
					height = result.y;
				width += result.x;
			}
			return repr.setSize(new Point(width, height)).subSize;
		case EDGE:
			return repr.setSize(measureLayout(repr.connected.get(1))).subSize;
		case NODE:
			for(GraphicalRepresentationElement sub : repr.connected)
			{
				if(sub.edgeType != EdgeType.CHILDLINK)
					continue;
				Point result = measureLayout(sub);
				if(result.y > height)
					height = result.y;
				// add 1 so that forelinks don't have a chance to pass through the node itself
				width += result.x;
			}
			return repr.setSize(new Point((width == 0) ? 1 : width + 1, height + 1)).subSize;
		default:
			return new Point(width, height);
		}
	}
	
	@Override
	public GraphicalRepresentationElement getRepresentation()
	{
		return (GraphicalRepresentationElement) theRepresentation;
	}
	
	@Override
	public GCanvas displayRepresentation()
	{
		// ((GraphicalRepresentationElement)theRepresentation).gelement.setMoveTo(new Point2D.Float(20, 10));
		
		return canvas;
	}
}
