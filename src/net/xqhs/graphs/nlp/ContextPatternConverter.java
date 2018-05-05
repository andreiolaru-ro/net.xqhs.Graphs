package net.xqhs.graphs.nlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.pattern.EdgeP;
import net.xqhs.graphs.pattern.NodeP;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.IntPair;

//should b static
public class ContextPatternConverter {
	public final static String determinerRole = "det";
	public final static String equivalence = "=";

	// private int genericIndex;
	//
	// public int getGenericIndex() {
	// return genericIndex;
	// }
	//
	// public void setGenericIndex(int genericIndex) {
	// this.genericIndex = genericIndex;
	// }
	//
	// public ContextPatternConverter(SimpleGraph g) {
	// genericIndex = 0;
	// for (Node node : g.getNodes()) {
	// if (((NodeP) node).isGeneric()) {
	// genericIndex++;
	// }
	// }
	// }

	public static ContextPattern invertAllEdges(ContextPattern g) {
		ArrayList<net.xqhs.graphs.graph.Edge> old = new ArrayList<net.xqhs.graphs.graph.Edge>();
		ArrayList<net.xqhs.graphs.graph.Edge> news = new ArrayList<net.xqhs.graphs.graph.Edge>();
		for (net.xqhs.graphs.graph.Edge e : g.getEdges()) {
			news.add(new EdgeP((NodeP) e.getTo(), (NodeP) e.getFrom(), e
					.getLabel()));
			old.add(e);
		}
		g.removeAll(old);
		g.addAll(news);
		return g;
	}

