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

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import net.xqhs.graphical.GCanvas;
import net.xqhs.graphical.GConnector;
import net.xqhs.graphical.GElement;
import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.pattern.GraphPattern;
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
 * Version 1.5 brings integration of some of the processes related to matching into the implementation of Match, such as
 * checking merge candidates, merging, etc.
 * 
 * @author Andrei Olaru
 */
public class Match
{
	/**
	 * Possible values for the situations in which two matches can be with regard to merging.
	 * 
	 * @author Andrei Olaru
	 */
	public static enum Candidacy {
		/**
		 * The two matches may be merged in the future (appropriate matches containing these two matches may be merged),
		 * but not immediately (they have no common vertices). They are 'outer' merge candidates for each other.
		 */
		OUTER,
		
		/**
		 * The two matches may be merged immediately (using {@link Match#merge(Match, Map, Map, MonitorPack)}) with no
		 * further checks. They are merge candidates for each other.
		 */
		IMMEDIATE,
		
		/**
		 * The two matches cannot be merged. This can be because:
		 * <ul>
		 * <li>they cover intersecting parts of the pattern;
		 * <li>their matched subgraphs intersect;
		 * <li>common vertices in the pattern do not have the same correspondent in the graph.
		 * </ul>
		 */
		NONE,
	}
	
	/**
	 * {@link Match} comparator.
	 * <p>
	 * The matches are sorted according to <code>k</code> (smaller k first). If equal, order by id.
	 */
	public static class MatchComparator implements Comparator<Match>
	{
		/**
		 * Link to the object measuring performance of the algorithm in terms of number of compared edges.
		 */
		@SuppressWarnings("unused")
		private MonitorPack	monitorLink	= null;
		
		/**
		 * Creates a match comparator.
		 * 
		 * @param monitor
		 *            - the object measuring performance in terms of edge matches.
		 */
		public MatchComparator(MonitorPack monitor)
		{
			monitorLink = monitor;
		}
		
		@Override
		public int compare(Match m1, Match m2)
		{
			if(m1.k != m2.k)
				return m1.k - m2.k;
			// dbg(D_G.D_MATCHING_INITIAL, "re-compare []", m1.id.compareTo(m2.id));
			if(m1.id.equals(m2.id))
				return m1.hashCode() - m2.hashCode();
			return m1.id.compareTo(m2.id);
		}
	}
	
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
	Map<Node, AtomicInteger>	frontier				= null;
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
	 * States that the match is still valid. In persistent matching, a match may become invalid but not yet removed from
	 * various structures.
	 * 
	 * @since 1.5
	 */
	boolean						valid					= true;
	
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
		Node ePFrom = eP.getFrom();
		Node ePTo = eP.getTo();
		// node function
		nodeFunction = new HashMap<Node, Node>();
		nodeFunction.put(ePFrom, e.getFrom());
		nodeFunction.put(ePTo, e.getTo());
		// edge function
		edgeFunction = new HashMap<Edge, List<Edge>>();
		List<Edge> eL = new ArrayList<Edge>();
		eL.add(e);
		edgeFunction.put(eP, eL);
		// the frontier contains both nodes (if it is the case), with their adjacent edges minus the matched edge
		frontier = new HashMap<Node, AtomicInteger>();
		if(p.getInEdges(ePFrom).size() + p.getOutEdges(ePFrom).size() > 1)
			frontier.put(eP.getFrom(),
					new AtomicInteger(p.getInEdges(ePFrom).size() + p.getOutEdges(ePFrom).size() - 1));
		if(p.getInEdges(ePTo).size() + p.getOutEdges(ePTo).size() > 1)
			frontier.put(eP.getTo(), new AtomicInteger(p.getInEdges(ePTo).size() + p.getOutEdges(ePTo).size() - 1));
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
		mergeCandidates = new TreeSet<Match>(new MatchComparator(null));
		mergeOuterCandidates = new TreeSet<Match>(new MatchComparator(null));
		
