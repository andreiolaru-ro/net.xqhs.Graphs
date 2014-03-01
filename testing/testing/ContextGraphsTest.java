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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.matchingPlatform.TrackingGraph;
import net.xqhs.graphs.matchingPlatform.TrackingGraph.Operation;
import net.xqhs.graphs.matchingPlatform.TrackingGraph.Transaction;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.UnitComponent;

/**
 * Tester for complex graphs, hyper graphs, transaction graphs, context graphs, and matching platforms.
 * 
 * @author Andrei Olaru
 */
public class ContextGraphsTest
{
	/**
	 * Log/unit name
	 */
	private static String			unitName	= "contextTestMain";
	/**
	 * Log
	 */
	private static UnitComponent	log;
	
	/**
	 * @param args
	 *            unused
	 * @throws IOException
	 *             if file not found or other IO exceptions.
	 */
	public static void main(String args[]) throws IOException
	{
		log = (UnitComponent) new UnitComponent().setUnitName(unitName).setLogLevel(Level.ALL);
		log.lf("Hello World");
		
		testTransactions();
		
		testTrackingGraph(-1);
		
		log.doExit();
	}
	
	/**
	 * Test adding operations to {@link Transaction} instances.
	 */
	protected static void testTransactions()
	{
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
		Map<GraphComponent, Operation> ops = new HashMap<GraphComponent, TrackingGraph.Operation>();
		ops.put(comps[1], Operation.REMOVE);
		ops.put(comps[3], Operation.ADD);
		ops.put(comps[0], Operation.ADD);
		Map<GraphComponent, Operation> ops2 = new HashMap<GraphComponent, TrackingGraph.Operation>();
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
	}
	
	/**
	 * Test correct workings of a tracking graph and shadows of the tracking graph.
	 * 
	 * @param seedPre
	 *            - pre-set random seed; <code>-1</code> if none.
	 */
	protected static void testTrackingGraph(long seedPre)
	{
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
		List<Transaction> ts = new ArrayList<TrackingGraph.Transaction>();
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
	}
}