	public static SimpleGraph relabelEdgesWithAuxWords(SimpleGraph g) {
		System.out
				.println("------------------PROCESSING FUNCTION WORDS---------------------");
		// move roles off labels and into special property
		g.getEdges()
				.stream()
				.forEach(

				// must find a way to eliminate roles such as nsubj:xsubj from
				// edges
						m -> {
							String[] splitLabel = m.getLabel().split(":");

							((NLEdge) m).setRole(splitLabel[0]);
							String label = m.getLabel().substring(
									splitLabel[0].length());
							m.setLabel(label.isEmpty() ? " " : label
									.substring(1));
							System.out.println(m.getFrom() + " --"
									+ m.getLabel() + " -->" + m.getTo()
									+ " has role:" + ((NLEdge) m).getRole());
						});

		for (Node node : g.getNodes()) {

			NLNode nlNode = (NLNode) node;
			// get all filterwords from their arraylist
			ArrayList<FunctionWord> fw = (ArrayList<FunctionWord>) nlNode
					.getAttributes().stream()
					.filter(a -> !a.getTag().equals("det"))
					.collect(Collectors.toList());
			if (!fw.isEmpty()) {
				// HashMap<String, ArrayList<FunctionWord>> copy = nlNode
				// .getAttributes();
				// System.out.println("Attributes of node " + nlNode + " :"
				// + nlNode.getAttributes());
				// for (String key : copy.keySet()) {
				// if (!key.equals("det")) {
				// fw.addAll(nlNode.getAttributes().get(key));
				// // nlNode.getAttributes().remove(key);
				// }
				//
				// }

				// assign them to edges using destination
				ArrayList<Edge> edges = new ArrayList<Edge>();
				edges.addAll(g.getInEdges(nlNode));
				edges.addAll(g.getOutEdges(nlNode));
				// sort function words by index

				fw.sort(new Comparator<FunctionWord>() {
					@Override
					public int compare(FunctionWord o1, FunctionWord o2) {
						return Integer.valueOf(o1.getWordIndex()).compareTo(
								Integer.valueOf(o2.getWordIndex()));
					}

				});
				// if the node is isolated and has function words attached merge
				// their labels
				if (edges == null || edges.isEmpty()) {
					System.out.println("Isolated node: " + nlNode);
					String finalLabel = null;
					FunctionWord last = null;
					for (int i = 0; i < fw.size()
							&& fw.get(i).getWordIndex() < nlNode.getWordIndex(); i++) {
						FunctionWord w = fw.get(i);

						finalLabel += w.getLabel();
						last = w;
					}
					finalLabel += nlNode.getLabel();
					for (int j = fw.indexOf(last); j < fw.size(); j++) {
						finalLabel += fw.get(j).getLabel();
					}
				} else {

					// sort edges by word index of the nodes opposite from
					// current
					edges.sort(new Comparator<Edge>() {

						@Override
						public int compare(Edge e1, Edge e2) {
							NLNode other1 = e1.getFrom().equals(e2.getFrom())
									|| e1.getFrom().equals(e2.getTo()) ? (NLNode) e1
									.getTo() : (NLNode) e1.getFrom();
							NLNode other2 = e2.getFrom().equals(e1.getFrom())
									|| e2.getFrom().equals(e1.getTo()) ? (NLNode) e2
									.getTo() : (NLNode) e2.getFrom();
							return Integer.valueOf(other1.getWordIndex())
									.compareTo(other2.getWordIndex());
						}
					});

					ArrayList<NLNode> neighbors;
					// determine all neighboring nodes by extracting them from
					// edges
					neighbors = (ArrayList<NLNode>) edges
							.stream()
							.map(q -> q.getFrom().equals(node) ? (NLNode) q
									.getTo() : (NLNode) q.getFrom())
							.collect(Collectors.toList());
					// ArrayList<String> already = new ArrayList<String>();
					System.out.println("Neighbors of node " + nlNode + " :"
							+ neighbors);
					// filter out words that have been assigned to edges by
					// default parser
					// fw.removeIf(f ->already.contains(f.getLabel()));
					// where already= list of all function words whose labels
					// are already
					// present on some edges in the form of space
					// separated words after the symbol : of the edge labels

					fw.removeIf(f -> (edges.stream().map(Edge::getLabel)
							.filter(e -> !e.isEmpty())
							.flatMap(e -> Arrays.asList(e.split(" ")).stream())
							.collect(Collectors.toList())).contains(f
							.getLabel()));
					System.out
							.println("Function words not already on edgelabels: "
									+ fw);
					// create the adjacency matrix
					if (!fw.isEmpty()) {
						ArrayList<ArrayList<Integer>> distances = new ArrayList<ArrayList<Integer>>();
						for (NLNode neighborNode : neighbors) {
							ArrayList<Integer> distancesFromFWsPerNode = new ArrayList<Integer>();

							for (FunctionWord w : fw) {
								distancesFromFWsPerNode.add(Math.abs(w
										.getWordIndex()
										- neighborNode.getWordIndex()));
							}
							distances.add(distancesFromFWsPerNode);
							System.out.println("The associations of node "
									+ neighborNode + " :"
									+ distancesFromFWsPerNode);
						}
						// create map of function word repartition
						HashMap<NLNode, ArrayList<FunctionWord>> toMoveMap4All = new HashMap<NLNode, ArrayList<FunctionWord>>();
						for (NLNode n : neighbors) {
							toMoveMap4All.put(n, new ArrayList<FunctionWord>());
						}
						// calculate vertical minimum in adjancency[dis not a
						// word?]
						// matrix
						for (FunctionWord w : fw) {
							ArrayList<Integer> vertical = new ArrayList<Integer>();
							Integer min = Integer.MAX_VALUE;
							for (NLNode neighbor : neighbors) {
								Integer current = distances.get(
										neighbors.indexOf(neighbor)).get(
										fw.indexOf(w));
								vertical.add(current);
								if (current < min) {
									min = current;
								}
							}
							final Integer minimum = min;
							// leave 1s where the minimum distances are and 0s
							// for
							// the
							// rest.
							vertical = (ArrayList<Integer>) vertical.stream()
									.map(x -> x > minimum ? 0 : 1)
									.collect(Collectors.toList());

							// assign

							boolean assigned = false;
							while (!assigned) {
								for (int i = vertical.size() - 1; i >= 0; i--) {
									int integer = vertical.get(i);
									if (integer != 0) {
										toMoveMap4All.get(
												neighbors.get(vertical
														.indexOf(integer)))
												.add(w);
										assigned = true;
										continue;
									}
								}
							}
						}

						for (Edge e : edges) {

							NLNode currentOppositeNode = e.getFrom().equals(
									node) ? (NLNode) e.getTo() : (NLNode) e
									.getFrom();

							// list of function words whose index is closest to
							// current opposite
							// node than to any other neighbor node

							ArrayList<FunctionWord> toMove = toMoveMap4All
									.get(currentOppositeNode);

							if (toMove != null && !toMove.isEmpty()) {
								String label = toMove
										.stream()
										.map(FunctionWord::getLabel)
										.reduce(e.getLabel(),
												(a, b) -> a += " : " + b + " ");
								// + e.getLabel();
								System.out.print("Relabeling edge "
										+ e.getFrom() + " -- " + e.getLabel()
										+ "->" + e.getTo() + "  to  ");
								e.setLabel(label);
								System.out.println(label);
								toMoveMap4All.remove(currentOppositeNode);

								System.out
										.println("Function words left after relabeling the edge to :"
												+ e);
								toMoveMap4All.values().stream()
										.forEach(w -> System.out.println(w));
							}
						}
					}
				}
			}
		}
		return g;
	}