		this.id = id;
	}
	
	// TODO: fix this that gives bugs:
	// @Override
	// public boolean equals(Object obj)
	// {
	// return (obj instanceof Match) ? this.id.equals(((Match) obj).id) : false;
	// }
	//
	// @Override
	// public int hashCode()
	// {
	// return id.hashCode();
	// }
	
	/**
	 * Invalidates the match. The method can be used classes extending {@link GraphMatcherQuick} by calling
	 * {@link GraphMatcherQuick#invalidateMatch(Match)}. This way matches cannot be invalidated by other classes.
	 * 
	 * @since 1.5
	 */
	protected void invalidate()
	{
		valid = false;
	}
	
	/**
	 * Checks if the match is valid. Invalid matches are on the way to be removed from various structures.
	 * 
	 * @return <code>true</code> if the match is still valid.
	 * 
	 * @since 1.5
	 */
	public boolean isValid()
	{
		return valid;
	}
	
	/**
	 * Gets the <i>k</i> of the match -- the number of edges from the pattern that don't have a match in the matched
	 * subgraph. Best k is <code>0</code>, worst k (single-edge match) is number of edges in the pattern minus 1.
	 * 
	 * @return the k of the match.
	 */
	public int getK()
	{
		return k;
	}
	
	/**
	 * @return the number of edges in the matched part (solved part) of the pattern.
	 */
	public int getSize()
	{
		return solvedPart.m();
	}
	
	/**
	 * @return the {@link Graph} against which is the match.
	 */
	public Graph getGraph()
	{
		return targetGraphLink;
	}
	
	/**
	 * @return the {@link GraphPattern} of which is the match.
	 */
	public GraphPattern getPattern()
	{
		return patternLink;
	}
	
	/**
	 * The method checks whether another match can be considered as a candidate for merger with this match, and, if yes,
	 * what kind (see {@link Candidacy}).
	 * <p>
	 * This version of the method relies on edge-to-containing-match and pattern-edge-to-containing-match indexes to
	 * locate potential intersection in the matches.
	 * 
	 * @param mc
	 *            - the other match.
	 * @param eMatchIndex
	 *            - the index of graph edge &rarr; matches that contain that edge.
	 * @param ePMatchIndex
	 *            - the index of graph pattern edge &rarr; matches that contain that edge.
	 * @param monitor
	 *            - the {@link MonitorPack} to use for performance measures.
	 * @return the appropriate {@link Candidacy} value for this match and the match in the argument.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the arguments is <code>null</code>.
	 */
	public Candidacy getCandidacy(Match mc, Map<Edge, Set<Match>> eMatchIndex, Map<Edge, Set<Match>> ePMatchIndex,
			MonitorPack monitor)
	{
		// TODO if any previous match invalidation could generate false negatives
		if((mc == null) || (ePMatchIndex == null) || (eMatchIndex == null) || (monitor == null))
			throw new IllegalArgumentException("Indexes must be non-null");
		// check that match mc does not already contain any edges in this match
		for(Edge eP : solvedPart.getEdges())
		{
			monitor.incrementEdgeReferenceOperation();
			if(ePMatchIndex.containsKey(eP) && ePMatchIndex.get(eP).contains(mc))
				return Candidacy.NONE;
		}
		// check that the subgraph matched by mc does not already contain any edges in this match
		for(Edge e : matchedGraph.getEdges())
		{
			monitor.incrementEdgeReferenceOperation();
			if(eMatchIndex.containsKey(e) && eMatchIndex.get(e).contains(mc))
				return Candidacy.NONE;
		}
		return getCandidacyInternal(mc, monitor);
	}
	
	/**
	 * Internal method that tests candidacy status based solely on internal properties of the matches.
	 * <p>
	 * More precisely, it checks that nodes on the frontiers of moth matches have the same correspondent nodes in the
	 * graph. It also checks if there is any intersection between the two frontiers, deciding between
	 * {@link Candidacy#OUTER} (no intersection) and {@link Candidacy#IMMEDIATE}.
	 * 
	 * @param mc
	 *            - the other match.
	 * @param monitor
	 *            - the {@link MonitorPack} to use for performance measures.
	 * @return the appropriate {@link Candidacy} value for this match and the match in the argument.
	 */
	protected Candidacy getCandidacyInternal(Match mc, MonitorPack monitor)
	{
		boolean outer = true;
		// iterate over frontier intersection, see if nodes correspond to the same target in the matched subgraph
		Set<Node> frontierIntersection = new HashSet<Node>(frontier.keySet());
		frontierIntersection.retainAll(mc.frontier.keySet());
		monitor.incrementNodeReferenceOperation(frontier.size());
		
		for(Node node : frontierIntersection)
		{
			monitor.incrementNodeReferenceOperation();
			if(nodeFunction.get(node) == mc.nodeFunction.get(node))
				outer = false;
			else
				return Candidacy.NONE;
		}
		if(outer)
			return Candidacy.OUTER;
		return Candidacy.IMMEDIATE;
	}
	
	/**
	 * The method checks if the given match is a merge candidate for this match, and if it is, the matches are added to
	 * the merge candidate lists of each other. This call <b>modifies</b> the matches (their candidate lists) in case
	 * candidacy is not {@link Candidacy#NONE}.
	 * 
	 * @param mc
	 *            - the other match.
	 * @param eMatchIndex
	 *            - the index of graph edge &rarr; matches that contain that edge.
	 * @param ePMatchIndex
	 *            - the index of graph pattern edge &rarr; matches that contain that edge.
	 * @param monitor
	 *            - the {@link MonitorPack} to use for performance measures.
	 * @return the appropriate {@link Candidacy} value for this match and the match in the argument.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the arguments is <code>null</code> or if the other match is not between the same graph and
	 *             pattern.
	 */
	public Candidacy considerCandidate(Match mc, Map<Edge, Set<Match>> eMatchIndex, Map<Edge, Set<Match>> ePMatchIndex,
			MonitorPack monitor)
	{
		if((mc == null) || (mc.targetGraphLink != targetGraphLink) || (mc.patternLink != patternLink))
			throw new IllegalArgumentException("Other match is not in the same space");
		Candidacy cand = getCandidacy(mc, eMatchIndex, ePMatchIndex, monitor);
		if(cand == Candidacy.IMMEDIATE)
		{
			mergeCandidates.add(mc);
			mc.mergeCandidates.add(this);
		}
		if(cand == Candidacy.OUTER)
		{
			mergeOuterCandidates.add(mc);
			mc.mergeOuterCandidates.add(this);
		}
		return cand;
	}
	
	/**
	 * Merges two matches into a new one and returns the result.
	 * <p>
	 * Matches are expected to be correct merge candidates, that is disjoint (in terms of edges), both as GmP (matched
	 * part of pattern) and as G' (matched subgraph); also, all common nodes in GmP must correspond to the same nodes in
	 * G' (this check should be done initially in {@link #getCandidacy(Match, Map, Map, MonitorPack)}.
	 * <p>
	 * <b>Attention:</b> matches are expected to be merge-able without any further checks.
	 * <p>
	 * If the passed indexes are not null, the match is added to the indexes.
	 * 
	 * @param m1
	 *            - the other match.
	 * @param eMatchIndex
	 *            - the index of graph edge &rarr; matches that contain that edge. If not <code>null</code>, it is used
	 *            for indexing the new match.
	 * @param ePMatchIndex
	 *            - the index of graph pattern edge &rarr; matches that contain that edge. If not <code>null</code>, it
	 *            is used for indexing the new match.
	 * @param monitor
	 *            - the {@link MonitorPack} to use for performance measures.
	 * @return the merged match.
	 */
	public Match merge(Match m1, Map<Edge, Set<Match>> eMatchIndex, Map<Edge, Set<Match>> ePMatchIndex,
			MonitorPack monitor)
	{
		// must handle (create in the new match, based on the two matches:
		// G and GP links -> in constructor
		// GmP -> obtained by adding edges from m1.GmP and m2.GmP and their adjacent vertices
		// G' -> obtained by adding the values of the edge and node functions, when adding edges in GmP
		// GxP -> obtained by removing edges added to GmP and nodes
		// k -> obtained by decrementing when adding edges
		// node function -> reuniting the node functions of the two matches
		// edge function -> reuniting the edge functions of the two matches
		// frontier -> practically adding nodes from solved part, always checking if they are still on the frontier
		// MC -> MC = (MC n MC2) u (MC1 n MO2) u (MC2 n MO1)
		// MO -> common outer candidates: MO = MO1 n MO2
		// id TODO
		
		// G and GP links -> set in constructor
		Match newM = new Match(targetGraphLink, patternLink);
		
		// add whole pattern (same for both matches) as unsolved part
		newM.unsolvedPart = new GraphPattern();
		newM.unsolvedPart.addAll(patternLink.getNodes());
		newM.unsolvedPart.addAll(patternLink.getEdges());
		newM.k = newM.unsolvedPart.getEdges().size();
		
		// there should be no duplicates as the solved parts should be disjoint. This is checked further on.
		Set<Edge> totalMatch = new HashSet<Edge>();
		
		totalMatch.addAll(solvedPart.getEdges());
		totalMatch.addAll(m1.solvedPart.getEdges());
		
		newM.solvedPart = new GraphPattern();
		newM.nodeFunction = new HashMap<Node, Node>();
		newM.edgeFunction = new HashMap<Edge, List<Edge>>();
		newM.matchedGraph = new SimpleGraph();
		newM.frontier = new HashMap<Node, AtomicInteger>();
		for(Edge eP : totalMatch)
		{
			monitor.incrementEdgeReferenceOperation(2);
			// GmP -> obtained by adding edges from m1.GmP and m2.GmP and their adjacent vertices
			newM.solvedPart.addEdge(eP).addNode(eP.getFrom()).addNode(eP.getTo());
			// GxP -> obtained by removing edges added to GmP and nodes
			newM.unsolvedPart.removeEdge(eP).removeNode(eP.getFrom()).removeNode(eP.getTo());
			// k -> obtained by decrementing when adding edges
			newM.k--;
			
			Match sourceMatch = null; // which match does eP come from
			if(solvedPart.contains(eP))
				sourceMatch = this;
			if(m1.solvedPart.contains(eP))
			{
				if(sourceMatch != null)
				{
					monitor.le("match-intersection pattern edge found: []", eP);
					System.out.println("\t\t [" + this + "] \t\t [" + m1 + "]");
					throw new IllegalArgumentException("match-intersection edge");
				}
				sourceMatch = m1;
			}
			if(sourceMatch == null)
				throw new InternalError("edge not found in total match"); // impossible
				
			// node function -> reuniting the node functions of the two matches
			newM.nodeFunction.put(eP.getFrom(), sourceMatch.nodeFunction.get(eP.getFrom()));
			newM.nodeFunction.put(eP.getTo(), sourceMatch.nodeFunction.get(eP.getTo()));
			// edge function -> reuniting the edge functions of the two matches
			newM.edgeFunction.put(eP, sourceMatch.edgeFunction.get(eP));
			// G' -> obtained by adding the values of the edge and node functions, when adding edges in GmP
			for(Edge em : sourceMatch.edgeFunction.get(eP))
			{
				newM.matchedGraph.addEdge(em).addNode(em.getFrom()).addNode(em.getTo());
				if(eMatchIndex != null)
				// add to index
				{
					if(!eMatchIndex.containsKey(em))
						eMatchIndex.put(em, new HashSet<Match>());
					eMatchIndex.get(em).add(newM);
				}
				monitor.incrementEdgeReferenceOperation(1);
			}
			if(ePMatchIndex != null)
			// add to index
			{
				if(!ePMatchIndex.containsKey(eP))
					ePMatchIndex.put(eP, new HashSet<Match>());
				ePMatchIndex.get(eP).add(newM);
				monitor.incrementEdgeReferenceOperation(1);
			}
			
			// frontier -> practically adding nodes from solved part, always checking if they are still on the frontier
			AtomicInteger fromIndex = newM.frontier.get(eP.getFrom());
			if(fromIndex != null)
				if(fromIndex.decrementAndGet() == 0)
					newM.frontier.remove(eP.getFrom());
				else
					newM.frontier.put(eP.getFrom(), fromIndex);
			else
				newM.frontier.put(eP.getFrom(), new AtomicInteger(patternLink.getInEdges(eP.getFrom()).size()
						+ patternLink.getOutEdges(eP.getFrom()).size() - 1));
			AtomicInteger toIndex = newM.frontier.get(eP.getTo());
			if(toIndex != null)
				if(toIndex.decrementAndGet() == 0)
					newM.frontier.remove(eP.getTo());
				else
					newM.frontier.put(eP.getTo(), toIndex);
			else
				newM.frontier.put(eP.getTo(), new AtomicInteger(patternLink.getInEdges(eP.getTo()).size()
						+ patternLink.getOutEdges(eP.getTo()).size() - 1));
			monitor.incrementNodeReferenceOperation(4); // 2 * (get + set)
		}
		// 'u' stands for reunion and 'n' for intersection
		// merge candidates: MC = (MC n MC2) u (MC1 n MO2) u (MC2 n MO1)
		// common merge candidates, and candidates of each match that were outer candidates for the other match
		newM.mergeCandidates = new HashSet<Match>(mergeCandidates);
		Set<Match> partB = new HashSet<Match>(mergeCandidates);
		Set<Match> partC = new HashSet<Match>(m1.mergeCandidates);
		newM.mergeCandidates.retainAll(m1.mergeCandidates);
		partB.retainAll(m1.mergeOuterCandidates);
		partC.retainAll(mergeOuterCandidates);
		newM.mergeCandidates.addAll(partB);
		newM.mergeCandidates.addAll(partC);
		
		// merge outer candidates: common outer candidates: MO = MO1 n MO2
		newM.mergeOuterCandidates = new HashSet<Match>(mergeOuterCandidates);
		newM.mergeOuterCandidates.retainAll(m1.mergeOuterCandidates);
		
		// check validity
		for(Iterator<Match> mi = mergeCandidates.iterator(); mi.hasNext();)
			if(!mi.next().isValid())
				mi.remove();
		for(Iterator<Match> mi = mergeOuterCandidates.iterator(); mi.hasNext();)
			if(!mi.next().isValid())
				mi.remove();
		
		monitor.incrementEdgeReferenceOperation(mergeCandidates.size() + m1.mergeCandidates.size()
				+ mergeOuterCandidates.size() + m1.mergeOuterCandidates.size());
		
		return newM;
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
		String ret = "M" + (valid ? "" : "[INVALID]") + "[" + id + "]K=" + k
				+ new TextGraphRepresentation(solvedPart).setLayout("", "", -1).update() + "|"
				+ new TextGraphRepresentation(matchedGraph).setLayout("", "", -1).update();
		return ret;
	}
	
	/**
	 * Provides a complete one-line representation of the match. Not to be confused with {@link #toStringLong()}.
	 * 
	 * @return the string representation.
	 */
	public String toStringExtended()
	{
		String ret = "match [" + id + "] (k=" + k + "): \t";
		ret += new TextGraphRepresentation(matchedGraph).setLayout("", " ", 2).update() + "\t : \t";
		ret += new TextGraphRepresentation(solvedPart).setLayout("", " ", 2).update() + "\t";
		ret += "Gx: " + new TextGraphRepresentation(unsolvedPart).setLayout("", " ", 2).update() + "\t";
		ret += "frontier: " + frontier + "; ";
		ret += "mCs: [";
		for(Match mi : mergeCandidates)
			if(mi.isValid())
				ret += mi.id + ", ";
		ret += "] mOCs: [";
		for(Match moi : mergeOuterCandidates)
			if(moi.isValid())
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
	
	/**
	 * Creates a graphical representation of the match and displays it using the specified {@link GCanvas}.
	 * 
	 * @param canvas
	 *            - the canvas to use for the representation.
	 * @param topleft
	 *            - the top left point for the representation, on the canvas.
	 * @param bottomright
	 *            - the bottom right point for the representation, on the canvas.
	 * @return - the created representation.
	 */
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
