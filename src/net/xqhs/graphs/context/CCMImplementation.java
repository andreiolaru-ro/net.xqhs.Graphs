package net.xqhs.graphs.context;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.xqhs.graphs.context.Instant.TimeKeeper;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.matcher.Match;
import net.xqhs.graphs.matcher.MonitorPack;
import net.xqhs.graphs.matchingPlatform.GMPImplementation;
import net.xqhs.graphs.matchingPlatform.GraphMatchingPlatform;
import net.xqhs.graphs.matchingPlatform.TrackingGraph;
import net.xqhs.graphs.matchingPlatform.TrackingGraph.ChangeNotificationReceiver;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.util.logging.Unit;

public class CCMImplementation extends Unit implements ContinuousContextMatchingPlatform, ChangeNotificationReceiver
{
	class MatchNotificationTarget
	{
		int							k;
		boolean						allMatches;
		MatchNotificationReceiver	receiver;

		public MatchNotificationTarget(int maxK, MatchNotificationReceiver notificationReceiver)
		{
			k = maxK;
			allMatches = false;
			receiver = notificationReceiver;
		}
		
		public MatchNotificationTarget(MatchNotificationReceiver notificationReceiver)
		{
			k = -1;
			allMatches = true;
			// FIXME duplicate code
			receiver = notificationReceiver;
		}
	}

	// MonitorPack monitor = new MonitorPack();
	TimeKeeper											theTime;
	GraphMatchingPlatform								matchingPlatform	= new GMPImplementation();
	boolean												continuousMatching	= false;
	Map<ContextPattern, Set<MatchNotificationTarget>>	notificationTargets	= new HashMap<ContextPattern, Set<MatchNotificationTarget>>();

	public CCMImplementation(TimeKeeper time, MonitorPack monitorLink)
	{
		theTime = time;
		if(monitorLink != null)
			// monitor = monitorLink;
			((GMPImplementation) matchingPlatform).setMonitor(monitorLink);
	}

	@Override
	public CCMImplementation setContextGraph(ContextGraph graph)
	{
		graph.setTimeKeeper(theTime);
		matchingPlatform.setPrincipalGraph(graph);
		graph.registerChangeNotificationReceiver(this);
		getMatching();
		return this;
	}

	@Override
	public CCMImplementation addContextPattern(ContextPattern pattern)
	{
		matchingPlatform.addPattern((GraphPattern) pattern.lock());
		getMatching();
		return this;
	}

	@Override
	public CCMImplementation removeContextPattern(ContextPattern pattern)
	{
		matchingPlatform.removePattern(pattern);
		return this;
	}

	@Override
	public ContinuousMatchingProcess addMatchNotificationTarget(MatchNotificationReceiver receiver)
	{
		// FIXME duplicate code
		if(!notificationTargets.containsKey(null))
			notificationTargets.put(null, new HashSet<MatchNotificationTarget>());
		notificationTargets.get(null).add(new MatchNotificationTarget(receiver));
		return this;
	}
	
	@Override
	public CCMImplementation addMatchNotificationTarget(int thresholdK, MatchNotificationReceiver receiver)
	{
		if(!notificationTargets.containsKey(null))
			notificationTargets.put(null, new HashSet<MatchNotificationTarget>());
		notificationTargets.get(null).add(new MatchNotificationTarget(thresholdK, receiver));
		return this;
	}

	@Override
	public CCMImplementation addMatchNotificationTarget(ContextPattern pattern, MatchNotificationReceiver receiver)
	{
		if(!notificationTargets.containsKey(pattern))
			notificationTargets.put(pattern, new HashSet<MatchNotificationTarget>());
		notificationTargets.get(pattern).add(new MatchNotificationTarget(-1, receiver));
		return this;
	}

	@Override
	public ContinuousMatchingProcess removeMatchNotificationTarget(MatchNotificationReceiver receiver)
	{
		for(Set<MatchNotificationTarget> targetSet : notificationTargets.values())
			targetSet.remove(receiver);
		return this;
	}

	@Override
	public ContinuousMatchingProcess startMatchingAgainstAllPatterns(Graph graph, int thresholdK,
			MatchNotificationReceiver receiver)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ContinuousMatchingProcess startMatchingAgainstGraph(Graph pattern, int thresholdK,
			MatchNotificationReceiver receiver)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CCMImplementation startContinuousMatching()
	{
		continuousMatching = true;
		getMatching();
		return this;
	}

	@Override
	public CCMImplementation stopContinuousMatching()
	{
		continuousMatching = false;
		// TODO stop thread
		return null;
	}

	@Override
	public boolean isContinuouslyMatching()
	{
		return continuousMatching;
	}

	protected void getMatching()
	{
		while(matchingPlatform.getMathingSequence() < matchingPlatform.getGraphSequence())
		{
			Set<Match> matches = matchingPlatform.incrementSequence();
			for(Match m : matches)
				for(Entry<ContextPattern, Set<MatchNotificationTarget>> entry : notificationTargets.entrySet())
					if((entry.getKey() == null) || (entry.getKey() == m.getPattern()))
						for(MatchNotificationTarget tg : entry.getValue())
							if(tg.allMatches || (m.getK() <= tg.k))
								tg.receiver.receiveMatchNotification(this, m);
		}
	}

	@Override
	public void notifyChange()
	{
		if(continuousMatching)
		{
			getMatching();
		}
	}

	@Override
	public TrackingGraph getContextGraphShadow()
	{
		return ((TrackingGraph) matchingPlatform.getPrincipalGraph()).createShadow();
	}

	@Override
	public Collection<ContextPattern> getContextPatterns()
	{
		Collection<ContextPattern> ret = new HashSet<ContextPattern>();
		for(GraphPattern p : matchingPlatform.getPatterns())
			ret.add((ContextPattern) p);
		return ret;
	}

	@Override
	public TimeKeeper getTimeKeeper()
	{
		return theTime;
	}

	// @Override
	// public void printindexes()
	// {
	// matchingPlatform.printindexes();
	// }
}
