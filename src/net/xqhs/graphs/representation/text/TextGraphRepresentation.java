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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.HyperNode;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleNode;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.pattern.NodeP;
import net.xqhs.graphs.representation.GraphRepresentation;
import net.xqhs.graphs.representation.RepresentationElement;
import net.xqhs.graphs.representation.VisualizableGraphComponent;
import net.xqhs.graphs.representation.linear.LinearGraphRepresentation;
import net.xqhs.graphs.representation.linear.PathElement;
import net.xqhs.graphs.representation.text.TextRepresentationElement.Symbol;
import net.xqhs.graphs.representation.text.TextRepresentationElement.Type;
import net.xqhs.graphs.util.ContentHolder;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.Unit;
import net.xqhs.util.logging.UnitComponent;

/**
 * The class implements {@link GraphRepresentation} to create (or read from) a textual representation of a graph. For
 * creating the representation, it relies on {@link LinearGraphRepresentation}.
 * <p>
 * The created (or read) representation contains a single occurrence of each edge and a number of occurrences of each
 * node equal to its in-degree (except for the 'root' node of each connected subgraph).
 * <p>
 * Using the default values for symbols (see {@link Symbol}), below are some representations of simple graphs:
 * <ul>
 * <li>three nodes and two edges: <code>A -&gt; B -&gt; C</code>;
 * <li>the same example, with labeled edges: <code>A -edge-1-&gt; B -edge-2-&gt; C</code>;
 * <li>a tree with a root and two leafs: <code>A (-&gt; B) -&gt; C</code>;
 * <li>a three-node cycle: <code>A -&gt; B -&gt; C -&gt; *A</code>;
 * </ul>
 * A 'backwards' representation is also possible, that may be adequate for some cases. Example:<br/>
 * <code>A (&lt;-inherits-from- B &lt;-inherits-from- C) &lt;-inherits-from- D</code>.
 * <p>
 * If the representation contains a description for the graph, it will be read, interpreted, and added.
 * <p>
 * For more details on the output and input formats, see {@link #displayRepresentation()} and
 * {@link #readRepresentation(String)}.
 * <p>
 * While the representation has been mentioned in previous works, the main publication that presents it is: Andrei
 * Olaru, Context Matching for Ambient Intelligence Applications, Proceedings of SYNASC 2013, 15th International
 * Symposium on Symbolic and Numeric Algorithms for Scientific Computing, September 23-26, 2013 Timisoara, Romania, IEEE
 * CPS, 2013.
 * 
 * @author Andrei Olaru
 */
public class TextGraphRepresentation extends LinearGraphRepresentation
{
	/**
	 * The default branch separator.
	 */
	public static final String	DEFAULT_BRANCH_SEPARATOR	= "";
	/**
	 * The default separator increment.
	 */
	public static final String	DEFAULT_SEPARATOR_INCREMENT	= "";
	/**
	 * The default increment limit.
	 */
	public static final int		DEFAULT_INCREMENT_LIMIT		= -1;
	
	/**
	 * Branch separator. See {@link #setLayout(String, String, int)}.
	 */
	protected String			indent						= DEFAULT_BRANCH_SEPARATOR;
	/**
	 * Separator increment. See {@link #setLayout(String, String, int)}.
	 */
	protected String			indentIncrement				= DEFAULT_SEPARATOR_INCREMENT;
	/**
	 * Increment limit. See {@link #setLayout(String, String, int)}.
	 */
	protected int				incrementLimit				= DEFAULT_INCREMENT_LIMIT;
	
	/**
	 * Creates a new instance and links it to the specified graph. No processing will occur until {@link #update()} is
	 * called.
	 * 
	 * @param graph
	 *            - the {@link Graph} instance that will be linked to this representation.
	 */
	public TextGraphRepresentation(Graph graph)
	{
		super(graph);
	}
	
