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

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;

import net.xqhs.graphical.GConnector;
import net.xqhs.graphical.GContainer;
import net.xqhs.graphical.GElement;
import net.xqhs.graphical.GElement.ReferenceType;
import net.xqhs.graphical.GLabel;
import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.representation.GraphRepresentation;
import net.xqhs.graphs.representation.RepresentationElement;
import net.xqhs.graphs.representation.VisualizableGraphComponent;

public class GraphicalRepresentationElement extends RepresentationElement
{
	enum Type {
		NODE, EDGE, ELEMENT_CONTAINER
	}
	
	enum EdgeType {
		CHILDLINK, SIDELINK, EXTLINK, FORELINK, BACKLINK
	}
	
	Type									type;
	
	GElement								gelement		= null;
	GLabel									glabel			= null;
	Point									gridPos			= null; // not applicable for edges
	Point									subSize			= null; // not applicable for edges
	String									label			= "";
	float									widthFactor		= 1;
	float									heightFactor	= 1;
	
	// GraphComponent peer = null;
	List<GraphicalRepresentationElement>	connected		= null;
	EdgeType								edgeType		= null;
	
	public GraphicalRepresentationElement(GraphRepresentation root, VisualizableGraphComponent component,
			Type elementType)
	{
		super(root, component);
		
		this.type = elementType;
		connected = new LinkedList<GraphicalRepresentationElement>();
		
		switch(elementType)
		{
		case ELEMENT_CONTAINER:
			gelement = new GContainer().setReferenceType(ReferenceType.TOPLEFT);
			break;
		case EDGE:
			gelement = new GConnector();
			glabel = new GLabel().setText(((Edge) this.getRepresentedComponent()).getLabel()).setParent(gelement);
			label = ((Edge) this.getRepresentedComponent()).getLabel();
			break;
		case NODE:
			gelement = new GElement();
			glabel = new GLabel().setText(((Node) this.getRepresentedComponent()).getLabel()).setParent(gelement);
			label = ((Node) this.getRepresentedComponent()).getLabel();
			break;
		default:
			gelement = new GElement();
			break;
		}
		gelement.setRepresented(getRepresentedComponent());
		if(getRepresentedComponent() != null)
			getRepresentedComponent().addRepresentation(this);
	}
	
	public GraphicalRepresentationElement setEdge(EdgeType type, GraphicalRepresentationElement from,
			GraphicalRepresentationElement to)
	{
		if(this.type != Type.EDGE)
			throw new IllegalArgumentException("function is only available for edges");
		this.connected.add(from);
		this.connected.add(to);
		this.edgeType = type;
		return this;
	}
	
	public GraphicalRepresentationElement setSize(Point size)
	{
		this.subSize = size;
		return this;
	}
	
	public GraphicalRepresentationElement setHighlighted(boolean doHighlight)
	{
		if(gelement != null)
		{
			if(doHighlight)
				gelement.setColor(Color.RED);
			else
				gelement.setColor(null);
		}
		return this;
	}
	
	public GElement getGElement()
	{
		return gelement;
	}
	
	public GraphicalRepresentationElement positionInGrid(Point position, float widthFactor, float heightFactor)
	{
		this.gridPos = new Point(position);
		this.widthFactor = widthFactor;
		this.heightFactor = heightFactor;
		
		gelement.setMoveTo(new Point2D.Float(gridPos.x * widthFactor, gridPos.y * heightFactor));
		return this;
	}
	
	public GraphicalRepresentationElement positionInRadial(Point position, Point2D center, float angleFactor,
			float rangeFactor)
	{
		this.gridPos = new Point(position);
		this.widthFactor = angleFactor;
		this.heightFactor = rangeFactor;
		double init = Math.PI / 6;
		double interval = Math.PI * 4 / 6;
		
		gelement.setMoveTo(new Point2D.Double(center.getX() + gridPos.y * rangeFactor
				* Math.sin(gridPos.x * angleFactor * interval + init), center.getY() + gridPos.y * rangeFactor
				* Math.cos(gridPos.x * angleFactor * interval + init)));
		return this;
	}
}
