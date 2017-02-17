package net.xqhs.graphs.nlp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.JFrame;

import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.pattern.EdgeP;
import net.xqhs.graphs.pattern.NodeP;
import net.xqhs.graphs.representation.graphical.GraphicalGraphRepresentation;
import net.xqhs.graphs.representation.graphical.RadialGraphRepresentation;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.Unit;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSinkImages;
import org.graphstream.stream.file.FileSinkImages.LayoutPolicy;
import org.graphstream.stream.file.FileSinkImages.OutputType;
import org.graphstream.stream.file.FileSinkImages.Quality;
import org.graphstream.stream.file.FileSinkImages.Resolutions;

import com.chaoticity.dependensee.Main;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.Mention;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations;
import edu.stanford.nlp.ie.machinereading.structure.RelationMention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;

public class Parser {
	private final static String PCG_MODEL = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";

	private final TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer
			.factory(new CoreLabelTokenFactory(), "invertible=true");

	private final LexicalizedParser parser = LexicalizedParser
			.loadModel(PCG_MODEL);

	public Tree parse(String str) {
		List<CoreLabel> tokens = tokenize(str);
		Tree tree = parser.apply(tokens);
		return tree;
	}

	private List<CoreLabel> tokenize(String str) {
		Tokenizer<CoreLabel> tokenizer = tokenizerFactory
				.getTokenizer(new StringReader(str));
		return tokenizer.tokenize();
	}

	public String getCorefChainz(Annotation document, PrintWriter writer) {
		Map<Integer, CorefChain> graph = document
				.get(CorefChainAnnotation.class);
		writer.println("------CorefChainz");
		String s = " ";
		for (CorefChain cc : graph.values()) {
			writer.println(cc);
			writer.println("representative mention: "
					+ cc.getRepresentativeMention());
			s = s + cc + cc.getRepresentativeMention();
		}
		return s;

	}

	public void getMentions(Annotation document, PrintWriter writer) {
		// displays mentions
		for (CoreMap sentence : document
				.get(CoreAnnotations.SentencesAnnotation.class)) {
			writer.println("---");
			writer.println("mentions");
			for (Mention m : sentence
					.get(CorefCoreAnnotations.CorefMentionsAnnotation.class)) {
				writer.println("\t" + m);
			}
		}
	}

	// from grapherTest
	private void getGraphicalGraph(boolean useRadial, Graph G) {
		String unitName = "contextTestMain";
		GraphicalGraphRepresentation G3RX = null;
		if (!useRadial)
			G3RX = new GraphicalGraphRepresentation(G);
		else
			G3RX = new RadialGraphRepresentation(G);

		G3RX.setBackwards().setUnitName(Unit.DEFAULT_UNIT_NAME)
				.setLink(unitName).setLogLevel(Level.ALL);

		JFrame acc = new JFrame(unitName);
		acc.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		acc.setLocation(100, 100);
		acc.setSize(800, 500);
		acc.add(((GraphicalGraphRepresentation) G3RX.update())
				.displayRepresentation());
		acc.setVisible(true);
	}

	public void getRelations(Annotation document, PrintWriter writer) {
		for (RelationMention sentence : document
				.get(MachineReadingAnnotations.RelationMentionsAnnotation.class)) {
			writer.println("---");
			writer.println("relations");

			writer.println("\t" + sentence);
		}
		// writer.println("-----------Mentions & Entities----------------");
		// // display MachineReading entities and relations
		// List<CoreMap> sentences = document
		// .get(CoreAnnotations.SentencesAnnotation.class);
		// for (CoreMap sentence : sentences) {
		//
		// List<EntityMention> entities = sentence
		// .get(MachineReadingAnnotations.EntityMentionsAnnotation.class);
		// if (entities != null) {
		// writer.println("Extracted the following MachineReading entity mentions:");
		// for (EntityMention e : entities) {
		// writer.print('\t');
		// writer.println(e);
		// }
		// }
		//
		// }
		// List<RelationMention> relations = sentence
		// .get(MachineReadingAnnotations.RelationMentionsAnnotation.class);
		// if (relations != null) {
		// writer.println("Extracted the following MachineReading relation mentions:");
		// for (RelationMention r : relations) {
		// writer.println(r);
		//
		// }
		// }
	}

