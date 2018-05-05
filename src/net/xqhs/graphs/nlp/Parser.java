package net.xqhs.graphs.nlp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.JFrame;

import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.SimpleGraph;
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
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.Layouts;
import org.graphstream.ui.swingViewer.GraphRenderer;
import org.graphstream.ui.view.Viewer;

import com.chaoticity.dependensee.Main;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.Mention;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations;
import edu.stanford.nlp.ie.machinereading.structure.RelationMention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
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

	public ArrayList<SemanticGraph> getEnhancedGraph(Annotation document,
			PrintWriter writer) {
		// try to get enhanced dependencies

		writer.println("---");
		writer.println("Enhanced PLUS PLUS dependencies");
		writer.println("---");
		ArrayList<SemanticGraph> result = new ArrayList<SemanticGraph>();
		for (CoreMap sentence : document.get(SentencesAnnotation.class)) {
			writer.println("---");

			if (sentence
					.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class) != null) {

				// SemanticGraph sg=
				// return sentence
				// .get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class);
				result.add(sentence
						.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class));
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
		return result;
	}

	public static String printPrettyTree(Tree tree) {
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

	public static StanfordCoreNLP init() {
		Properties props = new Properties();
		props.setProperty("annotators",
				"tokenize,ssplit,pos,lemma,ner,depparse,mention,coref");
		// "tokenize, ssplit, pos,lemma, depparse");
		// "tokenize,ssplit,pos,lemma,ner,parse,mention,coref,relation");
		props.setProperty("depparse.extradependencies", "MAXIMAL");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		return pipeline;
	}

	public static ArrayList<ContextPattern> convertContextPatterns(
			ArrayList<String> strs, StanfordCoreNLP pipeline) throws Exception {

		Parser parser = new Parser();
		ArrayList<ContextPattern> result = new ArrayList<ContextPattern>();
		if (strs != null && !strs.isEmpty()) {
			for (String str : strs) {

				// make output file
				String filename = "out//newtests//"
						+ System.currentTimeMillis()
						+ (str.substring(0, Math.min(str.length(), 15))).trim()
						+ "NLPAttern.txt";
				PrintWriter writer = new PrintWriter(filename, "UTF-8");
				writer.println(str);
				System.out.println("Output in " + filename);

				Annotation document = new Annotation(str);
				pipeline.annotate(document);
				System.out.println(parser.getCorefChainz(document, writer));
				// parser.getMentions(document, writer);
				// ContextPattern g =
				// parser.getContextPatternFromEnhanced(parser,
				// writer,
				// document);
				// ContextPatternConverter cPc = new ContextPatternConverter(g);
				parser.getVisual(str, writer, parser);
				System.out.println(str);
				// g = cPc.processCorefCP(g, document);
				parser.getEnhancedPlusPlusPlusPlus(document, writer);
				ContextPattern g = (ContextPattern) parser.getNLPattern(parser,
						writer, document, NLGraphType.PATTERN);

				// ContextPatternConverter cxt = new ContextPatternConverter(g);
				g = (ContextPattern) ContextPatternConverter
						.relabelEdgesWithAuxWords(g);
				g = (ContextPattern) ContextPatternConverter.processCorefCP(
						g.t, g, document);

				g = fixDeterminers(g);
				writer.println(g.toString());
				// parser.getCorefChainz(document, writer);
				// g = cPc.breakPatterns(g);
				System.out.println(g.toString());

				// g = cxt.removeDuplicateNN(g);
				g = ContextPatternConverter.removeDuplicates(g);
				// parser.contextPatternVisualize(g, true);
				// parser.getGraphicalGraph(true, g);

				writer.close();
				result.add(g);
			}
		} else
			throw new Exception("Cannot convert nothing into context patterns");
		return result;
	}

	/**
	 * @param g
	 * @param cxt
	 * @return
	 */
	public static ContextPattern fixDeterminers(ContextPattern g) {
		System.out
				.println("------------------INSTANTIATE NODES WITH DETERMINERS---------------------");

		ArrayList<NLNodeP> determinedNodes = new ArrayList<NLNodeP>();
		for (net.xqhs.graphs.graph.Node node : g.getNodes()) {
			NLNodeP nlnode = (NLNodeP) node;
			Iterator<FunctionWord> attIt = nlnode.getAttributes().iterator();
			System.out.println("Attributes of node " + nlnode);
			while (attIt.hasNext()) {
				System.out.println(" & " + attIt.next());
			}
			// determine which nodes have determiners
			ArrayList<FunctionWord> dets = nlnode.getAttributes("det");
			if (dets != null && !dets.isEmpty()) {
				determinedNodes.add(nlnode);
				System.out.println("Found  node with determiner: "
						+ nlnode.getLabel() + " " + dets);
			}
		}
		for (NLNodeP nlNodeP : determinedNodes) {

			g = ContextPatternConverter.breakDeterminer(g, nlNodeP);
		}
		return g;
	}

	// }

	public SimpleGraph getNLPattern(Parser parser, PrintWriter writer,
			Annotation document, NLGraphType t) {

		SimpleGraph g = NLGraphFactory.makeGraph(t);
		ArrayList<SemanticGraph> sgs = parser
				.getEnhancedGraph(document, writer);
		// foreach sentence in document make a graph and somehow stitch it
		// together
		for (SemanticGraph sg : sgs) {

			HashMap<String, NLNode> patNodes = new HashMap<String, NLNode>();
			HashMap<String, NLEdge> patEdges = new HashMap<String, NLEdge>();

			List<SemanticGraphEdge> edge_set1 = sg.edgeListSorted();

			patNodes.putAll(processLeafNodes(t, edge_set1, patNodes));
			System.out
					.println("Nodes in pattern before edge exam: " + patNodes);
			for (SemanticGraphEdge edge : edge_set1) {
				NLNode to = null, from = null;
				String flag = "REASONS";
				if (edge.getDependent().pseudoPosition() != Integer.MIN_VALUE) {
					// used to b createOrRetrieve
					from = retrieveNLNode(patNodes, edge.getDependent());
					if (from == null) {
						System.out.println("Dependent node: "
								+ edge.getDependent().word()
								+ " not registered in pattern." + edge);
					}
				} else
					flag = "Dependent node no longer in graph";
				if (edge.getGovernor().pseudoPosition() != Integer.MIN_VALUE) {
					// used to b createOrRetrieve
					to = retrieveNLNode(patNodes, edge.getGovernor());
					if (to == null) {
						System.out.println("Governor node: "
								+ edge.getGovernor().word()
								+ " not registered in pattern." + edge);
					}
				} else {

					// deal with multiple levels of nesting for function words
					flag = "Governor node no longer in graph";
					// function word masquerading as node
					NLNode zombie = retrieveNLNode(patNodes, edge.getGovernor());
					if (zombie != null) {
						System.out.println("Found zombie: " + zombie);

						IndexedWord iwGov = edge.getGovernor();
						List<SemanticGraphEdge> inEdgesZombie = sg
								.getIncomingEdgesSorted(iwGov);
						for (SemanticGraphEdge semanticGraphEdge : inEdgesZombie) {
							IndexedWord gov = semanticGraphEdge.getGovernor();
							if (patNodes.containsKey(gov.word() + gov.index())) {
								// find where the zombie is as an attribute of
								// some other word

								NLNode patGov = patNodes.get(gov.word()
										+ gov.index());
								boolean found = false;

								for (FunctionWord fw : patGov.getAttributes()) {
									if (fw.getLabel().equals(zombie.getLabel())
											&& fw.getWordIndex() == zombie
													.getWordIndex()) {
										found = true;
										System.out
												.println("Zombie found as attribute of "
														+ patGov);
									}
								}
								// and move all its attributes to its governor
								if (found) {
									patGov.getAttributes().addAll(
											zombie.getAttributes());
								}
							}
							break;
						}

						// maybe should only work with indexes since labels can
						// change with structures such as mwe
						if (patNodes.remove(zombie.getLabel()
								+ zombie.getWordIndex()) != null)
							System.out.println("Zombie removed: " + zombie);

						System.out.println("Nodes after removal: " + patNodes);
					}
				}
				if (edge.getWeight() != Double.MIN_VALUE && to != null
						&& from != null) {
					NLEdge nlEdge = NLEdgeFactory.makeNLEdge(t, from, to, edge
							.getRelation().toString());
					System.out.println("Created edge " + nlEdge);
					// NLEdgeP nlEdge = new NLEdgeP(from, to, edge.getRelation()
					// .toString());
					patEdges.put(
							from.getLabel() + from.getWordIndex()
									+ nlEdge.getLabel() + to.getLabel()
									+ to.getWordIndex(), nlEdge);
				} else
					System.out
							.println("Edge " + edge.getDependent().word()
									+ " --" + edge.getRelation() + "->"
									+ edge.getGovernor().word()
									+ " not added. " + flag);

			}
			System.out.println("Nodes to be added:");
			for (NLNode patNode : patNodes.values()) {
				System.out.print(" , " + patNode.getLabel());
			}
			if (t == NLGraphType.GRAPH) {
				for (String key : patNodes.keySet()) {
					NLNode n = patNodes.get(key);
					while (!patNodes
							.values()
							.stream()
							.filter(a -> a != n
									&& a.getLabel().equals(n.getLabel()))
							.collect(Collectors.toList()).isEmpty()) {
						System.out.println("Renaming node " + n);
						n.setLabel(n.getLabel() + " ");

					}
				}
			}
			g.addAll(patNodes.values());
			// for (String key : patNodes.keySet()) {
			// g.addNode(patNodes.get(key));
			// }
			// set unit name to use in print
			String unitName = " ";
			System.out.println("Total nodes:");
			for (net.xqhs.graphs.graph.Node node : g.getNodes()) {
				System.out.print(" , " + node);
				unitName += node.getLabel() + " ";
			}
			g.setUnitName(unitName);
			// g.addAll(patEdges.values());
			for (net.xqhs.graphs.graph.Edge edgeP : patEdges.values()) {
				System.out.println("Adding edge: " + edgeP);
				g.addEdge(edgeP);
			}

		}
		return g;
	}

	private HashMap<String, NLNode> processLeafNodes(NLGraphType t,
			List<SemanticGraphEdge> govEdges, HashMap<String, NLNode> nodes) {
		ArrayList<FunctionWord> fws = new ArrayList<FunctionWord>();
		NLNode gov;
		for (SemanticGraphEdge edge : govEdges) {
			gov = createOrRetrieveNLNode(t, nodes, edge.getGovernor());
			NLNode depp = retrieveNLNode(nodes, edge.getDependent());
			IndexedWord dep = edge.getDependent();

			if (edge.getDependent().index() == edge.getGovernor().index()) {
				System.out.println("Reflexive edge found:" + edge);
				edge.setWeight(Double.MIN_VALUE);
			} else {
				switch (edge.getRelation().getShortName()) {

				case "punct":
				case "discourse":

					edge.setWeight(Double.MIN_VALUE);
					// dep.setIndex(Integer.MIN_VALUE);
					dep.setPseudoPosition(Integer.MIN_VALUE);
					break;
				case "det":
				case "cop":
				case "case":
				case "aux":
				case "auxpass":
				case "cc":
				case "mark":
					// case "dep":

					// case "ref":
					String label = edge.getRelation().getShortName();
					FunctionWord fw = new FunctionWord(label,
							edge.getDependent());
					gov.getAttributes().add(fw);
					fws.add(fw);
					edge.setWeight(Double.MIN_VALUE);

					dep.setPseudoPosition(Integer.MIN_VALUE);
					if (depp != null) {
						nodes.remove(depp.getLabel() + depp.getWordIndex());
					}

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
					// dep.setIndex(Integer.MIN_VALUE);
					dep.setPseudoPosition(Integer.MIN_VALUE);
					if (depp != null) {
						nodes.remove(depp.getLabel() + depp.getWordIndex());
					}
					break;

				default:
					depp = createOrRetrieveNLNode(t, nodes, dep);
					break;
				}
			}
		}
		// avoid having mwes & other crazy language constructs as separate nodes
		// doesn't seem to work
		for (FunctionWord functionWord : fws) {
			if (nodes.containsKey(functionWord.getLabel()
					+ functionWord.getWordIndex())) {
				NLNode node = nodes.get(functionWord.getLabel()
						+ functionWord.getWordIndex());
				// no idea why i'm doing this
				System.out.println("Removed node " + node);
				functionWord.setLabel(node.getLabel());
				nodes.remove(node.getLabel() + node.getWordIndex());
			}

		}

		return nodes;
	}

	/**
	 * @param nodeMap
	 * @param word
	 * @return new or existing corresponding NLNode (with equal word and index)
	 */
	public NLNode createOrRetrieveNLNode(NLGraphType t,
			HashMap<String, NLNode> nodeMap, IndexedWord word) {
		NLNode w;

		String key = word.word() + word.index();
		if (!nodeMap.containsKey(key)) {
			w = NLNodeFactory.makeNode(t, word);
			nodeMap.put(key, w);
		} else
			w = nodeMap.get(key);

		return w;
	}

	public NLNode retrieveNLNode(HashMap<String, NLNode> nodeMap,
			IndexedWord word) {
		NLNode w;

		String key = word.word() + word.index();
		if (!nodeMap.containsKey(key)) {
			return null;
		} else
			w = nodeMap.get(key);

		return w;
	}

	public static Viewer contextPatternVisualize(SimpleGraph cxt,
			boolean andDisplay) throws IOException, InterruptedException {
		String graphId = cxt.getUnitName();// System.currentTimeMillis() +
											// cxtToSentence(cxt);

		DefaultGraph sg = new DefaultGraph(graphId, false, false);
		Map<NLNode, Node> isomorphism = new IdentityHashMap<NLNode, Node>();
		// add nodes to new graph
		for (net.xqhs.graphs.graph.Node nodd : cxt.getNodes()) {
			NLNode node = (NLNode) nodd;
			String label = node.getLabel();
			if (node instanceof NLNodeP) {
				NLNodeP nod = (NLNodeP) node;
				if (nod.isGeneric()) {
					label = node.getLabel() + nod.genericIndex();
				}
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
			NLNode fromN = (NLNode) edge.getFrom(), toN = (NLNode) edge.getTo();
			Node from = isomorphism.get(fromN);
			Node to = isomorphism.get(toN);
			String label = edge.getLabel();
			// no duplicate edges allowed
			// if (sg.getEdge(label) != null) {
			while (sg.getEdge(label) != null) {
				label += " ";
			}
			// }
			System.out.println("Adding for visualization edge " + from + " --"
					+ label + "-> " + to);
			Edge e = sg.addEdge(label, from, to, true);
			if (e != null) {
				e.addAttribute("ui.label", label);
			} else
				System.out.println("Edge not added for *REASONS*");
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
		Thread.sleep(300);
		pic.writeAll(sg, "out//newtests//img" + graphId + ".png");
		Viewer viewer;
		if (andDisplay) {

			viewer = sg.display(true);

		} else {
			viewer = new Viewer(sg, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
			GraphRenderer renderer = Viewer.newGraphRenderer();
			viewer.addView(Viewer.DEFAULT_VIEW_ID, renderer);
			Layout layout = Layouts.newLayoutAlgorithm();
			viewer.enableAutoLayout(layout);

		}
		return viewer;
	}

	// no work very bad
	public static String cxtToSentence(ContextPattern cxt) {
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

	// public static SimpleGraph collapseGenerix(SimpleGraph g) {
	//
	// }

	public static ArrayList<TreeNode<NLNode>> cxtAsForest(SimpleGraph cxt) {
		ArrayList<TreeNode<NLNode>> result = new ArrayList<TreeNode<NLNode>>();
		// populate with subtree foreach node
		for (net.xqhs.graphs.graph.Node nod : cxt.getNodes()) {

			NLNode node = (NLNode) nod;
			System.out.println("Creating subtree of node: " + node);
			TreeNode<NLNode> root = new TreeNode<NLNode>(node);
			root = getNodeSubtree(root, root, cxt);
			final TreeNode<NLNode> rroot = root;
			// if the subtree is already in the result
			if (!result.stream().anyMatch(new Predicate<TreeNode<NLNode>>() {

				@Override
				public boolean test(TreeNode<NLNode> t) {
					return t.containsSubtree(rroot);

				}
			}))
				result.add(root);
		}
		return result;
	}

	public static TreeNode<NLNode> getNodeSubtree(TreeNode<NLNode> root,
			TreeNode<NLNode> partialRoot, SimpleGraph cxt) {

		// should i add partialRoot here or will it b added by the previous
		// call? #qsqs
		Collection<net.xqhs.graphs.graph.Edge> ins = cxt.getInEdges(partialRoot
				.getData());
		if (ins != null & !ins.isEmpty()) {
			// System.out.println(" Children of: " + partialRoot.getData());

			for (net.xqhs.graphs.graph.Edge e : ins) {
				NLNode node = (NLNode) e.getFrom();
				TreeNode<NLNode> child = new TreeNode<NLNode>(node);
				// should make it into a binary search tree that keeps indexes
				// in
				// check to aid reverse transform

				// check for cycles

				if (partialRoot.addChildDuplicateSafe(root, child) != null) {
					System.out.println("   Added: " + child.getData() + " ");
					System.out.println();
					getNodeSubtree(root, child, cxt);
				} else {
					System.out.println("   -| Cycle detected");
					// return partialRoot;
				}
			}
		} else
			System.out.print(" -|");
		return partialRoot;
	}

	public static String cxtToStr(SimpleGraph cxt) {
		System.out.println("Turning to string...");
		String blah = " ";
		for (TreeNode<NLNode> root : cxtAsForest(cxt)) {
			// System.out.println("Rounded up all children of " + root.getData()
			// + " ");
			blah += " " + root.getData().toString() + ": ";
			root.getChildrenData().stream()
					.forEach(x -> System.out.print(x + " "));
			List<String> miniblah = root.getChildrenData().stream()
					.sorted(new Comparator<NLNode>() {
						@Override
						public int compare(NLNode o1, NLNode o2) {
							return Integer.valueOf(o1.getWordIndex())
									.compareTo(
											Integer.valueOf(o2.getWordIndex()));
						}

					}).map(n -> n.toString()).collect(Collectors.toList());
			blah += " " + miniblah.toString();
			blah += '\n';
		}
		System.out.println(blah);
		return blah;
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
