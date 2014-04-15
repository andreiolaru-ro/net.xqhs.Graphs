package net.xqhs.graphs.matchingPlatform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.matcher.GraphMatcherQuick;
import net.xqhs.graphs.matcher.GraphMatchingProcess;
import net.xqhs.graphs.matcher.Match;
import net.xqhs.graphs.matcher.Match.MatchComparator;
import net.xqhs.graphs.matcher.MonitorPack;
import net.xqhs.graphs.pattern.EdgeP;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.util.Debug.D_G;

/**
 * The class extends {@link GraphMatcherQuick} (and therefore implements {@link GraphMatchingProcess}) to handle
 * persistent matching -- matching in which the graph changes slightly from time to time. With each change, only
 * affected matches should be removed or created.
 * <p>
 * The pattern is not allowed to change.
 * 
 * @author Andrei Olaru
 */
public class GraphMatcherPersistent extends GraphMatcherQuick
{
	/**
	 * THe set of all matches, sorted by k (lowest k first).
	 */
	protected SortedSet<Match>		sortedMatches;
	
	/**
	 * An index containing the matches that contain each graph edge. The index contains only the edges that are
	 * contained in any matches.
	 */
	protected Map<Edge, Set<Match>>	eMatchIndex		= null;
	/**
	 * An index containing the matches that contain each pattern edge. The index contains only the edges that are
	 * contained in any matches.
	 */
	protected Map<Edge, Set<Match>>	ePMatchIndex	= null;
	
	/**
	 * Creates a new matcher for the specified graph and pattern. Any further changes to the graph will be signaled by
	 * calling {@link #addMatches(Edge)} and {@link #removeMatches(Edge)}.
	 * 
	 * @param graph
	 *            - the graph.
	 * @param pattern
	 *            - the pattern.
	 */
	protected GraphMatcherPersistent(Graph graph, GraphPattern pattern)
	{
		super(graph, pattern);
	}
	
	@Override
	public GraphMatcherPersistent initializeMatching()
	{
		sortedMatches = new TreeSet<Match>(new MatchComparator(monitor));
		eMatchIndex = new HashMap<Edge, Set<Match>>();
		ePMatchIndex = new HashMap<Edge, Set<Match>>();
		super.initializeMatching();
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
	
	/**
	 * Completes the matching process, growing all matches to their maximum coverage.
	 * 
	 * @return the instance itself.
	 */
	public GraphMatcherPersistent completeMatches()
	{
		// it doesn't matter what k is used, all matches will be grown anyway.
		getAllMatches(0);
		return this;
	}
	
	/**
	 * The method should be called for each new edge added to the graph. It is assumed that the graph contains the new
	 * edge when the method is called.
	 * <p>
	 * The method creates the initial matches containing the new edge, adds their merge candidates, but does not grow
	 * any matches. This can be requested through any of the match retrieval methods or by calling
	 * {@link #completeMatches()}.
	 * 
	 * @param e
	 *            - the new edge added to the graph.
	 * @return the instance itself.
	 */
	public GraphMatcherPersistent addMatches(Edge e)
	{
		if((matchQueue == null) || (allMatches == null))
			initializeMatching();
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
	
	/**
	 * The method should be called for each edge removed from the graph. It is assumed that the graph doesn't contain
	 * the edge anymore at the time the method is called.
	 * <p>
	 * The method only removes the edge from the edge &rarr; matches index, and marks the matches containing the edge as
	 * invalid. Whenever an iteration finds the invalidated match, it will be removed from the containing collection.
	 * This saves a large number of operations that would have been required by looping through the various lists and
	 * indexes.
	 * 
	 * @param edge
	 *            - the edge removed from the graph.
	 * @return the instance itself.
	 */
	public GraphMatcherPersistent removeMatches(Edge edge)
	{
		if((matchQueue == null) || (allMatches == null))
			// matching not initialized anyway (no matches)
			return this;
		if(!eMatchIndex.containsKey(edge))
			// there were no matches containing the edge
			return this;
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
		for(Edge neP : neighborEdgePs)
			if(ePMatchIndex.containsKey(neP))
				for(Iterator<Match> it = ePMatchIndex.get(neP).iterator(); it.hasNext();)
				{
					Match mc = it.next();
					if(mc.isValid())
						nMatches.add(mc);
					else
						it.remove();
				}
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
	
	/**
	 * @return an indication of the used memory (sizes of indexes).
	 */
	protected int getMemory()
	{
		int ret = (allMatches != null ? allMatches.size() : 0);
		// ret += (matchQueue != null ? matchQueue.size() : 0);
		// if(eMatchIndex != null)
		// for(Set<Match> item : eMatchIndex.values())
		// ret += item.size();
		// if(ePMatchIndex != null)
		// for(Set<Match> item : ePMatchIndex.values())
		// ret += item.size();
		return ret;
	}
}