	public void getVisual(String str, PrintWriter writer, Parser parser) {

		Tree tree = parser.parse(str);

		List<Tree> leaves = tree.getLeaves();
		// Print words and Pos Tags
		for (Tree leaf : leaves) {
			Tree parent = leaf.parent(tree);
			writer.println(leaf.label().value() + "-" + parent.label().value()
					+ " ");
			System.out.println(leaf.label().value() + "-"
					+ parent.label().value() + " ");
		}
		writer.println(parser.printPrettyTree(tree));
		System.out.println(parser.printPrettyTree(tree));
		writer.println("Dependensee repr");
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
		// LexicalizedParser lp = LexicalizedParser
		// .loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		// lp.setOptionFlags(new String[] { "-maxLength", "500",
		// "-retainTmpSubcategories" });
		TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(
				new CoreLabelTokenFactory(), "");
		List<CoreLabel> wordList = tokenizerFactory.getTokenizer(
				new StringReader(str)).tokenize();

		tree = parser.parser.apply(wordList);
		GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
		Collection<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
		for (TypedDependency typedDependency : tdl) {
			writer.println(typedDependency.toString());
		}

		try {
			Main.writeImage(
					tree,
					tdl,
					"out//img//img"
							+ str.substring(0, Math.min(10, str.length()))
							+ ".png", 3);
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	// returns a string of newline-separated deps
	public String getEnhancedPlusPlusPlusPlus(Annotation document,
			PrintWriter writer) {
		// try to get enhanced dependencies

		String s = "qq";
		writer.println("---");
		writer.println("Enhanced PLUS PLUS dependencies");
		writer.println("---");
		for (CoreMap sentence : document.get(SentencesAnnotation.class)) {
			writer.println("---");
			if (sentence
					.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class) != null) {
				s = s
						+ sentence
								.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class)
								.toList();
				writer.print(s);

			}
		}
		return s;
	}

