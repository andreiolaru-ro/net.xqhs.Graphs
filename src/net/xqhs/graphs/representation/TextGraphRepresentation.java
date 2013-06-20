package net.xqhs.graphs.representation;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import net.xqhs.graphs.graph.SimpleEdge;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.graph.SimpleNode;
import net.xqhs.graphs.pattern.NodeP;
import net.xqhs.graphs.representation.TextRepresentationElement.Symbol;
import net.xqhs.graphs.representation.TextRepresentationElement.TextRepElementConfig;
import net.xqhs.graphs.representation.TextRepresentationElement.Type;
import net.xqhs.graphs.util.ContentHolder;
import net.xqhs.util.logging.UnitComponent;
import net.xqhs.util.logging.UnitConfigData;

public class TextGraphRepresentation extends LinearGraphRepresentation
{
	public static class GraphConfig extends LinearGraphRepresentation.GraphConfig
	{
		protected String	indent			= "";
		protected String	indentIncrement	= "";
		protected int		incrementLimit	= -1;
		
		public GraphConfig(SimpleGraph g)
		{
			super(g);
		}
		
		@Override
		public GraphConfig makeDefaults()
		{
			super.makeDefaults();
			return this;
		}
		
		/**
		 * Configures the presentation of the linear graph representation. More precisely, configures what happens in
		 * the output when a branch appears. See parameter descriptions for more details.
		 * 
		 * @param _separator
		 *            - is added before each branch. Usually it is a newline.
		 * @param _indent
		 *            - <i>d</i> indentations are added after the separator, where <i>d</i> is the depth of the parent
		 *            node. Usually it is a tabbing string.
		 * @param limit
		 *            - the use of separator and indent are limited to a depth specified by this parameter. Use 0 so
		 *            that no separation will occur. Use -1 to not limit the separation.
		 * @return the {@link LinearGraphRepresentation} instance, for chained calls.
		 */
		public GraphConfig setLayout(String _separator, String _indent, int limit)
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
	}
	
	public TextGraphRepresentation(GraphConfig _config)
	{
		super(_config);
	}
	
	@Override
	protected void processGraph()
	{
		super.processGraph();
		
		Set<PathElement> blackNodes = new HashSet<>(); // contains all 'visited' nodes
		TextRepresentationElement textRepresentation = new TextRepresentationElement(new TextRepElementConfig(this,
				Type.ELEMENT_CONTAINER));
		
		boolean first = true;
		for(PathElement el : paths)
			// check all paths (subgraphs)
			if(!blackNodes.contains(el))
			{
				// subgraph: contains the representation of the whole subgraph
				TextRepresentationElement nodeRepr = new TextRepresentationElement(new TextRepElementConfig(this,
						el.node, Type.NODE));
				blackNodes.add(el);
				nodeRepr.addSub(buildTextChildren(el, 1, blackNodes));
				
				TextRepresentationElement repr = new TextRepresentationElement(new TextRepElementConfig(this,
						Type.SUBGRAPH, first));
				repr.addSub(nodeRepr);
				textRepresentation.addSub(repr);
				first = false;
			}
		this.theRepresentation = textRepresentation;
	}
	