	public static ContextPattern removeDuplicates(ContextPattern g) {
		System.out
				.println("------------------DUPLICATES REMOVAL---------------------");
		GraphOperations goG = new GraphOperations(NLGraphType.PATTERN, g);
		ArrayList<NLNodeP> froms = new ArrayList<NLNodeP>(), tos = new ArrayList<NLNodeP>();
		HashMap<String, ArrayList<NLNodeP>> abstractables = new HashMap<String, ArrayList<NLNodeP>>();
		for (Node node : g.getNodes()) {

			System.out.println("current node: " + node.getLabel()
					+ ((NLNodeP) node).getWordIndex());
			NLNodeP nlNode = (NLNodeP) node;
			if (!nlNode.isGeneric() && !tos.contains(nlNode)
					&& !froms.contains(nlNode)) {
				// check that node not already marked & not generic
				if (nlNode.getPos().contains("NN")) {
					if (g.getNodesNamed(nlNode.getLabel()).size() > 1) {

						for (Node duplicateNode : g.getNodesNamed(nlNode
								.getLabel())) {

							if (!((NLNodeP) duplicateNode).equals(nlNode)) {
								froms.add((NLNodeP) duplicateNode);
								tos.add(nlNode);
								System.out.println("NLNode " + node.getLabel()
										+ ((NLNodeP) node).getWordIndex()
										+ nlNode.getPos() + " will be removed");
								// goG.mergeNodes((NodeP) n, (NodeP) nlNode);
							}
						}

					}
				} else {
					if (g.getNodesNamed(nlNode.getLabel()).size() > 1) {

						if (abstractables.get(nlNode.getLabel()) != null) {
							abstractables.get(nlNode.getLabel()).add(nlNode);

						} else {
							ArrayList<NLNodeP> nodesWithSameName = new ArrayList<NLNodeP>();
							nodesWithSameName.add(nlNode);
							abstractables.put(nlNode.getLabel(),
									nodesWithSameName);

						}
					}

				}
			}
		}
		// doDelete
		// all nodes that require instantiation
		for (String key : abstractables.keySet()) {
			// create copy node
			NLNodeP conceptNode = (NLNodeP) goG.addNode(new NLNodeP(
					(abstractables.get(key)).iterator().next()));
			// /delete duplicates & reattach edges to instance nodes
			for (NLNodeP parasite : abstractables.get(key)) {

				NLNodeP genericNode = new NLNodeP();
				g.addNode(genericNode, true);
				genericNode.setLemma(parasite.getLemma());
				genericNode.setPos(parasite.getPos());
				genericNode.setWordIndex(parasite.getWordIndex());
				goG.moveEdges(parasite, genericNode, false);
				goG.addEdge(genericNode, conceptNode, " ", determinerRole);
				goG.removeNode(parasite);
			}
		}

		for (NLNodeP fromNode : froms) {
			goG.mergeNodes(fromNode, tos.get(froms.indexOf(fromNode)));
		}

		return g;
	}

