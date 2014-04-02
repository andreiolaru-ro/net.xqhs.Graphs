package net.xqhs.graphs.context;

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
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.util.logging.Unit;

public class CCMImplementation extends Unit implements ContinuousContextMatchingPlatform
{
	class MatchNotificationTarget
	{
		int							k;
		MatchNotificationReceiver	receiver;
		
		public MatchNotificationTarget(int maxK, MatchNotificationReceiver notificationReceiver)
		{
			k = maxK;
			receiver = notificationReceiver;
		}
	}
	
	// MonitorPack monitor = new MonitorPack();
	TimeKeeper										theTime;
	GraphMatchingPlatform							matchingPlatform	= new GMPImplementation();
	boolean											continuousMatching	= false;
	Map<GraphPattern, Set<MatchNotificationTarget>>	notificationTargets	= new HashMap<GraphPattern, Set<MatchNotificationTarget>>();
	
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
	public CCMImplementation setMatchNotificationTarget(int thresholdK, MatchNotificationReceiver receiver)
	{
		if(!notificationTargets.containsKey(null))
			notificationTargets.put(null, new HashSet<MatchNotificationTarget>());
		notificationTargets.get(null).add(new MatchNotificationTarget(thresholdK, receiver));
		return this;
	}
	
	@Override
	public CCMImplementation setMatchNotificationTarget(GraphPattern pattern, MatchNotificationReceiver receiver)
	{
		if(!notificationTargets.containsKey(pattern))
			notificationTargets.put(pattern, new HashSet<MatchNotificationTarget>());
		notificationTargets.get(pattern).add(new MatchNotificationTarget(-1, receiver));
		return this;
	}
	
	@Override
	public ContinuousMatchingProcess startMatchingAgainstAllPatterns(Graph graph, MatchNotificationReceiver receiver)
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
				for(Entry<GraphPattern, Set<MatchNotificationTarget>> entry : notificationTargets.entrySet())
					if((entry.getKey() == null) || (entry.getKey() == m.getPattern()))
						for(MatchNotificationTarget tg : entry.getValue())
							if((tg.k < 0) || (m.getK() <= tg.k))
								tg.receiver.receiveMatchNotification(this, m);
		}
	}
	
	protected void notifyContextChange()
	{
		if(continuousMatching)
		{
			getMatching();
		}
	}
	
	// @Override
	// public void printindexes()
	// {
	// matchingPlatform.printindexes();
	// }
}
