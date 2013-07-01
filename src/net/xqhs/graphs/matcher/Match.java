package net.xqhs.graphs.matcher;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import net.xqhs.graphical.GCanvas;
import net.xqhs.graphical.GConnector;
import net.xqhs.graphical.GElement;
import net.xqhs.graphs.graph.ConnectedNode;
import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.matcher.GraphMatcherQuick.MatchComparator;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.pattern.NodeP;
import net.xqhs.graphs.representation.GraphRepresentation;
import net.xqhs.graphs.representation.VisualizableGraphComponent;
import net.xqhs.graphs.representation.graphical.GraphicalRepresentationElement;
import net.xqhs.graphs.representation.graphical.RadialGraphRepresentation;
import net.xqhs.graphs.representation.text.TextGraphRepresentation;

/**
 * Class describing a [partial] match of GP in G. In time, matches go from a 1-edge match to a maximal match.
 * <p>
 * Main components:
 * <ul>
 * <li>the current matched part between the pattern and the graph:
 * <ul>
 * <li>GmP (or G_m^P) the solved part of the pattern - is connected;
 * <li>G' the subgraph of G that has been matched to GmP - is connected;
 * <li>the correspondence between the nodes in GmP and G' (<code>nodeFunction</code>) - a bijective function
 * <li>the correspondence between the edges in GmP and G' (<code>edgeFunction</code>) - also a bijective function, in
 * the sense that the values of the function (which are sets of edges) cover E' and do not have intersections.
 * </ul>
 * <li>the unsolved part of the pattern - GxP;
 * <li>the number of edges that are not yet matched - K = ||ExP||
 * <li>the frontier - the nodes in GmP that have adjacent edges that have not yet been included in the match (edges in
 * ExP); the frontier also contains information on how many such edges exist for each node in the frontier.
 * <li>a set of matches that are candidates to merge with this match.
 * </ul>
 * 
 * @author Andrei Olaru
 */
public class Match
{
	/**
	 * Reference to the graph G.
	 */
	Graph						targetGraphLink;
	/**
	 * Reference to the pattern GP
	 */
	GraphPattern				patternLink;
	
	/**
	 * G', the subgraph of G that has been matched. It is connected and it is a proper graph.
	 */
	Graph						matchedGraph;
	/**
	 * GmP, the part of GP that has been matched. It is connected and it is a proper graph.
	 */
	GraphPattern				solvedPart;
	/**
	 * GxP, the part of GP that has not been matched. It may contain edges without the adjacent nodes.
	 */
	GraphPattern				unsolvedPart;
	/**
	 * k, the number of edges in GxP
	 */
	int							k;
	
	/**
	 * The correspondence (node) function VmP -> V'
	 */
	Map<Node, Node>				nodeFunction;
	/**
	 * The correspondence (edge) function EmP -> E'
	 */
	Map<Edge, List<Edge>>		edgeFunction;
	/**
	 * The nodes on the frontier of GmP - nodes that have adjacent edges in ExP. Nodes are a subset of VmP.
	 * <p>
	 * For each node the number of remaining edges in ExP that are adjacent to it is given.
	 */
	Map<NodeP, AtomicInteger>	frontier				= null;
	/**
	 * MC, matches that could possibly be merged with this one (i.e. not intersecting and sharing at least one common
	 * vertex (with a common correspondent in the graph).
	 */
	Set<Match>					mergeCandidates			= null;
	/**
	 * MO, matches that could potentially merge with this one, but not immediately (they are not adjacent).
	 */
	Set<Match>					mergeOuterCandidates	= null;
	
	/**
	 * The name of the edge.
	 * <p>
	 * Initially (for single-edge matches) the id is the id of the pattern edge, dash, a counter for matches based on
	 * that edge.
	 */
	String						id						= "-";
	
	/**
	 * Create a new empty match; some parts may be uninitialized / undefined (like frontier, or matchCandidates)
	 * <p>
	 * This constructor is meant just to construct matches that will later be completely initialized as a result of
	 * merging two existing matches.
	 * 
	 * @param g
	 *            : the graph
	 * @param p
	 *            : the pattern
	 */
	public Match(Graph g, GraphPattern p)
	{
		targetGraphLink = g;
		patternLink = p;
	}
	
