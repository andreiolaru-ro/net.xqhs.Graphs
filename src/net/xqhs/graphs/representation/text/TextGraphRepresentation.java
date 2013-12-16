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
package net.xqhs.graphs.representation.text;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleEdge;
import net.xqhs.graphs.graph.SimpleNode;
import net.xqhs.graphs.pattern.NodeP;
import net.xqhs.graphs.representation.RepresentationElement;
import net.xqhs.graphs.representation.VisualizableGraphComponent;
import net.xqhs.graphs.representation.linear.LinearGraphRepresentation;
import net.xqhs.graphs.representation.linear.PathElement;
import net.xqhs.graphs.representation.text.TextRepresentationElement.Symbol;
import net.xqhs.graphs.representation.text.TextRepresentationElement.Type;
import net.xqhs.graphs.util.ContentHolder;
import net.xqhs.util.logging.UnitComponent;

public class TextGraphRepresentation extends LinearGraphRepresentation
{
	
	protected String	indent			= "";
	protected String	indentIncrement	= "";
	protected int		incrementLimit	= -1;
	
	public TextGraphRepresentation(Graph theGraph)
	{
		super(theGraph);
	}
	
	/**
	 * Configures the presentation of the linear graph representation. More precisely, configures what happens in the
	 * output when a branch appears. See parameter descriptions for more details.
	 * 
	 * @param _separator
	 *            - is added before each branch. Usually it is a newline.
	 * @param _indent
	 *            - <i>d</i> indentations are added after the separator, where <i>d</i> is the depth of the parent node.
	 *            Usually it is a tabbing string.
	 * @param limit
	 *            - the use of separator and indent are limited to a depth specified by this parameter. Use 0 so that no
	 *            separation will occur. Use -1 to not limit the separation.
	 * @return the {@link LinearGraphRepresentation} instance, for chained calls.
	 */
	public TextGraphRepresentation setLayout(String _separator, String _indent, int limit)
	{
		if(_separator != null)
			this.indent = _separator;
		if(_indent != null)
			this.indentIncrement = _indent;
		incrementLimit = limit;
		return this;
	}
	
	@Override
	protected String setDefaultName(String name)
	{
		return super.setDefaultName(name) + "T";
	}
	
	@Override
	protected void processGraph()
	{
		super.processGraph();
		
		Set<PathElement> blackNodes = new HashSet<>(); // contains all 'visited' nodes
		TextRepresentationElement textRepresentation = new TextRepresentationElement(this, Type.ELEMENT_CONTAINER);
		
		boolean first = true;
		for(PathElement el : paths)
			// check all paths (subgraphs)
			if(!blackNodes.contains(el))
			{
				// subgraph: contains the representation of the whole subgraph
				TextRepresentationElement nodeRepr = new TextRepresentationElement(this,
						(VisualizableGraphComponent) el.getNode(), Type.NODE);
				blackNodes.add(el);
				nodeRepr.addSub(buildTextChildren(el, 1, blackNodes));
				
				TextRepresentationElement repr = new TextRepresentationElement(this, Type.SUBGRAPH, first);
				repr.addSub(nodeRepr);
				textRepresentation.addSub(repr);
				first = false;
			}
		this.theRepresentation = textRepresentation;
	}
	
