package net.xqhs.graphs.matchingPlatform;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.matcher.GraphMatchingProcess;
import net.xqhs.graphs.matcher.Match;
import net.xqhs.graphs.matcher.MonitorPack;
import net.xqhs.graphs.matchingPlatform.Transaction.Operation;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.util.Debug.D_G;
import net.xqhs.util.logging.Unit;

/**
 * An implementation of {@link GraphMatchingPlatform} using {@link TrackingGraph} instances for the principal graph and
 * its shadows.
 * <p>
 * For each pattern in the platform, a {@link GraphMatcherPersistent} process is kept and updated with changes to the
 * principal graph.
 *
 * @author Andrei Olaru
 */
public class GMPImplementation extends Unit implements GraphMatchingPlatform
{
	
	/**
	 * An implementation of {@link GraphMatchingPlatform.PlatformPrincipalGraph} and
	 * {@link GraphMatchingPlatform.PlatformShadowGraph} that extends {@link TrackingGraph}.
	 *
	 * @author Andrei Olaru
	 */
	public static class PrincipalGraph extends TrackingGraph implements PlatformPrincipalGraph, PlatformShadowGraph
	{
		/**
		 * Default constructor.
		 */
		public PrincipalGraph()
		{
			super();
		}
		
		/**
		 * Protected constructor for constructing shadows of the principal graph.
		 *
		 * @param transactionsLink
		 *            - the transaction queue.
		 * @param initialSequence
		 *            - the initial sequence.
		 * @param initialGraph
		 *            - the {@link Graph} containing the initial nodes and edges.
		 */
		protected PrincipalGraph(Queue<Transaction> transactionsLink, int initialSequence, Graph initialGraph)
		{
			super(transactionsLink, initialSequence, initialGraph);
		}
		
		@Override
		public PlatformShadowGraph createShadowGraph()
		{
			return new PrincipalGraph(createShadowQueue(), getSequence(), this);
		}
	}
	
	/**
	 * The {@link MonitorPack} instance to use for performance measuring.
	 */
	MonitorPack									monitor			= new MonitorPack();
	
	/**
	 * The principal graph of the platform.
	 */
	PlatformPrincipalGraph						principalGraph	= null;
	/**
	 * The patterns in the platform, as a map between the patterns and the corresponding persistent matching processes.
	 */
	Map<GraphPattern, GraphMatcherPersistent>	patterns		= null;
	/**
	 * The graph against which the matching is done. This is updated to get closer to the current state of the principal
	 * graph with every sequence increment.
	 */
	PlatformShadowGraph							matchingGraph	= null;
	
	/**
	 * Sets the {@link MonitorPack} instance to use for monitoring.
	 *
	 * @param monitorLink
	 *            - the monitor.
	 * @return the platform itself.
	 */
	public GMPImplementation setMonitor(MonitorPack monitorLink)
	{
		if(monitor == null)
			monitor = new MonitorPack();
		else
			monitor = monitorLink;
		return this;
	}
	
	@Override
	public GMPImplementation setPrincipalGraph(PlatformPrincipalGraph graph)
	{
		if(graph == null)
			throw new IllegalArgumentException("Principal graph cannot be null");
		principalGraph = graph;
		matchingGraph = null;
		if((patterns != null) && !patterns.isEmpty())
		{
			// TODO check that this works correctly
			Set<GraphPattern> newP = new HashSet<GraphPattern>(patterns.keySet());
			patterns.clear();
			for(GraphPattern pattern : newP)
				addPattern(pattern);
		}
		return this;
	}
	
	@Override
	public PlatformPrincipalGraph getPrincipalGraph()
	{
		return principalGraph;
	}
	
	@Override
	public GMPImplementation addPattern(GraphPattern pattern)
	{
		if((patterns != null) && patterns.containsKey(pattern))
			lw("Pattern already contained.");
		else
		{
			if(patterns == null)
				patterns = new HashMap<GraphPattern, GraphMatcherPersistent>();
			if(matchingGraph == null)
				matchingGraph = principalGraph.createShadowGraph();
			// create new matching process
			GraphMatcherPersistent matchingProcess = GraphMatcherPersistent.getMatcher(matchingGraph, pattern, monitor);
			patterns.put(pattern, matchingProcess);
		}
		return this;
	}
	
