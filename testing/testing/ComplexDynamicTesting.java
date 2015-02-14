package testing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.graphs.context.CCMImplementation;
import net.xqhs.graphs.context.ContextGraph;
import net.xqhs.graphs.context.ContextGraph.ContextEdge;
import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.context.ContinuousContextMatchingPlatform;
import net.xqhs.graphs.context.ContinuousMatchingProcess;
import net.xqhs.graphs.context.ContinuousMatchingProcess.MatchNotificationReceiver;
import net.xqhs.graphs.context.Instant;
import net.xqhs.graphs.context.Instant.Offset;
import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.graph.SimpleNode;
import net.xqhs.graphs.matcher.Match;
import net.xqhs.graphs.matcher.MonitorPack;
import net.xqhs.graphs.matchingPlatform.TrackingGraph;
import net.xqhs.graphs.matchingPlatform.Transaction;
import net.xqhs.graphs.matchingPlatform.Transaction.Operation;
import net.xqhs.graphs.util.Debug.D_G;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.logging.Logging;
import testing.ContextGraphsTest.IntTimeKeeper;

@SuppressWarnings("javadoc")
public class ComplexDynamicTesting extends Tester
{
	public enum Location {
		BATHROOM(5, "Bathroom"),
		
		LIVINGROOM(20, "Living"),
		
		HALL(10, "Hall"),
		
		KITCHEN(10, "Kitchen"),
		
		;
		
		static Location[]	track	= new Location[] { BATHROOM, LIVINGROOM, HALL, KITCHEN };
		
		/**
		 * Time to cross the room, in seconds.
		 */
		int					crossingTime;
		/**
		 * The graph edge corresponding to the detection of the presence in this location.
		 */
		Edge				edge;
		
		/**
		 * @param time
		 *            - Time to cross the room, in seconds.
		 * @param graphNode
		 *            - name of the node in the graph that corresponds to the location.
		 */
		private Location(int time, String graphNode)
		{
			crossingTime = time;
			edge = new ContextEdge(new SimpleNode(USER), new SimpleNode(graphNode), LOCATION_EDGE, LOCATION_VALIDITY);
		}
		
		public List<Location> pathTo(Location target)
		{
			List<Location> ret = new ArrayList<Location>();
			int targetIdx = 0, sourceIdx = 0;
			for(int i = 0; i < track.length; i++)
			{
				if(track[i] == this)
					sourceIdx = i;
				if(track[i] == target)
					targetIdx = i;
			}
			int direction = (int) Math.signum(targetIdx - sourceIdx);
			if(direction == 0)
				// nowhere to go
				return ret;
			for(int i = sourceIdx; i != targetIdx; i += direction)
				ret.add(track[i]);
			ret.add(track[targetIdx]);
			
			return ret;
		}
		
		public int getCrossingTime()
		{
			return crossingTime;
		}
		
		public GraphComponent getGraphEdge()
		{
			return edge;
		}

		public static Location findLocation(TrackingGraph graph)
		{
			Collection<Edge> edges = graph.getEdges();
			String locationName = null;
			for(Edge e : edges)
				if(e.getLabel().equals(LOCATION_EDGE))
					locationName = e.getTo().getLabel();
			for(Location l : values())
				if(l.edge.getTo().getLabel().equals(locationName))
					return l;
			return null;
		}
	}
	
	/**
	 * Each instance describes an activity, that has a certain duration, a certain location, and a certain medium number
	 * of times it happens per day.
	 *
	 * @author Andrei Olaru
	 */
	public static class Activity
	{
		static
		{
			loadActivities();
		}
		
		public static Activity			BATHROOM, SHOWER, MEAL, WANDER;
		
		private static List<Activity>	values;
		
		String							activityName;
		Offset							period;
		Instant							last;
		boolean							isExclusive;
		float							sleepFactor;
		List<Transaction>				timeline;
		Location						location;
		Offset							duration;
		
		private Activity(String name, int activityPeriod, int lastOccurence)
		{
			this(name, activityPeriod, lastOccurence, false, 0);
		}
		
		private Activity(String name, int activityPeriod, int lastOccurence, boolean setExclusive, float factor)
		{
			activityName = name;
			period = new Offset(activityPeriod * HOUR);
			last = new Instant(lastOccurence * HOUR);
			isExclusive = setExclusive;
			sleepFactor = factor;
			timeline = new ArrayList<Transaction>();

			values.add(this);
		}
		
		private Activity setLocation(Location where)
		{
			location = where;
			return this;
		}
		
		private Activity setDuration(Offset time)
		{
			duration = time;
			return this;
		}
		
