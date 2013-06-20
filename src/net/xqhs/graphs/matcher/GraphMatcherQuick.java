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

import net.xqhs.graphs.graph.SimpleEdge;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.graph.GraphPattern;
import net.xqhs.graphs.graph.SimpleNode;
import net.xqhs.graphs.graph.GraphPattern.EdgeP;
import net.xqhs.graphs.graph.GraphPattern.NodeP;
import net.xqhs.graphs.representation.TextGraphRepresentation;
import net.xqhs.graphs.util.Debug.D_G;
import net.xqhs.util.logging.Log.Level;
import net.xqhs.util.logging.Unit;
import net.xqhs.util.logging.UnitConfigData;

/**
 * An algorithm that finds partial matches between a graph pattern GP (or G^P) and a graph (G). It implements the {@link GraphMatcher} interface.
 * 
 * @author Andrei Olaru
 */
public class GraphMatcherQuick extends Unit implements GraphMatcher
{
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
	 * <li>the correspondence between the edges in GmP and G' (<code>edgeFunction</code>) - also a bijective function,
	 * in the sense that the values of the function (which are sets of edges) cover E' and do not have intersections.
	 * </ul>
	 * <li>the unsolved part of the pattern - GxP;
	 * <li>the number of edges that are not yet matched - K = ||ExP||
	 * <li>the frontier - the nodes in GmP that have adjacent edges that have not yet been included in the match (edges
	 * in ExP); the frontier also contains information on how many such edges exist for each node in the frontier.
	 * <li>a set of matches that are candidates to merge with this match.
	 * </ul>
	 * 
	 * @author Andrei Olaru
	 */
	public static class Match
	{
		/**
		 * Reference to the graph G.
		 */
		SimpleGraph						targetGraphLink;
		/**
		 * Reference to the pattern GP
		 */
		GraphPattern				patternLink;
		
		/**
		 * G', the subgraph of G that has been matched. It is connected and it is a proper graph.
		 */
		SimpleGraph						matchedGraph;
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
		Map<NodeP, SimpleNode>			nodeFunction;
		/**
		 * The correspondence (edge) function EmP -> E'
		 */
		Map<EdgeP, List<SimpleEdge>>		edgeFunction;
		/**
		 * The nodes on the frontier of GmP - nodes that have adjacent edges in ExP. Nodes are a subset of VmP.
		 * <p>
		 * For each node the number of remaining edges in ExP that are adjacent to it is given.
		 */
		Map<NodeP, AtomicInteger>	frontier				= null;
		/**
		 * MC, matches that could possibly be merged with this one (i.e. not intersecting and sharing at least one
		 * common vertex (with a common correspondent in the graph).
		 */
		Set<Match>					mergeCandidates			= null;
		/**
		 * MO, matches that could potentially merge with this one, but not immediately (they are not adjacent).
		 */
		Set<Match>					mergeOuterCandidates	= null;
		