	/**
	 * Configures the presentation of the linear graph representation. More precisely, configures what happens in the
	 * output when a branch appears. See parameter descriptions for more details.
	 * 
	 * @param branchSeparator
	 *            - is added before each branch. Usually it is a newline.
	 * @param separatorIncrement
	 *            - <i>d</i> increments are added after the separator, where <i>d</i> is the depth of the parent node.
	 *            Usually it is a tabbing string.
	 * @param limit
	 *            - the use of separator and increment are limited to a depth specified by this parameter. Use 0 so that
	 *            no separation will occur. Use -1 to not limit the separation.
	 * @return the {@link LinearGraphRepresentation} instance, for chained calls.
	 */
	public TextGraphRepresentation setLayout(String branchSeparator, String separatorIncrement, int limit)
	{
		if(branchSeparator != null)
			this.indent = branchSeparator;
		if(separatorIncrement != null)
			this.indentIncrement = separatorIncrement;
		incrementLimit = limit;
		return this;
	}
	
	@Override
	protected String setDefaultName(String name)
	{
		return super.setDefaultName(name) + "T";
	}
	
	/**
	 * The method relies on a call of <code>buildPaths()</code> in {@link LinearGraphRepresentation}. After the paths
	 * are built, they are explored depth-first to directly build the text representation.
	 */
	@Override
	protected void processGraph()
	{
		super.processGraph();
		
		Set<PathElement> blackNodes = new HashSet<PathElement>(); // contains all 'visited' nodes
		TextRepresentationElement textRepresentation = new TextRepresentationElement(this, Type.ELEMENT_CONTAINER);
		textRepresentation.description = theGraph.getDescription();
		
		boolean first = true;
		for(PathElement el : paths)
			// check all paths (subgraphs)
			if(!blackNodes.contains(el))
			{
				// new subgraph: contains the representation of the whole subgraph
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
	
	/**
	 * Explores the children of the given path element, to create the list of corresponding representation elements. It
	 * recurses to completely explore the paths that start from the children.
	 * 
	 * @param el
	 *            - the element whose children to explore.
	 * @param level
	 *            - the current recursion level (starts at 1).
	 * @param blackNodes
	 *            - the set that contains the visited nodes, to which the method adds nodes that it visits.
	 * @return a {@link List} of {@link TextRepresentationElement} instances that correspond to the children of the
	 *         explored element.
	 */
	protected List<TextRepresentationElement> buildTextChildren(PathElement el, int level, Set<PathElement> blackNodes)
	{
		List<TextRepresentationElement> ret = new LinkedList<TextRepresentationElement>();
		
		int allchildren = el.getOtherChildren().size() + el.getChildren().size();
		int remainingChildren = allchildren;
		List<PathElement> others = new LinkedList<PathElement>(el.getOtherChildren());
		
		for(PathElement child : el.getChildren())
		{
			// attach 'other children' once they have been explored (in the past, or in the last recursive call)
			while(!others.isEmpty() && blackNodes.contains(others.get(0)))
			{
				remainingChildren--;
				PathElement other = others.get(0);
				// find edge to the other node
				for(Edge edge : (isBackwards ? theGraph.getInEdges(el.getNode()) : theGraph.getOutEdges(el.getNode())))
					if((isBackwards && (edge.getFrom() == other.getNode()))
							|| (!isBackwards && (edge.getTo() == other.getNode())))
					{
						// backlink
						// FIXME: check conversions
						TextRepresentationElement repr = new TextRepresentationElement(this,
								(VisualizableGraphComponent) edge, Type.INTERNAL_LINK, level, !(remainingChildren > 0),
								(allchildren == 1));
						repr.addSub(new TextRepresentationElement(this, (VisualizableGraphComponent) other.getNode(),
								Type.NODE));
						ret.add(repr);
					}
				others.remove(0);
			}
			
			remainingChildren--;
			boolean first = true;
			for(Edge edge : (isBackwards ? theGraph.getInEdges(el.getNode()) : theGraph.getOutEdges(el.getNode())))
				if((isBackwards && (edge.getFrom() == child.getNode()))
						|| (!isBackwards && (edge.getTo() == child.getNode())))
				{
					// branch
					// FIXME: check conversions
					Type type = first ? Type.BRANCH : Type.INTERNAL_LINK;
					TextRepresentationElement reprEdge = new TextRepresentationElement(this,
							(VisualizableGraphComponent) edge, type, level, !(remainingChildren > 0),
							(allchildren == 1));
					TextRepresentationElement reprNode = new TextRepresentationElement(this,
							(VisualizableGraphComponent) child.getNode(), Type.NODE);
					if(first)
						blackNodes.add(child);
					first = false;
					reprEdge.addSub(reprNode);
					reprNode.addSub(buildTextChildren(child, level + 1, blackNodes));
					ret.add(reprEdge);
				}
			
			// handle hypernodes - the node is a whole graph
			if(child.getNode() instanceof HyperNode)
			{
				TextRepresentationElement textRepresentation = new TextRepresentationElement(this,
						Type.ELEMENT_CONTAINER);
				first = true;
				for(PathElement elsub : paths)
					if(!blackNodes.contains(elsub))
					{
						TextRepresentationElement nodeRepr = new TextRepresentationElement(this,
								(VisualizableGraphComponent) elsub.getNode(), Type.NODE);
						blackNodes.add(elsub);
						nodeRepr.addSub(buildTextChildren(elsub, 1, blackNodes));
						
						TextRepresentationElement repr = new TextRepresentationElement(this, Type.SUBGRAPH, first);
						repr.addSub(nodeRepr);
						textRepresentation.addSub(repr);
						first = false;
					}
			}
		}
		
		// attach 'other children' that have been explored in the last recursive call
		while(!others.isEmpty())
		{
			remainingChildren--;
			PathElement other = others.get(0);
			boolean external = !blackNodes.contains(other);
			for(Edge edge : (isBackwards ? theGraph.getInEdges(el.getNode()) : theGraph.getOutEdges(el.getNode())))
				if((isBackwards && (edge.getFrom() == other.getNode()))
						|| (!isBackwards && (edge.getTo() == other.getNode())))
				{
					// backlinks and external links
					// FIXME: check conversions
					TextRepresentationElement repr = new TextRepresentationElement(this,
							(VisualizableGraphComponent) edge, (external ? Type.EXTERNAL_LINK : Type.INTERNAL_LINK),
							level, !(remainingChildren > 0), (allchildren == 1));
					repr.addSub(new TextRepresentationElement(this, (VisualizableGraphComponent) other.getNode(),
							Type.NODE));
					ret.add(repr);
				}
			blackNodes.add(other);
			others.remove(0);
		}
		
		return ret;
	}
	
	/**
	 * See {@link #displayRepresentation()}.
	 */
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
	
	@Override
	public boolean isBackwards()
	{
		return isBackwards;
	}
	
	/**
	 * Returns a text representation of the associated graph.
	 * <p>
	 * The representation uses the text-representations of the nodes (obtained by {@link Node#toString()}) and edges
	 * (obtained by {@link Edge#getLabel()}), and a few special symbols present in {@link Symbol}. By default,
	 * parentheses for branches (the last branch of a node is not surrounded by parentheses) and "*" to refer nodes that
	 * have already appeared in the representation earlier. Also, "^" for nodes outside the (sub)graph.
	 * <p>
	 * Example: a graph that is a triangle ABC with one other edge BD will be represented as (edges are not labeled): <br>
	 * A->B(-&gt;D)-&gt;C-&gt;*A<br>
	 * That is, there is a cycle A-B-C-A, and also there is also a branch from B to D.
	 * <p>
	 * The representation can be customized with the parameters set in {@link #setLayout(String, String, int)}.
	 * <p>
	 * If the graph is meant to be printed on several lines (<code>branchSeparator</code>) contains a new line, an
	 * additional <code>branchSeparator</code> will be added before the representation.
	 */
	@Override
	public String displayRepresentation()
	{
		String firstIndent = indent.contains("\n") ? indent : ""; // see javadoc
		if(theRepresentation != null)
			return firstIndent
					+ ((TextRepresentationElement) theRepresentation).toString(indent, indentIncrement, incrementLimit,
							isBackwards);
		le("representation not computed (use update()).");
		return null;
	}
	
	/**
	 * Reads the elements in the input into the represented graph. See {@link #readRepresentation(String)}.
	 * 
	 * @param stream
	 *            - the input stream.
	 * @return the graph instance.
	 */
	public Graph readRepresentation(InputStream stream)
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		StringBuilder builder = new StringBuilder();
		String line;
		try
		{
			while((line = reader.readLine()) != null)
			{
				builder.append(line);
				builder.append('\n');
			}
		} catch(IOException e)
		{
			le("Reading file failed: " + e.toString());
			return null;
		}
		return readRepresentation(builder.toString());
	}
	
	/**
	 * Reads the elements in the input into the represented graph. The representation must resemble one produced by
	 * method {@link #toString()} of this class, but only in syntax, and not necessarily with the same whitespace
	 * settings. The rules below are given in general terms, using the constants in {@link Symbol}, and have in
	 * parentheses the equivalent strings, using the default symbols.
	 * <ul>
	 * <li>if the input contains more than one graph, each graph should be contained between
	 * {@link Symbol#ELEMENT_CONTAINER_IN} and {@link Symbol#ELEMENT_CONTAINER_OUT} ([]).
	 * <li>if a description (e.g. name) for the graph is provided, it precedes the graph and is separated from the graph
	 * by {@link Symbol#DESCRIPTION_SEPARATOR} (:).
	 * <li>subgraph are separated by {@link Symbol#SUBGRAPH_SEPARATOR} (;).
	 * <li>node names / labels cannot contain {@link Symbol#DESCRIPTION_SEPARATOR}, {@link Symbol#EDGE_LIMIT},
	 * {@link Symbol#EDGE_ENDING_FORWARD}, or {@link Symbol#EDGE_ENDING_BACKWARD} (: - &lt; &gt;).
	 * <li>edge names / labels cannot contain {@link Symbol#DESCRIPTION_SEPARATOR}, {@link Symbol#EDGE_ENDING_FORWARD}
	 * or {@link Symbol#EDGE_ENDING_BACKWARD} (: &lt; &gt;).
	 * <li>all edges are uni-directional, and all edges are either to the right, or to the left (for 'backwards'
	 * representations).
	 * <li>a labeled edge begins with a {@link Symbol#EDGE_LIMIT} (-) (or ends, if the representation is backwards).
	 * <li>an edge in a forward representation ends with a {@link Symbol#EDGE_ENDING_FORWARD}, optionally preceded (no
	 * whitespace) by {@link Symbol#EDGE_LIMIT} (&gt; or -&gt;).
	 * <li>an edge in a backwards representation begins with {@link Symbol#EDGE_ENDING_BACKWARD}, optionally followed
	 * (no whitespace) by {@link Symbol#EDGE_LIMIT} (&lt; or &lt;-).
	 * <li>an unlabeled edge is either just the {@link Symbol#EDGE_ENDING_FORWARD} / {@link Symbol#EDGE_ENDING_BACKWARD}
	 * , or contains a {@link Symbol#EDGE_LIMIT} as well (&gt; or -&gt; / &lt; or &lt;-).
	 * <li>a subtree that is not the last subtree in a node that is the root of multiple subtrees is contained between
	 * {@link Symbol#BRANCH_IN} and {@link Symbol#BRANCH_OUT} (parentheses).
	 * <li>the target node (destination node in a backwards representation) of an edge is preceded (no whitespace) by
	 * {@link Symbol#INTERNAL_LINK_PREFIX} if the node has already been present in the input before, and by
	 * {@link Symbol#EXTERNAL_LINK_PREFIX} if it is not part of the current graph (FIXME currently not supported).
	 * <li>all whitespace between elements are accepted and ignored (unless stated otherwise above).
	 * </ul>
	 * All elements are attached to the existing elements in the represented graph, if any.
	 * 
	 * @param rawInput
	 *            - the input string.
	 * @return the graph instance.
	 * 
	 * @throws IllegalArgumentException
	 *             if the input is broken (referenced nodes not found, no target nodes for edges, etc.
	 */
	public Graph readRepresentation(String rawInput)
	{
		return readRepresentation(new ContentHolder<String>(rawInput));
	}
	
	/**
	 * Same as {@link #readRepresentation(String)}, but taking the input from a {@link ContentHolder}, leaving any
	 * un-consumed input in the holder.
	 * 
	 * @param input
	 *            - the input, held be a {@link ContentHolder} instance.
	 * @return the graph instance.
	 * 
	 * @throws IllegalArgumentException
	 *             if the input is broken (referenced nodes not found, no target nodes for edges, etc.
	 */
	public Graph readRepresentation(ContentHolder<String> input)
	{
		lf("reading graph", input.get());
		if(input.get().trim().length() == 0)
		{
			le("input is empty.");
			return null;
		}
		
		// detect backwards-ness
		boolean backwards = input.get().indexOf(Symbol.EDGE_ENDING_BACKWARD.toString()) >= 0;
		if(backwards && (input.get().indexOf(Symbol.EDGE_ENDING_FORWARD.toString()) >= 0))
			return (Graph) lr(null, "Representation contains both forward and backward edges.");
		
		setBackwards(backwards);
		
		// read the representation without connecting all the nodes and edges
		TextRepresentationElement rootRepr = TextRepresentationElement.readRepresentation(input, this,
				(UnitComponent) new UnitComponent().setUnitName(Unit.DEFAULT_UNIT_NAME).setLink(getUnitName())
						.setLogLevel(Level.WARN));
		theRepresentation = rootRepr;
		lf("result: [] \n====================================", theRepresentation.toString());
		
		// make all connections
		return buildGraph(rootRepr);
	}
	
	/**
	 * Based on the representation elements read from the input, the method assembles the represented graph.
	 * 
	 * @param rootRepresentation
	 *            - the root {@link TextRepresentationElement} of the representation (normally same as
	 *            #theRepresentation).
	 * @return the built {@link Graph}.
	 */
	protected Graph buildGraph(TextRepresentationElement rootRepresentation)
	{
		// start building the graph: create a tree with the occasional links between branches or inside the same branch.
		// the tree; the top of the stack is the current (deepest) level.
		Stack<Queue<TextRepresentationElement>> tree = new Stack<Queue<TextRepresentationElement>>();
		// the current level
		Queue<TextRepresentationElement> cLevel = new LinkedList<TextRepresentationElement>(), nLevel = null;
		// add the representation of the root element on the first level
		cLevel.add((TextRepresentationElement) theRepresentation);
		tree.add(cLevel);
		
		// very easy to build
		// for each element, its direct children are added in a new level of the tree
		// any internal links are to nodes that have been already defined (have been already inserted in the graph)
		while(!tree.isEmpty())
		{
			cLevel = tree.peek();
			lf("inspecting tree at level []", new Integer(tree.size()));
			if(cLevel.isEmpty())
				tree.pop(); // completely explored the level, backtrack
			else
			{
				lf("[] element(s) left to inspect", new Integer(cLevel.size()));
				
				TextRepresentationElement element = cLevel.poll();
				Type type = element.linkType;
				switch(type)
				{
				case ELEMENT_CONTAINER:
					if(element.description != null)
						theGraph.setDescription(element.description);
					//$FALL-THROUGH$
				case SUBGRAPH:
					// nothing to do, just register children (content) for processing
					lf("inspecting []", type);
					nLevel = new LinkedList<TextRepresentationElement>();
					nLevel.addAll(element.content);
					tree.push(nLevel);
					break;
				case NODE:
				{ // add a new node
					Node node = (Node) element.getRepresentedComponent();
					lf("inspecting []: []", type, node);
					li("adding to graph node []", node);
					// FIXME: check casts
					// FIXME: check if index already exists.
					if((node instanceof NodeP) && ((NodeP) node).isGeneric() && ((NodeP) node).genericIndex() > 0)
						((GraphPattern) theGraph).addNode(node, false);
					else
						theGraph.addNode(node);
					
					if(element.content.isEmpty())
						break; // no edges
					nLevel = new LinkedList<TextRepresentationElement>();
					tree.push(nLevel);
					
					// add outgoing / incoming (for backwards) edges
					for(TextRepresentationElement edgeEl : element.content)
					{
						SettableEdge edge = (SettableEdge) edgeEl.getRepresentedComponent();
						if(!isBackwards)
							edge.setFrom(node);
						else
							edge.setTo(node);
						lf("adding to queue []: []", edgeEl.linkType, edge);
						nLevel.add(edgeEl);
					}
					break;
				}
				case BRANCH:
				case INTERNAL_LINK:
				{ // add a new edge
					SettableEdge edge = (SettableEdge) element.getRepresentedComponent();
					lf("inspecting []: []", type, edge);
					
					Node targetNode = null; // target node of the edge
					if(element.content.isEmpty())
						throw new IllegalArgumentException("Edge representation element contains no target");
					TextRepresentationElement targetNodeEl = element.content.iterator().next();
					if(element.linkType == Type.INTERNAL_LINK)
					{ // edge is to already existing node
						Node dummyTargetNode = (Node) targetNodeEl.getRepresentedComponent();
						if(dummyTargetNode instanceof NodeP && ((NodeP) dummyTargetNode).isGeneric())
						{ // target is pattern / generic node
							lf("searching pattern target node []", dummyTargetNode);
							for(Node candidateNode : theGraph.getNodesNamed(dummyTargetNode.getLabel()))
								if(candidateNode instanceof NodeP
										&& ((NodeP) dummyTargetNode).genericIndex() == ((NodeP) candidateNode)
												.genericIndex())
									targetNode = candidateNode;
							if(targetNode == null)
								throw new IllegalArgumentException("Target pattern node [" + dummyTargetNode
										+ "] not found in the graph.");
							lf("found old target pattern node []", targetNode);
						}
						else
						{ // target is a normally labeled node
							lf("searching target node []", dummyTargetNode);
							Collection<Node> candidates = theGraph.getNodesNamed(dummyTargetNode.getLabel());
							if(candidates.isEmpty())
								throw new IllegalArgumentException("Target pattern node [" + dummyTargetNode
										+ "] not found in the graph.");
							targetNode = candidates.iterator().next();
							lf("found old target node []", targetNode);
						}
					}
					else
					{ // actual new node - edge is a normal BRANCH
						targetNode = (SimpleNode) targetNodeEl.getRepresentedComponent();
						if(!theGraph.getNodesNamed(targetNode.getLabel()).isEmpty())
							if(targetNode instanceof NodeP)
							{
								Collection<Node> similarNodes = theGraph.getNodesNamed(targetNode.getLabel());
								for(Node n : similarNodes)
									if(((NodeP) n).genericIndex() == ((NodeP) targetNode).genericIndex())
										throw new IllegalArgumentException("Node [" + targetNode.toString()
												+ "] already present in the graph by not referred as internal link.");
							}
							else
								throw new IllegalArgumentException("Node [" + targetNode.toString()
										+ "] already present in the graph by not referred as internal link.");
						nLevel = new LinkedList<TextRepresentationElement>();
						nLevel.add(targetNodeEl);
						tree.push(nLevel);
						lf("target node added to queue []", targetNode);
					}
					if(!isBackwards)
						edge.setTo(targetNode);
					else
						edge.setFrom(targetNode);
					li("adding to graph edge []", edge);
					theGraph.addEdge(edge.toSimpleEdge());
					break;
				}
				case EXTERNAL_LINK:
					// FIXME: support external links
					le("external links not supported");
				}
			}
		}
		return theGraph;
	}
}
