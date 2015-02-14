package net.xqhs.graphs.context;

import java.util.Collection;

import net.xqhs.graphs.context.Instant.TimeKeeper;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.matcher.Match;
import net.xqhs.graphs.matchingPlatform.TrackingGraph;
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
public interface ContinuousContextMatchingPlatform extends ContinuousMatchingProcess
{
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

	/**
	 * Adds a notification target for matches of the specified pattern.
	 *
	 * @param pattern
	 *            - the {@link GraphPattern} whose new matches will be notified to the receiver.
	 * @param receiver
	 *            - the receiver of notifications.
	 * @return the platform itself.
	 */
	public ContinuousContextMatchingPlatform addMatchNotificationTarget(ContextPattern pattern,
			MatchNotificationReceiver receiver);

	/**
	 * Creates a new {@link ContinuousMatchingProcess}, unrelated to the platform except for the set of patterns, to
	 * match all patterns against the specified {@link Graph} instance.
	 * <p>
	 * The matching process is started immediately
	 * <p>
	 * When matches of any <i>k</i> below or equal to the specified threshold are detected, the notification receiver is
	 * invoked.
	 *
	 * @param graph
	 *            - the graph to match the patterns against.
	 * @param thresholdK
	 *            - the threshold <i>k</i> (see {@link Match}).
	 * @param receiver
	 *            - the receiver for match notifications.
	 * @return the newly created and started {@link ContinuousMatchingProcess} instance.
	 */
	public ContinuousMatchingProcess startMatchingAgainstAllPatterns(Graph graph, int thresholdK,
			MatchNotificationReceiver receiver);

	/**
	 * Creates a new {@link ContinuousMatchingProcess}, unrelated to the platform except for the current sequence of the
	 * Context Graph (not the matching sequence), that matches the context graph against the specified pattern.
	 * <p>
	 * Matching is done against a shadow of the context graph. Any subsequent changes to the context graph will not be
	 * visible in the matching process.
	 * <p>
	 * Except for the snapshot of the context graph, this method uses no resources of the platform.
	 * <p>
	 * The matching process is started immediately.
	 * <p>
	 * When matches of any <i>k</i> below or equal to the specified threshold are detected, the notification receiver is
	 * invoked.
	 *
	 * @param pattern
	 *            - the pattern to match (can be a normal {@link Graph}).
	 * @param thresholdK
	 *            - the threshold <i>k</i> (see {@link Match}).
	 * @param receiver
	 *            - the receiver for match notifications.
	 * @return the newly created and started {@link ContinuousMatchingProcess} instance.
	 */
	public ContinuousMatchingProcess startMatchingAgainstGraph(Graph pattern, int thresholdK,
			MatchNotificationReceiver receiver);
	
	public TrackingGraph getContextGraphShadow();
	
	public Collection<ContextPattern> getContextPatterns();

	public TimeKeeper getTimeKeeper();

	// DEBUG ONLY
	// public void printindexes();
}
