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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.pattern.EdgeP;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.pattern.NodeP;
import net.xqhs.graphs.representation.VisualizableGraphComponent;
import net.xqhs.graphs.util.Debug.D_G;

/**
 * An algorithm that finds partial matches between a graph pattern GP (or G^P) and a graph (G).
 * <p>
 * Most of the methods in this implementation are <code>public static</code>, so as to be used easily in other classes.
 * <p>
 * In order to evaluate the performance and to visualize the process, a {@link MonitorPack} instance is used throughout
 * the code.
 * <p>
 * All matches generated in the matching process are retained throughout the life of the instance. They can be cleared
 * with a call to {@link #clearData()}. If a call to {@link #clearData()} is not issued, queries for matches that have
 * already been found in the past will complete quickly, as it will only take a pass through the list of generated
 * matches. However, memory will remain occupied until {@link #clearData()} is issued (or the instance is
 * garbage-collected).
 * <p>
 * The algorithm has been published in: Andrei Olaru, Context Matching for Ambient Intelligence Applications,
 * Proceedings of SYNASC 2013, 15th International Symposium on Symbolic and Numeric Algorithms for Scientific Computing,
 * September 23-26, 2013 Timisoara, Romania, IEEE CPS, 2013.
 * 
 * @author Andrei Olaru
 */
public class GraphMatcherQuick implements GraphMatchingProcess
{
	/**
	 * {@link Match} comparator.
	 * <p>
	 * The matches are sorted according to:
	 * <ul>
	 * <li>if the match is single-edge and the <code>distances</code> to the start vertex are given (otherwise order by
	 * id), use the distance (of the closest adjacent vertex) to the start vertex. If it's the same distance, order by
	 * the match's id.
	 * <li>for matches with more than one edge, order by <code>k</code> (smaller k first). If equal, order by id.
	 * </ul>
	 */
	protected static class MatchComparator implements Comparator<Match>
	{
		/**
		 * Vector of distances between every node and the start vertex.
		 */
		private Map<Node, Integer>	distances	= null;
		
		/**
		 * Link to the object measuring performance of the algorithm in terms of number of compared edges.
		 */
		private MonitorPack			monitorLink	= null;
		
		/**
		 * Constructor used internally by a {@link Match}. Does not use vertex distances from root vertex.
		 */
		protected MatchComparator()
		{
		}
		
		/**
		 * Creates a match comparator that uses distances of vertices with respect to the root vertex.
		 * 
		 * @param vertexDistances
		 *            - {@link Map} of distances between nodes and the start vertex.
		 * @param monitor
		 *            - the object measuring performance in terms of edge matches.
		 */
		protected MatchComparator(Map<Node, Integer> vertexDistances, MonitorPack monitor)
		{
			distances = vertexDistances;
			monitorLink = monitor;
		}
		
		@Override
		public int compare(Match m1, Match m2)
		{
			// single-edge matches (in case distances is defined)
			if((m1.solvedPart.m() == 1) && (m2.solvedPart.m() == 1) && (distances != null))
			{
				Edge e1 = m1.solvedPart.getEdges().iterator().next();
				Edge e2 = m2.solvedPart.getEdges().iterator().next();
				int result = Math.min(distances.get(e1.getFrom()).intValue(), distances.get(e1.getTo()).intValue())
						- Math.min(distances.get(e2.getFrom()).intValue(), distances.get(e2.getTo()).intValue());
				// dbg(D_G.D_MATCHING_INITIAL, "compare [] [] : [] (for [] vs [])", result, e1, e2, m1.id, m2.id);
				monitorLink.incrementEdgeReferenceOperation(2);
				if(result != 0)
					return result;
			}
			if(m1.k != m2.k)
				return m1.k - m2.k;
			// dbg(D_G.D_MATCHING_INITIAL, "re-compare []", m1.id.compareTo(m2.id));
			if(m1.id.equals(m2.id))
				return m1.hashCode() - m2.hashCode();
			return m1.id.compareTo(m2.id);
		}
	}
	
	/**
	 * The graph to match the pattern to (G).
	 */
	protected Graph					graph;
	/**
	 * The pattern to match to the graph (GP).
	 */
	protected GraphPattern			pattern;
	
	/**
	 * The {@link MonitorPack} instance to use for performance information and visualization.
	 */
	protected MonitorPack			monitor;
	