	public static ContextPattern removeDuplicateNN(ContextPattern g) {

		System.out
				.println("------------------DUPLICATE NOUN REMOVAL---------------------");
		GraphOperations goG = new GraphOperations(NLGraphType.PATTERN, g);

		ArrayList<NLNodeP> froms = new ArrayList<NLNodeP>(), tos = new ArrayList<NLNodeP>();
		// HashMap<String, ArrayList<NLNodeP>> abstractables = new
		// HashMap<String, ArrayList<NLNodeP>>();
		for (Node node : g.getNodes()) {

			System.out.println("current node: " + node.getLabel()
					+ ((NLNodeP) node).getWordIndex());
			NLNodeP nlNode = (NLNodeP) node;
			if (!nlNode.isGeneric() && !tos.contains(nlNode)
					&& !froms.contains(nlNode)) {
				// check that node not already marked & not generic
				if (nlNode.getPos().contains("NN")) {
					if (g.getNodesNamed(nlNode.getLabel()).size() > 1) {

						for (Node duplicateNode : g.getNodesNamed(nlNode
								.getLabel())) {

							if (!((NLNodeP) duplicateNode).equals(nlNode)) {
								froms.add((NLNodeP) duplicateNode);
								tos.add(nlNode);
								System.out.println("NLNode " + node.getLabel()
										+ ((NLNodeP) node).getWordIndex()
										+ nlNode.getPos() + " will be removed");
								// goG.mergeNodes((NodeP) n, (NodeP) nlNode);
							}
						}

					}
				}
			}

		}
		// fix reflexive edges
		for (NLNodeP fromNode : froms) {
			goG.mergeNodes(fromNode, tos.get(froms.indexOf(fromNode)));
		}

		return g;
	}

	public static SimpleGraph processCorefCP(NLGraphType t, SimpleGraph g,
			Annotation document) throws Exception {
		System.out
				.println("\n------------------COREF RESOLUTION---------------------");
		GraphOperations goCxt = new GraphOperations(t, g);

		Map<Integer, CorefChain> chains = document
				.get(CorefCoreAnnotations.CorefChainAnnotation.class);
		for (CorefChain chain : chains.values()) {
			// get repr mention
			CorefMention bossMention = chain.getRepresentativeMention();
			NLNode headNLNode = goCxt.getByIndex(bossMention.headIndex);
			if (headNLNode != null) {

				System.out.println("Head mention: " + bossMention + " head: "
						+ headNLNode.getLabel() + " startindex= "
						+ bossMention.startIndex + " endindex"
						+ bossMention.endIndex);

				// get other mentions & merge them with repr mention
				for (IntPair k : chain.getMentionMap().keySet()) {

					for (CorefMention mention : chain.getMentionMap().get(k)) {

						if (!mention.mentionSpan
								.equals(bossMention.mentionSpan)) {

							NLNode nlNode = goCxt.getByIndex(mention.headIndex);

							if (nlNode != null) {

								System.out.println("Mention: " + mention
										+ " head: " + nlNode + " =?= "
										+ mention.headIndex + " startindex= "
										+ mention.startIndex + " endindex"
										+ mention.endIndex);
								if (!nlNode.equals(headNLNode)) {
									goCxt.addEdge(nlNode, headNLNode, "==",
											equivalence);
								} else
									System.out
											.println("Not inserting reflexive edge.");
							}

						}
					}
				}
				// does this ever make sense?
				// Collection<Edge> es = g.getOutEdges(headNLNode);
				// if (es != null && !es.isEmpty())
				// instantiate(g, headNLNode);
			}
		}

		return g;
	}