	@Override
	public GMPImplementation removePattern(GraphPattern pattern)
	{
		if(!patterns.containsKey(pattern))
			le("Pattern not contained.");
		else
		{
			// remove matching data
			patterns.get(pattern).clearData();
			// remove pattern and process
			patterns.remove(pattern);
		}
		return this;
	}
	
	@Override
	public Collection<GraphPattern> getPatterns()
	{
		return Collections.unmodifiableCollection(patterns.keySet());
	}
	
	@Override
	public Set<Match> incrementSequence()
	{
		// FIXME take this out of here
		int mem = 0;
		int stored = 0;
		for(GraphMatcherPersistent gm : patterns.values())
		{
			mem += gm.getMemory();
			stored += gm.getStoredMatches();
		}
		monitor.setMemoryIndication(mem);
		monitor.setStoredMatches(stored);
		
		if(!matchingGraph.canIncrement())
			return null;
		Map<GraphComponent, Operation> operations = matchingGraph.getNextSequenceOperations();
		// modify graph
		matchingGraph.incrementSequence();
		// TODO: what if a node is removed?
		// remove matches that don't match anymore (edges ONLY)
		for(Map.Entry<GraphComponent, Operation> op : operations.entrySet())
			if((op.getValue() == Operation.REMOVE) && (op.getKey() instanceof Edge))
				for(GraphMatcherPersistent gm : patterns.values())
					gm.removeMatches((Edge) op.getKey());
		// TODO: what if a node is added resulting in an older edge having both nodes in the graph?
		// add new matches for newly added edges (edges ONLY)
		for(Map.Entry<GraphComponent, Operation> op : operations.entrySet())
			if((op.getValue() == Operation.ADD) && (op.getKey() instanceof Edge))
				for(GraphMatcherPersistent gm : patterns.values())
					gm.addMatches((Edge) op.getKey());
		
		Set<Match> ret = new HashSet<Match>();
		for(GraphMatcherPersistent gm : patterns.values())
		{
			if(D_G.D_NO_SAVED_DATA.toBool())
				gm.clearData();
			ret.addAll(gm.getAllCompleteMatches());
		}
		return ret;
	}
	
	// FIXME: if there are no elements in the queue, the sequence is not incremented. should check for
	// desynchronization; is it possible?
	@Override
	public List<Entry<Integer, Set<Match>>> incrementSequence(int targetSequence)
	{
		List<Entry<Integer, Set<Match>>> ret = new ArrayList<Map.Entry<Integer, Set<Match>>>();
		while(getMathingSequence() < Math.min(getGraphSequence(), (targetSequence > 0 ? targetSequence
				: getGraphSequence())))
		{
			Set<Match> result = incrementSequence();
			if(result != null)
				ret.add(new AbstractMap.SimpleEntry<Integer, Set<Match>>(new Integer(getMathingSequence()), result));
		}
		return ret;
	}
	
	@Override
	public List<Entry<Integer, Set<Match>>> incrementSequenceFastForward()
	{
		return incrementSequence(-1);
	}
	
	@Override
	public Set<Match> getMatches(GraphPattern pattern, int maxK)
	{
		if(!patterns.containsKey(pattern))
			throw new IllegalArgumentException("Pattern is not part of this platform");
		GraphMatchingProcess GM = patterns.get(pattern);
		return new HashSet<Match>(GM.getAllMatches(maxK));
	}
	
	@Override
	public int getMathingSequence()
	{
		if(matchingGraph == null)
		{
			// no matching has started yet, therefore synchronized
			if(principalGraph == null)
				return -1;
			return principalGraph.getSequence();
		}
		return matchingGraph.getSequence();
	}
	
	@Override
	public int getGraphSequence()
	{
		if(principalGraph == null)
			return -1;
		return principalGraph.getSequence();
	}
	
	@Override
	public GraphMatchingProcess getMatcherAgainstGraph(GraphPattern pattern)
	{
		return GraphMatcherPersistent.getMatcher(principalGraph.createShadowGraph(), pattern, monitor);
	}
	
	// @Override
	// public void printindexes()
	// {
	// for(GraphMatcherPersistent gm : patterns.values())
	// {
	// System.out.println(gm.toString() + gm.eMatchIndex);
	// System.out.println(gm.toString() + gm.ePMatchIndex);
	// }
	// }
}
