/*******************************************************************************
 * Copyright (C) 2013 Andrei Olaru.
 *
 * This file is part of net.xqhs.Graphs.
 *
 * net.xqhs.Graphs is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * net.xqhs.Graphs is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with net.xqhs.Graphs.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package testing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.xqhs.graphs.context.CCMImplementation;
import net.xqhs.graphs.context.ContextGraph;
import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.context.ContinuousContextMatchingPlatform;
import net.xqhs.graphs.context.ContinuousMatchingProcess;
import net.xqhs.graphs.context.ContinuousMatchingProcess.MatchNotificationReceiver;
import net.xqhs.graphs.context.Instant;
import net.xqhs.graphs.context.Instant.Offset;
import net.xqhs.graphs.context.Instant.TickReceiver;
import net.xqhs.graphs.context.Instant.TimeKeeper;
import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleEdge;
import net.xqhs.graphs.matcher.Match;
import net.xqhs.graphs.matcher.MonitorPack;
import net.xqhs.graphs.matchingPlatform.GMPImplementation;
import net.xqhs.graphs.matchingPlatform.GMPImplementation.PrincipalGraph;
import net.xqhs.graphs.matchingPlatform.GraphMatcherPersistent;
import net.xqhs.graphs.matchingPlatform.GraphMatchingPlatform;
import net.xqhs.graphs.matchingPlatform.GraphMatchingPlatform.PlatformPrincipalGraph;
import net.xqhs.graphs.matchingPlatform.TrackingGraph;
import net.xqhs.graphs.matchingPlatform.Transaction;
import net.xqhs.graphs.matchingPlatform.Transaction.Operation;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.representation.text.TextGraphRepresentation;
import net.xqhs.graphs.representation.text.TextRepresentationElement.Symbol;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.logging.Logging;

/**
 * Tester for complex graphs, hyper graphs, transaction graphs, context graphs, and matching platforms.
 *
 * @author Andrei Olaru
 */
public class ContextGraphsTest extends Tester
{
	@SuppressWarnings("javadoc")
	enum ActualState {
		SLEEP, MEAL, WANDERING, SHOWER, BATHROOM, TV, NOTHING,

		;

		boolean exclusive()
		{
			return (this == SHOWER) || (this == BATHROOM);
		}

		boolean mustContinue()
		{
			return (this == MEAL) || (this == TV) || (this == SLEEP);
		}

		boolean idle()
		{
			return (this == WANDERING) || (this == NOTHING);
		}
	}

	/**
	 * Time keeper having an integer time coordinate, incremented at request.
	 *
	 * @author Andrei Olaru
	 */
	public static class IntTimeKeeper implements TimeKeeper
	{
		/**
		 * Tick receivers.
		 */
		Set<TickReceiver>	receivers	= new HashSet<TickReceiver>();
		/**
		 * Time coordinate.
		 */
		long				now			= 0;

		@Override
		public void registerTickReceiver(TickReceiver receiver, Offset tickLength)
		{
			receivers.add(receiver);
		}

		@Override
		public Instant now()
		{
			return new Instant(now);
		}

		/**
		 * Increments the time and notifies receivers (regardless of their tick length preference. //FIXME
		 */
		public void tickUp()
		{
			now++;
			for(TickReceiver rcv : receivers)
				rcv.tick(this, new Instant(now));
		}

	}

	/**
	 * Member used in context matching tester (2)
	 */
	long	lastBathroomTime	= 0;
	/**
	 * Member used in context matching tester (2)
	 */
	String	detectedState		= null;

	/**
	 * @param args
	 *            unused
	 */
	@SuppressWarnings("unused")
	public static void main(String args[])
	{
		new ContextGraphsTest();
	}

