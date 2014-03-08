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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import javax.swing.JFrame;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.HyperGraph;
import net.xqhs.graphs.graph.HyperNode;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleEdge;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.graph.SimpleNode;
import net.xqhs.graphs.representation.graphical.GraphicalGraphRepresentation;
import net.xqhs.graphs.representation.graphical.RadialGraphRepresentation;
import net.xqhs.graphs.representation.text.TextGraphRepresentation;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.Unit;
import net.xqhs.util.logging.UnitComponent;

@SuppressWarnings("javadoc")
public class GrapherTest
{
	private static String			unitName	= "grapherTestMain";
	private static UnitComponent	log;
	
	/**
	 * @throws IOException
	 *             if file not found or other IO exceptions.
	 */
	public static void main(String args[]) throws IOException
	{
		log = (UnitComponent) new UnitComponent().setUnitName(unitName).setLogLevel(Level.ALL);
		log.lf("Hello World");
		
		// testTextRepresentation();
		
		// testSelfReading();
		
		// testGraphTextReading("Emily/EmilyP");
		
		// testTextRepresentationReading("conf/confPR");
		
		// testGraphicalContainerGraph(false);
		
		// testHyperGraphs();
		
		// testMultilevelRepresentation();
		
		// testDotConversion(new String[] { "alice", "aliceP", "conf", "confP", "book", "bookP" });
		
		log.doExit();
	}
	