	/**
	 * The {@link PriorityQueue} of matches that still have merge candidates. It is kept until a {@link #clearData()} is
	 * issued.
	 */
	protected PriorityQueue<Match>	matchQueue		= null;
	/**
	 * A list of all generated matches. It is kept until a {@link #clearData()} is issued.
	 */
	protected List<Match>			allMatches		= null;
	/**
	 * An {@link Iterator} over {@link #allMatches} that keeps is used to remember the already-returned matches. It is
	 * reset with {@link #resetIterator()} or when the list of matches has been completely itterated over and no
	 * satisfactory match has been found.
	 */
	protected Iterator<Match>		matchIterator	= null;
	/**
	 * Is <code>true</code> if the iterator has just been reset. Becomes <code>false</code> with the first call to
	 * {@link #getNextMatch()}.
	 */
	protected boolean				initialState	= true;
	
	/**
	 * The current <i>k</i> threshold. Matches returned by {@link #getNextMatch()} have a <i>k</i> lower than or equal
	 * to this threshold.
	 */
	protected int					kThreshold		= 0;
	
	/**
	 * Initializes a matcher. Does not do any matching.
	 * 
	 * @param graph
	 *            : the graph (G).
	 * @param pattern
	 *            : the pattern (GP).
	 */
	protected GraphMatcherQuick(Graph graph, GraphPattern pattern)
	{
		super();
		this.graph = graph;
		this.pattern = pattern;
	}
	
	/**
	 * Calling this method with a different {@link MonitorPack} instance than previously set does not result in keeping
	 * any information from one monitor to the other, and aggregation of indicators and output will have to be done
	 * manually.
	 * 
	 * @param monitoring
	 *            - the {@link MonitorPack} to use.
	 * 
	 * @return the matcher itself.
	 */
	public GraphMatcherQuick setMonitor(MonitorPack monitoring)
	{
		monitor = monitoring;
		return this;
	}
	
	/**
	 * Initializes the matching progress, by creating the match comparator and adding the initial matches to the match
	 * queue.
	 * 
	 * @return the instance itself.
	 */
	@SuppressWarnings("unchecked")
	public GraphMatcherQuick initializeMatching()
	{
		matchQueue = initializeMatchQueue(graph, pattern, monitor);
		addInitialMatches(graph, pattern, matchQueue, monitor, (Comparator<Match>) matchQueue.comparator());
		allMatches = new ArrayList<Match>(matchQueue);
		initialState = true;
		return this;
	}
	
	@Override
	public GraphMatcherQuick resetIterator()
	{
		initialState = true;
		return this;
	}
	
	@Override
	public GraphMatcherQuick resetIterator(int k)
	{
		kThreshold = k;
		return resetIterator();
	}
	
	/**
	 * Clears the match queue and the list of all matches.
	 */
	@Override
	public void clearData()
	{
		matchQueue.clear();
		allMatches.clear();
		matchQueue = null;
		allMatches = null;
	}
	
	/**
	 * Searches for the next match with a <i>k</i> lower than or equal to the current threshold. The match returned is
	 * the first match that was not previously returned after the last call to {@link #resetIterator()}.
	 * <p>
	 * It the matching process has not been already initialized, first all initial matches will be created.
	 * <p>
	 * If a satisfactory match has already been generated (and no call to {@link #clearData()} has been issued in the
	 * mean time), that match will be found in the list of existing matches and returned.
	 * <p>
	 * Otherwise, the matching process will continue (potentially based on existing information from previous calls)
	 * until a satisfactory match will be found or the matching process completes.
	 * <p>
	 * In the latter case, <code>null</code> is returned.
	 */
	@Override
	public Match getNextMatch()
	{
		monitor.dbg(D_G.D_MATCHING_PROGRESS, "iterating;queue;all: === ", (matchIterator != null) ? "Y" : "N",
				(matchQueue != null) ? new Integer(matchQueue.size()) : "-", (allMatches != null) ? new Integer(
						allMatches.size()) : "-");
		
		if((matchQueue == null) || (allMatches == null))
			initializeMatching();
		if(initialState)
			// reinitialize iteration over the match collection
			matchIterator = allMatches.iterator();
		initialState = false;
		if(matchIterator != null)
			// existing matches not completely checked; check for elements to return
			while(matchIterator.hasNext())
			{
				Match m = matchIterator.next();
				if(m.k <= kThreshold)
					return m;
			}
		monitor.dbg(D_G.D_MATCHING_PROGRESS, "iterating;queue;all: =:= ", (matchIterator != null) ? "Y" : "N",
				(matchQueue != null) ? new Integer(matchQueue.size()) : "-", (allMatches != null) ? new Integer(
						allMatches.size()) : "-");
		matchIterator = null;
		// existing matches exhausted from the beginning, or exhausted in the preceding while cycle
		List<Match> result = growMatches(kThreshold, true, matchQueue, allMatches, monitor);
		monitor.dbg(D_G.D_MATCHING_PROGRESS, "iterating;queue;all: ==/ ", (matchIterator != null) ? "Y" : "N",
				(matchQueue != null) ? new Integer(matchQueue.size()) : "-", (allMatches != null) ? new Integer(
						allMatches.size()) : "-");
		if(result.size() > 0)
			return result.get(0);
		return null;
	}
	
