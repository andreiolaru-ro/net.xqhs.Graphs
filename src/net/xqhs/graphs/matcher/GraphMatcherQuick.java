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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import net.xqhs.graphs.graph.ConnectedNode;
import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.pattern.EdgeP;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.pattern.NodeP;
import net.xqhs.graphs.representation.VisualizableGraphComponent;
import net.xqhs.graphs.util.Debug.D_G;
import net.xqhs.util.logging.Unit;

/**
 * An algorithm that finds partial matches between a graph pattern GP (or G^P) and a graph (G). It implements the
 * {@link GraphMatcher} interface.
 * 
 * @author Andrei Olaru
 */
public class GraphMatcherQuick extends Unit implements GraphMatcher
{
	/**
	 * Match comparator
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
		private Map<Node, Integer>	distances				= null;
		
		private AtomicInteger		performanceEdgesLink	= null;
		
		/**
		 * Constructor used internally by a {@link Match}. Does not use vertex distances from root vertex.
		 */
		protected MatchComparator()
		{
		}
		
		/**
		 * Creates a match comparator that uses distances of vertices with respect to the root vertex.
		 * 
		 * @param distances
		 */
		protected MatchComparator(Map<Node, Integer> distances, AtomicInteger performanceEdges)
		{
			this.distances = distances;
			this.performanceEdgesLink = performanceEdges;
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
				// dbg(D_G.D_MATCHING_INITIAL, "compare (" + result + ") " + e1 + " : " + e2 + " (for " + m1.id + " vs "
				// + m2.id + ")");
				performanceEdgesLink.incrementAndGet();
				if(result != 0)
					return result;
			}
			if(m1.k != m2.k)
				return m1.k - m2.k;
			// dbg(D_G.D_MATCHING_INITIAL, "re-compare (" + m1.id.compareTo(m2.id) + ")");
			return m1.id.compareTo(m2.id);
		}
	}
	
	/**
	 * The graph to match the pattern to (G).
	 */
	Graph				graph;
	/**
	 * The pattern to match to the graph (GP).
	 */
	GraphPattern		pattern;
	
	/**
	 * Matching visualizer to view the matching process.
	 */
	MatchingVisualizer	visual	= null;
	
	AtomicInteger		performanceNodes;
	AtomicInteger		performanceEdges;
	
	/**
	 * Initializes a matcher. Does not do the matching.
	 * 
	 * @param graph
	 *            : the graph (G).
	 * @param pattern
	 *            : the pattern (GP).
	 */
	public GraphMatcherQuick(SimpleGraph graph, GraphPattern pattern)
	{
		super();
		this.graph = graph;
		this.pattern = pattern;
	}
	
	@Override
	protected String getDefaultUnitName()
	{
		if(graph != null && graph instanceof SimpleGraph && pattern != null)
			return ((SimpleGraph) graph).getUnitName() + ":" + pattern.getUnitName();
		return super.getDefaultUnitName();
	}
	
	/**
	 * @param viz
	 *            - the visualizer to use
	 * @return the matcher itself
	 */
	public GraphMatcherQuick setVisual(MatchingVisualizer viz)
	{
		visual = viz;
		return this;
	}
	
	/**
	 * Performs the matching process.
	 * 
	 * @return number of matches found // TODO: return the maximal match(es).
	 */
	public int doMatching()
	{
		if(visual != null)
		{
			visual.feedLine(graph, null, "the graph");
			visual.feedLine(pattern, null, "the pattern");
		}
		performanceNodes = new AtomicInteger();
		performanceEdges = new AtomicInteger();
		int mergeCount = 0;
		
		Map<Node, Integer> distances = computeVertexDistances();
		
		Comparator<Match> matchComparator = new MatchComparator(distances, performanceEdges);
		
		PriorityQueue<Match> matchQueue = new PriorityQueue<>(1, matchComparator);
		
		addInitialMatches(matchQueue, matchComparator);
		
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
				// remove the candidate from the list, and the match from the candidate's list
				Match mc = itm.next();
				itm.remove();
				mc.mergeCandidates.remove(m);
				lf("merging \t " + m + " \n \t\t\t and \t\t " + mc);
				Match mr = merge(m, mc);
				if(mr != null) // merge should never fail // FIXME
				{
					lf("new match:\t " + mr.toString() + "\n");
					if(visual != null)
						visual.feedLine(m, mc, mr, "new match [k=" + mr.k + "]");
					// matchQueue.remove(mc); // TODO: tentative.
					matchQueue.add(mr);
					mergeCount++;
				}
				else
				{
					lf("match failed\n");
					if(visual != null)
						visual.feedLine("match failed");
				}
				lf("merge count: " + mergeCount + ";\t nodes: " + performanceNodes + ";\t edges: " + performanceEdges
						+ "\n");
			}
		}
		
		return 0; // FIXME
	}
	
	/**
	 * Decides which is the "start vertex" in the pattern (maximum in-degree minus out-degree).
	 * <p>
	 * Then, computes the distances of each vertex in the pattern from the start vertex.
	 * 
	 * @return the distance map.
	 */
	protected Map<Node, Integer> computeVertexDistances()
	{
		/**
		 * Vertices ordered by out-degree (minus in-degree) (first is greatest).
		 * 
		 * Other criteria assure that the sorting will always be the same.
		 */
		SortedSet<Node> vertexSet = new TreeSet<>(new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2)
			{
				performanceNodes.incrementAndGet();
				if(n1 instanceof ConnectedNode && n2 instanceof ConnectedNode)
				{
					int out1 = ((ConnectedNode) n1).getOutEdges().size() - ((ConnectedNode) n1).getInEdges().size();
					int out2 = ((ConnectedNode) n2).getOutEdges().size() - ((ConnectedNode) n2).getInEdges().size();
					if(out1 != out2)
						return -(out1 - out2);
				}
				if(n1 instanceof NodeP && n2 instanceof NodeP)
				{
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
				if(n1.getLabel().compareTo(n2.getLabel()) == 0)
					return n1.hashCode() - n2.hashCode();
				return n1.getLabel().compareTo(n2.getLabel());
			}
		});
		vertexSet.addAll(pattern.getNodes());
		dbg(D_G.D_MATCHING_INITIAL, "sorted vertex set: " + vertexSet);
		/**
		 * The start vertex.
		 */
		Node vMP = vertexSet.first();
		lf("start vertex: " + vMP);
		if(visual != null && vMP instanceof VisualizableGraphComponent)
			visual.feedLine(pattern, (VisualizableGraphComponent) vMP, "start vertex");
		
		/*
		 * Distances of vertices relative to the start vertex. Used in sorting single-edge matches in the match queue.
		 */
		final Map<Node, Integer> distances = pattern.computeDistancesFromUndirected(vMP);
		dbg(D_G.D_MATCHING_INITIAL, "vertex distances: " + distances);
		
		return distances;
	}
	
	/**
	 * Add initial (i.e. all single-edge) matches to the match queue.
	 * 
	 * @param matchQueue
	 *            : the empty match queue.
	 * @param comparator
	 *            : used for debugging
	 */
	protected void addInitialMatches(PriorityQueue<Match> matchQueue, Comparator<Match> comparator)
	{
		Comparator<Edge> edgeComparator = new Comparator<Edge>() {
			@Override
			public int compare(Edge e1, Edge e2)
			{
				performanceEdges.incrementAndGet();
				if(e1.getLabel() == null && e2.getLabel() == null)
					return e1.toString().compareTo(e2.toString());
				if(e1.getLabel() == null)
					return -1;
				if(e2.getLabel() == null)
					return 1;
				if(e1.getLabel().compareTo(e2.getLabel()) == 0)
					return e1.toString().compareTo(e2.toString());
				return e1.getLabel().compareTo(e2.getLabel());
			}
		};
		
		/**
		 * Ordered pattern edges, according to label.
		 */
		SortedSet<Edge> sortedEdges = new TreeSet<>(edgeComparator);
		sortedEdges.addAll(pattern.getEdges());
		
		/**
		 * Ordered graph edges, according to label.
		 */
		SortedSet<Edge> sortedGraphEdges = new TreeSet<>(edgeComparator);
		sortedGraphEdges.addAll(graph.getEdges());
		
		// for each edge in the pattern, create an id and build a match.
		int edgeId = 0;
		for(Edge ePi : sortedEdges)
		{
			// TODO check cast
			EdgeP eP = (EdgeP) ePi;
			if(!eP.isGeneric())
			{
				int matchId = 0;
				lf("edge " + eP + " has id [" + edgeId + "]");
				for(Edge e : sortedGraphEdges)
				{
					dbg(D_G.D_MATCHING_INITIAL, "trying edges: " + eP + " : " + e);
					if(isMatch(eP, e))
					{
						Match m = new Match(graph, pattern, e, eP, edgeId + ":" + matchId);
						addInitialMatchToQueue(m, matchQueue);
						li("new initial match: (" + m.id + ") " + m.solvedPart.getEdges().iterator().next() + " : "
								+ m.matchedGraph.getEdges().iterator().next());
						
						if(D_G.D_MATCHING_INITIAL.toBool())
						{
							dbg(D_G.D_MATCHING_INITIAL, "=======");
							String dbg_match = "=============== match queue ===============================> ";
							Match[] dbg_sorted = matchQueue.toArray(new Match[1]);
							Arrays.sort(dbg_sorted, comparator);
							for(Match mdbg : dbg_sorted)
								dbg_match += mdbg.id + ", ";
							dbg(D_G.D_MATCHING_INITIAL, dbg_match);
						}
						
						matchId++;
					}
				}
				edgeId++;
			}
		}
		
		Match[] sorted = matchQueue.toArray(new Match[1]);
		Arrays.sort(sorted, comparator);
		String string = "[\n ";
		if(visual != null)
			visual.feedLine("initial matches: " + matchQueue.size());
		for(Match m : sorted)
		{
			string += m.toString() + ", \n";
			if(visual != null)
				visual.feedLine(m, "initial match");
		}
		string += "]";
		lf("initial matches (" + matchQueue.size() + "): " + string + "-------------------------");
	}
	
	/**
	 * Test the match between two edges: matching from and to nodes, matching label.
	 * 
	 * @param eP
	 *            : the edge in the pattern (eP in EP)
	 * @param e
	 *            : the edge in the graph (eP in E)
	 * @return <code>true</code> if the edges match.
	 */
	// used to be static, but needs performance evaluation
	protected boolean isMatch(EdgeP eP, Edge e)
	{
		performanceEdges.incrementAndGet();
		performanceNodes.addAndGet(2);
		// reject if: the from node of eP is not generic and does not have the same label as the from node of E
		if(!((NodeP) eP.getFrom()).isGeneric() && !eP.getFrom().getLabel().equals(e.getFrom().getLabel()))
			return false;
		// reject if: the to node of eP is not generic and does not have the same label as the to node of E
		if(!((NodeP) eP.getTo()).isGeneric() && !eP.getTo().getLabel().equals(e.getTo().getLabel()))
			return false;
		
		if(!eP.isGeneric())
		{
			// accept if: eP is not labeled
			// accept if: e is not labeled (or has a void label)
			// accept if: eP has the same label as e
			if(eP.getLabel() == null)
				return true;
			if((e.getLabel() == null) || (e.getLabel().equals("")))
				return true;
			if((e.getLabel() != null) && eP.getLabel().equals(e.getLabel()))
				return true;
			// reject otherwise (e and eP are labeled and labels don't match)
			return false;
		}
		return false; // TODO: support RegExp edges.
	}
	
	/**
	 * Adds a single-edge match to the matching queue and add matches from the queue to its merge candidate list (as
	 * well as adding the match to other matches' merge candidates)
	 * 
	 * @param m
	 *            : the match to add to the queue
	 * @param queue
	 *            : the match queue
	 */
	// used to be static, but needs performance evaluation
	protected void addInitialMatchToQueue(Match m, PriorityQueue<Match> queue)
	{
		// take all matches already in the queue and see if they are compatible
		for(Match mi : queue)
		{
			boolean accept = false;
			boolean reject = false;
			// reject if: the two matches intersect (contain common pattern edges
			performanceEdges.incrementAndGet();
			if(new HashSet<>(m.solvedPart.getEdges()).removeAll(mi.solvedPart.getEdges())
					|| new HashSet<>(m.matchedGraph.getEdges()).removeAll(mi.matchedGraph.getEdges()))
				reject = true;
			else
				// build merge candidates
				// iterate on the frontier of the potential candidate
				// TODO: it should iterate on the frontier of the candidate with a shorter frontier
				for(Map.Entry<NodeP, AtomicInteger> frontierV : mi.frontier.entrySet())
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
						performanceNodes.incrementAndGet();
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
	 * Merges two matches into one.
	 * <p>
	 * Matches are expected to be disjoint, both as GmP and as G' (in terms of edges); also, all common nodes in GmP
	 * must correspond to the same nodes in G' (this check should be done in <code>addMatcheToQueue</code>.
	 * <p>
	 * <b>Attention:</b> matches are expected to be merge-able without checks.
	 * 
	 * @param m1
	 *            : the first match.
	 * @param m2
	 *            : the second match.
	 * @return the merged match.
	 */
	protected Match merge(Match m1, Match m2)
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
		
		Set<Edge> totalMatch = new HashSet<>(); // there should be no duplicates as the solved parts should be disjoint.
		totalMatch.addAll(m1.solvedPart.getEdges());
		totalMatch.addAll(m2.solvedPart.getEdges());
		
		newM.solvedPart = new GraphPattern();
		newM.nodeFunction = new HashMap<>();
		newM.edgeFunction = new HashMap<>();
		newM.matchedGraph = new SimpleGraph();
		newM.frontier = new HashMap<>();
		for(Edge e : totalMatch)
		{
			performanceEdges.incrementAndGet();
			// TODO check cast
			EdgeP eP = (EdgeP) e;
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
					le("match-intersection pattern edge found: [" + eP + "]");
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
			AtomicInteger fromIndex = newM.frontier.get(e.getFrom());
			if(fromIndex != null)
				if(fromIndex.decrementAndGet() == 0)
					newM.frontier.remove(e.getFrom());
				else
					newM.frontier.put((NodeP) e.getFrom(), fromIndex);
			else
				newM.frontier.put((NodeP) eP.getFrom(), new AtomicInteger(((ConnectedNode) eP.getFrom()).getInEdges()
						.size() + ((ConnectedNode) eP.getFrom()).getOutEdges().size() - 1));
			AtomicInteger toIndex = newM.frontier.get(e.getTo());
			if(toIndex != null)
				if(toIndex.decrementAndGet() == 0)
					newM.frontier.remove(e.getTo());
				else
					newM.frontier.put((NodeP) e.getTo(), toIndex);
			else
				newM.frontier.put((NodeP) eP.getTo(), new AtomicInteger(((ConnectedNode) eP.getTo()).getInEdges()
						.size() + ((ConnectedNode) eP.getTo()).getOutEdges().size() - 1));
		}
		// merge candidates: MC = (MC n MC2) u (MC1 n MO2) u (MC2 n MO1)
		// common merge candidates, and candidates of each match that were outer candidates for the other match
		newM.mergeCandidates = new HashSet<>(m1.mergeCandidates);
		Set<Match> partB = new HashSet<>(m1.mergeCandidates);
		Set<Match> partC = new HashSet<>(m2.mergeCandidates);
		newM.mergeCandidates.retainAll(m2.mergeCandidates);
		partB.retainAll(m2.mergeOuterCandidates);
		partC.retainAll(m1.mergeOuterCandidates);
		newM.mergeCandidates.addAll(partB);
		newM.mergeCandidates.addAll(partC);
		
		// merge outer candidates: common outer candidates: MO = MO1 n MO2
		newM.mergeOuterCandidates = new HashSet<>(m1.mergeOuterCandidates);
		newM.mergeOuterCandidates.retainAll(m2.mergeOuterCandidates);
		
		performanceEdges.addAndGet(m1.mergeCandidates.size() + m2.mergeCandidates.size()
				+ m1.mergeOuterCandidates.size() + m2.mergeOuterCandidates.size());
		
		return newM;
	}
}