		/**
		 * The name of the edge.
		 * <p>
		 * Initially (for single-edge matches) the id is the id of the pattern edge, dash, a counter for matches based
		 * on that edge.
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
		public Match(SimpleGraph g, GraphPattern p)
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
		public Match(SimpleGraph g, GraphPattern p, SimpleEdge e, EdgeP eP, String id)
		{
			this(g, p);
			
			// G' contains the edge and the two adjacent nodes
			matchedGraph = new SimpleGraph().addNode(e.getFrom()).addNode(e.getTo()).addEdge(e);
			// GmP contains the pattern edge and the two adjacent nodes
			solvedPart = (GraphPattern) new GraphPattern().addNode((NodeP) eP.getFrom(), false)
					.addNode((NodeP) eP.getTo(), false).addEdge(eP);
			// node function
			nodeFunction = new HashMap<>();
			nodeFunction.put((NodeP) eP.getFrom(), e.getFrom());
			nodeFunction.put((NodeP) eP.getTo(), e.getTo());
			// edge function
			edgeFunction = new HashMap<>();
			List<SimpleEdge> eL = new ArrayList<>();
			eL.add(e);
			edgeFunction.put(eP, eL);
			// the frontier contains both nodes (if it is the case), with their adjacent edges minus the matched edge
			frontier = new HashMap<>();
			if(eP.getFrom().inEdges.size() + eP.getFrom().outEdges.size() > 1)
				frontier.put((NodeP) eP.getFrom(), new AtomicInteger(eP.getFrom().inEdges.size() + eP.getFrom().outEdges.size() - 1));
			if(eP.getTo().inEdges.size() + eP.getTo().outEdges.size() > 1)
				frontier.put((NodeP) eP.getTo(), new AtomicInteger(eP.getTo().inEdges.size() + eP.getTo().outEdges.size() - 1));
			// unsolved part (all nodes and edges except the matched ones)
			unsolvedPart = new GraphPattern();
			for(SimpleNode vP : p.nodes)
				if((vP != eP.getFrom()) && (vP != eP.getTo()))
					unsolvedPart.addNode((NodeP) vP, false);
			for(SimpleEdge ePi : p.edges)
				if(ePi != eP)
					unsolvedPart.addEdge(ePi);
			k = unsolvedPart.edges.size();
			
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
			String ret = "match [" + id + "] (k=" + k + "): ";
			ret += new TextGraphRepresentation(new TextGraphRepresentation.GraphConfig(matchedGraph).setLayout("", " ",
					2)) + " : ";
			ret += new TextGraphRepresentation(
					new TextGraphRepresentation.GraphConfig(solvedPart).setLayout("", " ", 2))
					+ "\n\t";
			ret += "Gx: "
					+ new TextGraphRepresentation(new TextGraphRepresentation.GraphConfig(unsolvedPart).setLayout("",
							" ", 2)) + "\n\t";
			ret += "frontier: " + frontier + "; ";
			ret += "mCs: [";
			for(Match mi : mergeCandidates)
				ret += mi.id + ", ";
			ret += "] mOCs: [";
			for(Match moi : mergeOuterCandidates)
				ret += moi.id + ", ";
			ret += "] \n\t";
			ret += "fv: " + nodeFunction;
			// ret += "fe: " + edgeFunction + "\n\t";
			return ret;
		}
		
		public String toStringLong()
		{
			String ret = "match: \n\t";
			ret += "G': "
					+ new TextGraphRepresentation(new TextGraphRepresentation.GraphConfig(matchedGraph).setLayout("",
							" ", 2)) + "\n\t";
			ret += "Gm: "
					+ new TextGraphRepresentation(new TextGraphRepresentation.GraphConfig(solvedPart).setLayout("",
							" ", 2)) + "\n\t";
			ret += "Gx: "
					+ new TextGraphRepresentation(new TextGraphRepresentation.GraphConfig(unsolvedPart).setLayout("",
							" ", 2)) + "\n\t";
			ret += "fv: " + nodeFunction + "\n\t";
			ret += "fe: " + edgeFunction + "\n\t";
			ret += "k=" + k;
			
			return ret;
		}
	}
	
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
		private Map<SimpleNode, Integer>	distances	= null;
		
		protected MatchComparator()
		{
		}
		
		protected MatchComparator(Map<SimpleNode, Integer> distances)
		{
			this.distances = distances;
		}
		
		@Override
		public int compare(Match m1, Match m2)
		{
			// single-edge matches (in case distances is defined)
			if((m1.solvedPart.m() == 1) && (m2.solvedPart.m() == 1) && (distances != null))
			{
				SimpleEdge e1 = m1.solvedPart.edges.iterator().next();
				SimpleEdge e2 = m2.solvedPart.edges.iterator().next();
				int result = Math.min(distances.get(e1.getFrom()).intValue(), distances.get(e1.getTo()).intValue())
						- Math.min(distances.get(e2.getFrom()).intValue(), distances.get(e2.getTo()).intValue());
				// dbg(D_G.D_MATCHING_INITIAL, "compare (" + result + ") " + e1 + " : " + e2 + " (for " + m1.id + " vs "
				// + m2.id + ")");
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
	SimpleGraph			graph;
	/**
	 * The pattern to match to the graph (GP).
	 */
	GraphPattern	pattern;
	
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
		super(new UnitConfigData().setName(Unit.DEFAULT_UNIT_NAME).setLevel(Level.ALL));
		this.graph = graph;
		this.pattern = pattern;
	}
	
