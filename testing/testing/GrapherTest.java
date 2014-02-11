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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import net.xqhs.graphs.graph.ConnectedNode;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.SimpleEdge;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.graph.SimpleNode;
import net.xqhs.graphs.representation.text.TextGraphRepresentation;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.Unit;
import net.xqhs.util.logging.UnitComponent;

@SuppressWarnings("javadoc")
public class GrapherTest
{
	private static String			unitName	= "grapherTestMain";
	private static UnitComponent	log;
	
	@SuppressWarnings("unused")
	private static String			testDir		= "playground/graphplay/";
	
	/**
	 * @throws IOException
	 *             if file not found or other IO exceptions.
	 */
	public static void main(String args[]) throws IOException
	{
		log = (UnitComponent) new UnitComponent().setUnitName(unitName).setLogLevel(Level.ALL);
		log.lf("Hello World");
		
		testTextRepresentation();
		
		// testSelfReading();
		
		// testGraphTextReading("ConfP");
		
		// testTextRepresentationReading("test");
		
		// G3RT.update(); // log.li(G3RT.displayRepresentation());
		
		// GraphicalGraphRepresentation.GraphConfig configX = new GraphicalGraphRepresentation.GraphConfig(G3);
		// configX.setBackwards().setName(Unit.DEFAULT_UNIT_NAME).setLink(unitName).setLevel(Level.li);
		// GraphicalGraphRepresentation G3RX = new GraphicalGraphRepresentation(configX);
		// log.li(G3RX.displayRepresentation());
		//
		// String containers[] = { "AdministrationContainer", "RoomContainer", "AliceContainer" };
		// Map<Node, Node> agentLevel = new HashMap<Graph.Node, Graph.Node>();
		// for(Edge edge : G3.getEdges())
		// if(edge.getName() == "resides-on")
		// agentLevel.put(edge.getFrom(), edge.getTo());
		// Map<Node, Node> containersLevel = new HashMap<Graph.Node, Graph.Node>();
		// for(String containerName : containers)
		// containersLevel.put(G3.getNodesNamed(containerName).iterator().next(), null);
		// LinkedList<Map<Node, Node>> levels = new LinkedList<Map<Node, Node>>();
		// levels.add(agentLevel);
		// levels.add(containersLevel);
		// MultilevelGraphRepresentation G3R = new TextMultilevelGraphRepresentation(G3, levels, null);
		// log.li("\n\n" + G3R.toString() + "\n");
		//
		// JFrame acc = new JFrame(unitName);
		// acc.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// acc.setLocation(100, 100);
		// acc.setSize(800, 500);
		// acc.add(G3RX.displayRepresentation());
		// acc.setVisible(true);
		
		//
		
		// transform linear representation files into .dot files
		// String files[] = new String[] { "alice", "aliceP", "conf", "confP", "book", "bookP" };
		// String extension_in = ".txt";
		// String extension_out = ".dot.txt";
		// for(String file : files)
		// {
		// Graph graph = TextGraphRepresentation.readRepresentation(FileUtils.fileToString(testDir + file
		// + extension_in)); // , null, new
		// // UnitConfigData().setName(Unit.DEFAULT_UNIT_NAME).setLevel(Level.ALL));
		// TextGraphRepresentation graphR = new TextGraphRepresentation(
		// new TextGraphRepresentation.GraphConfig(graph).setLayout("\n", "\t", 5));
		// log.li("\n\n" + graph.toString() + "\n");
		// log.li("\n\n" + graphR.toString() + "\n");
		// FileUtils.stringToFile(testDir + file + extension_out, graph.toDot());
		// }
		
		log.doExit();
	}
	
	// @SuppressWarnings("unused")
	private static void testTextRepresentation()
	{
		// generate graph
		Graph G;
		G = staticTest(4);
		
		// G = randomTest(6, 8, -1, true);
		// G = randomTest(6, 8, 1307714367060L, false);
		// G = randomTest(6, 8, 1310205248694L, false);
		// G = randomTest(6, 8, 1310208355983L, false);
		// G = randomTest(9, 8, 1310475944100L, false);
		
		// print out the graph
		
		TextGraphRepresentation GRa = new TextGraphRepresentation(G);
		// GRa.setLayout("", " ", -1);
		GRa.setLayout("\n", "\t", 3);
		// GRa.setUnitName(Unit.DEFAULT_UNIT_NAME).setLink(unitName).setLogLevel(Level.ALL);
		GRa.update();
		log.li(GRa.toString());
	}
	