	public SemanticGraph getEnhancedGraph(Annotation document,
			PrintWriter writer) {
		// try to get enhanced dependencies

		writer.println("---");
		writer.println("Enhanced PLUS PLUS dependencies");
		writer.println("---");
		for (CoreMap sentence : document.get(SentencesAnnotation.class)) {
			writer.println("---");

			if (sentence
					.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class) != null) {

				// SemanticGraph sg=
				return sentence
						.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class);

				// for (CoreLabel token:
				// sentence.get(CoreAnnotations.TokensAnnotation.class)) {
				//
				// String word =
				// token.get(CoreAnnotations.TextAnnotation.class);
				// // this is the POS tag of the token
				// String pos =
				// token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
				// System.out.println(word + "/" + pos);
				// sg.getNodeByIndex(3).ta
				// }

			}
		}
		return null;
	}

	public String printPrettyTree(Tree tree) {
		if (tree != null) {
			String s = tree.label().toString();
			if (!tree.isLeaf()) {
				for (Tree kid : tree.getChildrenAsList()) {
					s += " \t-> " + printPrettyTree(kid);
				}
				return s + "\t";
			}
			return s + "\n";
		}
		return " X ";

	}

	public static void example(ArrayList<String> strs) throws Exception {

		Parser parser = new Parser();
		Properties props = new Properties();
		props.setProperty("annotators",
				"tokenize,ssplit,pos,lemma,ner,depparse,mention,coref");
		// "tokenize, ssplit, pos,lemma, depparse");
		// "tokenize,ssplit,pos,lemma,ner,parse,mention,coref,relation");
		props.setProperty("depparse.extradependencies", "MAXIMAL");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		// for (String str : strs) {
		// String str =
		// "The world is like an apple spinning silently in space.";
		String str = "A weak pawn is one that is not defended by another pawn, which means that it must be defended by other pieces, when it is under attack.";
		// make output file
		String filename = "out//superstitions//"
				+ (str.substring(0, Math.min(str.length(), 15))).trim()
				+ "NLPAttern.txt";
		PrintWriter writer = new PrintWriter(filename, "UTF-8");
		writer.println(str);
		System.out.println("Output in " + filename);

		Annotation document = new Annotation(str);
		pipeline.annotate(document);
		System.out.println(parser.getCorefChainz(document, writer));
		// parser.getMentions(document, writer);
		// ContextPattern g = parser.getContextPatternFromEnhanced(parser,
		// writer,
		// document);
		// ContextPatternConverter cPc = new ContextPatternConverter(g);
		parser.getVisual(str, writer, parser);
		System.out.println(str);
		// g = cPc.processCorefCP(g, document);
		parser.getEnhancedPlusPlusPlusPlus(document, writer);
		ContextPattern g = parser.getNLPattern(parser, writer, document);

		ContextPatternConverter cxt = new ContextPatternConverter(g);
		g = cxt.relabelEdgesWithAuxWords(g);
		g = cxt.processCorefCP(g, document);

		g = fixDeterminers(g);
		writer.println(g.toString());
		// parser.getCorefChainz(document, writer);
		// g = cPc.breakPatterns(g);
		System.out.println(g.toString());

		// g = cxt.removeDuplicateNN(g);
		g = cxt.removeDuplicates(g);
		// g=cxt.invertAllEdges(g);
		parser.contextPatternVisualize(g, true);
		// parser.getGraphicalGraph(true, g);

		writer.close();
	}

	/**
	 * @param g
	 * @param cxt
	 * @return
	 */
	public static ContextPattern fixDeterminers(ContextPattern g) {
		System.out
				.println("------------------INSTANTIATE NODES WITH DETERMINERS---------------------");
		ContextPatternConverter cxt = new ContextPatternConverter(g);
		ArrayList<NLNodeP> determinedNodes = new ArrayList<NLNodeP>();
		for (net.xqhs.graphs.graph.Node node : g.getNodes()) {
			NLNodeP nlnode = (NLNodeP) node;
			Iterator<ArrayList<FunctionWord>> attIt = nlnode.getAttributes()
					.values().iterator();
			while (attIt.hasNext()) {
				System.out.println(" & " + attIt.next());
			}
			if (nlnode.getAttributes("det") != null) {
				determinedNodes.add(nlnode);
				System.out.println("Found  node with determiner: "
						+ nlnode.getLabel());
			}
		}
		for (NLNodeP nlNodeP : determinedNodes) {
			g = cxt.breakDeterminer(g, nlNodeP);
		}
		return g;
	}

	// }

	public ContextPattern getNLPattern(Parser parser, PrintWriter writer,
			Annotation document) {
		SemanticGraph sg = parser.getEnhancedGraph(document, writer);

		ContextPattern g = new ContextPattern();

		List<IndexedWord> nodes = sg.vertexListSorted();
		HashMap<String, NLNodeP> patNodes = new HashMap<String, NLNodeP>();
		HashMap<String, NLEdgeP> patEdges = new HashMap<String, NLEdgeP>();
		Set<IndexedWord> leaves = sg.getLeafVertices();
		List<SemanticGraphEdge> edge_set1 = sg.edgeListSorted();

		// process det, mwe, case, cop
		// for (IndexedWord indexedWord : leaves) {
		// List<SemanticGraphEdge> govEdges = sg
		// .getIncomingEdgesSorted(indexedWord);
		// patNodes.putAll(processLeafNodes(govEdges, patNodes));
		// }
		patNodes.putAll(processLeafNodes(edge_set1, patNodes));
		for (SemanticGraphEdge edge : edge_set1) {
			NLNodeP to = null, from = null;
			String flag = "REASONS";
			if (edge.getDependent().index() != Integer.MIN_VALUE) {
				from = createOrRetrieveNLNode(patNodes, edge.getDependent());
			} else
				flag = "Dependent node no longer in graph";
			if (edge.getGovernor().index() != Integer.MIN_VALUE) {
				to = createOrRetrieveNLNode(patNodes, edge.getGovernor());
			} else
				flag = "Governor node no longer in graph";
			if (edge.getWeight() != Double.MIN_VALUE && to != null
					&& from != null) {

				NLEdgeP nlEdge = new NLEdgeP(from, to, edge.getRelation()
						.toString());
				patEdges.put(
						from.getLabel() + from.getWordIndex()
								+ nlEdge.getLabel() + to.getLabel()
								+ to.getWordIndex(), nlEdge);
			} else
				System.out.println("Edge " + edge.getDependent().word() + " --"
						+ edge.getRelation() + "->" + edge.getGovernor().word()
						+ " not added. " + flag);

		}
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sent : sentences) {
			List<CoreLabel> tokens = sent.get(TokensAnnotation.class);

			for (IndexedWord nodeSG : nodes) {
				if (nodeSG.index() != Integer.MIN_VALUE) {
					if (!patNodes.containsKey(nodeSG.word() + nodeSG.index())) {
						NLNodeP node = createOrRetrieveNLNode(patNodes, nodeSG);
						if (!node.isGeneric()) {
							CoreLabel tok = tokens.get(node.getWordIndex());
							node.setPos(tok
									.getString(PartOfSpeechAnnotation.class));
						}
					}
				} else
					patNodes.remove(nodeSG.word() + nodeSG.index());
			}
		}
		// add results of leaf processing
		// nodes first

		for (String key : patNodes.keySet()) {
			g.addNode(patNodes.get(key));
		}

		for (EdgeP edgeP : patEdges.values()) {
			g.addEdge(edgeP);
		}
		return g;
	}

	public ContextPattern getNLPattern2(Parser parser, PrintWriter writer,
			Annotation document) {
		SemanticGraph sg = parser.getEnhancedGraph(document, writer);

		ContextPattern g = new ContextPattern();

		List<IndexedWord> nodes = sg.vertexListSorted();
		HashMap<String, NLNodeP> patNodes = new HashMap<String, NLNodeP>();
		HashMap<String, NLEdgeP> patEdges = new HashMap<String, NLEdgeP>();
		Set<IndexedWord> leaves = sg.getLeafVertices();
		List<SemanticGraphEdge> edge_set1 = sg.edgeListSorted();

		// process det, mwe, case, cop
		for (IndexedWord indexedWord : leaves) {
			List<SemanticGraphEdge> govEdges = sg
					.getIncomingEdgesSorted(indexedWord);
			patNodes.putAll(processLeafNodes(govEdges, patNodes));
		}

		for (SemanticGraphEdge edge : edge_set1) {
			NLNodeP to = null, from = null;
			String flag = "REASONS";
			if (edge.getDependent().index() != Integer.MIN_VALUE) {
				from = createOrRetrieveNLNode(patNodes, edge.getDependent());
			} else
				flag = "Dependent node no longer in graph";
			if (edge.getGovernor().index() != Integer.MIN_VALUE) {
				to = createOrRetrieveNLNode(patNodes, edge.getGovernor());
			} else
				flag = "Governor node no longer in graph";
			if (edge.getWeight() != Double.MIN_VALUE && to != null
					&& from != null) {

				NLEdgeP nlEdge = new NLEdgeP(from, to, edge.getRelation()
						.getShortName());
				patEdges.put(
						from.getLabel() + from.getWordIndex()
								+ nlEdge.getLabel() + to.getLabel()
								+ to.getWordIndex(), nlEdge);
			} else
				System.out.println("Edge " + edge.getDependent().word() + " --"
						+ edge.getRelation().getShortName() + "->"
						+ edge.getGovernor().word() + " not added. " + flag);

		}
		for (IndexedWord nodeSG : nodes) {
			if (nodeSG.index() != Integer.MIN_VALUE) {
				if (!patNodes.containsKey(nodeSG.word() + nodeSG.index())) {
					createOrRetrieveNLNode(patNodes, nodeSG);
				}
			} else
				patNodes.remove(nodeSG.word() + nodeSG.index());
		}
		// add results of leaf processing
		// nodes first

		for (String key : patNodes.keySet()) {
			g.addNode(patNodes.get(key));
		}

		for (EdgeP edgeP : patEdges.values()) {
			g.addEdge(edgeP);
		}
		return g;
	}

	// public ContextPattern getContextPatternFromEnhanced(Parser parser,
	// PrintWriter writer, Annotation document) {
	// SemanticGraph sg = parser.getEnhancedGraph(document, writer);
	// ContextPattern g = new ContextPattern();
	// GraphOperations goCtx = new GraphOperations(g);
	// List<IndexedWord> nodes = sg.vertexListSorted();
	// HashMap<String, GraphComponent> leafProcessedGraphComponents = new
	// HashMap<String, GraphComponent>();
	// Set<IndexedWord> leaves = sg.getLeafVertices();
	// List<SemanticGraphEdge> edge_set1 = sg.edgeListSorted();
	//
	// // process det, mwe, case, cop
	// for (IndexedWord indexedWord : leaves) {
	// List<SemanticGraphEdge> govEdges = sg
	// .getIncomingEdgesSorted(indexedWord);
	// leafProcessedGraphComponents.putAll(processLeafNodes(govEdges));
	// }
	//
	// for (SemanticGraphEdge edge : edge_set1) {
	// NLNodeP to = null, from = null;
	// String flag = "REASONS: ";
	//
	// if (edge.getDependent().index() != Integer.MIN_VALUE) {
	// from = createNLNode(leafProcessedGraphComponents,
	// edge.getDependent());
	// } else
	// flag += "Dependent node no longer in graph";
	//
	// if (edge.getGovernor().index() != Integer.MIN_VALUE) {
	// to = createNLNode(leafProcessedGraphComponents,
	// edge.getGovernor());
	// } else
	// flag += "Governor node no longer in graph";
	// if (edge.getWeight() != Double.MIN_VALUE && to != null
	// && from != null) {
	//
	// NLEdgeP nlEdge = new NLEdgeP(from, to, edge.getRelation()
	// .getShortName());
	// leafProcessedGraphComponents.put(
	// "EE" + from.getLabel() + from.getSentenceIndex()
	// + nlEdge.getLabel() + to.getLabel()
	// + to.getSentenceIndex(), nlEdge);
	// } else
	// System.out.println("Edge " + edge.getDependent().word() + " --"
	// + edge.getRelation().getShortName() + "->"
	// + edge.getGovernor().word() + " not added. " + flag);
	//
	// }
	// for (IndexedWord nodeSG : nodes) {
	// if (!leafProcessedGraphComponents.containsKey(nodeSG.word()
	// + nodeSG.index())) {
	// createNLNode(leafProcessedGraphComponents, nodeSG);
	// }
	// }
	// g.addAll(leafProcessedGraphComponents.values());
	//
	// HashMap<String, NodeP> patNodes = new HashMap<String, NodeP>();
	// ArrayList<EdgeP> patEdges = new ArrayList<EdgeP>();
	// // construct hashmap of nodes+wordIndex for determining edges
	// for (IndexedWord nodeSG : nodes) {
	//
	// NodeP patNode = new NodeP(String.format(nodeSG.word() + "%2d",
	// nodeSG.index()));
	// patNodes.put(patNode.getLabel(), patNode);
	//
	// }
	//
	// for (SemanticGraphEdge edge : edge_set1) {
	//
	// String dep = String.format(edge.getDependent().word() + "%2d", edge
	// .getDependent().index());
	// String gov = String.format(edge.getGovernor().word() + "%2d", edge
	// .getGovernor().index());
	//
	// GrammaticalRelation relation = edge.getRelation();
	//
	// patEdges.add(new EdgeP(patNodes.get(dep), patNodes.get(gov),
	// relation.toString()));
	// // System.out.println("Converting " + relation.toPrettyString());
	// System.out.println("Adding edge:" + dep + "--"
	// + relation.toString() + "->" + gov);
	//
	// }
	// for (NodeP nodeP : patNodes.values()) {
	// String l = nodeP.getLabel();
	// l = l.substring(0, l.length() - 2);
	// while (!g.getNodesNamed(nodeP.getLabel()).isEmpty()) {
	// l.concat("*");
	// }
	// nodeP.setLabel(l);
	// goCtx.addNode(nodeP);
	// // g.addNode(nodeP);
	// }
	// g.addAll(patEdges);
	//
	// return g;
	// }

	private HashMap<String, NLNodeP> processLeafNodes(
			List<SemanticGraphEdge> govEdges, HashMap<String, NLNodeP> nodes) {

		NLNodeP gov;
		for (SemanticGraphEdge edge : govEdges) {
			gov = createOrRetrieveNLNode(nodes, edge.getGovernor());
			IndexedWord dep = edge.getDependent();
			switch (edge.getRelation().getShortName()) {

			case "punct":
			case "discourse":

				edge.setWeight(Double.MIN_VALUE);
				dep.setIndex(Integer.MIN_VALUE);
				break;
			case "det":
			case "cop":
			case "case":
			case "aux":
			case "auxpass":
			case "cc":
			case "mark":
			case "dep":
			case "ref":
				String label = edge.getRelation().getShortName();
				gov.addAttribute(new FunctionWord(label, edge.getDependent()));
				edge.setWeight(Double.MIN_VALUE);
				dep.setIndex(Integer.MIN_VALUE);
				break;
			case "flat":
			case "fixed":
			case "compound":
			case "comp":
			case "mwe":

				if (dep.index() < gov.getWordIndex()) {
					gov.setLabel(dep.word() + " " + gov.getLabel());
				} else
					gov.setLabel(gov.getLabel() + " " + dep.word());

				edge.setWeight(Double.MIN_VALUE);
				dep.setIndex(Integer.MIN_VALUE);
				break;

			default:

				break;
			}
		}
		return nodes;
	}

	/**
	 * @param nodeMap
	 * @param word
	 * @return new or existing corresponding NLNode (with equal word and index)
	 */
	public NLNodeP createOrRetrieveNLNode(HashMap<String, NLNodeP> nodeMap,
			IndexedWord word) {
		NLNodeP w;

		String key = word.word() + word.index();
		if (!nodeMap.containsKey(key)) {
			w = new NLNodeP(word);
			nodeMap.put(key, w);
		} else
			w = nodeMap.get(key);

		return w;
	}

	public void contextPatternVisualize(ContextPattern cxt, boolean andDisplay)
			throws IOException, InterruptedException {
		String graphId = "[PREDTRIALS]" + cxtToSentence(cxt);

		DefaultGraph sg = new DefaultGraph(graphId, false, false);
		Map<NodeP, Node> isomorphism = new IdentityHashMap<NodeP, Node>();
		// add nodes to new graph
		for (net.xqhs.graphs.graph.Node nod : cxt.getNodes()) {
			NodeP node = (NodeP) nod;
			String label = node.getLabel();
			if (node.isGeneric()) {
				label = node.getLabel() + node.genericIndex();
			} else
				while (sg.getNode(label) != null) {
					label += "*";
				}
			Node n = sg.addNode(label);
			isomorphism.put(node, n);
			System.out.println("Added for visualization node " + n.getId()
					+ " equivalent in cxt of " + node.getLabel());
			n.addAttribute("ui.label", label);

		}
		// add edges
		for (net.xqhs.graphs.graph.Edge edge : cxt.getEdges()) {
			NodeP fromN = (NodeP) edge.getFrom(), toN = (NodeP) edge.getTo();
			Node from = isomorphism.get(fromN);
			Node to = isomorphism.get(toN);
			String label = edge.getLabel();
			// no duplicate edges allowed
			if (sg.getEdge(label) != null) {
				while (sg.getEdge(label) != null) {
					label += "*";
				}
			}
			System.out.println("Adding for visualization edge " + from + " --"
					+ label + "-> " + to);
			Edge e = sg.addEdge(label, from, to, true);
			e.addAttribute("ui.label", label);
		}
		sg.addAttribute("ui.stylesheet", "node { text-size: 18px; } "
				+ "edge { text-size: 16px; } ");
		sg.addAttribute("ui.quality", "4");
		// makes bad screenshot with all nodes on top of each other
		// sg.addAttribute("ui.screenshot", "out//img//" + graphId + ".png");
		FileSinkImages pic = new FileSinkImages(OutputType.PNG, Resolutions.VGA);
		pic.setAutofit(true);
		pic.setClearImageBeforeOutputEnabled(true);
		pic.setLayoutPolicy(LayoutPolicy.COMPUTED_FULLY_AT_NEW_IMAGE);
		pic.setQuality(Quality.HIGH);
		pic.setResolution(Resolutions.HD720);
		Thread.sleep(100);
		pic.writeAll(sg, "out//img//" + graphId + ".png");
		if (andDisplay) {

			sg.display().enableAutoLayout();
		}
	}

	private String cxtToSentence(ContextPattern cxt) {
		HashMap<Integer, NLNodeP> nodes = new HashMap<Integer, NLNodeP>();
		int minOutEdges = Integer.MAX_VALUE;
		NLNodeP root = null;
		for (net.xqhs.graphs.graph.Node nodeP : cxt.getNodes()) {
			NLNodeP node = (NLNodeP) nodeP;
			if (!node.isGeneric()) {
				nodes.put(node.getWordIndex(), node);
			}
			if (cxt.getOutEdges(nodeP).size() < minOutEdges) {
				minOutEdges = cxt.getOutEdges(nodeP).size();
				root = node;
			}
		}
		// String out = "[" + root + "]";
		String out = "[DuplicatesSolved]";
		for (Integer key : nodes.keySet()) {
			NLNodeP nlNodeP = nodes.get(key);
			out += " " + nlNodeP.getLabel();
		}
		return out;
	}

	/**
	 * @param writer
	 * @param document
	 */
	public String getSkeleton(PrintWriter writer, Annotation document) {
		writer.print("------------ Phrase skeleton---------------");
		List<CoreMap> sentences = document
				.get(CoreAnnotations.SentencesAnnotation.class);
		if (!sentences.isEmpty()) {
			for (CoreMap sentence : sentences) {
				Tree tree = sentence
						.get(TreeCoreAnnotations.TreeAnnotation.class);
				// sentence.get(POSTaggerAnnotator.PartOfSpeechAnnotation.class);
				String s = printPrettyTree(tree);
				writer.println(s);
				return s;
			}
		}

		return "Nothing to parse";

	}
}
// dinosaurs

