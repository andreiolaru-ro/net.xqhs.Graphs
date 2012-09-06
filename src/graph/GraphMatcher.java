package graph;

import graph.GraphPattern.EdgeP;
import graph.GraphPattern.NodeP;

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
import java.util.Vector;

import net.xqhs.util.logging.Log.Level;
import net.xqhs.util.logging.Unit;
import net.xqhs.util.logging.UnitConfigData;
import representation.TextGraphRepresentation;
import util.Debug.D_G;

/**
 * An algorithm that finds partial matches between a graph pattern GP (or G^P) and a graph (G).
 * 
 * @author Andrei Olaru
 */
public class GraphMatcher extends Unit
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
		Graph				  targetGraphLink;
		/**
		 * Reference to the pattern GP
		 */
		GraphPattern		   patternLink;
		
		/**
		 * G', the subgraph of G that has been matched. It is connected and it is a proper graph.
		 */
		Graph				  matchedGraph;
		/**
		 * GmP, the part of GP that has been matched. It is connected and it is a proper graph.
		 */
		GraphPattern		   solvedPart;
		/**
		 * GxP, the part of GP that has not been matched. It may contain edges without the adjacent nodes.
		 */
		GraphPattern		   unsolvedPart;
		/**
		 * k, the number of edges in GxP
		 */
		int					k;
		
		/**
		 * The correspondence function VmP -> V'
		 */
		Map<NodeP, Node>	   nodeFunction;
		/**
		 * The correspondence function EmP -> E'
		 */
		Map<EdgeP, List<Edge>> edgeFunction;
		/**
		 * The nodes on the frontier of GmP - nodes that have adjacent edges in ExP. Nodes are a subset of VmP.
		 * <p>
		 * For each node the number of remaining edges in ExP that are adjacent to it is given.
		 */
		Map<NodeP, Integer>	frontier		= null;
		/**
		 * Matches that could possibly be merged to this one (i.e. not intersecting and sharing at least one common
		 * vertex (with a common correspondent in the graph).
		 */
		Set<Match>			 mergeCandidates = null;
		
		/**
		 * The name of the edge.
		 * <p>
		 * Initially (for single-edge matches) the id is the id of the pattern edge, dash, a counter for matches based
		 * on that edge.
		 */
		String				 id			  = "-";
		
		/**
		 * Create a new empty match; some parts may be uninitialized / undefined (like frontier, or matchCandidates)
		 * <p>
		 * This constructor is meant just to construct matches that will later be completely initialized as a result of
		 * merging two existing matches.
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
		public Match(Graph g, GraphPattern p, Edge e, EdgeP eP, String id)
		{
			this(g, p);
			
			// G' contains the edge and the two adjacent nodes
			matchedGraph = new Graph().addNode(e.from).addNode(e.to).addEdge(e);
			// GmP contains the pattern edge and the two adjacent nodes
			solvedPart = (GraphPattern) new GraphPattern().addNode((NodeP) eP.from, false)
					.addNode((NodeP) eP.to, false).addEdge(eP);
			// node function
			nodeFunction = new HashMap<>();
			nodeFunction.put((NodeP) eP.from, e.from);
			nodeFunction.put((NodeP) eP.to, e.to);
			// edge function
			edgeFunction = new HashMap<>();
			List<Edge> eL = new Vector<>(1, 0);
			eL.add(e);
			edgeFunction.put(eP, eL);
			// the frontier contains both nodes (if it is the case), with their adjacent edges minus the matched edge
			frontier = new HashMap<>();
			if(eP.from.inEdges.size() + eP.from.outEdges.size() > 1)
				frontier.put((NodeP) eP.from, new Integer(eP.from.inEdges.size() + eP.from.outEdges.size() - 1));
			if(eP.to.inEdges.size() + eP.to.outEdges.size() > 1)
				frontier.put((NodeP) eP.to, new Integer(eP.to.inEdges.size() + eP.to.outEdges.size() - 1));
			// unsolved part (all nodes and edges except the matched ones)
			unsolvedPart = new GraphPattern();
			for(Node vP : p.nodes)
				if((vP != eP.from) && (vP != eP.to))
					unsolvedPart.addNode((NodeP) vP, false);
			for(Edge ePi : p.edges)
				if(ePi != eP)
					unsolvedPart.addEdge(ePi);
			k = unsolvedPart.edges.size();
			
			// no match candidates added; candidates will be ordered by id, for easier reading
			mergeCandidates = new TreeSet<>(new Comparator<Match>() {
				@Override
				public int compare(Match m1, Match m2)
				{
					return m1.id.compareTo(m2.id);
				}
			});
			
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
			ret += "fv: <node function>\n\t";
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
			ret += "] \n\t";
			ret += "fv: " + nodeFunction + "\n\t";
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
	 * The graph to match the pattern to (G).
	 */
	Graph		graph;
	/**
	 * The pattern to match to the graph (GP).
	 */
	GraphPattern pattern;
	
	/**
	 * Initializes a matcher. Does not do the matching.
	 * 
	 * @param graph
	 *            : the graph (G).
	 * @param pattern
	 *            : the pattern (GP).
	 */
	public GraphMatcher(Graph graph, GraphPattern pattern)
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
		Node vMP = vertexSet.first();
		lf("start vertex: " + vMP);
		/**
		 * Distances of vertices relative to the start vertex. Used in sorting single-edge matches in the match queue.
		 */
		final Map<Node, Integer> distances = pattern.computeDistancesFromUndirected(vMP);
		dbg(D_G.D_MATCHING_INITIAL, "vertex distances: " + distances);
		
		/**
		 * Match sorter queue
		 * <p>
		 * The matches are sorted according to:
		 * <ul>
		 * <li>if the match is single-edge, use the distance (of the closest adjacent vertex) to the start vertex. If
		 * it's the same distance, order by the match's id.
		 * <li>for matches with more than one edge, order by <code>k</code> (smaller k first). If equal, order by id.
		 * </ul>
		 */
		PriorityQueue<Match> matchQueue = new PriorityQueue<>(1, new Comparator<Match>() {
			@SuppressWarnings("synthetic-access")
			@Override
			public int compare(Match m1, Match m2)
			{
				if((m1.solvedPart.m() == 1) && (m2.solvedPart.m() == 1))
				{
					Edge e1 = m1.solvedPart.edges.iterator().next();
					Edge e2 = m2.solvedPart.edges.iterator().next();
					int result = Math.min(distances.get(e1.from).intValue(), distances.get(e1.to).intValue())
							- Math.min(distances.get(e2.from).intValue(), distances.get(e2.to).intValue());
					dbg(D_G.D_MATCHING_INITIAL, "compare (" + result + ") " + e1 + " : " + e2);
					if(result != 0)
						return result;
					return m1.id.compareTo(m2.id);
				}
				if(m1.k != m2.k)
					return m1.k - m2.k;
				return m1.id.compareTo(m2.id);
			}
		});
		
		// /////// build initial matches
		
		Comparator<Edge> edgeComparator = new Comparator<Edge>() {
			@Override
			public int compare(Edge e1, Edge e2)
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
		SortedSet<Edge> sortedEdges = new TreeSet<>(edgeComparator);
		sortedEdges.addAll(pattern.edges);
		
		/**
		 * Ordered graph edges, according to label.
		 */
		SortedSet<Edge> sortedGraphEdges = new TreeSet<>(edgeComparator);
		sortedGraphEdges.addAll(graph.edges);
		
		// for each edge in the pattern, create an id and build a match.
		int edgeId = 0;
		for(Edge ePi : sortedEdges)
		{
			EdgeP eP = (EdgeP) ePi;
			if(!eP.generic)
			{
				int matchId = 0;
				lf("edge " + eP + " has id [" + edgeId + "]");
				for(Edge e : sortedGraphEdges)
				{
					dbg(D_G.D_MATCHING_INITIAL, "trying edges: " + eP + " : " + e);
					if(isMatch(eP, e))
					{
						Match m = new Match(graph, pattern, e, eP, edgeId + ":" + matchId);
						addMatchToQueue(m, matchQueue);
						li("new initial match: " + m.solvedPart.edges.iterator().next() + " : "
								+ m.matchedGraph.edges.iterator().next());
						matchId++;
					}
				}
				edgeId++;
			}
		}
		
		lf("initial matches (" + matchQueue.size() + "): " + matchQueue + "-------------------------");
		
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
				if(mr != null) // merge should naver fail // FIXME
					addMatchToQueue(mr, matchQueue);
			}
		}
		
		return 0; // FIXME
	}
	
	/**
	 * Merges to matches into one.
	 * 
	 * @param m1
	 *            : the first match.
	 * @param m2
	 *            : the second match.
	 * @return the merged match.
	 */
	protected Match merge(Match m1, Match m2)
	{
		GraphPattern pt = m1.patternLink;
		Match newM = new Match(m1.targetGraphLink, pt);
		
		newM.unsolvedPart = new GraphPattern();
		newM.unsolvedPart.edges.addAll(pt.edges);
		newM.unsolvedPart.nodes.addAll(pt.nodes);
		newM.k = newM.unsolvedPart.edges.size();
		
		Set<Edge> totalMatch = new HashSet<>(); // is a set, so it will contain no duplicates
		totalMatch.addAll(m1.solvedPart.edges);
		totalMatch.addAll(m2.solvedPart.edges);
		
		for(Edge e : totalMatch)
		{
			EdgeP eP = (EdgeP) e;
			boolean fitted = false;
			newM.solvedPart.addEdge(eP).addNode(eP.from).addNode(eP.to);
			// TODO check if the elements were contained in unsolvedPart
			newM.unsolvedPart.removeEdge(eP).removeNode(eP.from).removeNode(eP.to);
			newM.k--;
			if(newM.frontier == null)
				newM.frontier = new HashMap<>();
			// newM.frontier.put((NodeP)eP.from, new Integer(eP.from.inEdges.size() + eP.from.outEdges.size() - 1)); //
			// FIXME
			// newM.frontier.put((NodeP)eP.to, new Integer(eP.to.inEdges.size() + eP.to.outEdges.size() - 1)); // FIXME
			
			if(m1.solvedPart.contains(eP))
			{
				fitted = true;
				for(Edge em : m1.edgeFunction.get(eP))
					newM.matchedGraph.addEdge(em).addNode(em.from).addNode(em.to);
			}
			if(m2.solvedPart.contains(eP))
			{
				if(fitted)
				{
					le("match-intersection pattern edge found: [" + eP + "]");
					throw new IllegalArgumentException("match-intersection edge");
				}
				fitted = true;
				for(Edge em : m2.edgeFunction.get(eP))
					newM.matchedGraph.addEdge(em).addNode(em.from).addNode(em.to);
			}
		}
		
		return null;
	}
	
	/**
	 * Adds the match to the matching queue and add matches from the queue to its merge candidate list (as well as
	 * adding the match to other matches' merge candidates)
	 * 
	 * @param m
	 *            : the match to add to the queue
	 * @param queue
	 *            : the match queue
	 */
	protected static void addMatchToQueue(Match m, PriorityQueue<Match> queue)
	{
		// take all matches already in the queue and see if they are compatible
		for(Match mi : queue)
			// check that the two matches don't intersect
			if(!new HashSet<>(m.solvedPart.edges).removeAll(mi.solvedPart.edges)
					&& !new HashSet<>(m.matchedGraph.edges).removeAll(mi.matchedGraph.edges))
			{
				boolean accept = false;
				boolean reject = false;
				// iterate on the frontier of the potential candidate
				// TODO: it should iterate on the frontier of the candidate with a shorter frontier
				for(Map.Entry<NodeP, Integer> frontierV : mi.frontier.entrySet())
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
					// QUEST: what if one match contains interior nodes from the other? is that possible?
				}
				if(accept && !reject)
				{ // then each match is a merge candidate for the other
					m.mergeCandidates.add(mi);
					mi.mergeCandidates.add(m);
				}
			}
		// add the match to the queue
		queue.add(m);
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
	protected static boolean isMatch(EdgeP eP, Edge e)
	{
		// reject if: the from node of eP is not generic and does not have the same label as the from node of E
		if(!((NodeP) eP.from).generic && !eP.from.label.equals(e.from.label))
			return false;
		// reject if: the to node of eP is not generic and does not have the same label as the to node of E
		if(!((NodeP) eP.to).generic && !eP.to.label.equals(e.to.label))
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
}
