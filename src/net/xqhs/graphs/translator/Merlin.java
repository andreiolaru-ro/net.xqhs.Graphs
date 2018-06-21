package net.xqhs.graphs.translator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import net.xqhs.graphs.context.ContextGraph;
import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.matcher.MonitorPack;
import net.xqhs.graphs.nlp.GraphConverter;
import net.xqhs.graphs.nlp.NLEdge;
import net.xqhs.graphs.nlp.NLEdgeFactory;
import net.xqhs.graphs.nlp.NLGraphType;
import net.xqhs.graphs.nlp.NLNodeP;
import net.xqhs.graphs.nlp.Parser;

public class Merlin {

	StanfordCoreNLP pipeline;
	Map<String, ContextGraph> wisdom;
	Map<ContextPattern, UserRequest.RequestType> scrollOfQueries = new LinkedHashMap<ContextPattern, UserRequest.RequestType>();
	ArrayList<String> failLines = new ArrayList<String>();
	MonitorPack monitoring;

	public Merlin(String theFileOfWisdom) {
	pipeline = Parser.init();
	monitoring = new MonitorPack();
	wisdom = new TreeMap<String, ContextGraph>();

	failLines.add("I am not wise enough to know that.");
	failLines.add("My wisdom is not infinite.");
	failLines.add("My memory is letting me down, let's talk about something else.");

	ArrayList<String> rawWisdom;
	try {
	rawWisdom = readFile(theFileOfWisdom);
	for (String w : rawWisdom) {
	System.out.println(w);
	wisdom.put(w, new GraphConverter(Arrays.asList(w), pipeline).getG());
	}
	} catch (Exception e) {
	e.printStackTrace();
	}

	System.out.println("Hello! I'm Merlin. Ask me anything you want to know.");

	}

	public String askMerlin(String query) throws Exception {
	UserRequest ur = null;
	String wisdomToBeShared;
	ContextPattern cp;

	ur = new UserRequest(query, pipeline);
	cp = ur.getReqPattern();

	// System.out.println("------Context patt before match to previous-----");
	// for (Edge e : cp.getEdges())
	// System.out.println(e.toString());

	if (ur.getRequestType() == UserRequest.RequestType.DETAIL) {
	UserRequest.RequestType rType = matchToPreviousRequest(cp);
	if (rType != null)
		ur.setRequestType(rType);
	}

	// System.out.println("------Context patt after match to previous-----");
	// for (Edge e : cp.getEdges())
	// System.out.println(e.toString());

	scrollOfQueries.put(cp, ur.getRequestType());

	wisdomToBeShared = ur.getStringReply(wisdom, monitoring);
	if (wisdomToBeShared.isEmpty()) {
	int line = new Random().nextInt(failLines.size());
	return (String) failLines.toArray()[line];
	}

	return wisdomToBeShared;

	}

	private Edge findEdge(Node n1, Node n2, ContextPattern cp) {
	System.out.println("Search edge " + n1.getLabel() + " -> " + n2.getLabel());
	for (Edge e : cp.getOutEdges(n1))
		if (n2.getLabel().equals(e.getTo().getLabel())) {
		System.out.println("Edge found!");
		return e;
		}

	return null;
	}

	private Boolean haveCommonNodes(ContextPattern p1, ContextPattern p2) {
	ArrayList<String> p1Nodes = new ArrayList<String>();
	ArrayList<String> p2Nodes = new ArrayList<String>();

	for (Node n : p1.getNodes()) {
	NLNodeP np = (NLNodeP) n;
	if (!np.isGeneric())
		p1Nodes.add(np.getLabel());
	}

	for (Node n : p2.getNodes()) {
	NLNodeP np = (NLNodeP) n;
	if (!np.isGeneric())
		p2Nodes.add(np.getLabel());
	}

	return !Collections.disjoint(p1Nodes, p2Nodes);
	}

	private Node getFirstGenericNode(ContextPattern cp) {
	for (Node n : cp.getNodes())
		if (((NLNodeP) n).isGeneric())
			return n;

	return null;
	}

	private Node createNodeIfNotExists(Node n, ContextPattern cp) {
	NLNodeP node = (NLNodeP) n;

	if (node.isGeneric()) {
	Node newNode = getFirstGenericNode(cp);
	if (newNode == null) {
	cp.addNode(node);
	}
	return getFirstGenericNode(cp);
	}

	Collection<Node> nodes = cp.getNodesNamed(node.getLabel());
	if (nodes.isEmpty()) { // must create new node to2 equivalent to to1 in cp
	cp.addNode(new NLNodeP(node));
	nodes = cp.getNodesNamed(node.getLabel());

	}
	return nodes.iterator().next();

	}

	private UserRequest.RequestType matchToPreviousRequest(ContextPattern cp) {
	if (scrollOfQueries.isEmpty())
		return null;

	List<Entry<ContextPattern, UserRequest.RequestType>> oldQueries = new ArrayList<Map.Entry<ContextPattern, UserRequest.RequestType>>(scrollOfQueries.entrySet());
	Entry<ContextPattern, UserRequest.RequestType> lastQuery = oldQueries.get(oldQueries.size() - 1);

	if (!haveCommonNodes(cp, lastQuery.getKey()))
		return null;

	NLNodeP to1, from1, to2, from2;

	for (Edge e : lastQuery.getKey().getEdges()) {
	to1 = (NLNodeP) e.getTo();
	from1 = (NLNodeP) e.getFrom();

	// get or create node to1 in cp
	to2 = (NLNodeP) createNodeIfNotExists(to1, cp);
	// get of create node from1 in cp
	from2 = (NLNodeP) createNodeIfNotExists(from1, cp);

	if (findEdge(from2, to2, cp) == null) { // edge must be added
	NLEdge newEdge = NLEdgeFactory.makeNLEdge(NLGraphType.PATTERN, from2, to2, e.getLabel());
	cp.addEdge(newEdge);
	}
	}

	return lastQuery.getValue();

	}

	private static ArrayList<String> readFile(String file) throws IOException {
	// Construct BufferedReader from FileReader
	BufferedReader br = new BufferedReader(new FileReader(new File(file)));
	ArrayList<String> sentences = new ArrayList<String>();

	String line = null;
	while ((line = br.readLine()) != null) {
	sentences.add(line);
	}
	br.close();

	return sentences;
	}

}
