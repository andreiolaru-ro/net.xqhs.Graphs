package net.xqhs.graphs.nlp;

import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;

import net.xqhs.graphs.context.ContextGraph;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class GraphConverter {
	String inLocation;
	ContextGraph g;

	public ContextGraph getG() {
		return g;
	}

	public void setG(ContextGraph g) {
		this.g = g;
	}

	// needs
	public GraphConverter(String str) throws Exception {
		if (str != null) {
			g = new ContextGraph();
			Parser parser = new Parser();
			Properties props = new Properties();
			props.setProperty("annotators",
					"tokenize,ssplit,pos,lemma,ner,depparse,mention,coref");

			props.setProperty("depparse.extradependencies", "MAXIMAL");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
			// String str =
			// "A speck in the visual field, though it need not be red, must have some colour: it is, so to speak, surrounded by colour-space.";
			// make output file
			String filename = "out//img//TESTCXTGRPH"
					+ (str.substring(0, Math.min(str.length(), 15))).trim()
					+ "NLPAttern.txt";
			PrintWriter writer = new PrintWriter(filename, "UTF-8");
			writer.println(str);
			System.out.println("Output in " + filename);

			Annotation document = new Annotation(str);
			pipeline.annotate(document);
			System.out.println(parser.getCorefChainz(document, writer));

			// parser.getVisual(str, writer, parser);
			System.out.println(str);
			g = (ContextGraph) parser.getNLPattern(parser, writer, document,
					NLGraphType.GRAPH);
			g.setUnitName("testGraph");

			parser.getEnhancedPlusPlusPlusPlus(document, writer);

			g = (ContextGraph) ContextPatternConverter.processCorefCP(
					NLGraphType.GRAPH, g, document);
			g = (ContextGraph) ContextPatternConverter
					.relabelEdgesWithAuxWords(g);
			g = (ContextGraph) ContextPatternConverter.removeDuplicates(
					NLGraphType.GRAPH, g);
			Parser.contextPatternVisualize(g, true);
		}
	}

	public GraphConverter(List<String> strs, StanfordCoreNLP pipeline)
			throws Exception {
		if (strs != null) {
			Parser parser = new Parser();
			for (String str : strs) {

				// Properties props = new Properties();
				// props.setProperty("annotators",
				// "tokenize,ssplit,pos,lemma,ner,depparse,mention,coref");

				// props.setProperty("depparse.extradependencies", "MAXIMAL");
				// StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
				// String str =
				// "A speck in the visual field, though it need not be red, must have some colour: it is, so to speak, surrounded by colour-space.";
				// make output file
				String filename = "out//img//TESTCXTGRPH"
						+ (str.substring(0, Math.min(str.length(), 15))).trim()
						+ "NLPAttern.txt";
				PrintWriter writer = new PrintWriter(filename, "UTF-8");
				writer.println(str);
				System.out.println("Output in " + filename);

				Annotation document = new Annotation(str);
				pipeline.annotate(document);
				System.out.println(parser.getCorefChainz(document, writer));

				// parser.getVisual(str, writer, parser);
				System.out.println(str);
				g = (ContextGraph) parser.getNLPattern(parser, writer,
						document, NLGraphType.GRAPH);
				g.setUnitName("testGraph");

				parser.getEnhancedPlusPlusPlusPlus(document, writer);

				g = (ContextGraph) ContextPatternConverter.processCorefCP(
						NLGraphType.GRAPH, g, document);
				g = (ContextGraph) ContextPatternConverter
						.relabelEdgesWithAuxWords(g);
				g = (ContextGraph) ContextPatternConverter.removeDuplicates(
						NLGraphType.GRAPH, g);
				Parser.contextPatternVisualize(g, true);
			}
		}
	}

}