	@Override
	protected void doTesting()
	{
		testGraphName = Tester.NAME_GENERAL_GRAPH + "#" + 0;

		super.doTesting();

		Logging.getMasterLogging().setLogLevel(Level.WARN);

		// testTransactions();

		// testTrackingGraph(-1);

		defaultFileDir = "playground/platform/";

		try
		{
			// testPersistentMatching("bathroom-time-1");

			// testContextMatching1("bathroom-time-1");

			testContextMatching2("house");

		} catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Test adding operations to {@link Transaction} instances.
	 */
	protected void testTransactions()
	{
		printSeparator(-2, "transactions");
		Graph GT = GrapherTest.staticTest(1);
		List<GraphComponent> compsL = new ArrayList<GraphComponent>();
		compsL.addAll(GT.getNodes());
		compsL.addAll(GT.getEdges());
		GraphComponent comps[] = compsL.toArray(new GraphComponent[0]);

		Transaction t = new Transaction(comps[0], Operation.ADD); // single direct
		log.lf(t.toString());
		t.put(comps[4], Operation.ADD); // single to multiple
		t.put(comps[0], Operation.REMOVE); // multiple
		log.lf(t.toString());
		log.lf("=== clear");
		t.clear();
		t.put(comps[0], Operation.REMOVE); // empty to single
		log.lf(t.toString());
		Map<GraphComponent, Operation> ops = new HashMap<GraphComponent, Operation>();
		ops.put(comps[1], Operation.REMOVE);
		ops.put(comps[3], Operation.ADD);
		ops.put(comps[0], Operation.ADD);
		Map<GraphComponent, Operation> ops2 = new HashMap<GraphComponent, Operation>();
		ops2.put(comps[5], Operation.ADD);
		ops2.put(comps[2], Operation.REMOVE);
		ops2.put(comps[3], Operation.REMOVE);
		t.putAll(ops); // single to multiple; detect same component
		log.lf(t.toString());
		t.putAll(ops2); // multiple to multiple; detect same component
		log.lf(t.toString());
		log.lf("=== clear");
		t.clear();
		t.putAll(ops); // zero to multiple
		log.lf(t.toString());
		t = new Transaction(); // empty direct
		t.put(comps[4], Operation.ADD); // empty to single
		log.lf(t.toString());
		t = new Transaction(); // empty direct
		t.putAll(ops); // empty to multiple
		log.lf(t.toString());
		t.remove(comps[3]);
		log.lf(t.toString());
		printSeparator(2, "transactions");
	}

	/**
	 * Test correct workings of a tracking graph and shadows of the {@link TrackingGraph}.
	 *
	 * @param seedPre
	 *            - pre-set random seed; <code>-1</code> if none.
	 */
	protected void testTrackingGraph(long seedPre)
	{
		printSeparator(-2, "tracking graph");
		long seed = System.currentTimeMillis();
		if(seedPre >= 0)
			seed = seedPre;
		log.lf("seed was " + seed);
		Random rand = new Random(seed);

		TrackingGraph TG = new TrackingGraph();
		int nShadows = 3;
		TrackingGraph shadows[] = new TrackingGraph[3];
		for(int i = 0; i < nShadows; i++)
			shadows[i] = TG.createShadow();
		TrackingGraph lastShadow = shadows[nShadows - 1].createShadow();
		log.lf("parent graph: [].", TG.toString());

		// generate additions
		Graph GT = GrapherTest.staticTest(1);
		List<GraphComponent> compsL = new ArrayList<GraphComponent>();
		compsL.addAll(GT.getNodes());
		compsL.addAll(GT.getEdges());
		GraphComponent comps[] = compsL.toArray(new GraphComponent[0]);
		boolean addedComps[] = new boolean[comps.length];
		for(int i = 0; i < comps.length; i++)
			addedComps[i] = false;
		List<Transaction> ts = new ArrayList<Transaction>();
		int added = 0;
		while(added < comps.length)
		{
			int toAdd = Math.min(rand.nextInt(3) + 1, comps.length - added);
			Transaction t = new Transaction();
			for(int i = 0; i < toAdd; i++)
			{
				int c = -1;
				while((c < 0) || addedComps[c])
					c = rand.nextInt(comps.length);
				t.put(comps[c], Operation.ADD);
				addedComps[c] = true;
			}
			ts.add(t);
			added += toAdd;
		}

		// initial presentation
		log.lf("=====");
		log.lf("\t\t[][]", TG, new Integer(TG.getSequence()));
		for(int i = 0; i < nShadows; i++)
			log.lf("\t\t[][]", shadows[i], new Integer(shadows[i].getSequence()));
		log.lf("\t\t[][]", lastShadow, new Integer(lastShadow.getSequence()));

		// start adding
		for(Transaction t : ts)
		{
			TG.applyTransaction(t);
			log.lf("applied []", t);
			log.lf("\t\t[][]", TG, new Integer(TG.getSequence()));
			for(int i = 0; i < nShadows; i++)
			{
				boolean update = rand.nextInt(nShadows) < nShadows / 3;
				boolean updateFull = rand.nextInt(nShadows) == 1;
				if(updateFull)
					shadows[i].incrementSequenceFastForward();
				else if(update)
					if(shadows[i].canIncrement())
						shadows[i].incrementSequence();
				log.lf("\t[]\t[][]", updateFull ? "sync" : (update ? "updtd" : "   "), shadows[i], new Integer(
						shadows[i].getSequence()));
			}
			if(lastShadow.canIncrement())
				lastShadow.incrementSequence();
			if(lastShadow.canIncrement() && rand.nextBoolean())
				lastShadow.incrementSequence();
			log.lf("\t\t[][]", lastShadow, new Integer(lastShadow.getSequence()));
		}
		// end up
		log.lf("=====");
		log.lf("\t\t[][]", TG, new Integer(TG.getSequence()));
		for(int i = 0; i < nShadows; i++)
		{
			shadows[i].incrementSequence(TG.getSequence() - 1);
			log.lf("\t\t[][]", shadows[i], new Integer(shadows[i].getSequence()));
		}
		log.lf("\t\t[][]", lastShadow, new Integer(lastShadow.getSequence()));
		printSeparator(2, "tracking graph");
	}

	/**
	 * Tests {@link GMPImplementation} and {@link GraphMatcherPersistent}.
	 *
	 * @param file
	 *            - input file containing the initial graph and all patterns.
	 *
	 * @throws IOException
	 */
	protected void testPersistentMatching(String file) throws IOException
	{
		printSeparator(-2, "persistent");

		Map<String, Graph> testPack = loadGraphsAndPatterns(file, null, null);

		// load graph
		PlatformPrincipalGraph CG = new PrincipalGraph();
		Graph g = getTestGraph(testPack);

		// load patterns
		List<GraphPattern> GPs = getGraphPatterns(testPack);

		// prepare GMP
		MonitorPack monitor = new MonitorPack();// .setLog(log);
		GraphMatchingPlatform GMP = new GMPImplementation().setMonitor(monitor).setPrincipalGraph(CG);
		for(GraphPattern pattern : GPs)
			GMP.addPattern(pattern);

		// prepare transactions
		long seed = System.currentTimeMillis();
		Random rand = new Random(seed);
		GraphComponent comps[] = g.getComponents().toArray(new GraphComponent[0]);
		boolean addedComps[] = new boolean[comps.length];
		for(int i = 0; i < comps.length; i++)
			addedComps[i] = false;
		List<Transaction> ts = new ArrayList<Transaction>();
		int added = 0;
		while(added < comps.length)
		{
			int toAdd = Math.min(rand.nextInt(3) + 1, comps.length - added);
			Transaction t = new Transaction();
			for(int i = 0; i < toAdd; i++)
			{
				int c = -1;
				while((c < 0) || addedComps[c])
					c = rand.nextInt(comps.length);
				t.put(comps[c], Operation.ADD);
				addedComps[c] = true;
			}
			ts.add(t);
			added += toAdd;
		}
		printSeparator(0, "");
		for(Transaction t : ts)
		{
			((TrackingGraph) CG).applyTransaction(t);
			log.li("CG: []", CG.toString());

			log.lf(GMP.incrementSequence().toString());
			log.li(monitor.printStats());
			for(GraphPattern pattern : GPs)
				for(int k = 0; k < pattern.maxK() - 1; k++)
					log.lf("matches for GP=[] k=[]: []", new TextGraphRepresentation(pattern).update().toString(),
							new Integer(k), GMP.getMatches(pattern, k));
			log.li(monitor.printStats());
			printSeparator(0, "");
		}
		// TODO: check removal of edges
		// TODO: check adding patterns later
		// TODO: check setting a new principal graph
		printSeparator(2, "persistent");
	}

	/**
	 * Tests context matching.
	 *
	 * @param file
	 *            - input file containing the initial graph and all patterns.
	 * @throws IOException
	 */
	protected void testContextMatching1(String file) throws IOException
	{
		printSeparator(-2, "context");

		// make ticker
		IntTimeKeeper ticker = new IntTimeKeeper();

		// prepare CCM
		MonitorPack monitor = new MonitorPack(); // .setLog(log);
		ContinuousContextMatchingPlatform CCM = new CCMImplementation(ticker, monitor);

		Map<String, Graph> testPack = loadGraphsAndPatterns(file, null, null);

		// load graph
		ContextGraph CG = new ContextGraph();
		CCM.setContextGraph(CG);
		Graph g = getTestGraph(testPack);
		log.li("graph: []", new TextGraphRepresentation(g).update());

		// load patterns
		List<ContextPattern> GPs = getContextPatterns(testPack);

		// CCM setup
		CCM.addMatchNotificationTarget(2, new MatchNotificationReceiver() {
			@Override
			public void receiveMatchNotification(ContinuousMatchingProcess platform, Match m)
			{
				getLog().li("new match: []", m);
			}
		});
		CCM.addMatchNotificationTarget(GPs.get(1), new MatchNotificationReceiver() {
			@Override
			public void receiveMatchNotification(ContinuousMatchingProcess platform, Match m)
			{
				getLog().li("new match for pattern 1: []", m);
			}
		});
		CCM.startContinuousMatching();
		CCM.setContextGraph(CG);
		for(ContextPattern pattern : GPs)
			CCM.addContextPattern(pattern);

		long seed = 1394727231768L; // System.currentTimeMillis();
		log.lf("seed: []", new Long(seed));
		Random rand = new Random(seed);
		GraphComponent comps[] = g.getComponents().toArray(new GraphComponent[0]);
		boolean addedComps[] = new boolean[comps.length];
		int added = 0;
		for(int i = 0; i < comps.length; i++)
			addedComps[i] = false;
		for(int tick = 0; tick < 50; tick++)
		{
			printSeparator(0, "tick start [" + tick + "]");

			int nToAdd = Math.min(rand.nextInt(3) + 1, comps.length - added);
			Transaction t = new Transaction();
			for(int i = 0; i < nToAdd; i++)
			{
				int c = -1;
				while((c < 0) || addedComps[c])
					c = rand.nextInt(comps.length);
				t.put(comps[c], Operation.ADD);
				if(comps[c] instanceof Edge)
				{
					Edge e = (Edge) comps[c];
					if(!CG.contains(e.getFrom()))
						t.put(e.getFrom(), Operation.ADD);
					if(!CG.contains(e.getTo()))
						t.put(e.getTo(), Operation.ADD);
				}
				addedComps[c] = true;
				added++;
			}

			int nToRem = Math.min(rand.nextInt(1) + 1, CG.m() - nToAdd);
			for(int i = 0; i < nToRem; i++)
			{
				int c = -1;
				while((c < 0) || !addedComps[c] || t.containsKey(comps[c]) || (comps[c] instanceof Node))
					c = rand.nextInt(comps.length);
				t.put(comps[c], Operation.REMOVE);
				addedComps[c] = false;
				added--;
			}

			log.lf("transaction: []", t);
			CG.applyTransaction(t);
			log.lf("CG: []", CG);
			// CCM.printindexes();
			log.lf(monitor.printStats());
			ticker.tickUp();
		}

		printSeparator(2, "context");
	}

	/**
	 * Tests context matching with some concrete scenarios.
	 *
	 * @param file
	 *            - input file containing the initial graph and all patterns.
	 * @throws IOException
	 */
	protected void testContextMatching2(String file) throws IOException
	{
		printSeparator(-2, "context2");

		// long seed = System.currentTimeMillis();
		long seed = 1399038417008L;
		log.lf("seed: []", new Long(seed));
		Random rand = new Random(seed);

		// make ticker
		final IntTimeKeeper ticker = new IntTimeKeeper();

		// prepare CCM
		MonitorPack monitor = new MonitorPack(); // .setLog(log);
		ContinuousContextMatchingPlatform CCM = new CCMImplementation(ticker, monitor);

		Map<String, Graph> testPack = loadGraphsAndPatterns(file, null, null);
		Graph g = getTestGraph(testPack);

		// CCM setup
		CCM.addMatchNotificationTarget(2, new MatchNotificationReceiver() {
			@Override
			public void receiveMatchNotification(ContinuousMatchingProcess platform, Match m)
			{
				long h = ticker.now().toLong() / 60;
				long min = ticker.now().toLong() % 60;
				String time = (h >= 10 ? "" + h : "0" + h) + "." + (min >= 10 ? "" + min : "0" + min);
				String pattern = m.getPattern().getDescription().toString();
				if(!pattern.equals("LIVINGROOM"))
					detectedState = pattern;
				if((detectedState != null) && detectedState.equals("BATHROOM"))
				{
					lastBathroomTime = ticker.now().toLong();
					// getLog().li("BATHROOM TIME");
				}
				getLog().li("new match: [][]", time, m);
			}
		});

		// String notificationTargetName = Tester.NAME_GENERAL_GRAPH + "#" + 1;
		// CCM.addMatchNotificationTarget(
		// (ContextPattern) new ContextPattern().addAll(testPack.get(notificationTargetName).getComponents()),
		// new MatchNotificationReceiver() {
		// @Override
		// public void receiveMatchNotification(ContinuousMatchingProcess platform, Match m)
		// {
		// getLog().li("new match for pattern 1: []", m);
		// }
		// });
		ContextGraph CG = new ContextGraph();
		CCM.startContinuousMatching();
		CCM.setContextGraph(CG);
		for(ContextPattern pattern : getContextPatterns(testPack))
			CCM.addContextPattern(pattern);

		long ntime = 60 * 24;
		long wakeup = 60 * 7;
		long sleep = 60 * 22;

		long lastBathroom = -6 * 60;
		long lastShower = -12 * 60;
		long lastMeal = -4 * 60;
		long lastWander = -8 * 60;

		// boolean emergency = true;

		Node Emily = g.getNodesNamed("Emily").iterator().next();
		Node Living = g.getNodesNamed("Living").iterator().next();
		Node Bathroom = g.getNodesNamed("Bathroom").iterator().next();
		Node Hall = g.getNodesNamed("Hall").iterator().next();
		Node Kitchen = g.getNodesNamed("Kitchen").iterator().next();
		Edge locationLiving = new SimpleEdge(Emily, Living, "is-in");
		Edge locationHall = new SimpleEdge(Emily, Hall, "is-in");
		Edge locationKitchen = new SimpleEdge(Emily, Kitchen, "is-in");
		Edge nearBathroom = new SimpleEdge(Emily, Bathroom, "near");

		Edge[] track = new Edge[] { locationKitchen, locationHall, locationLiving };
		Edge cLocation = locationLiving;
		g.add(Living);
		g.add(cLocation);

		CG.addAll(g.getComponents());

		ActualState actualState = ActualState.SLEEP;
		ActualState stackedState = null;
		Edge stackedLocation = null;
		Deque<Transaction> todo = new LinkedList<Transaction>();
		Deque<Transaction> stackedTodos = new LinkedList<Transaction>();
		boolean justdone = false;
		for(long tick = 0; tick < ntime; tick++)
		{
			if((tick % 60) == 0)
			{
				// + ((tick - lastBathroom) / (4 * .6)));
				printSeparator(0, "hour start [" + tick / 60 + "] [" + actualState + "][" + stackedState + "] ");
				log.lf("CG: []", CG);
				log.lf(monitor.printStats());
			}

			if(!todo.isEmpty())
			{
				justdone = true;
				Transaction t = todo.pollFirst();
				CG.applyTransaction(t);
			}
			else if(justdone)
			{
				printSeparator(1, "done " + actualState);
				justdone = false;

				if(stackedState != null)
				{
					actualState = stackedState;
					stackedState = null;
					todo.addAll(stackedTodos);
					stackedTodos.clear();
				}
				else
					actualState = ActualState.NOTHING;
			}

			// current location and actual state
			String actStateLog = "\t actual state: [" + actualState + "] \t detected state: [" + detectedState + "]";
			long h = ticker.now().toLong() / 60;
			long min = ticker.now().toLong() % 60;
			actStateLog += "time:" + (h >= 10 ? "" + h : "0" + h) + "." + (min >= 10 ? "" + min : "0" + min);
			boolean found = false;
			for(Edge loc : track)
				if(CG.contains(loc))
				{
					found = true;
					if(loc != cLocation)
					{
						cLocation = loc;
						String locS = cLocation.toString();
						log.lf("location: []" + actStateLog,
								locS.substring(locS.indexOf(Symbol.EDGE_ENDING_FORWARD.toString()) + 1).toUpperCase());
					}
				}
			if(!found)
			{
				cLocation = null;
				log.lf("location: [none]" + actStateLog);
			}

			if(!actualState.exclusive()
					&& (rand.nextInt(Math.max(1, (int) ((tick - lastBathroom) / (((tick < wakeup) ? 8 : 4) * 0.6)))) > 80))
			{ // go to bathroom
				printSeparator(-1, "bathroom");
				if(actualState.mustContinue())
				{
					log.li("stacking []", actualState);
					stackedState = actualState;
					stackedTodos.addAll(todo);
				}
				actualState = ActualState.BATHROOM;
				stackedLocation = cLocation;
				todo.clear();
				for(int i = 0; i < track.length - 1; i++)
					if(track[i] == cLocation)
					{
						todo.add(new Transaction().putR(cLocation, Operation.REMOVE).putR(track[i + 1], Operation.ADD));
						cLocation = track[i + 1];
					}
				todo.add(new Transaction().putR(nearBathroom, Operation.ADD));
				todo.add(new Transaction().putR(nearBathroom, Operation.REMOVE).putR(locationLiving, Operation.REMOVE));
				todo.add(new Transaction());
				todo.add(new Transaction());
				todo.add(new Transaction().putR(nearBathroom, Operation.ADD).putR(locationLiving, Operation.ADD));
				todo.add(new Transaction().putR(nearBathroom, Operation.REMOVE));
				for(int i = track.length - 1; i > 0; i--)
				{
					if(cLocation == stackedLocation)
						break;
					if(track[i] == cLocation)
					{
						todo.add(new Transaction().putR(cLocation, Operation.REMOVE).putR(track[i - 1], Operation.ADD));
						cLocation = track[i - 1];
					}
				}
				stackedLocation = null;
				lastBathroom = tick;
			}

			if(!actualState.exclusive() && (tick > wakeup) && (tick < sleep)
					&& (rand.nextInt(Math.max(1, (int) ((tick - lastMeal) / (10 * 0.6)))) > 80))
			{ // go to eat
				printSeparator(-1, "meal");
				actualState = ActualState.MEAL;
				todo.clear();
				for(int i = track.length - 1; i > 0; i--)
					if(track[i] == cLocation)
					{
						todo.add(new Transaction().putR(cLocation, Operation.REMOVE).putR(track[i - 1], Operation.ADD));
						cLocation = track[i - 1];
					}
				for(int i = 0; i < 30; i++)
					todo.add(new Transaction());
				todo.add(new Transaction().putR(locationKitchen, Operation.REMOVE).putR(locationHall, Operation.ADD));
				todo.add(new Transaction().putR(locationHall, Operation.REMOVE).putR(locationLiving, Operation.ADD));
				lastMeal = tick;
			}

			if(actualState.idle() && (tick > wakeup) && (tick < sleep)
					&& (rand.nextInt(Math.max(1, (int) ((tick - lastShower) / (20 * 0.6)))) > 80))
			{ // go to shower
				printSeparator(-1, "shower");
				actualState = ActualState.SHOWER;
				todo.clear();
				for(int i = 0; i < track.length - 1; i++)
					if(track[i] == cLocation)
					{
						todo.add(new Transaction().putR(cLocation, Operation.REMOVE).putR(track[i + 1], Operation.ADD));
						cLocation = track[i + 1];
					}
				todo.add(new Transaction().putR(nearBathroom, Operation.ADD));
				todo.add(new Transaction().putR(nearBathroom, Operation.REMOVE).putR(locationLiving, Operation.REMOVE));
				for(int i = 0; i < 20; i++)
					todo.add(new Transaction());
				todo.add(new Transaction().putR(nearBathroom, Operation.ADD).putR(locationLiving, Operation.ADD));
				todo.add(new Transaction().putR(nearBathroom, Operation.REMOVE));
				lastShower = tick;
			}

			if(actualState.idle() && (actualState != ActualState.WANDERING) && (tick > wakeup) && (tick < sleep)
					&& (rand.nextInt(Math.max(1, (int) ((tick - lastWander) / (7 * 0.6)))) > 80))
			{
				printSeparator(-1, "wandering");
				actualState = ActualState.WANDERING;

				for(int i = track.length - 1; i > 0; i--)
					if(track[i] == cLocation)
					{
						todo.add(new Transaction().putR(cLocation, Operation.REMOVE).putR(track[i - 1], Operation.ADD));
						cLocation = track[i - 1];
					}
				for(int i = 0; i < 5 + rand.nextInt(60); i++)
					todo.add(new Transaction());
				todo.add(new Transaction().putR(locationKitchen, Operation.REMOVE).putR(locationHall, Operation.ADD));
				todo.add(new Transaction().putR(locationHall, Operation.REMOVE).putR(locationLiving, Operation.ADD));
				lastWander = tick;
			}

			if(actualState.idle() && (tick >= sleep))
				actualState = ActualState.SLEEP;

			ticker.tickUp();
		}

		printSeparator(2, "context2");
	}
}