	protected List<TextRepresentationElement> buildTextChildren(PathElement el, int level, Set<PathElement> blackNodes)
	{
		List<TextRepresentationElement> ret = new LinkedList<>();
		
		int allchildren = el.getOtherChildren().size() + el.getChildren().size();
		int remainingChildren = allchildren;
		List<PathElement> others = new LinkedList<>(el.getOtherChildren());
		
		for(PathElement child : el.getChildren())
		{
			while(!others.isEmpty() && blackNodes.contains(others.get(0)))
			{
				remainingChildren--;
				PathElement other = others.get(0);
				// FIXME: works only for a single edge between two nodes
				// FIXME: can there be null returns
				Edge edge = null;
				if(isBackwards)
					edge = el.getNode().getEdgesFrom(other.getNode()).iterator().next();
				else
					edge = el.getNode().getEdgesTo(other.getNode()).iterator().next();
				// backlink
				// FIXME: check conversions
				TextRepresentationElement repr = new TextRepresentationElement(this, (VisualizableGraphComponent) edge,
						Type.INTERNAL_LINK, level, !(remainingChildren > 0), (allchildren == 1));
				repr.addSub(new TextRepresentationElement(this, (VisualizableGraphComponent) other.getNode(), Type.NODE));
				ret.add(repr);
				
				others.remove(0);
			}
			
			remainingChildren--;
			Edge edge = null;
			if(isBackwards)
				edge = el.getNode().getEdgesFrom(child.getNode()).iterator().next();
			else
				edge = el.getNode().getEdgesTo(child.getNode()).iterator().next();
			// branch
			// FIXME: check conversions
			TextRepresentationElement reprEdge = new TextRepresentationElement(this, (VisualizableGraphComponent) edge,
					Type.BRANCH, level, !(remainingChildren > 0), (allchildren == 1));
			TextRepresentationElement reprNode = new TextRepresentationElement(this,
					(VisualizableGraphComponent) child.getNode(), Type.NODE);
			blackNodes.add(child);
			reprEdge.addSub(reprNode);
			reprNode.addSub(buildTextChildren(child, level + 1, blackNodes));
			ret.add(reprEdge);
		}
		
		while(!others.isEmpty())
		{
			remainingChildren--;
			PathElement other = others.get(0);
			boolean external = !blackNodes.contains(other);
			Edge edge = null;
			if(isBackwards)
				edge = el.getNode().getEdgesFrom(other.getNode()).iterator().next();
			else
				edge = el.getNode().getEdgesTo(other.getNode()).iterator().next();
			// backlinks and external links
			TextRepresentationElement repr = new TextRepresentationElement(this, (VisualizableGraphComponent) edge,
					(external ? Type.EXTERNAL_LINK : Type.INTERNAL_LINK), level, !(remainingChildren > 0),
					(allchildren == 1));
			repr.addSub(new TextRepresentationElement(this, (VisualizableGraphComponent) other.getNode(), Type.NODE));
			ret.add(repr);
			blackNodes.add(other);
			others.remove(0);
		}
		
		return ret;
	}
	
	@Override
	public String toString()
	{
		return displayRepresentation();
	}
	
	@Override
	public RepresentationElement getRepresentation()
	{
		return theRepresentation;
	}
	
	public boolean isBackwards()
	{
		return isBackwards;
	}
	
	/**
	 * Returns a text representation of the associated graph, on one line.
	 * 
	 * <p>
	 * The representation uses the text-representations of the nodes (written by {@link SimpleNode}.toString() ) and
	 * edges (written by {@link SimpleEdge} .toStringShort()}, and a few special symbols: parentheses for branches (the
	 * last branch of a node is not surrounded by parentheses) and "*" to refer nodes that have already appeared in the
	 * representation earlier. Also, "^" for nodes outside the (sub)graph.
	 * 
	 * <p>
	 * Example: a graph that is a triangle ABC with one other edge BD will be represented as (edges are not labeled): <br>
	 * A->B(->D)->C->*A<br>
	 * That is, there is a cycle A-B-C-A, and also there is also a branch from B to D.
	 */
	@Override
	public String displayRepresentation()
	{
		return ((TextRepresentationElement) theRepresentation).toString(indent, indentIncrement, incrementLimit,
				isBackwards);
	}
	
