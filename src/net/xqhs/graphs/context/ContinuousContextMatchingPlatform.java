package net.xqhs.graphs.context;

import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.matcher.Match;
import net.xqhs.graphs.pattern.GraphPattern;

/**
 * @author Andrei Olaru
 */
public interface ContinuousContextMatchingPlatform
{
	public interface MatchNotificationReceiver
	{
		public void receiveMatchNotification(ContinuousContextMatchingPlatform platform, Match m);
	}
	
	public ContinuousContextMatchingPlatform setContextGraph(ContextGraph graph);
	
	public ContinuousContextMatchingPlatform addContextPattern(ContextPattern pattern);
	
	public ContinuousContextMatchingPlatform removeContextPattern(ContextPattern pattern);
	
	public ContinuousContextMatchingPlatform setMatchNotificationTarget(int thresholdK,
			MatchNotificationReceiver receiver);
	
	public ContinuousContextMatchingPlatform setMatchNotificationTarget(GraphPattern pattern,
			MatchNotificationReceiver receiver);
	
	public ContinuousMatchingProcess startMatchingAgainstAllPatterns(Graph graph, MatchNotificationReceiver receiver);
	
	public ContinuousContextMatchingPlatform startContinuousMatching();
	
	public ContinuousContextMatchingPlatform stopContinuousMatching();
	
	public boolean isContinuouslyMatching();
	
	public void printindexes();
}