		private void setOperations(long timelineStep, long fillLength, Operation op, GraphComponent... components)
		{
			int t;
			for(t = timeline.size(); t <= timelineStep + fillLength - 1; t++)
				timeline.add(new Transaction());
			t = 0;
			for(Transaction transaction : timeline)
			{
				if((t >= timelineStep) && (t <= timelineStep + fillLength - 1))
				{
					for(GraphComponent comp : components)
						transaction.put(comp, op);
				}
				t++;
			}
		}
		
		public List<Transaction> makeInstance(Location currentLocation)
		{
			List<Transaction> ret = new ArrayList<Transaction>();
			if(this == WANDER)
			{
				// TODO
			}
			else
			{
				if(location != null)
					for(Location room : currentLocation.pathTo(location))
						for(int i = 0; i < room.getCrossingTime(); i++)
							ret.add(new Transaction(room.getGraphEdge(), Operation.ADD));
				ret.addAll(timeline);
				if((location != null)) // TODO and if should return
					for(Location room : location.pathTo(currentLocation))
						for(int i = 0; i < room.getCrossingTime(); i++)
							ret.add(new Transaction(room.getGraphEdge(), Operation.ADD));
			}
			return ret;
		}
		
		public boolean isTime(Instant now, Activity current)
		{
			// System.out.println("is " + now.toString() + " time? " + this.toString());
			int per;
			if(Event.isAwake(now))
				per = (int) period.toLong();
			else if(sleepFactor > 0)
				per = (int) (period.toLong() * sleepFactor);
			else
				return false;
			
			int pressure = (int) (last.getOffsetTo(now).toLong() / per * 100);
			return !((current != null) && current.isExclusive) && (randomGenerator.nextInt(Math.max(1, pressure)) > 80);
		}
		
		@Override
		public String toString()
		{
			return this.activityName + " " + (isExclusive ? "X" : "") + "|" + period.toStringPrint() + "/S"
					+ sleepFactor + "(" + last.toStringPrint() + ")";
		}
		
		private static void loadActivities()
		{
			values = new ArrayList<Activity>();
			
			BATHROOM = new Activity("BATHROOM", 4, -6, true, .5f).setLocation(Location.BATHROOM).setDuration(
					new Offset(2 * MINUTE));
			BATHROOM.timeline.add(new Transaction(new ContextEdge(new SimpleNode(USER), new SimpleNode("Bathroom"),
					"near", LOCATION_VALIDITY), Operation.ADD));
			BATHROOM.setOperations(1, BATHROOM.duration.toLong(), null);
			BATHROOM.timeline.add(new Transaction(new ContextEdge(new SimpleNode(USER), new SimpleNode("Bathroom"),
					"near", LOCATION_VALIDITY), Operation.ADD));
			
			SHOWER = new Activity("SHOWER", 20, -12).setLocation(Location.BATHROOM)
					.setDuration(new Offset(20 * MINUTE));
			SHOWER.timeline.add(new Transaction(new ContextEdge(new SimpleNode(USER), new SimpleNode("Bathroom"),
					"near", LOCATION_VALIDITY), Operation.ADD));
			SHOWER.setOperations(1, SHOWER.duration.toLong(), null);
			SHOWER.timeline.add(new Transaction(new ContextEdge(new SimpleNode(USER), new SimpleNode("Bathroom"),
					"near", LOCATION_VALIDITY), Operation.ADD));
			
			MEAL = new Activity("MEAL", 8, -4).setLocation(Location.KITCHEN).setDuration(new Offset(30 * MINUTE));
			
			// WANDER = new Activity(7, -8);
		}
		
		public static Collection<Activity> values()
		{
			return values;
		}
	}
	
	/**
	 * Each instance describes an event, that happens at a particular time in the day.
	 *
	 * @author Andrei Olaru
	 */
	public enum Event {
		
		MIDNIGHT(24),
		
		WAKEUP(7),
		
		GOTOBED(22),
		
		;
		
		/**
		 * Time of the event, in seconds.
		 */
		Instant	time;
		
		/**
		 *
		 *
		 * @param eventTime
		 *            in hours, but can be a fraction (e.g. 14.5 means half past 14)
		 */
		private Event(float eventTime)
		{
			time = new Instant((long) (HOUR * eventTime));
		}
		
		// private int time()
		// {
		// return time;
		// }
		
		public boolean happenedSince(Instant now, Instant since)
		{
			return (time.after(since) && time.before(now)) || time.equals(now);
		}
		
		public static boolean isAwake(Instant time)
		{
			return time.after(WAKEUP.time) && time.before(GOTOBED.time);
		}
	}
	
	public class Simulator extends TimerTask
	{
		TrackingGraph		graph;
		/**
		 * Keeps seconds.
		 */
		IntTimeKeeper		time;
		MonitorPack			monitor;
		Timer				t;
		int					multiplier;
		boolean				inTask			= false;
		/**
		 * frequency of updates and actions, in virtual time (in seconds).
		 */
		final static int	FREQUENCY		= 1;
		