	protected List<TextRepresentationElement> buildTextChildren(PathElement el, int level, Set<PathElement> blackNodes)
	{
		GraphConfig conf = (GraphConfig) this.config;
		
		List<TextRepresentationElement> ret = new LinkedList<>();
		
		int allchildren = el.otherChildren.size() + el.children.size();
		int remainingChildren = allchildren;
		List<PathElement> others = new LinkedList<>(el.otherChildren);
		
		for(PathElement child : el.children)
		{
			while(!others.isEmpty() && blackNodes.contains(others.get(0)))
			{
				remainingChildren--;
				PathElement other = others.get(0);
				SimpleEdge edge = conf.isBackwards ? el.node.getEdgesFrom(other.node) : el.node.getEdgesTo(other.node);
				// backlink
				// ret.add(new TextRepresentationElement(new TextRepElementConfig(this, other.node.toString(),
				// edge.toStringShort(conf.isBackwards),
				// Type.INTERNAL_LINK, level, !(remainingChildren > 0), (allchildren == 1))));
				TextRepresentationElement repr = new TextRepresentationElement(new TextRepElementConfig(this, edge,
						Type.INTERNAL_LINK, level, !(remainingChildren > 0), (allchildren == 1)));
				repr.addSub(new TextRepresentationElement(new TextRepElementConfig(this, other.node, Type.NODE)));
				ret.add(repr);
				
				others.remove(0);
			}
			
			remainingChildren--;
			SimpleEdge edge = conf.isBackwards ? el.node.getEdgesFrom(child.node) : el.node.getEdgesTo(child.node);
			// branch
			TextRepresentationElement reprEdge = new TextRepresentationElement(new TextRepElementConfig(this, edge,
					Type.BRANCH, level, !(remainingChildren > 0), (allchildren == 1)));
			TextRepresentationElement reprNode = new TextRepresentationElement(new TextRepElementConfig(this,
					child.node, Type.NODE));
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
			SimpleEdge edge = conf.isBackwards ? el.node.getEdgesFrom(other.node) : el.node.getEdgesTo(other.node);
			// backlinks and external links
			TextRepresentationElement repr = new TextRepresentationElement(new TextRepElementConfig(this, edge,
					(external ? Type.EXTERNAL_LINK : Type.INTERNAL_LINK), level, !(remainingChildren > 0),
					(allchildren == 1)));
			repr.addSub(new TextRepresentationElement(new TextRepElementConfig(this, other.node, Type.NODE)));
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
	
	protected boolean isBackwards()
	{
		return ((GraphConfig) config).isBackwards;
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
		return ((TextRepresentationElement) theRepresentation).toString(((GraphConfig) config).indent,
				((GraphConfig) config).indentIncrement, ((GraphConfig) config).incrementLimit);
	}
	
	public static SimpleGraph readRepresentation(String rawInput)
	{
		return readRepresentation(rawInput, null, null);
	}
	
	public static SimpleGraph readRepresentation(String rawInput, UnitConfigData graphUnitConfig,
			UnitConfigData thisUnitConfig)
	{
		UnitComponent log = new UnitComponent(thisUnitConfig);
		
		log.li("reading graph");
		ContentHolder<String> input = new ContentHolder<>(rawInput);
		SimpleGraph g = new SimpleGraph(graphUnitConfig);
		
		boolean isBackwards = input.get().indexOf(Symbol.EDGE_ENDING_BACKWARD.toString()) >= 0;
		TextGraphRepresentation repr = new TextGraphRepresentation(
				(GraphConfig) new TextGraphRepresentation.GraphConfig(g).setBackwards(isBackwards));
		repr.theRepresentation = TextRepresentationElement.readRepresentation(input, repr, log);
		log.lf("result:" + repr.theRepresentation.toString() + "\n====================================");
		
		// start building the graph
		Stack<Queue<TextRepresentationElement>> tree = new Stack<>();
		Queue<TextRepresentationElement> cLevel = new LinkedList<>(), nLevel = null;
		cLevel.add((TextRepresentationElement) repr.theRepresentation);
		tree.add(cLevel);
		
		// very easy to build: any internal links are to nodes that have been already defined
		while(!tree.isEmpty())
		{
			cLevel = tree.peek();
			log.lf("inspecting tree at level [" + tree.size() + "]");
			if(cLevel.isEmpty())
				tree.pop();
			else
			{
				log.lf("[" + cLevel.size() + "] elements left to inspect");
				
				TextRepresentationElement element = cLevel.poll();
				Type type = ((TextRepElementConfig) element.config).linkType;
				switch(type)
				{
				case ELEMENT_CONTAINER:
				case SUBGRAPH:
					log.lf("inspecting [" + type + "]");
					nLevel = new LinkedList<>();
					nLevel.addAll(element.content);
					tree.push(nLevel);
					break;
				case NODE:
				{
					SimpleNode node = (SimpleNode) element.config.representedComponent;
					log.lf("inspecting [" + type + "]: [" + node + "]");
					log.li("adding to graph node [" + node + "]");
					g.addNode(node);
					
					nLevel = new LinkedList<>();
					tree.push(nLevel);
					
					// add edges
					for(TextRepresentationElement edgeEl : element.content)
					{
						SimpleEdge edge = (SimpleEdge) edgeEl.config.representedComponent;
						if(!isBackwards)
							edge.setFrom(node);
						else
							edge.setTo(node);
						log.lf("adding to queue [" + ((TextRepElementConfig) edgeEl.config).linkType + "]: [" + edge
								+ "]");
						nLevel.add(edgeEl);
					}
					break;
				}
				case BRANCH:
				case INTERNAL_LINK:
				case EXTERNAL_LINK:
				{
					SimpleEdge edge = (SimpleEdge) element.config.representedComponent;
					log.lf("inspecting [" + type + "]: [" + edge + "]");
					
					nLevel = new LinkedList<>();
					tree.push(nLevel);
					SimpleNode targetNode = null;
					TextRepresentationElement targetNodeEl = element.content.iterator().next();
					if(((TextRepElementConfig) element.config).linkType == Type.INTERNAL_LINK)
					{
						SimpleNode dummyTargetNode = (SimpleNode) targetNodeEl.config.representedComponent;
						if(dummyTargetNode instanceof NodeP && ((NodeP) dummyTargetNode).isGeneric())
						{
							log.lf("searching pattern target node [" + dummyTargetNode + "]");
							for(SimpleNode candidateNode : g.getNodesNamed(dummyTargetNode.getLabel()))
								if(candidateNode instanceof NodeP
										&& ((NodeP) dummyTargetNode).genericIndex() == ((NodeP) candidateNode)
												.genericIndex())
									targetNode = candidateNode;
							log.lf("found old target pattern node [" + targetNode + "]");
						}
						else
						{
							log.lf("searching target node [" + dummyTargetNode + "]");
							targetNode = g.getNodesNamed(dummyTargetNode.getLabel()).iterator().next();
							log.lf("found old target node [" + targetNode + "]");
						}
					}
					else
					{ // no external links should actually appear here, i think? TODO: is it?
						// actual new node
						targetNode = (SimpleNode) targetNodeEl.config.representedComponent;
						nLevel.add(targetNodeEl);
						log.lf("target node (added to queue) [" + targetNode + "]");
					}
					if(!isBackwards)
						edge.setTo(targetNode);
					else
						edge.setFrom(targetNode);
					log.li("adding to graph edge [" + edge + "]");
					g.addEdge(edge);
				}
				}
			}
		}
		log.doExit();
		return g;
	}
}
