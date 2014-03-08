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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import net.xqhs.graphical.GCanvas;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.matcher.GraphMatcherQuick;
import net.xqhs.graphs.matcher.GraphMatchingProcess;
import net.xqhs.graphs.matcher.Match;
import net.xqhs.graphs.matcher.MatchingVisualizer;
import net.xqhs.graphs.matcher.MonitorPack;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.representation.text.TextGraphRepresentation;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.Unit;
import net.xqhs.util.logging.UnitComponent;

/**
 * Tester for {@link GraphMatcherQuick}.
 * 
 * @author Andrei Olaru
 * 
 */
public class GraphMatcherTest
{
	/**
	 * Name of the key in a testPack that designates the graph.
	 */
	private static final String	NAME_GRAPH		= "graph";
	/**
	 * Name of the key in a testPack that designates the pattern, in case only one pattern exists.
	 */
	private static final String	NAME_PATTERN	= "pattern";
	
	/**
	 * The name of the logging unit.
	 */
	private static String		unitName		= "graphMatcherTestMain";
	
	/**
	 * Directory with test files.
	 */
	static String				filedir			= "playground/";
	/**
	 * Extension of graph files.
	 */
	static String				filexext		= ".txt";
	/**
	 * Whatever is added after the file name to form the filename for the pattern.
	 */
	static String				patternpart		= "P";
	
	/**
	 * @param args
	 *            : arguments
	 */
	public static void main(String[] args)
	{
		UnitComponent log = (UnitComponent) new UnitComponent().setUnitName(unitName).setLogLevel(Level.ALL);
		log.lf("Hello World");
		
		String filename = "conf/conf";
		boolean visual = false;
		
		Map<String, Graph> testPack = loadTestGraphPattern(filename);
		
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
		
		testMatchingProcess(testPack, monitoring, log);
		
		log.doExit();
	}
	
	/**
	 * Loads a testPack formed of a graph and a pattern. It uses the specified filename for the graph and the filename
	 * with {@value #patternpart} added for the pattern.
	 * 
	 * @param filename
	 *            - the file name.
	 * @return the testPack {@link Map} of graph name &rarr; {@link Graph} instance.
	 */
	protected static Map<String, Graph> loadTestGraphPattern(String filename)
	{
		Map<String, Graph> testPack = new HashMap<String, Graph>();
		
		SimpleGraph G;
		try
		{
			G = ((SimpleGraph) new SimpleGraph().setUnitName("G").setLogLevel(Level.INFO).setLink(unitName))
					.readFrom(new FileInputStream(filedir + filename + filexext));
		} catch(FileNotFoundException e)
		{
			e.printStackTrace();
			return null;
		}
		
		GraphPattern GP;
		try
		{
			GP = (GraphPattern) ((SimpleGraph) new GraphPattern().setUnitName("GP").setLogLevel(Level.INFO)
					.setLink(unitName)).readFrom(new FileInputStream(filedir + filename + patternpart + filexext));
		} catch(FileNotFoundException e)
		{
			e.printStackTrace();
			return null;
		}
		testPack.put(NAME_GRAPH, G);
		testPack.put(NAME_PATTERN, GP);
		return testPack;
	}
	
	/**
	 * Prints all the graphs in a testPack.
	 * 
	 * @param testPack
	 *            - the testPack {@link Map} of graph name &rarr; {@link Graph} instance.
	 * @param printSimple
	 *            - if <code>true</code>, the {@link #toString()} version of the graph will be printed before the text
	 *            representation.
	 * @param separator
	 *            - see {@link TextGraphRepresentation#setLayout(String, String, int)}.
	 * @param separatorIncrement
	 *            - see {@link TextGraphRepresentation#setLayout(String, String, int)}.
	 * @param incrementLimit
	 *            - see {@link TextGraphRepresentation#setLayout(String, String, int)}.
	 * @param log
	 *            - the {@link LoggerSimple} instance to use to display the graph.
	 */
	protected static void printTestPack(Map<String, Graph> testPack, boolean printSimple, String separator,
			String separatorIncrement, int incrementLimit, LoggerSimple log)
	{
		for(Map.Entry<String, Graph> entry : testPack.entrySet())
		{
			log.li("[]: []", entry.getKey(), entry.getValue().toString());
			TextGraphRepresentation GR = (TextGraphRepresentation) new TextGraphRepresentation(entry.getValue())
					.setLayout(separator, separatorIncrement, incrementLimit).setUnitName(Unit.DEFAULT_UNIT_NAME)
					.setLink(unitName).setLogLevel(Level.ERROR);
			GR.update();
			log.li(GR.displayRepresentation());
		}
	}
	
	/**
	 * Tests {@link GraphMatchingProcess} with a graph and a pattern.
	 * 
	 * @param testPack
	 * @param monitoring
	 * @param log
	 */
	protected static void testMatchingProcess(Map<String, Graph> testPack, MonitorPack monitoring, LoggerSimple log)
	{
		Graph G = testPack.get(NAME_GRAPH);
		GraphPattern GP = (GraphPattern) testPack.get(NAME_PATTERN);
		
		GraphMatchingProcess GMQ = GraphMatcherQuick.getMatcher(G, GP, monitoring);
		GMQ.resetIterator(4);
		while(true)
		{
			Match m = GMQ.getNextMatch();
			if(m == null)
				break;
			log.li("============== new match\n[]", m);
		}
		monitoring.printStats();
		log.li("===============================================");
		GMQ.resetIterator(3);
		while(true)
		{
			Match m = GMQ.getNextMatch();
			if(m == null)
				break;
			log.li("============== new match\n[]", m);
		}
		monitoring.printStats();
		log.li("===============================================");
		// GMQ.clearData();
		log.li(GMQ.getAllMatches(3).toString()); // is a long line
		monitoring.printStats();
	}
	
}