	/**
	 * Performs the matching process.
	 * 
	 * @return number of matches found // TODO: return the maximal match(es).
	 */
	public int doMatching()
	{
		Map<SimpleNode, Integer> distances = computeVertexDistances();
		
		Comparator<Match> matchComparator = new MatchComparator(distances);
		
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
				lf("merging " + m + " and " + mc);
				Match mr = merge(m, mc);
				if(mr != null) // merge should never fail // FIXME
				{
					lf("new match: " + mr.toString());
					// addInitialMatchToQueue(mr, matchQueue);
				}
				else
					lf("match failed");
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
	protected Map<SimpleNode, Integer> computeVertexDistances()
	{
		/**
		 * Vertices ordered by out-degree (minus in-degree) (first is greatest).
		 * 
		 * Other criteria assure that the sorting will always be the same.
		 */
		SortedSet<NodeP> vertexSet = new TreeSet<>(new Comparator<NodeP>() {
			@Override
			public int compare(NodeP n1, NodeP n2)
			{
				int out1 = n1.outEdges.size() - n1.inEdges.size();
				int out2 = n2.outEdges.size() - n2.inEdges.size();
				if(out1 != out2)
					return -(out1 - out2);
				if(n1.isGeneric() && n2.isGeneric())
				{
					if(n1.genericIndex() == n2.genericIndex())
						return n1.hashCode() - n2.hashCode();
					return n1.genericIndex() - n2.genericIndex();
				}
				if(n1.isGeneric())
					return -1;
				if(n2.isGeneric())
					return 1;
				if(n1.label.compareTo(n2.label) == 0)
					return n1.hashCode() - n2.hashCode();
				return n1.label.compareTo(n2.label);
			}
		});
		vertexSet.addAll(pattern.getNodesP());
		dbg(D_G.D_MATCHING_INITIAL, "sorted vertex set: " + vertexSet);
		/**
		 * The start vertex.
		 */
		SimpleNode vMP = vertexSet.first();
		lf("start vertex: " + vMP);
		/**
		 * Distances of vertices relative to the start vertex. Used in sorting single-edge matches in the match queue.
		 */
		final Map<SimpleNode, Integer> distances = pattern.computeDistancesFromUndirected(vMP);
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
		Comparator<SimpleEdge> edgeComparator = new Comparator<SimpleEdge>() {
			@Override
			public int compare(SimpleEdge e1, SimpleEdge e2)
			{
				if(e1.label == null && e2.label == null)
					return e1.toString().compareTo(e2.toString());
				if(e1.label == null)
					return -1;
				if(e2.label == null)
					return 1;
				if(e1.label.compareTo(e2.label) == 0)
					return e1.toString().compareTo(e2.toString());
				return e1.label.compareTo(e2.label);
			}
		};
		
		/**
		 * Ordered pattern edges, according to label.
		 */
		SortedSet<SimpleEdge> sortedEdges = new TreeSet<>(edgeComparator);
		sortedEdges.addAll(pattern.edges);
		
		/**
		 * Ordered graph edges, according to label.
		 */
		SortedSet<SimpleEdge> sortedGraphEdges = new TreeSet<>(edgeComparator);
		sortedGraphEdges.addAll(graph.edges);
		
		// for each edge in the pattern, create an id and build a match.
		int edgeId = 0;
		for(SimpleEdge ePi : sortedEdges)
		{
			EdgeP eP = (EdgeP) ePi;
			if(!eP.generic)
			{
				int matchId = 0;
				lf("edge " + eP + " has id [" + edgeId + "]");
				for(SimpleEdge e : sortedGraphEdges)
				{
					dbg(D_G.D_MATCHING_INITIAL, "trying edges: " + eP + " : " + e);
					if(isMatch(eP, e))
					{
						Match m = new Match(graph, pattern, e, eP, edgeId + ":" + matchId);
						addInitialMatchToQueue(m, matchQueue);
						li("new initial match: (" + m.id + ") " + m.solvedPart.edges.iterator().next() + " : "
								+ m.matchedGraph.edges.iterator().next());
						
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
		for(Match m : sorted)
			string += m.toString() + ", \n";
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
	protected static boolean isMatch(EdgeP eP, SimpleEdge e)
	{
		// reject if: the from node of eP is not generic and does not have the same label as the from node of E
		if(!((NodeP) eP.getFrom()).generic && !eP.getFrom().label.equals(e.getFrom().label))
			return false;
		// reject if: the to node of eP is not generic and does not have the same label as the to node of E
		if(!((NodeP) eP.getTo()).generic && !eP.getTo().label.equals(e.getTo().label))
			return false;
		
		if(!eP.generic)
		{
			// accept if: eP is not labeled
			// accept if: e is not labeled (or has a void label)
			// accept if: eP has the same label as e
			if((eP.label == null) || (e.label == null) || (e.label.equals("")) || (eP.label.equals(e.label)))
				return true;
			// reject otherwise (both e and eP are labeled and labels don't match)
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
	protected static void addInitialMatchToQueue(Match m, PriorityQueue<Match> queue)
	{
		// take all matches already in the queue and see if they are compatible
		for(Match mi : queue)
		{
			boolean accept = false;
			boolean reject = false;
			// reject if: the two matches intersect (contain common pattern edges
			if(new HashSet<>(m.solvedPart.edges).removeAll(mi.solvedPart.edges)
					&& !new HashSet<>(m.matchedGraph.edges).removeAll(mi.matchedGraph.edges))
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
		newM.unsolvedPart.edges.addAll(pt.edges);
		newM.unsolvedPart.nodes.addAll(pt.nodes);
		newM.k = newM.unsolvedPart.edges.size();
		
		Set<SimpleEdge> totalMatch = new HashSet<>(); // there should be no duplicates as the solved parts should be disjoint.
		totalMatch.addAll(m1.solvedPart.edges);
		totalMatch.addAll(m2.solvedPart.edges);
		
		newM.solvedPart = new GraphPattern();
		newM.nodeFunction = new HashMap<>();
		newM.edgeFunction = new HashMap<>();
		newM.matchedGraph = new SimpleGraph();
		newM.frontier = new HashMap<>();
		for(SimpleEdge e : totalMatch)
		{
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
			newM.nodeFunction.put((NodeP) eP.getFrom(), sourceMatch.nodeFunction.get(eP.getFrom()));
			newM.nodeFunction.put((NodeP) eP.getTo(), sourceMatch.nodeFunction.get(eP.getTo()));
			// edge function -> reuniting the edge functions of the two matches
			newM.edgeFunction.put(eP, sourceMatch.edgeFunction.get(eP));
			// G' -> obtained by adding the values of the edge and node functions, when adding edges in GmP
			for(SimpleEdge em : sourceMatch.edgeFunction.get(eP))
				newM.matchedGraph.addEdge(em).addNode(em.getFrom()).addNode(em.getTo());
			
			// frontier -> practically adding nodes from solved part, always checking if they are still on the frontier
			AtomicInteger fromIndex = newM.frontier.get(e.getFrom());
			if(fromIndex != null)
				if(fromIndex.decrementAndGet() == 0)
					newM.frontier.remove(e.getFrom());
				else
					newM.frontier.put((NodeP) e.getFrom(), fromIndex);
			else
				newM.frontier.put((NodeP) eP.getFrom(), new AtomicInteger(eP.getFrom().inEdges.size() + eP.getFrom().outEdges.size()
						- 1));
			AtomicInteger toIndex = newM.frontier.get(e.getTo());
			if(toIndex != null)
				if(toIndex.decrementAndGet() == 0)
					newM.frontier.remove(e.getTo());
				else
					newM.frontier.put((NodeP) e.getTo(), toIndex);
			else
				newM.frontier.put((NodeP) eP.getTo(), new AtomicInteger(eP.getTo().inEdges.size() + eP.getTo().outEdges.size() - 1));
			
			// merge candidates: MC = (MC n MC2) u (MC1 n MO2) u (MC2 n MO1)
			// common merge candidates, and candidates of each match that were outer candidates for the other match
		}
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
		
		return newM;
	}
}