	public static ContextPattern breakDeterminer(ContextPattern g, NLNodeP gov) {

		// // GraphOperations gocxt = new GraphOperations(g);
		ArrayList<String> dets = new ArrayList<String>();
		for (FunctionWord fw : gov.getAttributes("det")) {
			dets.add(fw.getLabel());

		}
		System.out.println("Determiners of node " + gov + " :" + dets);
		if (!gov.isGeneric()) {
			// gov.getAttributes().removeIf(a -> a.getTag().equals("det"));
			// gov.getAttributes().removeAll(gov.getAttributes("det"));
			gov.getAttributes().removeAll(dets);
			for (String string : dets) {
				instantiate(g, gov, string);
			}
		} else {
			System.out
					.println("Generic node with determiner. Attempt to relabel");
			gov.setLabel(gov.getLabel() + dets.toString());
		}
		return g;
	}

	public ArrayList<Predicate> getPredicates(ContextPattern g) {
		ArrayList<Predicate> result = null;
		HashMap<NLNodeP, Set<NLNodeP>> roots = getRoots(g);
		// predicate needs to take edges into consideration
		for (NLNodeP root : roots.keySet()) {
			// if (root.getPos().equals("VB")) {
			// HashMap<String, ArrayList<String>> args = roots.get(root)
			// .stream()
			// .collect(Collectors.toMap(NLNodeP::get, valueMapper));
			// // Predicate s= new Predicate(root.getLabel(),
			//
			// }
		}
		return result;
	}

	public static HashMap<NLNodeP, Set<NLNodeP>> getRoots(ContextPattern g) {
		HashMap<NLNodeP, Set<NLNodeP>> global = new HashMap<NLNodeP, Set<NLNodeP>>();
		for (Node n : g.getNodes()) {
			global.put((NLNodeP) n, new HashSet<NLNodeP>());
			// all higher nodes of one node. may be more than 1 coz coref
			HashSet<NLNodeP> roots = (HashSet<NLNodeP>) g.getOutEdges(n)
					.stream().map(e -> (NLNodeP) e.getTo())
					.collect(Collectors.toSet());
			// all nodes on a level
			HashMap<NLNodeP, Set<NLNodeP>> level = (HashMap<NLNodeP, Set<NLNodeP>>) roots
					.stream().collect(
							Collectors.toMap(
									NLNodeP::identity,
									r -> g.getInEdges(r).stream()
											.map(e -> (NLNodeP) e.getFrom())
											.collect(Collectors.toSet())));
			global.putAll(level);

		}
		return global;
	}

	/**
	 * @param graph
	 * @param node
	 *            to be instantiated
	 * @param label
	 * @return The graph with an extra generic node that takes over all its
	 *         edges and connects to the initial node via an edge labeled @link
	 *         label. Only for patterns
	 */
	public static NLNodeP instantiate(ContextPattern g, NLNodeP p, String label) {

		System.out.println("INSTANTIATION OF NODE " + p);
		GraphOperations gocxt = new GraphOperations(NLGraphType.PATTERN, g);
		NLNodeP genericNode = new NLNodeP();
		g.addNode(genericNode, true);
		System.out.println("Added generic node " + genericNode);

		genericNode.setLemma(p.getLemma());
		genericNode.setPos(p.getPos());
		// genericNode.setWordIndex(p.getWordIndex());// 2 do R 0 2 do
		// p.setWordIndex(Integer.MAX_VALUE);
		// genericNode.getAttributes().addAll(p.getAttributes());
		genericNode.setWordIndex(Integer.MAX_VALUE);
		gocxt.moveEdges(p, genericNode, false);

		gocxt.addEdge(genericNode, p, label, "det");

		return genericNode;
	}

	/**
	 * @param g
	 * @param p
	 * @return The graph with an extra generic node that takes over all its
	 *         edges and connects to the initial node via an "is" edge
	 */
	public NodeP instantiate(ContextPattern g, NLNodeP p) {
		return instantiate(g, p, "iz");
	}