	@SuppressWarnings("unused")
	private static Graph staticTest(int version)
	{
		Graph G = new SimpleGraph();
		
		int nNodes = 9;
		ConnectedNode nodes[] = new ConnectedNode[nNodes];
		
		for(int i = 0; i < nNodes; i++)
		{
			nodes[i] = new SimpleNode(Character.toString((char) ('A' + i)));
			G.addNode(nodes[i]);
		}
		
		switch(version)
		{
		case 2:
			G.addEdge(new SimpleEdge(nodes[2], new SimpleNode("hello"), null)); // node hello is outside G
			new SimpleEdge(nodes[2], nodes[3], "hello_edge"); // edge is outside G (but nodes are not)
			//$FALL-THROUGH$
		case 1:
			G.addEdge(new SimpleEdge(nodes[4], nodes[0], null));
			G.addEdge(new SimpleEdge(nodes[4], nodes[3], null));
			G.addEdge(new SimpleEdge(nodes[3], nodes[5], null));
			G.addEdge(new SimpleEdge(nodes[0], nodes[6], null));
			G.addEdge(new SimpleEdge(nodes[6], nodes[5], null));
			G.addEdge(new SimpleEdge(nodes[3], nodes[1], null));
			G.addEdge(new SimpleEdge(nodes[1], nodes[2], null));
			G.addEdge(new SimpleEdge(nodes[2], nodes[7], null));
			G.addEdge(new SimpleEdge(nodes[7], nodes[8], null));
			G.addEdge(new SimpleEdge(nodes[5], nodes[8], null));
			break;
		case 3:
			G.addEdge(new SimpleEdge(nodes[0], nodes[1], null));
			G.addEdge(new SimpleEdge(nodes[0], nodes[4], null));
			G.addEdge(new SimpleEdge(nodes[1], nodes[0], null));
			G.addEdge(new SimpleEdge(nodes[1], nodes[2], null));
			G.addEdge(new SimpleEdge(nodes[2], nodes[3], null));
			G.addEdge(new SimpleEdge(nodes[5], nodes[1], null));
			G.addEdge(new SimpleEdge(nodes[5], nodes[4], null));
			G.addEdge(new SimpleEdge(nodes[4], nodes[5], null));
			G.addEdge(new SimpleEdge(nodes[4], nodes[0], null));
			break;
		case 4:
			G.addEdge(new SimpleEdge(nodes[0], nodes[1], null));
			G.addEdge(new SimpleEdge(nodes[0], null, null));
			break;
		}
		
		return G;
	}
	
	/**
	 * 
	 * @param nodes
	 * @param edges
	 * @param seed
	 *            - use -1 for a new seed.
	 * @return
	 */
	private static Graph randomTest(int nNodes, int nEdges, long seedPre, boolean labelEdges)
	{
		Graph G = new SimpleGraph();
		
		long seed = System.currentTimeMillis();
		if(seedPre >= 0)
			seed = seedPre;
		log.lf("seed was " + seed);
		Random rand = new Random(seed);
		ConnectedNode nodes[] = new ConnectedNode[nNodes];
		
		for(int i = 0; i < nNodes; i++)
		{
			nodes[i] = new SimpleNode(Character.toString((char) ('A' + i)));
			G.addNode(nodes[i]);
		}
		
		for(int i = 0; i < nEdges; i++)
		{
			int from = rand.nextInt(nNodes);
			int to = rand.nextInt(nNodes);
			while((from == to) || (nodes[from].getOutNodes().contains(nodes[to])))
				to = rand.nextInt(nNodes);
			G.addEdge(new SimpleEdge(nodes[from], nodes[to], !labelEdges ? null : Character.toString((char) ('a' + i))));
		}
		
		return G;
	}
	
