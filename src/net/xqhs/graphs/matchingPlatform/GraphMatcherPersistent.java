package net.xqhs.graphs.matchingPlatform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.matcher.GraphMatcherQuick;
import net.xqhs.graphs.matcher.Match;
import net.xqhs.graphs.matcher.Match.MatchComparator;
import net.xqhs.graphs.matcher.MonitorPack;
import net.xqhs.graphs.pattern.EdgeP;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.util.Debug.D_G;

public class GraphMatcherPersistent extends GraphMatcherQuick
{
	protected SortedSet<Match>		sortedMatches;
	
	protected Map<Edge, Set<Match>>	eMatchIndex		= null;
	protected Map<Edge, Set<Match>>	ePMatchIndex	= null;
	
	protected GraphMatcherPersistent(Graph graph, GraphPattern pattern)
	{
		super(graph, pattern);
	}
	
	@Override
	public GraphMatcherPersistent initializeMatching()
	{
		super.initializeMatching();
		sortedMatches = new TreeSet<Match>(new MatchComparator(monitor));
		eMatchIndex = new HashMap<Edge, Set<Match>>();
		ePMatchIndex = new HashMap<Edge, Set<Match>>();
		return this;
	}
	
	@Override
	public GraphMatcherPersistent clearData()
	{
		super.clearData();
		sortedMatches.clear();
		eMatchIndex.clear();
		ePMatchIndex.clear();
		return this;
	}
	
	public GraphMatcherPersistent completeMatches()
	{
		// it doesn't matter what k is used, all matches will be grown anyway.
		getAllMatches(0);
		return this;
	}
	
	public GraphMatcherPersistent addMatches(Edge e)
	{
		/**
		 * Ordered pattern edges, according to label.
		 */
		SortedSet<Edge> sortedEdges = new TreeSet<Edge>(new EdgeComparator(monitor));
		sortedEdges.addAll(pattern.getEdges());
		
		int edgeId = 0; // TODO
		int matchId = 0; // TODO
		for(Edge eP : sortedEdges)
		{
			// no generic pattern edges in initial matches
			if(!((eP instanceof EdgeP) && ((EdgeP) eP).isGeneric()))
			{
				monitor.lf("edge [] has id []", eP, new Integer(edgeId));
				monitor.dbg(D_G.D_MATCHING_INITIAL, "trying edges: [] : []", eP, e);
				if(isMatch(eP, e))
				{
					Match m = addInitialMatch(e, eP, edgeId + ":" + matchId);
					monitor.incrementMatchCount();
					monitor.lf("new single match: []", m.toString());
					matchId++;
				}
			}
		}
		return this;
	}
	
	public GraphMatcherPersistent removeMatches(Edge edge)
	{
		if(!eMatchIndex.containsKey(edge))
			throw new IllegalStateException("Edge not found.");
		Set<Match> toRemove = eMatchIndex.get(edge);
		eMatchIndex.remove(edge);
		for(Match m : toRemove)
			invalidateMatch(m);
		toRemove.clear();
		return this;
	}
	
	@Override
	protected Match addInitialMatch(Edge e, Edge eP, String matchID)
	{
		Match m = new Match(graph, pattern, e, eP, matchID);
		
		// get neighbor edges in pattern and all matches containing these neighbor edges
		Set<Edge> neighborEdgePs = new HashSet<Edge>();
		neighborEdgePs.addAll(pattern.getInEdges(eP.getFrom()));
		neighborEdgePs.addAll(pattern.getOutEdges(eP.getFrom()));
		neighborEdgePs.addAll(pattern.getInEdges(eP.getTo()));
		neighborEdgePs.addAll(pattern.getOutEdges(eP.getTo()));
		Set<Match> nMatches = new HashSet<Match>();
		for(Edge ne : neighborEdgePs)
			nMatches.addAll(ePMatchIndex.get(ne));
		// add other matches to candidates list
		for(Match mi : nMatches)
			m.considerCandidate(mi, eMatchIndex, ePMatchIndex, monitor);
		// add to indexes
		if(!eMatchIndex.containsKey(e))
			eMatchIndex.put(e, new HashSet<Match>());
		eMatchIndex.get(e).add(m);
		if(!ePMatchIndex.containsKey(eP))
			ePMatchIndex.put(eP, new HashSet<Match>());
		ePMatchIndex.get(eP).add(m);
		// add to global lists
		matchQueue.add(m);
		allMatches.add(m);
		return m;
	}
	
	@Override
	protected Match addMergeMatch(Match m1, Match m2)
	{
		// create
		// add to indexes
		Match newM = m1.merge(m2, eMatchIndex, ePMatchIndex, monitor);
		
		// add to global lists
		matchQueue.add(newM);
		allMatches.add(newM);
		
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
	public static GraphMatcherPersistent getMatcher(Graph graph, GraphPattern pattern, MonitorPack monitoring)
	{
		if(monitoring == null)
			throw new IllegalArgumentException();
		if(monitoring.getVisual() != null)
		{
			monitoring.getVisual().feedLine(graph, null, "the graph");
			monitoring.getVisual().feedLine(pattern, null, "the pattern");
		}
		return (GraphMatcherPersistent) new GraphMatcherPersistent(graph, pattern).setMonitor(monitoring);
	}
}
