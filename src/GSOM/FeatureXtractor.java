package GSOM;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.xqhs.graphs.context.ContextGraph;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.nlp.ContextPatternConverter;
import net.xqhs.graphs.nlp.NLGraphType;
import net.xqhs.graphs.nlp.NLNode;
import net.xqhs.graphs.nlp.Parser;
import net.xqhs.graphs.nlp.forestation.AdjacencyMatrix;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class FeatureXtractor {
	public static Parser parser = new Parser();
	public static int testno;
	public static String file;
	public static PrintWriter writer;

	public static void init(int iter) throws FileNotFoundException {
		file = "out//gsom//test" + iter;
		writer = new PrintWriter(file);

	}

	public static ArrayList<Datapoint> convertor(ArrayList<String> strs,
			StanfordCoreNLP pipeline) {
		ArrayList<Datapoint> result = new ArrayList<Datapoint>();

		if (strs != null && !strs.isEmpty()) {

			ArrayList<Datapoint> lostInTranslation = new ArrayList<Datapoint>();

			int n = 0;
			// foreach paragraph
			for (String str : strs) {
				Datapoint datapoint = new Datapoint(n, str);
				n++;
				// String filename = "out//img//TESTCXTGRPH"
				// + (str.substring(0, Math.min(str.length(), 15))).trim()
				// + "NLPAttern.txt";
				// PrintWriter writer = new PrintWriter(filename, "UTF-8");
				// writer.println(str);
				// System.out.println("Output in " + filename);

				Annotation document = new Annotation(str);
				pipeline.annotate(document);
				System.out.println(parser.getCorefChainz(document, writer));

				// parser.getVisual(str, writer, parser);
				System.out.println(str);
				try {
					ContextGraph g = (ContextGraph) parser.getNLPattern(parser,
							writer, document, NLGraphType.GRAPH);
					g.setUnitName("testGraph");

					parser.getEnhancedPlusPlusPlusPlus(document, writer);

					g = (ContextGraph) ContextPatternConverter.processCorefCP(
							NLGraphType.GRAPH, g, document);
					g = (ContextGraph) ContextPatternConverter
							.relabelEdgesWithAuxWords(g);
					datapoint.g = g;
					datapoint.features = new ArrayList<Double>(
							featureExtract(g).values());
					result.add(datapoint);
				} catch (Exception e) {
					lostInTranslation.add(datapoint);
				}
			}
		}
		return result;
	}

	public static ArrayList<Datapoint> rescale(ArrayList<Datapoint> points) {
		ArrayList<Double> rangeBottom = new ArrayList<Double>();
		ArrayList<Double> rangetTop = new ArrayList<Double>();
		for (int i = 0; i < points.get(0).features.size(); i++) {
			rangeBottom.add(Double.MAX_VALUE);
			rangetTop.add(0.0);
		}

		for (Datapoint point : points) {
			for (int i = 0; i < point.features.size(); i++) {
				if (point.features.get(i) > rangetTop.get(i)) {
					rangetTop.set(i, point.features.get(i));
				}
				if (point.features.get(i) < rangeBottom.get(i)) {
					rangeBottom.set(i, point.features.get(i));
				}
			}
		}

		for (Datapoint pt : points) {
			for (int i = 0; i < pt.features.size(); i++) {
				double oldVal = pt.features.get(i);
				double maxOld = rangetTop.get(i);
				double minOld = rangeBottom.get(i);
				pt.features.set(i, 1 / maxOld - minOld * (oldVal - maxOld + 1));
			}
		}
		return points;

	}

	public static Map<String, Double> featureExtract(SimpleGraph g) {
		Double nodeNo = (double) g.getNodes().size();
		Double edgeNo = (double) g.getEdges().size();
		double avgTotalDegree = 0;

		AdjacencyMatrix adj = new AdjacencyMatrix(g);
		double rootNo = adj.getRoots().size();
		double loopNo = adj.numberOfLoops();
		double avgShortestPath = 0;
		double diameter = 0;
		int sum = 0;
		double determinerFreq = 0;
		double iNodes = 0;
		for (Node node : g.getNodes()) {

			avgTotalDegree += g.getInEdges(node).size()
					+ g.getOutEdges(node).size();

			NLNode n = (NLNode) node;
			if (n.getLabel().equals("I") || n.getLabel().equalsIgnoreCase("me")) {
				iNodes++;
			}
			determinerFreq += n.getAttributes("det").size();

			Map<Node, Integer> map = g.computeDistancesFromUndirected(node);
			for (Integer i : map.values()) {
				if (i > diameter) {
					diameter = i;
				}
				sum += i;
			}
		}
		determinerFreq = determinerFreq / nodeNo;

		avgShortestPath = sum / 2 * nodeNo * (nodeNo - 1);
		avgTotalDegree = avgTotalDegree / nodeNo;
		Map<String, Double> result = new HashMap<String, Double>();
		result.put("nodeNo", nodeNo);
		result.put("edgeNo", edgeNo);
		result.put("avgTotalDegree", avgTotalDegree);
		result.put("rootNo", rootNo);
		result.put("loopNo", loopNo);
		result.put("avgShortestPath", avgShortestPath);
		result.put("diameter", diameter);
		result.put("determinerFreq", determinerFreq);
		result.put("iNodes", iNodes);

		return result;
	}
}
