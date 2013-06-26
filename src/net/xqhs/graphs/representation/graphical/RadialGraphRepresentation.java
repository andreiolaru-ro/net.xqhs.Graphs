package net.xqhs.graphs.representation.graphical;

import java.awt.Point;
import java.awt.geom.Point2D;

import net.xqhs.graphical.GConnector;
import net.xqhs.graphical.GContainer;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.representation.graphical.GraphicalRepresentationElement.EdgeType;
import net.xqhs.graphs.representation.graphical.GraphicalRepresentationElement.Type;

/**
 * A representation based on the {@link GraphicalGraphRepresentation}, but using a radial layout instead of a top-down
 * right-to-left one.
 * 
 * @author Andrei Olaru
 * 
 */
public class RadialGraphRepresentation extends GraphicalGraphRepresentation
{
	
	/**
	 * Creates a new representation for the specified {@link Graph} instance.
	 * 
	 * @param theGraph
	 *            - the graph
	 */
	public RadialGraphRepresentation(Graph theGraph)
	{
		super(theGraph);
	}
	
	@Override
	protected void doLayout()
	{
		float w = (float) (bottomright.getX() - topleft.getX());
		float h = (float) (bottomright.getY() - topleft.getY());
		
		Point measurement = measureLayout((GraphicalRepresentationElement) theRepresentation);
		int wc = measurement.x;
		int hc = measurement.y;
		lf("measured wc = " + wc + " and hc = " + hc);
		
		GraphicalRepresentationElement repr = (GraphicalRepresentationElement) theRepresentation;
		((GContainer) repr.gelement).setSize(w, h).setMoveTo(
				new Point2D.Double(topleft.getX() + w / 2, topleft.getY() + h / 2));
		
		doLayout((GraphicalRepresentationElement) theRepresentation, new Point(-1, 0), new Point2D.Float(h / (hc - .5f)
				/ 2, h / 2), (wc > 2 ? 1f / (wc - 2) : 0), (float) (h / (hc - .5) / 2 * Math.sqrt(2)), repr);
	}
	
	protected void doLayout(GraphicalRepresentationElement repr, Point cPos, Point2D ref, float aFactor, float rFactor,
			GraphicalRepresentationElement container)
	{
		repr.gelement.setCanvas(canvas);
		if(repr.glabel != null)
			repr.glabel.setCanvas(canvas);
		if(repr.type == Type.NODE)
			((GContainer) container.gelement).addReferencingElement(repr.gelement);
		
		li("layout (r) for: [" + repr.label + "] at [" + cPos + "] having size [" + repr.subSize + "] : "
				+ (repr.edgeType == null ? "-" : repr.edgeType));
		switch(repr.type)
		{
		case EDGE:
			if(repr.edgeType == EdgeType.CHILDLINK)
				doLayout(repr.connected.get(1), new Point(cPos), ref, aFactor, rFactor, container);
			((GConnector) repr.gelement).setFrom(repr.connected.get(0).gelement).setTo(repr.connected.get(1).gelement);
			break;
		case NODE:
			repr.positionInRadial(new Point(cPos), ref, aFactor, rFactor);
			// add 1 to x to avoid forelinks passing through the node itself
			cPos.setLocation(cPos.x + 1, cPos.y + 1);
			for(GraphicalRepresentationElement sub : repr.connected)
			{
				doLayout(sub, new Point(cPos), ref, aFactor, rFactor, container);
				if(sub.edgeType == EdgeType.CHILDLINK)
					cPos.setLocation(cPos.x + sub.subSize.x, cPos.y);
			}
			break;
		case ELEMENT_CONTAINER:
			cPos.setLocation(cPos.x, cPos.y);
			for(GraphicalRepresentationElement sub : repr.connected)
			{
				doLayout(sub, new Point(cPos), ref, aFactor, rFactor, container);
				cPos.setLocation(cPos.x + sub.subSize.x, cPos.y);
			}
		}
	}
}