	/**
	 * As with {@link #getNextMatch()}, satisfactory matches are searched in the list of existing matches. Next, the
	 * matching process is completed and any newly found matches are added to the result.
	 */
	@Override
	public List<Match> getAllMatches(int k)
	{
		if((matchQueue == null) || (allMatches == null))
			initializeMatching();
		resetIterator(k);
		matchIterator = allMatches.iterator();
		List<Match> result = new ArrayList<Match>();
		// check existing matches first
		while(matchIterator.hasNext())
		{
			Match m = matchIterator.next();
			if(m.k <= kThreshold)
				result.add(m);
		}
		matchIterator = null;
		// then check for any new ones
		result.addAll(growMatches(k, false, matchQueue, allMatches, monitor));
		return result;
	}
	
	@Override
	public List<Match> getAllCompleteMatches()
	{
		return getAllMatches(0);
	}
	
	/**
	 * The method initializes the match queue by creating an appropriate comparator (based on distances of edges to a
	 * start vertex).
	 * 
	 * @param graph
	 *            - the graph.
	 * @param pattern
	 *            - the pattern.
	 * @param monitor
	 *            - the monitoring instance.
	 * @return an empty {@link PriorityQueue} with the appropriate comparator.
	 */
	public static PriorityQueue<Match> initializeMatchQueue(Graph graph, GraphPattern pattern, MonitorPack monitor)
	{
		Map<Node, Integer> distances = computeVertexDistances(pattern, monitor);
		
		Comparator<Match> matchComparator = new MatchComparator(distances, monitor);
		
		return new PriorityQueue<Match>(1, matchComparator);
	}
	