	/**
	 * Create a match, using an initial matching edge.
	 * 
	 * @param g
	 *            : the graph
	 * @param p
	 *            : the pattern
	 * @param e
	 *            : the matching edge in the graph
	 * @param eP
	 *            : the matching edge in the pattern
	 * @param id
	 *            : the matching graph edge's id
	 */
	public Match(Graph g, GraphPattern p, Edge e, Edge eP, String id)
	{
		this(g, p);
		
		// G' contains the edge and the two adjacent nodes
		matchedGraph = new SimpleGraph().addNode(e.getFrom()).addNode(e.getTo()).addEdge(e);
		// GmP contains the pattern edge and the two adjacent nodes
		solvedPart = (GraphPattern) new GraphPattern().addNode(eP.getFrom(), false).addNode(eP.getTo(), false)
				.addEdge(eP);
		// TODO: check casts
		ConnectedNode ePFrom = (ConnectedNode) eP.getFrom();
		ConnectedNode ePTo = (ConnectedNode) eP.getTo();
		// node function
		nodeFunction = new HashMap<>();
		nodeFunction.put(ePFrom, e.getFrom());
		nodeFunction.put(ePTo, e.getTo());
		// edge function
		edgeFunction = new HashMap<>();
		List<Edge> eL = new ArrayList<>();
		eL.add(e);
		edgeFunction.put(eP, eL);
		// the frontier contains both nodes (if it is the case), with their adjacent edges minus the matched edge
		frontier = new HashMap<>();
		if(ePFrom.getInEdges().size() + ePFrom.getOutEdges().size() > 1)
			frontier.put((NodeP) eP.getFrom(), new AtomicInteger(ePFrom.getInEdges().size()
					+ ePFrom.getOutEdges().size() - 1));
		if(ePTo.getInEdges().size() + ePTo.getOutEdges().size() > 1)
			frontier.put((NodeP) eP.getTo(),
					new AtomicInteger(ePTo.getInEdges().size() + ePTo.getOutEdges().size() - 1));
		// unsolved part (all nodes and edges except the matched ones)
		unsolvedPart = new GraphPattern();
		for(Node vP : p.getNodes())
			if((vP != eP.getFrom()) && (vP != eP.getTo()))
				unsolvedPart.addNode(vP, false);
		for(Edge ePi : p.getEdges())
			if(ePi != eP)
				unsolvedPart.addEdge(ePi);
		k = unsolvedPart.getEdges().size();
		
		// no match candidates added; they will be added in addInitialMatches()
		mergeCandidates = new TreeSet<>(new MatchComparator());
		mergeOuterCandidates = new TreeSet<>(new MatchComparator());
		
		this.id = id;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return (obj instanceof Match) ? this.id.equals(((Match) obj).id) : false;
	}
	
	@Override
	public int hashCode()
	{
		return id.hashCode();
	}
	
	/**
	 * Shows a legend of the <code>toString</code> output.
	 * 
	 * @return the output.
	 */
	public static String toStringDemo()
	{
		String ret = "match [ <id> ] (k= <k> ): ";
		ret += "<G'>" + "<GmP>" + "\n\t";
		ret += "Gx: <GxP>" + "\n\t";
		ret += "frontier: <frontier>";
		ret += "mCs: [ <merge candidates ids> ] \n\t";
		ret += "fv: <node function>";
		return ret;
	}
	
	@Override
	public String toString()
	{
		String ret = "match [" + id + "] (k=" + k + "): \t";
		ret += new TextGraphRepresentation(matchedGraph).setLayout("", " ", 2).update() + "\t : \t";
		ret += new TextGraphRepresentation(solvedPart).setLayout("", " ", 2).update() + "\t";
		ret += "Gx: " + new TextGraphRepresentation(unsolvedPart).setLayout("", " ", 2).update() + "\t";
		ret += "frontier: " + frontier + "; ";
		ret += "mCs: [";
		for(Match mi : mergeCandidates)
			ret += mi.id + ", ";
		ret += "] mOCs: [";
		for(Match moi : mergeOuterCandidates)
			ret += moi.id + ", ";
		ret += "] \t";
		ret += "fv: " + nodeFunction;
		// ret += "fe: " + edgeFunction + "\n\t";
		return ret;
	}
	
	/**
	 * Provides an extensive string representation of the match, spanning multiple rows.
	 * 
	 * @return the string representation
	 */
	public String toStringLong()
	{
		String ret = "match: \n\t";
		ret += "G': " + new TextGraphRepresentation(matchedGraph).setLayout("", " ", 2).update() + "\n\t";
		ret += "Gm: " + new TextGraphRepresentation(solvedPart).setLayout("", " ", 2).update() + "\n\t";
		ret += "Gx: " + new TextGraphRepresentation(unsolvedPart).setLayout("", " ", 2).update() + "\n\t";
		ret += "fv: " + nodeFunction + "\n\t";
		ret += "fe: " + edgeFunction + "\n\t";
		ret += "k=" + k;
		
		return ret;
	}
	
	public GraphRepresentation toVisual(GCanvas canvas, Point topleft, Point bottomright)
	{
		int tw = bottomright.x - topleft.x;
		GraphRepresentation GR = new RadialGraphRepresentation(targetGraphLink).setCanvas(canvas).setOrigin(topleft)
				.setBottomRight(new Point(topleft.x + tw / 2, bottomright.y)).update();
		GraphRepresentation GPR = new RadialGraphRepresentation(patternLink).setCanvas(canvas)
				.setOrigin(new Point(topleft.x + tw / 2, topleft.y)).setBottomRight(bottomright).update();
		for(Map.Entry<Edge, List<Edge>> corr : edgeFunction.entrySet())
		{
			// TODO check casts
			GElement el1 = ((GraphicalRepresentationElement) ((VisualizableGraphComponent) corr.getKey())
					.getFirstRepresentationForRoot(GPR)).getGElement();
			GElement el2 = ((GraphicalRepresentationElement) ((VisualizableGraphComponent) corr.getValue().iterator()
					.next()).getFirstRepresentationForRoot(GR)).getGElement();
			el1.setColor(Color.RED);
			el2.setColor(Color.RED);
			new GConnector().setFrom(el1).setTo(el2).setCanvas(canvas).setColor(Color.RED).setStrokeWidth(.5f);
		}
		return GPR;
	}
}