// String eppdsa = parser
// .getEnhancedPlusPlusPlusPlus(document, writer);

// String[] eppdsas = eppdsa.split("\\(|\\,|\n");
// System.out.println(eppdsas);
// for (CoreMap sentence : document.get(SentencesAnnotation.class))
// {
// SemanticGraph dependencies1 = sentence
// .get(CollapsedDependenciesAnnotation.class);
// String dep_type = "CollapsedDependenciesAnnotation";
// writer.println();
// writer.println(dep_type + " ===>>");
// writer.println("Sentence: " + sentence.toString());
// writer.println("DEPENDENCIES: " + dependencies1.toList());
// writer.println("DEPENDENCIES SIZE: " + dependencies1.size());

// writer.println("---");
// writer.println("coref chains");
// for (CorefChain cc : document.get(
// CorefCoreAnnotations.CorefChainAnnotation.class)
// .values()) {
// writer.println("\t" + cc);
// // List<CorefChain.CorefMention> mentions=
// for (CorefChain.CorefMention corefMention : cc
// .getMentionsInTextualOrder()) {
// writer.println(corefMention.mentionSpan + "  "
// + corefMention.position + " "
// + corefMention.mentionType);
// }
// }

// // reset annotators / flush before
// StanfordCoreNLP.clearAnnotatorPool();
// props.setProperty("annotators",
// "tokenize,ssplit,pos,lemma,ner,parse");
// pipeline = new StanfordCoreNLP(props);
//
// System.out.println("----switching parsers -----");
//
// for (String str : strs) {
// String filename = "out//"
// + (str.substring(0, Math.min(str.length(), 10))).trim()
// + "depCore.txt";
// try {
// PrintWriter writer = new PrintWriter(new BufferedWriter(
// new FileWriter(filename, true)));
// // printwriter that
// //appends
//
// System.out.println("Output in " + filename);
// Annotation document = new Annotation(str);
// pipeline.annotate(document);
// String treeStr = parser.getSkeleton(writer, document);
// writer.close();
// } catch (Exception e) {
// throw e;
// }
// }