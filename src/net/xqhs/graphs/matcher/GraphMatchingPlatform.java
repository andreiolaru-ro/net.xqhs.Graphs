package net.xqhs.graphs.matcher;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.pattern.GraphPattern;

/**
 * Implementations of this interface serve as platforms that handle continuous or on-demand matching of various graphs
 * and patterns against a designated, 'principal' {@link Graph}.
 * 
 * @author Andrei Olaru
 */
public interface GraphMatchingPlatform
{
	public GraphMatchingPlatform setPrincipalGraph(PlatformPrincipalGraph graph);
	
	public GraphMatchingPlatform addPattern(GraphPattern pattern);
	
	public GraphMatchingPlatform removePattern(GraphPattern pattern);
	
	public GraphMatchingProcess getMatcherAgainstGraph(GraphPattern pattern);
	
	public List<Match> getNewMatches(AtomicInteger sequence);
	
	public int getSequence();
}
