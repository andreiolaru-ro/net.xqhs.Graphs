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
import java.io.ByteArrayInputStream;

import javax.swing.JFrame;

import net.xqhs.graphical.GCanvas;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.pattern.EdgeP;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.pattern.NodeP;
import net.xqhs.graphs.representation.GraphRepresentation;
import net.xqhs.graphs.representation.graphical.RadialGraphRepresentation;
import net.xqhs.graphs.representation.text.TextGraphRepresentation;
import net.xqhs.util.logging.Log.Level;
import net.xqhs.util.logging.Unit;
import net.xqhs.util.logging.UnitComponent;

public class GraphMatcherTest
{
	private static String	unitName	= "graphMatcherTestMain";
	
	public static void main(String[] args)
	{
		UnitComponent unit = (UnitComponent) new UnitComponent().setUnitName(unitName);
		unit.lf("Hello World");
		
		String input = "";
		input += "AIConf -> conftime;";
		input += "conftime -isa> interval;";
		input += "AIConf -> CFP;";
		input += "CFP -> AIConf;";
		input += "CFP -isa> document;";
		input += "CFP -contains> 05012011;";
		input += "05012011 -isa> date;";
		input += "CFP -contains> 30032011;";
		input += "30032011 -isa> date;";
		input += "AIConf -> 30032011;";
		input += "CFP -contains> conftime;";
		SimpleGraph G = ((SimpleGraph) new SimpleGraph().setUnitName("G").setLogLevel(Level.INFO).setLink(unitName))
				.readFrom(new ByteArrayInputStream(input.getBytes()));
		unit.li(G.toString());
		
		TextGraphRepresentation GRT = (TextGraphRepresentation) new TextGraphRepresentation(G).setLayout("\n", "\t", 2)
				.setUnitName(Unit.DEFAULT_UNIT_NAME).setLink(unitName).setLogLevel(Level.ALL);
		GRT.update();
		unit.li(GRT.displayRepresentation());
		
		GraphPattern GP = (GraphPattern) new GraphPattern().setUnitName("GP").setLogLevel(Level.INFO).setLink(unitName);
		NodeP nConf = new NodeP();
		NodeP nDeadline = new NodeP();
		NodeP nCFP = new NodeP();
		NodeP nArticle = new NodeP();
		NodeP nConfType = new NodeP("conference");
		NodeP nDocumentType = new NodeP("document");
		NodeP nDateType = new NodeP("date");
		GP.addNode(nConf);
		GP.addNode(nDeadline);
		GP.addNode(nCFP);
		GP.addNode(nArticle);
		GP.addNode(nConfType);
		GP.addNode(nDocumentType);
		GP.addNode(nDateType);
		GP.addEdge(new EdgeP(nConf, nConfType, "isa"));
		GP.addEdge(new EdgeP(nConf, nArticle, "article"));
		GP.addEdge(new EdgeP(nConf, nCFP, "CFP"));
		GP.addEdge(new EdgeP(nConf, nDeadline, "deadline"));
		GP.addEdge(new EdgeP(nDeadline, nDateType, "isa"));
		GP.addEdge(new EdgeP(nCFP, nDeadline, "contains"));
		GP.addEdge(new EdgeP(nArticle, nDocumentType, "isa"));
		GP.addEdge(new EdgeP(nCFP, nDocumentType, "isa"));
		// GraphPattern GP = GraphPattern.readFrom(new ByteArrayInputStream(input2.getBytes()), new UnitConfigData("GP")
		// .setLevel(Level.INFO).setLink(unitName));
		unit.li(GP.toString());
		
		TextGraphRepresentation GPRT = (TextGraphRepresentation) new TextGraphRepresentation(GP)
				.setLayout("\n", "\t", 2).setUnitName(Unit.DEFAULT_UNIT_NAME).setLink(unitName)
				.setLogLevel(Level.ERROR);
		GPRT.update();
		unit.li(GPRT.displayRepresentation());
		
		JFrame frame = new JFrame(unitName);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		GCanvas canvas = new GCanvas();
		
		GraphRepresentation GRG = (GraphRepresentation) new RadialGraphRepresentation(G)
				.setOrigin(new Point(-200, -100)).setBottomRight(new Point(-10, 100)).setCanvas(canvas)
				.setUnitName(Unit.DEFAULT_UNIT_NAME).setLink(unitName).setLogLevel(Unit.DEFAULT_LEVEL);
		GRG.update();
		GraphRepresentation GPRG = (GraphRepresentation) new RadialGraphRepresentation(GP)
				.setOrigin(new Point(10, -100)).setBottomRight(new Point(200, 100)).setCanvas(canvas)
				.setUnitName(Unit.DEFAULT_UNIT_NAME).setLink(unitName).setLogLevel(Unit.DEFAULT_LEVEL);
		GPRG.update();
		
		canvas.setZoom(6);
		canvas.resetLook();
		frame.add(canvas);
		
		frame.setLocation(1450, 10);
		frame.setSize(1500, 600);
		frame.setVisible(true);
		
		// new GraphMatcherQuick(G, GP).doMatching();
		
		unit.doExit();
	}
}