	/**
	 * Tests if the representation implementation can read what it wrote.
	 */
	@SuppressWarnings("unused")
	private static void testSelfReading()
	{
		Graph G = randomTest(9, 8, -1, true);
		TextGraphRepresentation GRa = new TextGraphRepresentation(G);
		GRa.setLayout("\n", "\t", 3);
		GRa.update();
		log.li(GRa.toString());
		
		// test reading
		Graph GR = new TextGraphRepresentation(new SimpleGraph()).readRepresentation(GRa.toString());
		TextGraphRepresentation GRR = new TextGraphRepresentation(GR).setLayout("", " ", 3);
		GRR.update();
		log.li("\n\n [] \n", GR.toString());
		log.li("\n\n [] \n", GRR.toString());
		log.li(GRR.setLayout("\n", "\t", 3).toString().equals(GRa.toString()) ? "===OK" : "===NOPE");
	}
	
	@SuppressWarnings("unused")
	private static void testGraphTextReading(int variant)
	{
		String input = "";
		
		switch(variant)
		{
		case 0:
			input = "hello - world \n world -> big \n whoa - is for > hello";
			break;
		case 1:
			input =
			
			"Albert -in> London ;" +
			
			"" +
			
			"Albert -isa> User \n" +
			
			"Schedule -of> Albert \n" +
			
			"attend -part-of>Schedule \n" +
			
			"flight -part-of> attend \n" +
			
			"flight -from>LHR \n" +
			
			"flight -to> CDG \n" +
			
			"LHR -in> London \n" +
			
			"CDG -in> Paris \n" +
			
			"CNAM -in> Paris \n" +
			
			"AI Conf -venue> CNAM \n" +
			
			"AI Conf -isa> Activity \n" +
			
			"attend -isa> Activity \n" +
			
			"flight -isa> Activity \n" +
			
			"attend -> AI Conf \n" +
			
			"LHR -isa> airport \n" +
			
			"CDG -isa> airport \n";
			break;
		case 2:
			input = "A -label-> B; B - C; C -label- A; C-la-b-el>D; D-h-ell-o-B; D > long node here; long node here -edge- the other node";
			// expect 6 nodes and 11 edges (3 uni-, 4 bi-directional)
			break;
		case 3:
			input = "";
			input += "UniversityUPMCAgent -resides-on> AdministrationContainer;";
			input += "SchedulerUPMCAgent -resides-on> AdministrationContainer;";
			input += "CourseCSAgent -resides-on> AdministrationContainer;";
			input += "Room04Agent -resides-on> RoomContainer;";
			input += "AliceAgent -resides-on> AliceContainer;";
			input += "Room04Agent-has-parent>UniversityUPMCAgent;";
			input += "AliceAgent -has-parent> CourseCSAgent;";
			input += "CourseCSAgent -has-parent> UniversityUPMCAgent;";
			input += "CourseCSAgent -has-parent> Room04Agent;";
			input += "SchedulerUPMCAgent -has-parent> UniversityUPMCAgent;";
			break;
		}
		
		testGraphTextReading(new ByteArrayInputStream(input.getBytes()));
	}
	
	@SuppressWarnings("unused")
	private static void testGraphTextReading(String fileVariant) throws IOException
	{
		String filedir = "playground/";
		String filexext = ".txt";
		
		InputStream source = new FileInputStream(filedir + fileVariant + filexext);
		testGraphTextReading(source);
		source.close();
	}
	
	private static void testGraphTextReading(InputStream source)
	{
		Graph G = new SimpleGraph().readFrom(source);
		log.li(G.toString());
		
		TextGraphRepresentation GR = (TextGraphRepresentation) new TextGraphRepresentation(G).setLayout("\n", "\t", 2)
				.update();
		log.li("\n\n [] \n", GR.toString());
	}
	
	@SuppressWarnings("unused")
	private static void testTextRepresentationReading(String filename)
	{
		String filedir = "playground/";
		String filexext = ".txt";
		
		Graph G;
		try
		{
			G = ((TextGraphRepresentation) new TextGraphRepresentation(new SimpleGraph()).setUnitName(
					Unit.DEFAULT_UNIT_NAME).setLogLevel(Level.ALL)).readRepresentation(new FileInputStream(filedir
					+ filename + filexext));
		} catch(FileNotFoundException e)
		{
			e.printStackTrace();
			return;
		}
		log.li(G.toString());
		
		TextGraphRepresentation GR = (TextGraphRepresentation) new TextGraphRepresentation(G).setLayout("\n", "\t", -1)
				.update();
		log.li("\n\n [] \n", GR.toString());
	}
}
