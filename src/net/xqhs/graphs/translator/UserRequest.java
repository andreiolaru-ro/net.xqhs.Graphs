package net.xqhs.graphs.translator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import net.xqhs.graphs.context.ContextGraph;
import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.matcher.GraphMatcherQuick;
import net.xqhs.graphs.matcher.GraphMatchingProcess;
import net.xqhs.graphs.matcher.Match;
import net.xqhs.graphs.matcher.MatchMaker;
import net.xqhs.graphs.matcher.MonitorPack;
import net.xqhs.graphs.nlp.NLNodeP;
import net.xqhs.graphs.nlp.Parser;
import net.xqhs.graphs.pattern.GraphPattern;

public class UserRequest {

	public enum RequestType {
		IF, INFO, WHQUESTION, DETAIL, NEW_INFO
	}

	private enum WhWords {
		WP, WRB, WDT
	}

	private String req;
	private RequestType type;
	private ContextPattern reqPattern;
	private StanfordCoreNLP pipeline;

	public UserRequest(String req, StanfordCoreNLP pipeline) throws Exception {
		this.pipeline = pipeline;
		this.req = req;
		this.reqPattern = computePattern(req);
		this.type = computeRequestType();
	}

	public String getRequest() {
		return this.req;
	}

	public void setRequest(String req) throws Exception {
		this.req = req;
		this.reqPattern = computePattern(req);
		this.type = computeRequestType();
	}

	public ContextPattern getReqPattern() {
		return this.reqPattern;
	}

	public void setReqPattern(ContextPattern cp) {
		this.reqPattern = cp;
	}

	public RequestType getRequestType() {
		return this.type;
	}

	public void setRequestType(RequestType type) {
		this.type = type;
	}

	private ContextPattern computePattern(String s) throws Exception {
		ArrayList<String> r = new ArrayList<String>(Arrays.asList(s));
		ContextPattern cp = null;
		try {
			cp = Parser.convertContextPatterns(r, pipeline).get(0);
		} catch (Exception e) {
			throw new Exception("Cannot convert request to ContextPattern:" + s);
		}

		return cp;
	}

	private RequestType computeRequestType() {
		ArrayList<String> partsOfSpeech = new ArrayList<String>();
		String[] words = req.trim().split("\\s+");
		String firstWord = words[0];

		if (firstWord.toLowerCase().equals("if"))
			return RequestType.IF;

		Collection<Node> nodes = reqPattern.getNodesNamed(firstWord);
		int firstWordIdx = 0;
		while (nodes.isEmpty()) {
			firstWord = words[++firstWordIdx];
			nodes = reqPattern.getNodesNamed(firstWord);
		}
		NLNodeP fn = (NLNodeP) nodes.iterator().next();
		if (fn.getPos().equals("VB"))
			return RequestType.INFO;

		for (Node n : reqPattern.getNodes()) {
			NLNodeP nn = (NLNodeP) n;
			if (!nn.isGeneric())
				partsOfSpeech.add(nn.getPos());
		}

		for (WhWords w : WhWords.values())
			if (partsOfSpeech.contains(w.toString().toUpperCase()))
				return RequestType.INFO;

		if (partsOfSpeech.contains("VBZ") || partsOfSpeech.contains("VBP"))
			return RequestType.NEW_INFO;
		if (req.contains(" is "))
			return RequestType.NEW_INFO;

		return RequestType.DETAIL;

	}

	private ArrayList<String> getNodeLabels(SimpleGraph g) {
		ArrayList<String> labels = new ArrayList<String>();

		for (Node n : g.getNodes())
			labels.add(n.getLabel());

		return labels;
	}

	private void removeUnwantedNodes(SimpleGraph g, ArrayList<String> u) {
		ArrayList<GraphComponent> toRemove = new ArrayList<GraphComponent>();

		for (Edge e : g.getEdges())
			if (u.contains(e.getTo().getLabel()) || u.contains(e.getFrom().getLabel()))
				toRemove.add(e);
		for (Node n : g.getNodes())
			if (u.contains(n.getLabel()))
				toRemove.add(n);

		g.removeAll(toRemove);
	}

	public SimpleGraph getGraphReply(Map<String, ContextGraph> cgs, MonitorPack monitoring) {
		GraphMatchingProcess GMQ;
		GraphTranslator GT = new GraphTranslator();
		SimpleGraph g = null;
		ContextPattern cp = null;
		ArrayList<String> labels, reqLabels;

		for (Map.Entry<String, ContextGraph> entry : cgs.entrySet()) {
			GMQ = GraphMatcherQuick.getMatcher(entry.getValue(), reqPattern, monitoring);
			if (!GMQ.getBestMatches().isEmpty()) {
				Match bestMatch = GMQ.getBestMatches().get(0);
				SimpleGraph gph = (SimpleGraph) bestMatch.getGraph();

				GraphPattern uns = bestMatch.getUnsolvedPart();
				MatchMaker mm = new MatchMaker();
				g = mm.nowKiss(gph, uns);
				labels = getNodeLabels(entry.getValue());

				switch (type) {
					case IF:
					removeUnwantedNodes(g, labels);
					g = GT.flattenGraph(g);
					GT.computeArticles(reqPattern, g);

					break;
					case INFO:
					reqLabels = getNodeLabels(reqPattern);
					reqLabels.removeAll(labels);
					removeUnwantedNodes(g, reqLabels);
					g = GT.flattenGraph(g);
					try {
						cp = computePattern(entry.getKey());
						GT.computeArticles(cp, g);
					} catch (Exception e) {
						e.printStackTrace();
					}

					break;
					case NEW_INFO: // nothing to do here
					return null;
					case WHQUESTION:
					reqLabels = getNodeLabels(reqPattern);
					removeUnwantedNodes(g, reqLabels);
					g = GT.flattenGraph(g);
					try {
						cp = computePattern(entry.getKey());
						GT.computeArticles(cp, g);
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				}
			}
		}

		return g;
	}

	public String getStringReply(Map<String, ContextGraph> cgs, MonitorPack monitoring) {
		SimpleGraph reply = getGraphReply(cgs, monitoring);
		if (reply == null)
			return "";

		return new GraphTranslator().graphToString(reply);
	}
}