	/**
	 * Decides which is the "start vertex" in the pattern (maximum value of in-degree minus out-degree).
	 * <p>
	 * Then, computes the distances of each vertex in the pattern from the start vertex.
	 * 
	 * @param pattern
	 *            - the pattern.
	 * @param monitor
	 *            - the monitoring instance.
	 * @return the distance map.
	 */
	public static Map<Node, Integer> computeVertexDistances(final GraphPattern pattern, final MonitorPack monitor)
	{
		/**
		 * Vertices ordered by out-degree (minus in-degree) (first is greatest).
		 * 
		 * Other criteria assure that the sorting will always be the same.
		 */
		SortedSet<Node> vertexSet = new TreeSet<Node>(new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2)
			{
				monitor.incrementNodeReferenceOperation();
				int out1 = pattern.getOutEdges(n1).size() - pattern.getInEdges(n1).size();
				int out2 = pattern.getOutEdges(n2).size() - pattern.getInEdges(n2).size();
				if(out1 != out2)
					return -(out1 - out2);
				if(n1 instanceof NodeP && n2 instanceof NodeP)
				{
					monitor.incrementNodeReferenceOperation();
					NodeP n1P = (NodeP) n1, n2P = (NodeP) n2;
					if(n1P.isGeneric() && n2P.isGeneric())
					{
						if(n1P.genericIndex() == n2P.genericIndex())
							return n1P.hashCode() - n2P.hashCode();
						return n1P.genericIndex() - n2P.genericIndex();
					}
					if(n1P.isGeneric())
						return -1;
					if(n2P.isGeneric())
						return 1;
				}
				monitor.incrementNodeLabelComparison();
				if(n1.getLabel().equals(n2.getLabel()))
					return n1.hashCode() - n2.hashCode();
				return n1.getLabel().compareTo(n2.getLabel());
			}
		});
		vertexSet.addAll(pattern.getNodes());
		monitor.dbg(D_G.D_MATCHING_INITIAL, "sorted vertex set: []", vertexSet);
		/**
		 * The start vertex.
		 */
		Node vMP = vertexSet.first();
		monitor.lf("start vertex: ", vMP);
		if(monitor.getVisual() != null && vMP instanceof VisualizableGraphComponent)
			monitor.getVisual().feedLine(pattern, (VisualizableGraphComponent) vMP, "start vertex");
		
		/*
		 * Distances of vertices relative to the start vertex. Used in sorting single-edge matches in the match queue.
		 */
		final Map<Node, Integer> distances = pattern.computeDistancesFromUndirected(vMP);
		monitor.dbg(D_G.D_MATCHING_INITIAL, "vertex distances: []", distances);
		
		return distances;
	}
	
	/**
	 * Add initial (i.e. all single-edge) matches to the match queue.
	 * 
	 * @param graph
	 *            - the graph.
	 * @param pattern
	 *            - the pattern
	 * @param matchQueue
	 *            - the empty match queue.
	 * @param monitor
	 *            - the monitoring instance.
	 * @param comparator
	 *            - used for debugging. It can be the comparator used by the MatchQueue. Can be <code>null</code>.
	 */
	public static void addInitialMatches(Graph graph, GraphPattern pattern, PriorityQueue<Match> matchQueue,
			final MonitorPack monitor, Comparator<Match> comparator)
	{
		Comparator<Edge> edgeComparator = new Comparator<Edge>() {
			@Override
			public int compare(Edge e1, Edge e2)
			{
				if(e1.getLabel() == null)
					return -1;
				if(e2.getLabel() == null)
					return 1;
				monitor.incrementEdgeLabelComparison();
				if(!e1.getLabel().equals(e2.getLabel()))
					return e1.getLabel().compareTo(e2.getLabel());
				return e1.hashCode() - e2.hashCode();
			}
		};
		
		/**
		 * Ordered pattern edges, according to label.
		 */
		SortedSet<Edge> sortedEdges = new TreeSet<Edge>(edgeComparator);
		sortedEdges.addAll(pattern.getEdges());
		
		/**
		 * Ordered graph edges, according to label.
		 */
		SortedSet<Edge> sortedGraphEdges = new TreeSet<Edge>(edgeComparator);
		sortedGraphEdges.addAll(graph.getEdges());
		
		// for each edge in the pattern, create an id and build a match.
		int edgeId = 0;
		for(Edge eP : sortedEdges)
		{
			if(!((eP instanceof EdgeP) && ((EdgeP) eP).isGeneric()))
			{
				int matchId = 0;
				monitor.lf("edge [] has id []", eP, new Integer(edgeId));
				for(Edge e : sortedGraphEdges)
				{
					monitor.dbg(D_G.D_MATCHING_INITIAL, "trying edges: [] : []", eP, e);
					if(isMatch(eP, e, monitor))
					{
						Match m = new Match(graph, pattern, e, eP, edgeId + ":" + matchId);
						monitor.incrementMatchCount();
						addInitialMatchToQueue(m, matchQueue, monitor);
						monitor.lf("new initial match: [] [] : []", m.id, m.solvedPart.getEdges().iterator().next(),
								m.matchedGraph.getEdges().iterator().next());
						
						if(D_G.D_MATCHING_INITIAL.toBool())
						{
							monitor.dbg(D_G.D_MATCHING_INITIAL, "=======");
							String dbg_match = "=============== match queue ===============================> ";
							Match[] dbg_sorted = matchQueue.toArray(new Match[1]);
							if(comparator != null)
								Arrays.sort(dbg_sorted, comparator);
							for(Match mdbg : dbg_sorted)
								dbg_match += mdbg.id + ", ";
							monitor.dbg(D_G.D_MATCHING_INITIAL, dbg_match);
						}
						
						matchId++;
					}
				}
				edgeId++;
			}
		}
		
		String string = "[\n ";
		if(!matchQueue.isEmpty())
		{
			Match[] sorted = matchQueue.toArray(new Match[1]);
			if(comparator != null)
				Arrays.sort(sorted, comparator);
			if(monitor.getVisual() != null)
				monitor.getVisual().feedLine("initial matches: " + matchQueue.size());
			for(Match m : sorted)
			{
				string += m.toString() + ", \n";
				if(monitor.getVisual() != null)
					monitor.getVisual().feedLine(m, "initial match");
			}
		}
		string += "]";
		monitor.lf("initial matches []: []-------------------------", new Integer(matchQueue.size()), string);
	}
	
	/**
	 * Adds a single-edge match to the matching queue and add matches from the queue to its merge candidate list (as
	 * well as adding the match to other matches' merge candidates)
	 * 
	 * @param m
	 *            - the match to add to the queue.
	 * @param queue
	 *            - the match queue.
	 * @param monitor
	 *            - the monitoring instance.
	 */
	// used to be static, but needs performance evaluation
	protected static void addInitialMatchToQueue(Match m, PriorityQueue<Match> queue, MonitorPack monitor)
	{
		// take all matches already in the queue and see if they are compatible
		for(Match mi : queue)
		{
			boolean accept = false;
			boolean reject = false;
			// reject if: the two matches intersect (contain common pattern edges)
			monitor.incrementEdgeReferenceOperation(2); // single-edge matches
			// FIXME optimize the operation?
			if(new HashSet<Edge>(m.solvedPart.getEdges()).removeAll(mi.solvedPart.getEdges())
					|| new HashSet<Edge>(m.matchedGraph.getEdges()).removeAll(mi.matchedGraph.getEdges()))
				reject = true;
			else
				// build merge candidates
				// iterate on the frontier of the potential candidate
				// TODO: it should iterate on the frontier of the candidate with a shorter frontier
				for(Map.Entry<Node, AtomicInteger> frontierV : mi.frontier.entrySet())
				{
					// accept if: the two matches contain the same node and the node corresponds, in both matches, to
					// the same node in G
					// reject if: the two matches contain the same node and the node corresponds, in the two matches, to
					// different nodes in G
					if(m.frontier.containsKey(frontierV.getKey()))
					{
						if(!accept && m.nodeFunction.get(frontierV.getKey()) == mi.nodeFunction.get(frontierV.getKey()))
							accept = true;
						if(m.nodeFunction.get(frontierV.getKey()) != mi.nodeFunction.get(frontierV.getKey()))
						{
							reject = true;
							break;
						}
						monitor.incrementNodeReferenceOperation(2);
					}
				}
			if(!reject)
			{
				if(accept)
				{ // then each match is a merge candidate for the other
					m.mergeCandidates.add(mi);
					mi.mergeCandidates.add(m);
				}
				else
				{
					m.mergeOuterCandidates.add(mi);
					mi.mergeOuterCandidates.add(m);
				}
			}
		}
		// add the match to the queue
		queue.add(m);
	}
	
	/**
	 * Test the match between two edges: matching from and to nodes, matching label.
	 * 
	 * @param eP
	 *            - the edge in the pattern (eP in EP).
	 * @param e
	 *            - the edge in the graph (eP in E).
	 * @param monitor
	 *            - the monitoring instance.
	 * @return <code>true</code> if the edges match.
	 */
	protected static boolean isMatch(Edge eP, Edge e, MonitorPack monitor)
	{
		monitor.incrementEdgeReferenceOperation();
		
		Node fromP = eP.getFrom();
		Node toP = eP.getTo();
		boolean fromGeneric = (fromP instanceof NodeP) && ((NodeP) fromP).isGeneric();
		boolean toGeneric = (toP instanceof NodeP) && ((NodeP) toP).isGeneric();
		
		monitor.incrementNodeLabelComparison();
		// reject if: the from node of eP is not generic and does not have the same label as the from node of e
		if(!fromGeneric && !fromP.getLabel().equals(e.getFrom().getLabel()))
			return false;
		// reject if: the to node of eP is not generic and does not have the same label as the to node of e
		monitor.incrementNodeLabelComparison();
		if(!toGeneric && !toP.getLabel().equals(e.getTo().getLabel()))
			return false;
		
		// accept if: eP is not labeled
		// accept if: e is not labeled (or has a void label)
		// accept if: eP has the same label as e
		if(eP.getLabel() == null)
			return true;
		if((e.getLabel() == null) || (e.getLabel().equals("")))
			return true;
		monitor.incrementEdgeLabelComparison();
		if((e.getLabel() != null) && eP.getLabel().equals(e.getLabel()))
			return true;
		// reject otherwise (e and eP are labeled and labels don't match)
		return false;
	}
	
	/**
	 * Grows incrementally the list of matches, by merging existing matches from the match queue with their merge
	 * candidates. In case <code>stopAtFirstMatch</code> is <code>true</code>, the process is interrupted when the first
	 * satisfactory match is created, and the match is returned.
	 * 
	 * @param kThreshold
	 *            - the threshold for matches: satisfactory matches have a <i>k</i> lower than or equal to this number.
	 * @param stopAtFirstMatch
	 *            - if <code>true</code>, the method returns after the first satisfactory match is found.
	 * @param matchQueue
	 *            - the queue containing existing matches that still have merge candidates.
	 * @param allMatches
	 *            - the list of all existing matches, that will be updated with all newly generated matches.
	 * @param monitor
	 *            - the monitoring instance.
	 * @return the list of satisfactory matches. If <code>stopAtFirstMatch</code> is <code>true</code>, the returned
	 *         list will have at most one element.
	 */
	public static List<Match> growMatches(int kThreshold, boolean stopAtFirstMatch, PriorityQueue<Match> matchQueue,
			List<Match> allMatches, MonitorPack monitor)
	{
		List<Match> result = new ArrayList<Match>();
		/**
		 * The main process is the merging (growing) of matches.
		 * <p>
		 * Note that after each cycle, the chosen match will not exist any more - it is removed from the queue and from
		 * all the candidate's candidates list - it will either grow (by merging with various candidates) or disappear
		 * completely.
		 */
		while(!matchQueue.isEmpty())
		{
			Match m = matchQueue.poll(); // matches are sorted according to the criteria above
			for(Iterator<Match> itm = m.mergeCandidates.iterator(); itm.hasNext();)
			{
				// remove the candidate from the list, and the current match from the candidate's list
				Match mc = itm.next();
				itm.remove();
				mc.mergeCandidates.remove(m);
				monitor.lf("merging \t []\n \t\t\t and \t\t []", m, mc);
				Match mr = merge(m, mc, monitor);
				if(mr != null) // merge should never fail
				{
					monitor.lf("new match:\t []\n", mr);
					if(monitor.getVisual() != null)
						monitor.getVisual().feedLine(m, mc, mr, "new match [k=" + mr.k + "]");
					matchQueue.add(mr);
					allMatches.add(mr);
					monitor.incrementMergeCount();
					monitor.incrementMatchCount();
					if(mr.k <= kThreshold)
					{
						result.add(mr);
						if(stopAtFirstMatch)
							return result;
					}
				}
				else
				{
					monitor.le("merge failed\n");
					if(monitor.getVisual() != null)
						monitor.getVisual().feedLine("merge failed");
				}
			}
		}
		return result;
	}
	
	/**
	 * Merges two matches into one.
	 * <p>
	 * Matches are expected to be disjoint, both as GmP and as G' (in terms of edges); also, all common nodes in GmP
	 * must correspond to the same nodes in G' (this check should be done in <code>addMatcheToQueue</code>.
	 * <p>
	 * <b>Attention:</b> matches are expected to be merge-able without checks.
	 * 
	 * @param m1
	 *            - the first match.
	 * @param m2
	 *            - the second match.
	 * @param monitor
	 *            - the monitoring instance.
	 * @return the merged match.
	 */
	protected static Match merge(Match m1, Match m2, MonitorPack monitor)
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
		
		GraphPattern pt = m1.patternLink;
		// G and GP links -> set in constructor
		Match newM = new Match(m1.targetGraphLink, pt);
		
		newM.unsolvedPart = new GraphPattern();
		newM.unsolvedPart.getEdges().addAll(pt.getEdges());
		newM.unsolvedPart.getNodes().addAll(pt.getNodes());
		newM.k = newM.unsolvedPart.getEdges().size();
		
		Set<Edge> totalMatch = new HashSet<Edge>(); // there should be no duplicates as the solved parts should be
													// disjoint.
		totalMatch.addAll(m1.solvedPart.getEdges());
		totalMatch.addAll(m2.solvedPart.getEdges());
		
		newM.solvedPart = new GraphPattern();
		newM.nodeFunction = new HashMap<Node, Node>();
		newM.edgeFunction = new HashMap<Edge, List<Edge>>();
		newM.matchedGraph = new SimpleGraph();
		newM.frontier = new HashMap<Node, AtomicInteger>();
		for(Edge eP : totalMatch)
		{
			monitor.incrementEdgeReferenceOperation();
			// GmP -> obtained by adding edges from m1.GmP and m2.GmP and their adjacent vertices
			newM.solvedPart.addEdge(eP).addNode(eP.getFrom()).addNode(eP.getTo());
			// GxP -> obtained by removing edges added to GmP and nodes
			newM.unsolvedPart.removeEdge(eP).removeNode(eP.getFrom()).removeNode(eP.getTo());
			// k -> obtained by decrementing when adding edges
			newM.k--;
			
			Match sourceMatch = null; // which match does eP come from
			if(m1.solvedPart.contains(eP))
				sourceMatch = m1;
			if(m2.solvedPart.contains(eP))
			{
				if(sourceMatch != null)
				{
					monitor.le("match-intersection pattern edge found: []", eP);
					throw new IllegalArgumentException("match-intersection edge");
				}
				sourceMatch = m2;
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
				newM.matchedGraph.addEdge(em).addNode(em.getFrom()).addNode(em.getTo());
			
			// frontier -> practically adding nodes from solved part, always checking if they are still on the frontier
			AtomicInteger fromIndex = newM.frontier.get(eP.getFrom());
			if(fromIndex != null)
				if(fromIndex.decrementAndGet() == 0)
					newM.frontier.remove(eP.getFrom());
				else
					newM.frontier.put(eP.getFrom(), fromIndex);
			else
				newM.frontier
						.put(eP.getFrom(),
								new AtomicInteger(pt.getInEdges(eP.getFrom()).size()
										+ pt.getOutEdges(eP.getFrom()).size() - 1));
			AtomicInteger toIndex = newM.frontier.get(eP.getTo());
			if(toIndex != null)
				if(toIndex.decrementAndGet() == 0)
					newM.frontier.remove(eP.getTo());
				else
					newM.frontier.put(eP.getTo(), toIndex);
			else
				newM.frontier.put(eP.getTo(),
						new AtomicInteger(pt.getInEdges(eP.getTo()).size() + pt.getOutEdges(eP.getTo()).size() - 1));
		}
		// merge candidates: MC = (MC n MC2) u (MC1 n MO2) u (MC2 n MO1)
		// common merge candidates, and candidates of each match that were outer candidates for the other match
		newM.mergeCandidates = new HashSet<Match>(m1.mergeCandidates);
		Set<Match> partB = new HashSet<Match>(m1.mergeCandidates);
		Set<Match> partC = new HashSet<Match>(m2.mergeCandidates);
		newM.mergeCandidates.retainAll(m2.mergeCandidates);
		partB.retainAll(m2.mergeOuterCandidates);
		partC.retainAll(m1.mergeOuterCandidates);
		newM.mergeCandidates.addAll(partB);
		newM.mergeCandidates.addAll(partC);
		
		// merge outer candidates: common outer candidates: MO = MO1 n MO2
		newM.mergeOuterCandidates = new HashSet<Match>(m1.mergeOuterCandidates);
		newM.mergeOuterCandidates.retainAll(m2.mergeOuterCandidates);
		
		monitor.incrementEdgeReferenceOperation(m1.mergeCandidates.size() + m2.mergeCandidates.size()
				+ m1.mergeOuterCandidates.size() + m2.mergeOuterCandidates.size());
		
		return newM;
	}
	
	/**
	 * Returns a newly created {@link GraphMatcherQuick} instance for the specified graph and pattern.
	 * 
	 * @param graph
	 *            - the graph.
	 * @param pattern
	 *            - the pattern.
	 * @param monitoring
	 *            - the monitoring instance. It must not be <code>null</code> but it can be a newly created instance
	 *            with no configuration.
	 * @return the {@link GraphMatcherQuick} instance.
	 * 
	 * @throws IllegalArgumentException
	 *             if the <code>monitoring</code> argument is <code>null</code>.
	 */
	public static GraphMatcherQuick getMatcher(Graph graph, GraphPattern pattern, MonitorPack monitoring)
	{
		if(monitoring == null)
			throw new IllegalArgumentException();
		if(monitoring.getVisual() != null)
		{
			monitoring.getVisual().feedLine(graph, null, "the graph");
			monitoring.getVisual().feedLine(pattern, null, "the pattern");
		}
		return new GraphMatcherQuick(graph, pattern).setMonitor(monitoring);
	}
}