		Activity			stackedActivity	= null;
		Activity			current;
		Deque<Transaction>	todo			= new LinkedList<Transaction>();
		
		public Simulator(TrackingGraph CG, IntTimeKeeper timeKeeper, int compression, MonitorPack mon)
		{
			graph = CG;
			time = timeKeeper;
			multiplier = compression;
			monitor = mon;
			
			t = new Timer();
			t.scheduleAtFixedRate(this, INITIAL_DELAY * SECOND2MILLIS, FREQUENCY * SECOND2MILLIS / compression);
		}
		
		@Override
		public void run()
		{
			if(inTask)
				throw new IllegalStateException("timer tasks overlapped");
			inTask = true;
			
			Instant timeLast = time.now();
			time.tickUp();
			Instant timeNow = time.now();
			long timeLong = timeNow.toLong();
			if((timeLong % HOUR) == 0)
			{
				printSeparator(0, "hour start [" + Instant.printTime(timeLong) + "] [" + current + "][] ");
				log.lf("CG: []", graph);
				log.lf(monitor.printStats());
			}
			
			for(Event event : Event.values())
				if(event.happenedSince(timeNow, timeLast))
					log.li("Event: ", event);
			
			for(Activity activity : Activity.values())
				if(activity.isTime(timeNow, current))
				{
					log.li("Time [] is for []", timeNow.toStringPrint(), activity);
					
					if(current != null)
						stackedActivity = current;
					current = activity;
					activity.last = timeNow;
					Location location = Location.findLocation(graph);
					List<Transaction> newTodo = new ArrayList<Transaction>();
					newTodo.addAll(current.makeInstance(location));
					newTodo.addAll(todo);
					todo = new LinkedList<Transaction>(newTodo);
				}
			if(!todo.isEmpty())
			{
				Transaction tr = todo.pollFirst();
				log.dbg(D_G.D_TEST_GRAPH_CONSTRUCTION, "executing ", tr);
				graph.applyTransaction(tr);
				log.dbg(D_G.D_TEST_GRAPH_CONSTRUCTION, "CG: []", graph);
			}
			else if(stackedActivity != null)
				current = stackedActivity;
			else
				current = null;
			
			inTask = false;
		}
	}
	
	public static int		HOUR				= 3600;
	public static int		MINUTE				= 60;
	public static int		HOUR2MINUTES		= 60;
	public static int		SECOND2MILLIS		= 1000;
	
	public static int		INITIAL_DELAY		= 1;
	
	static final String		USER				= "Emily";
	static final String		LOCATION_EDGE		= "is-in";
	static final Offset		LOCATION_VALIDITY	= new Offset(5);
	protected static long	seed				= System.currentTimeMillis();
	// protected long seed = 1399038417008L; // save
	protected static Random	randomGenerator		= new Random(seed);
	
	protected IntTimeKeeper	ticker;
	
	@Override
	protected void doTesting()
	{
		testGraphName = Tester.NAME_GENERAL_GRAPH + "#" + 0;
		log.setLogLevel(Level.ALL);
		try
		{
			initiate("platform/house2");
		} catch(IOException e)
		{
			e.printStackTrace();
		}
		
		try
		{
			synchronized(this)
			{
				this.wait();
			}
		} catch(InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	protected ContinuousContextMatchingPlatform initiate(String file) throws IOException
	{
		ticker = new IntTimeKeeper();
		MonitorPack monitor = new MonitorPack(); // .setLog(log);
		
		// prepare CCM
		ContinuousContextMatchingPlatform CCM = new CCMImplementation(ticker, monitor);
		
		Map<String, Graph> testPack = loadGraphsAndPatterns(file, null, null);
		
		// CCM setup
		CCM.addMatchNotificationTarget(2, new MatchNotificationReceiver() {
			@Override
			public void receiveMatchNotification(ContinuousMatchingProcess platform, Match m)
			{
				getLog().dbg(D_G.D_TEST_MATCH_NOTIFICATION, "new match: [][]", ticker.now().toStringPrint(), m);
			}
		});
		
		log.lf("loaded graphs");
		
		ContextGraph CG = (ContextGraph) new ContextGraph().addAll(getTestGraph(testPack).getComponents());
		CCM.setContextGraph(CG);
		log.lf("CG set");
		for(ContextPattern pattern : getContextPatterns(testPack))
			CCM.addContextPattern(pattern);
		log.lf("Patterns set");
		
		CCM.startContinuousMatching();
		
		@SuppressWarnings("unused")
		Simulator simulator = new Simulator(CG, ticker, 1000, monitor);
		
		printSeparator(1, "initiated");
		
		return CCM;
	}
	
	public static void main(String[] args)
	{
		Logging.getMasterLogging().setLogLevel(Level.ALL);

		@SuppressWarnings("unused")
		ComplexDynamicTesting complexDynamicTesting = new ComplexDynamicTesting();
	}
}
