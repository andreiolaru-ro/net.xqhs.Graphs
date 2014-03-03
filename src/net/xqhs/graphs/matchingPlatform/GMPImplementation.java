package net.xqhs.graphs.matchingPlatform;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.matcher.GraphMatchingProcess;
import net.xqhs.graphs.matcher.Match;
import net.xqhs.graphs.matcher.MonitorPack;
import net.xqhs.graphs.matchingPlatform.TrackingGraph.Operation;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.util.logging.Unit;

public class GMPImplementation extends Unit implements GraphMatchingPlatform
{
	
	static class PrincipalGraph extends TrackingGraph implements PlatformPrincipalGraph, PlatformShadowGraph
	{
		@Override
		public PlatformShadowGraph createShadowGraph()
		{
			return (PlatformShadowGraph) super.createShadow();
		}
	}
	
	PlatformPrincipalGraph						principalGraph	= null;
	Map<GraphPattern, GraphMatcherPersistent>	patterns		= null;
	
	PlatformShadowGraph							matchingGraph	= null;
	
	@Override
	public GraphMatchingPlatform setPrincipalGraph(PlatformPrincipalGraph graph)
	{
		if(graph == null)
			throw new IllegalArgumentException("Principal graph cannot be null");
		principalGraph = graph;
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
		if(patterns.containsKey(pattern))
			lw("Pattern already contained.");
		else
		{
			// create new matching process
			// TODO monitor
			GraphMatcherPersistent matchingProcess = GraphMatcherPersistent.getMatcher(principalGraph, pattern,
					new MonitorPack());
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
			ret.addAll(gm.getAllCompleteMatches());
		return ret;
	}
	
	@Override
	public List<Entry<Integer, Set<Match>>> incrementSequence(int targetSequence)
	{
		List<Entry<Integer, Set<Match>>> ret = new ArrayList<Map.Entry<Integer, Set<Match>>>();
		while(getMathingSequence() < Math.min(getGraphSequence(), (targetSequence > 0 ? targetSequence
				: getGraphSequence())))
			ret.add(new AbstractMap.SimpleEntry<Integer, Set<Match>>(new Integer(getMathingSequence()),
					incrementSequence()));
		return ret;
	}
	
	@Override
	public List<Entry<Integer, Set<Match>>> incrementSequenceFastForward()
	{
		return incrementSequence(-1);
	}
	
	@Override
	public int getMathingSequence()
	{
		return matchingGraph.getSequence();
	}
	
	@Override
	public int getGraphSequence()
	{
		return principalGraph.getSequence();
	}
	
	@Override
	public GraphMatchingProcess getMatcherAgainstGraph(GraphPattern pattern)
	{
		// TODO monitor
		return GraphMatcherPersistent.getMatcher(principalGraph.createShadowGraph(), pattern, new MonitorPack());
	}
}