	/**
	 * Reads the elements in the input into the represented graph.
	 * 
	 * @param rawInput
	 *            - the input stream
	 * @return the graph instance
	 */
	public Graph readRepresentation(String rawInput)
	{
		li("reading graph");
		ContentHolder<String> input = new ContentHolder<>(rawInput);
		
		setBackwards(input.get().indexOf(Symbol.EDGE_ENDING_BACKWARD.toString()) >= 0);
		theRepresentation = TextRepresentationElement.readRepresentation(input, this,
				(UnitComponent) new UnitComponent().setLink(getUnitName()));
		lf("result:" + theRepresentation.toString() + "\n====================================");
		
		// start building the graph
		Stack<Queue<TextRepresentationElement>> tree = new Stack<>();
		Queue<TextRepresentationElement> cLevel = new LinkedList<>(), nLevel = null;
		cLevel.add((TextRepresentationElement) theRepresentation);
		tree.add(cLevel);
		
		// very easy to build: any internal links are to nodes that have been already defined
		while(!tree.isEmpty())
		{
			cLevel = tree.peek();
			lf("inspecting tree at level [" + tree.size() + "]");
			if(cLevel.isEmpty())
				tree.pop();
			else
			{
				lf("[" + cLevel.size() + "] elements left to inspect");
				
				TextRepresentationElement element = cLevel.poll();
				Type type = element.linkType;
				switch(type)
				{
				case ELEMENT_CONTAINER:
				case SUBGRAPH:
					lf("inspecting [" + type + "]");
					nLevel = new LinkedList<>();
					nLevel.addAll(element.content);
					tree.push(nLevel);
					break;
				case NODE:
				{
					SimpleNode node = (SimpleNode) element.getRepresentedComponent();
					lf("inspecting [" + type + "]: [" + node + "]");
					li("adding to graph node [" + node + "]");
					theGraph.addNode(node);
					
					nLevel = new LinkedList<>();
					tree.push(nLevel);
					
					// add edges
					for(TextRepresentationElement edgeEl : element.content)
					{
						SettableEdge edge = (SettableEdge) edgeEl.getRepresentedComponent();
						if(!isBackwards)
							edge.setFrom(node);
						else
							edge.setTo(node);
						lf("adding to queue [" + edgeEl.linkType + "]: [" + edge + "]");
						nLevel.add(edgeEl);
					}
					break;
				}
				case BRANCH:
				case INTERNAL_LINK:
				case EXTERNAL_LINK:
				{
					SettableEdge edge = (SettableEdge) element.getRepresentedComponent();
					lf("inspecting [" + type + "]: [" + edge + "]");
					
					nLevel = new LinkedList<>();
					tree.push(nLevel);
					Node targetNode = null;
					TextRepresentationElement targetNodeEl = element.content.iterator().next();
					if(element.linkType == Type.INTERNAL_LINK)
					{
						SimpleNode dummyTargetNode = (SimpleNode) targetNodeEl.getRepresentedComponent();
						if(dummyTargetNode instanceof NodeP && ((NodeP) dummyTargetNode).isGeneric())
						{
							lf("searching pattern target node [" + dummyTargetNode + "]");
							for(Node candidateNode : theGraph.getNodesNamed(dummyTargetNode.getLabel()))
								if(candidateNode instanceof NodeP
										&& ((NodeP) dummyTargetNode).genericIndex() == ((NodeP) candidateNode)
												.genericIndex())
									targetNode = candidateNode;
							lf("found old target pattern node [" + targetNode + "]");
						}
						else
						{
							lf("searching target node [" + dummyTargetNode + "]");
							targetNode = theGraph.getNodesNamed(dummyTargetNode.getLabel()).iterator().next();
							lf("found old target node [" + targetNode + "]");
						}
					}
					else
					{ // no external links should actually appear here, i think? TODO: is it?
						// actual new node
						targetNode = (SimpleNode) targetNodeEl.getRepresentedComponent();
						nLevel.add(targetNodeEl);
						lf("target node (added to queue) [" + targetNode + "]");
					}
					if(!isBackwards)
						edge.setTo(targetNode);
					else
						edge.setFrom(targetNode);
					li("adding to graph edge [" + edge + "]");
					theGraph.addEdge(edge.toSimpleEdge());
				}
				}
			}
		}
		return theGraph;
	}
}
