package net.xqhs.graphs.nlp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import net.xqhs.graphs.context.ContextPattern;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class Tests {
	public Parser p;

	/**
	 * @param strs
	 * @throws Exception
	 *             Initial testing center
	 */
	public static void example(HashMap<String, String> strs) throws Exception {

		if (strs != null) {
			Parser parser = new Parser();
			Properties props = new Properties();
			props.setProperty("annotators",
					"tokenize,ssplit,pos,lemma,ner,depparse,mention,coref");
			// "tokenize, ssplit, pos,lemma, depparse");
			// "tokenize,ssplit,pos,lemma,ner,parse,mention,coref,relation");
			props.setProperty("depparse.extradependencies", "MAXIMAL");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

			for (String key : strs.keySet()) {

				String str = strs.get(key);
				System.out.println("Processing paragraph: " + key);
				String filename = "out//tract//" + key // (str.substring(0,
														// Math.min(str.length(),
														// 15))).trim()
						+ System.currentTimeMillis() + "NLPAttern.txt";
				PrintWriter writer = new PrintWriter(filename, "UTF-8");
				writer.println(str);
				System.out.println("Output in " + filename);

				Annotation document = new Annotation(str);
				pipeline.annotate(document);
				System.out.println(parser.getCorefChainz(document, writer));
				// parser.getVisual(str, writer, parser);
				System.out.println(str);
				ContextPattern g = (ContextPattern) parser.getNLPattern(parser,
						writer, document, NLGraphType.PATTERN);
				g.setUnitName(key);
				// ContextPatternConverter cxt = new ContextPatternConverter(g);

				parser.getEnhancedPlusPlusPlusPlus(document, writer);

				g = (ContextPattern) ContextPatternConverter.processCorefCP(
						g.t, g, document);
				g = (ContextPattern) ContextPatternConverter
						.relabelEdgesWithAuxWords(g);

				g = Parser.fixDeterminers(g);
				writer.println(g.toString());
				parser.getCorefChainz(document, writer);

				System.out.println(g.toString());

				g = ContextPatternConverter.removeDuplicateNN(g);
				System.out.println(g.toString());
				// g = cxt.removeDuplicates(g);
				// g=cxt.invertAllEdges(g);
				parser.contextPatternVisualize(g, false);
				// parser.getGraphicalGraph(true, g);

				writer.close();
			}
		}
	}

	// public static HashMap<String, String> readPrisonrules(){
	//
	// }

	public static HashMap<String, String> readTractatus() {
		ArrayList<String> lines = new ArrayList<String>();
		HashMap<String, String> hashLines = new HashMap<String, String>();
		try {
			FileReader f = new FileReader("playground//rules//tractatusTXT.txt");

			BufferedReader br = new BufferedReader(f);

			String sCurrentLine;
			String truLine = null;
			while ((sCurrentLine = br.readLine()) != null) {

				if (sCurrentLine != null && !sCurrentLine.isEmpty()) {

					if (truLine == null)
						truLine = sCurrentLine;
					else
						truLine += " " + sCurrentLine;
				} else if (truLine != null) {
					System.out.println("current line: " + truLine);
					lines.add(truLine);
					truLine = null;
				}
			}
			br.close();
			for (String line : lines) {
				String key = line.substring(0, line.indexOf(" "));
				String val = line.substring(line.indexOf(" "));
				System.out.println("key is " + key + ".val is " + val);
				hashLines.put(key, val);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return hashLines;
	}
}