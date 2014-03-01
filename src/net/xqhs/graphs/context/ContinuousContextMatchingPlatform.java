package net.xqhs.graphs.context;

import java.util.Collection;

import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.matchingPlatform.GraphMatchingPlatform;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.util.config.Configurable;

/**
 * Implementations of this interface serve as platforms that handle continuous or on-demand matching of various graphs
 * and patterns against a designated, 'principal' {@link Graph}.
 * <p>
 * The platform is associated with the principal graph throughout its entire lifetime, after the first matching has been
 * initialized (the {@link Configurable} paradigm is used).
 * 
 * @author Andrei Olaru
 */
public interface ContinuousContextMatchingPlatform extends GraphMatchingPlatform
{
	public interface MatchNotifcationReceiver
	{
		
	}
	
	public ContinuousContextMatchingPlatform setMatchNotificationTarget(MatchNotifcationReceiver receiver);
	
	public ContinuousContextMatchingPlatform setMatchNotificationTarget(GraphPattern pattern,
			MatchNotifcationReceiver receiver);
	
	public ContinuousContextMatchingPlatform setMatchNotificationTarget(Collection<GraphPattern> patterns,
			MatchNotifcationReceiver receiver);
	
	public ContinuousMatchingProcess startMatchingAgainstAllPatterns(Graph graph, MatchNotifcationReceiver receiver);
	
	public ContinuousContextMatchingPlatform startContinuousMatching();
	
	public ContinuousContextMatchingPlatform stopContinuousMatching();
	
	public boolean isContinuouslyMatching();
}