	// just keeping it to have a list of all possible dependencies
	public ContextPattern breakPatterns(ContextPattern g) {
		Collection<Edge> edges = g.getEdges();
		HashMap<NodeP, NodeP> caseNodes = new HashMap<NodeP, NodeP>();// key:from
																		// val:to
		HashMap<NodeP, NodeP> detNodes = new HashMap<NodeP, NodeP>();

		GraphOperations goCtx = new GraphOperations(NLGraphType.PATTERN, g);

		for (Edge edge : edges) {
			switch (edge.getLabel()) {
			// useful for quickly checking list of possible relations
			case "det":
				if (edge.getFrom().getLabel().toString()
						.equalsIgnoreCase("the")) {
					detNodes.put((NodeP) edge.getFrom(), (NodeP) edge.getTo());
				}

			case "case":
				caseNodes.put((NodeP) edge.getFrom(), (NodeP) edge.getTo());
				break;

			case "advcl":
				break;
			case "acl":
				break;

			case "appos":
				break;
			case "aux":
				break;

			case "cc":
				break;

			case "comp":
				break;

			case "clf":
				break;

			case "compound":
				break;

			case "conj":
				break;

			case "cop":
				break;

			case "csubj":
				break;

			case "dep":
				break;

			case "discourse":
				break;
			case "dislocated":
				break;
			case "expl":
				break;
			case "fixed":
				break;
			case "flat":
				break;
			case "goeswith":
				break;
			case "iobj":
				break;
			case "list":
				break;
			case "mark":
				break;
			case "nmod":
				break;
			case "nsubj":
				break;
			case "nummod":
				break;
			case "obj":
				break;
			case "obl":
				break;
			case "orphan":
				break;
			case "parataxis":
				break;
			case "punct":
				break;
			case "reparandum":
				break;
			case "vocative":
				break;

			case "xcomp":
				break;
			default:
				break;
			}

		}
		for (NodeP keyfrNodeP : detNodes.keySet()) {
			// breakDeterminer(g, keyfrNodeP, detNodes.get(keyfrNodeP));
		}

		for (NodeP From : caseNodes.keySet()) {
			goCtx.mergeNodes((NLNodeP) From, (NLNodeP) caseNodes.get(From));
		}

		return g;
	}

	public String flipToWords(ContextPattern pat) {
		// int minindex = Integer.MAX_VALUE;
		ArrayList<NodeWithIndex> wordz = new ArrayList<NodeWithIndex>();
		for (Node node : pat.getNodes()) {
			if (!(node instanceof NLNodeP)) {
				return "FAIL";
			}
			NLNodeP nlnode = (NLNodeP) node;
			// if(nlnode.getWordIndex()<minindex)
			// minindex=nlnode.getWordIndex();
			for (FunctionWord fw : nlnode.getAttributes()) {
				// if(fw.getIndex()<minindex)
				// minindex=fw.getIndex();
				wordz.add(fw);
			}
			wordz.add(nlnode);
		}
		// sort by word index
		wordz = (ArrayList<NodeWithIndex>) wordz.stream()
				.sorted(new Comparator<NodeWithIndex>() {

					@Override
					public int compare(NodeWithIndex o1, NodeWithIndex o2) {
						return o1.getWordIndex() < o2.getWordIndex() ? -1 : (o1
								.getWordIndex() == o2.getWordIndex() ? 0 : 1);
					}
				}).collect(Collectors.toList());
		String s = null;
		for (NodeWithIndex nodeWithIndex : wordz) {
			s += " " + nodeWithIndex.getLabel();
		}
		return s;
	}

	public ArraySet<GraphComponent> getSubgraphOfNode(ContextPattern p,
			NLNodeP start) {
		ArraySet<GraphComponent> result = new ArraySet<GraphComponent>();
		result.add(start);
		for (Edge inedge : p.getInEdges(start)) {
			result.add(inedge);
			// tryna prevent eternal cycling
			if (result.addAll(getSubgraphOfNode(p, (NLNodeP) inedge.getFrom())) == false) {
				return result;
			}
		}
		return result;

	}
}
