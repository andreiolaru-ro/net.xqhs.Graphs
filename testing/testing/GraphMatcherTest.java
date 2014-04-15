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

import java.awt.Point;
import java.util.Map;

import javax.swing.JFrame;

import net.xqhs.graphical.GCanvas;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.matcher.GraphMatcherQuick;
import net.xqhs.graphs.matcher.GraphMatchingProcess;
import net.xqhs.graphs.matcher.Match;
import net.xqhs.graphs.matcher.MatchingVisualizer;
import net.xqhs.graphs.matcher.MonitorPack;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.UnitComponent;

/**
 * Tester for {@link GraphMatcherQuick}.
 * 
 * @author Andrei Olaru
 * 
 */
public class GraphMatcherTest extends Tester
{
	/**
	 * @param args
	 *            : arguments
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args)
	{
		new GraphMatcherTest();
	}
	
	@Override
	protected void doTesting()
	{
		super.doTesting();
		
		String filename = "conf/conf";
		boolean visual = false;
		
		Map<String, Graph> testPack = loadTestGraphPattern(filename, null, Level.INFO);
		
		printTestPack(testPack, true, "\n", "\t", 2, log);
		
		GCanvas canvas = null;
		if(visual)
		{
			JFrame frame = new JFrame(unitName);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			canvas = new GCanvas();
			canvas.setZoom(2);
			canvas.resetLook();
			frame.add(canvas);
			frame.setLocation(10, 30);
			frame.setSize(1100, 700);
			frame.setVisible(true);
		}
		
		MonitorPack monitoring = new MonitorPack().setLog((LoggerSimple) new UnitComponent().setUnitName("matcher")
				.setLogLevel(Level.INFO));
		if(visual)
			monitoring.setVisual(new MatchingVisualizer().setCanvas(canvas).setTopLeft(new Point(-400, 0)));
		
		testMatchingProcess(testPack, monitoring);
	}
	
	/**
	 * Tests {@link GraphMatchingProcess} with a graph and a pattern.
	 * 
	 * @param testPack
	 * @param monitoring
	 * @param log
	 */
	protected void testMatchingProcess(Map<String, Graph> testPack, MonitorPack monitoring)
	{
		Graph G = testPack.get(NAME_GRAPH);
		GraphPattern GP = (GraphPattern) testPack.get(NAME_PATTERN);
		
		GraphMatchingProcess GMQ = GraphMatcherQuick.getMatcher(G, GP, monitoring);
		printSeparator(0, "individual matches [4]");
		GMQ.resetIterator(4);
		while(true)
		{
			Match m = GMQ.getNextMatch();
			if(m == null)
				break;
			log.li("============== new match\n[]", m);
		}
		monitoring.printStats();
		
		printSeparator(0, "individual matches [3]"); // =================================
		GMQ.resetIterator(3);
		while(true)
		{
			Match m = GMQ.getNextMatch();
			if(m == null)
				break;
			log.li("============== new match\n[]", m);
		}
		monitoring.printStats();
		
		printSeparator(0, "all matches [3]"); // ============================
		// GMQ.clearData();
		log.li(GMQ.getAllMatches(3).toString()); // is a long line
		monitoring.printStats();
		
		printSeparator(0, "best matches"); // ============================
		log.li(GMQ.getBestMatches().toString()); // is a long line
		monitoring.printStats();
	}
	
}
