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
package net.xqhs.graphs.matcher;

import java.awt.Font;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import net.xqhs.graphical.GCanvas;
import net.xqhs.graphical.GElement;
import net.xqhs.graphical.GElement.ReferenceType;
import net.xqhs.graphical.GLabel;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.representation.GraphRepresentation;
import net.xqhs.graphs.representation.VisualizableGraphComponent;
import net.xqhs.graphs.representation.graphical.GraphicalRepresentationElement;
import net.xqhs.graphs.representation.graphical.RadialGraphRepresentation;
import net.xqhs.util.logging.Unit;

/**
 * Visualizer for the matching process. Uses a {@link GCanvas} that is passed to the respective graphical
 * representations.
 * 
 * @author Andrei Olaru
 * 
 */
public class MatchingVisualizer extends Unit
{
	/**
	 * The canvas to use.
	 */
	GCanvas	theCanvas	= null;
	
	/**
	 * The current position - top left corner for the current line.
	 */
	Point	topleftLine	= null;
	
	/**
	 * The height of a line.
	 */
	int		lineHeight	= 200;
	
	/**
	 * Fraction of the line height to consider as spacing between lines.
	 */
	float	lineSpacing	= .2f;
	
	int		fontSize	= 20;
	
	/**
	 * @param canvas
	 *            - the {@link GCanvas} to use
	 * @return the instance itself
	 */
	public MatchingVisualizer setCanvas(GCanvas canvas)
	{
		theCanvas = canvas;
		return this;
	}
	
	/**
	 * @param topleft
	 *            - the coordinate on the canvas to use as top left corner for the visualization
	 * @return the instance itself
	 */
	public MatchingVisualizer setTopLeft(Point topleft)
	{
		topleftLine = topleft;
		return this;
	}
	
	/**
	 * @param height
	 *            - the height of a feed line
	 * @return the instance itself
	 */
	public MatchingVisualizer setLineHeight(int height)
	{
		lineHeight = height;
		return this;
	}
	
	public MatchingVisualizer feedLine(Match m1, String comment)
	{
		List<Match> ms = new ArrayList<>(1);
		ms.add(m1);
		return feedLine(ms, comment);
	}
	
	public MatchingVisualizer feedLine(Match m1, Match m2, String comment)
	{
		List<Match> ms = new ArrayList<>(2);
		ms.add(m1);
		ms.add(m2);
		return feedLine(ms, comment);
	}
	
	public MatchingVisualizer feedLine(Match m1, Match m2, Match m3, String comment)
	{
		List<Match> ms = new ArrayList<>(3);
		ms.add(m1);
		ms.add(m2);
		ms.add(m3);
		return feedLine(ms, comment);
	}
	
	public MatchingVisualizer feedLine(List<Match> ms, String comment)
	{
		GraphRepresentation lastrepr = null;
		int i = 0;
		for(Match m : ms)
		{
			lastrepr = m.toVisual(theCanvas, new Point(topleftLine.x + i * 2 * lineHeight, topleftLine.y), new Point(
					topleftLine.x + (i + 1) * 2 * lineHeight, topleftLine.y + lineHeight));
			i++;
		}
		
		new GLabel()
				.setCanvas(theCanvas)
				.setFont(Font.PLAIN, fontSize)
				.setText(comment)
				.setReference(((GraphicalRepresentationElement) lastrepr.getRepresentation()).getGElement(),
						ReferenceType.CENTER);
		theCanvas.repaint();
		
		topleftLine.setLocation(topleftLine.x, topleftLine.y + lineHeight * (1 + lineSpacing));
		return this;
	}
	
	/**
	 * Displays in the visualizer a line showing a graphical representation of the specified graph, with the specified
	 * component highlighted, and the specified comment next to the representation.
	 * 
	 * @param graph
	 *            - the graph
	 * @param highlight
	 *            - the highlighted component
	 * @param comment
	 *            - the comment / message to display
	 * @return the instance itself
	 */
	public MatchingVisualizer feedLine(Graph graph, VisualizableGraphComponent highlight, String comment)
	{
		GraphRepresentation repr = new RadialGraphRepresentation(graph).setCanvas(theCanvas).setOrigin(topleftLine)
				.setBottomRight(new Point(topleftLine.x + lineHeight, topleftLine.y + lineHeight)).update();
		
		if(highlight != null)
			((GraphicalRepresentationElement) highlight.getFirstRepresentationForRoot(repr)).setHighlighted(true);
		new GLabel()
				.setCanvas(theCanvas)
				.setFont(Font.PLAIN, fontSize)
				.setText(comment)
				.setReference(((GraphicalRepresentationElement) repr.getRepresentation()).getGElement(),
						ReferenceType.CENTER);
		theCanvas.repaint();
		
		topleftLine.setLocation(topleftLine.x, topleftLine.y + lineHeight * (1 + lineSpacing));
		return this;
	}
	
	/**
	 * Displays in the visualizer a line showing only one comment.
	 * 
	 * @param comment
	 *            - the comment
	 * @return the instance itself
	 */
	public MatchingVisualizer feedLine(String comment)
	{
		GElement label = new GLabel().setCanvas(theCanvas).setFont(Font.PLAIN, fontSize).setText(comment)
				.setMoveTo(new Point2D.Float(topleftLine.x, topleftLine.y));
		theCanvas.repaint();
		
		topleftLine.setLocation(topleftLine.x, topleftLine.y + label.getCurrentBox().getHeight() + lineHeight
				* lineSpacing);
		return this;
	}
}
