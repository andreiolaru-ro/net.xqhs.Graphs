package net.xqhs.graphs.examples;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleEdge;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.graph.SimpleNode;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.representation.text.TextGraphRepresentation;

@SuppressWarnings("javadoc")
public class SimpleGraphUsageExamples
{

	public static void main(String[] args) throws IOException
	{
		// see how a simple graphs is created, read from a file, displayed, and updated.

		SimpleGraph g = new SimpleGraph();

		// reading a graph from a list of edges:
		InputStream input = new FileInputStream("playground/conf/conf.txt");
		g.readFrom(input);
		input.close();

		// printing the graph as a list of nodes and edges
		System.out.println(g.toString());

		System.out.println("============================================================");
		g = new GraphPattern();

		// reading the graph using TextGraphRepresentation
		input = new FileInputStream("playground/conf/confPR.txt");
		new TextGraphRepresentation(g).readRepresentation(input);
		input.close();

		System.out.println(g.toString());
		System.out.println("=============");

		// printing the graph using TextGraphRepresentation
		System.out.println(new TextGraphRepresentation(g).update().displayRepresentation());

		System.out.println("============================================================");

		// adding new edges and nodes
		Node n1 = new SimpleNode("MyNodeA");
		Node n2 = new SimpleNode("MyNodeB");
		g.addNode(n1).addNode(n2).addEdge(new SimpleEdge(n1, n2, "my-edge"));

		System.out.println(new TextGraphRepresentation(g).update().displayRepresentation());

		System.out.println("============================================================");

		// customizing the output
		System.out.println(new TextGraphRepresentation(g).setLayout("\n", "\t", 2).update().displayRepresentation());

		// the end.
	}
}