	@SuppressWarnings("unused")
	private static void testTextRepresentation()
	{
		// generate graph
		Graph G;
		G = staticTest(3); // 1 to 4
		
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
	public static Graph staticTest(int version)
	{
		Graph G = new SimpleGraph();
		
		int nNodes = 9;
		Node nodes[] = new Node[nNodes];
		
		for(int i = 0; i < nNodes; i++)
		{
			nodes[i] = new SimpleNode(Character.toString((char) ('A' + i)));
			G.addNode(nodes[i]);
		}
		
		switch(version)
		{
		case 2:
			// case 1 plus: C -> hello
			G.addEdge(new SimpleEdge(nodes[2], new SimpleNode("hello"), null)); // node hello is outside G
			new SimpleEdge(nodes[2], nodes[3], "hello_edge"); // edge is outside G (but nodes are not)
			//$FALL-THROUGH$
		case 1:
			// E (-> D (-> B -> C -> H -> I) -> F -> *I) -> A -> G -> *F
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
			// F (-> B (-> C -> D) -> A (->*B) -> E (-> *F) -> *A) -> *E; G; H; I
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
	public static Graph randomTest(int nNodes, int nEdges, long seedPre, boolean labelEdges)
	{
		Graph G = new SimpleGraph();
		
		long seed = System.currentTimeMillis();
		if(seedPre >= 0)
			seed = seedPre;
		log.lf("seed was " + seed);
		Random rand = new Random(seed);
		Node nodes[] = new Node[nNodes];
		
		for(int i = 0; i < nNodes; i++)
		{
			nodes[i] = new SimpleNode(Character.toString((char) ('A' + i)));
			G.addNode(nodes[i]);
		}
		
		for(int i = 0; i < nEdges; i++)
		{
			int from = rand.nextInt(nNodes);
			int to = rand.nextInt(nNodes);
			while(true)
			{
				if(from != to)
				{
					boolean exists = false;
					for(Edge e : G.getOutEdges(nodes[from]))
						if(e.getTo() == nodes[to])
							exists = true;
					if(!exists)
						break;
				}
				to = rand.nextInt(nNodes);
			}
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
		Graph G = randomTest(9, 8, -1L, true);
		log.li(G.toString());
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
			input = getUPMCTest();
			break;
		}
		
		testGraphTextReading(new ByteArrayInputStream(input.getBytes()));
	}
	
	private static String getUPMCTest()
	{
		String input = "";
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
		return input;
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
		log.li(GR.toString());
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
		log.li(GR.toString());
	}
	
	@SuppressWarnings("unused")
	private static void testGraphicalContainerGraph(boolean useRadial)
	{
		Graph G3 = new SimpleGraph().readFrom(new ByteArrayInputStream(getUPMCTest().getBytes()));
		log.li(G3.toString());
		log.li(new TextGraphRepresentation(G3).setLayout("\n", "\t", -1).setBackwards().update().toString());
		
		GraphicalGraphRepresentation G3RX = null;
		if(!useRadial)
			G3RX = new GraphicalGraphRepresentation(G3);
		else
			G3RX = new RadialGraphRepresentation(G3);
		
		G3RX.setBackwards().setUnitName(Unit.DEFAULT_UNIT_NAME).setLink(unitName).setLogLevel(Level.ALL);
		
		JFrame acc = new JFrame(unitName);
		acc.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		acc.setLocation(100, 100);
		acc.setSize(800, 500);
		acc.add(((GraphicalGraphRepresentation) G3RX.update()).displayRepresentation());
		acc.setVisible(true);
	}
	
	@SuppressWarnings("unused")
	private static void testHyperGraphs()
	{
		Node emily = new SimpleNode("Emily");
		Node room1 = new SimpleNode("Room1");
		Node room2 = new SimpleNode("Room2");
		Node hn1 = new HyperNode(new SimpleGraph().addNode(emily).addNode(room1)
				.addEdge(new SimpleEdge(emily, room1, "is-in")));
		Node hn2 = new HyperNode(new SimpleGraph().addNode(emily).addNode(room2)
				.addEdge(new SimpleEdge(emily, room2, "is-in")));
		Graph HG = new HyperGraph().addNode(hn1).addNode(hn2).addEdge(new SimpleEdge(hn1, hn2, null));
		
		TextGraphRepresentation HGR = new TextGraphRepresentation(HG).setLayout("\n", "\t", -1);
		HGR.update();
		log.li(HGR.toString());
	}
	
	@SuppressWarnings("unused")
	private static void testMultilevelRepresentation()
	{
		Graph G3 = new SimpleGraph().readFrom(new ByteArrayInputStream(getUPMCTest().getBytes()));
		log.li(new TextGraphRepresentation(G3).setLayout("\n", "\t", -1).setBackwards().update().toString());
		
		String containers[] = { "AdministrationContainer", "RoomContainer", "AliceContainer" };
		Map<Node, Node> agentLevel = new HashMap<Node, Node>();
		for(Edge edge : G3.getEdges())
			if(edge.getLabel().equals("resides-on"))
				agentLevel.put(edge.getFrom(), edge.getTo());
		Map<Node, Node> containersLevel = new HashMap<Node, Node>();
		for(String containerName : containers)
			containersLevel.put(G3.getNodesNamed(containerName).iterator().next(), null);
		LinkedList<Map<Node, Node>> levels = new LinkedList<Map<Node, Node>>();
		levels.add(agentLevel);
		levels.add(containersLevel);
		// MultilevelGraphRepresentation G3R = new TextMultilevelGraphRepresentation(G3, levels, null);
		// log.li("\n\n" + G3R.toString() + "\n");
	}
	
	@SuppressWarnings("unused")
	private static void testDotConversion(String files[])
	{
		// transform linear representation files into .dot files
		String extension_in = ".txt";
		String extension_out = ".dot.txt";
		String testDir = "/playground";
		for(String file : files)
		{
			TextGraphRepresentation graphR = new TextGraphRepresentation(new SimpleGraph());
			try
			{
				InputStream is = new FileInputStream(testDir + file + extension_in);
				SimpleGraph graph = (SimpleGraph) graphR.readRepresentation(is);
				is.close();
				graphR.setLayout("\n", "\t", 5);
				log.li("\n\n" + graph.toString() + "\n");
				log.li("\n\n" + graphR.toString() + "\n");
				OutputStream os = new FileOutputStream(testDir + file + extension_out, false);
				os.write(graph.toDot().getBytes());
				os.close();
			} catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		
	}
}
