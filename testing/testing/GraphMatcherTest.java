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

import javax.swing.JFrame;

import net.xqhs.graphical.GCanvas;
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
	 * The name of the logging unit.
	 */
	private static String	unitName	= "graphMatcherTestMain";
	
	/**
	 * @param args
	 *            : arguments
	 */
	public static void main(String[] args)
	{
		UnitComponent log = (UnitComponent) new UnitComponent().setUnitName(unitName).setLogLevel(Level.ALL);
		log.lf("Hello World");
		
		String filedir = "playground/";
		String filexext = ".txt";
		String patternpart = "P";
		boolean visual = false;
		
		String filename = "conf";
		
		SimpleGraph G;
		try
		{
			G = ((SimpleGraph) new SimpleGraph().setUnitName("G").setLogLevel(Level.INFO).setLink(unitName))
					.readFrom(new FileInputStream(filedir + filename + filexext));
		} catch(FileNotFoundException e)
		{
			e.printStackTrace();
			return;
		}
		log.li(G.toString());
		
		TextGraphRepresentation GRT = (TextGraphRepresentation) new TextGraphRepresentation(G).setLayout("\n", "\t", 2)
				.setUnitName(Unit.DEFAULT_UNIT_NAME).setLink(unitName).setLogLevel(Level.ERROR);
		GRT.update();
		log.li(GRT.displayRepresentation());
		
		GraphPattern GP;
		try
		{
			GP = (GraphPattern) ((SimpleGraph) new GraphPattern().setUnitName("GP").setLogLevel(Level.INFO)
					.setLink(unitName)).readFrom(new FileInputStream(filedir + filename + patternpart + filexext));
		} catch(FileNotFoundException e)
		{
			e.printStackTrace();
			return;
		}
		log.li(GP.toString());
		
		TextGraphRepresentation GPRT = (TextGraphRepresentation) new TextGraphRepresentation(GP)
				.setLayout("\n", "\t", 2).setUnitName(Unit.DEFAULT_UNIT_NAME).setLink(unitName)
				.setLogLevel(Level.ERROR);
		GPRT.update();
		log.li(GPRT.displayRepresentation());
		
		JFrame frame = new JFrame(unitName);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		GCanvas canvas = null;
		if(visual)
		{
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
		
		log.doExit();
	}
}
