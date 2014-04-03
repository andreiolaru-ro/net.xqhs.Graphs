package net.xqhs.graphs.context;

import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.matcher.Match;
import net.xqhs.graphs.pattern.GraphPattern;

/**
 * The interface is meant to be implemented by classes offering complete, real-time (asynchronous) context matching
 * functionality.
 * <p>
 * An implementation relies on a principal, context graph, and a set of context patterns. A class using the
 * implementation is able to set notification receivers in case that matches are identified.
 * 
 * @author Andrei Olaru
 */
public interface ContinuousContextMatchingPlatform
{
	
	/**
	 * An implementation is able to act as a receiver for match notifications.
	 * <p>
	 * It is recommended that implementations return quickly from the receiver method and delegate any lengthy
	 * processing to a different thread.
	 * 
	 * @author Andrei Olaru
	 */
	public interface MatchNotificationReceiver
	{
		/**
		 * The method is called by a {@link ContinuousContextMatchingPlatform} when a match is detected that conforms to
		 * the notification settings.
		 * 
		 * @param platform
		 *            - the platform that issued the notification.
		 * @param m
		 *            - the match for which the notification has been issued.
		 */
		public void receiveMatchNotification(ContinuousContextMatchingPlatform platform, Match m);
	}
	
	/**
	 * Assigns a new context graph to this implementation. It will also set the time keeper of the graph to the time
	 * keeper of the platform.
	 * <p>
	 * The platform will begin matching the current set of patterns to the new graph immediately.
	 * 
	 * @param graph
	 *            - the {@link ContextGraph}.
	 * @return the platform itself.
	 */
	public ContinuousContextMatchingPlatform setContextGraph(ContextGraph graph);
	
	/**
	 * Adds a context pattern to the set of context patterns, if not already existing.
	 * <p>
	 * The pattern will not allow modifications after this method is called (it will be {@link GraphPattern#locked()}.
	 * <p>
	 * Matching against the pattern will begin immediately.
	 * 
	 * @param pattern
	 *            - the {@link GraphPattern} to add.
	 * @return the platform itself.
	 */
	public ContinuousContextMatchingPlatform addContextPattern(ContextPattern pattern);
	
	/**
	 * Removes a pattern from the platform.
	 * 
	 * @param pattern
	 *            - the {@link ContextPattern} to remove.
	 * @return the platform itself.
	 */
	public ContinuousContextMatchingPlatform removeContextPattern(ContextPattern pattern);
	
	public ContinuousContextMatchingPlatform setMatchNotificationTarget(int thresholdK,
			MatchNotificationReceiver receiver);
	
	public ContinuousContextMatchingPlatform setMatchNotificationTarget(GraphPattern pattern,
			MatchNotificationReceiver receiver);
	
	public ContinuousMatchingProcess startMatchingAgainstAllPatterns(Graph graph, MatchNotificationReceiver receiver);
	
	public ContinuousContextMatchingPlatform startContinuousMatching();
	
	public ContinuousContextMatchingPlatform stopContinuousMatching();
	
	public boolean isContinuouslyMatching();
	
	// DEBUG ONLY
	// public void printindexes();
}
