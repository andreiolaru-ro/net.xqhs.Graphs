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
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.pattern.EdgeP;
import net.xqhs.graphs.pattern.NodeP;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.IntPair;

public class ContextPatternConverter {
	private int genericIndex;

	public int getGenericIndex() {
		return genericIndex;
	}

	public void setGenericIndex(int genericIndex) {
		this.genericIndex = genericIndex;
	}

	public ContextPatternConverter(ContextPattern g) {
		genericIndex = 0;
		for (Node node : g.getNodes()) {
			if (((NodeP) node).isGeneric()) {
				genericIndex++;
			}
		}
	}

	public ContextPattern invertAllEdges(ContextPattern g) {
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

	public ContextPattern relabelEdgesWithAuxWords(ContextPattern g) {
		System.out
				.println("------------------PROCESSING FUNCTION WORDS---------------------");
		// move roles off labels and into special property
		g.getEdges()
				.stream()
				.forEach(
						m -> {
							((NLEdgeP) m).setRole(m.getLabel().split(":")[0]);
							String label = m.getLabel().substring(
									m.getLabel().split(":")[0].length());
							m.setLabel(label.isEmpty() ? " " : label
									.substring(1));
							System.out.println(m.getFrom() + " --"
									+ m.getLabel() + " -->" + m.getTo()
									+ " has role:" + ((NLEdgeP) m).getRole());
						});

		for (Node node : g.getNodes()) {

			NLNodeP nlNode = (NLNodeP) node;
			// get all filterwords from their arraylist
			ArrayList<FunctionWord> fw = new ArrayList<FunctionWord>();
			if (!nlNode.getAttributes().isEmpty()) {
				HashMap<String, ArrayList<FunctionWord>> copy = nlNode
						.getAttributes();
				System.out.println("Attributes of node " + nlNode + " :"
						+ nlNode.getAttributes());
				for (String key : copy.keySet()) {
					if (!key.equals("det")) {
						fw.addAll(nlNode.getAttributes().get(key));
						// nlNode.getAttributes().remove(key);
					}

				}

				// assign them to edges using destination
				ArrayList<Edge> edges = new ArrayList<Edge>();
				edges.addAll(g.getInEdges(nlNode));
				edges.addAll(g.getOutEdges(nlNode));
				// sort function words by index
				fw.sort(new Comparator<FunctionWord>() {
					@Override
					public int compare(FunctionWord o1, FunctionWord o2) {
						return Integer.valueOf(o1.getIndex()).compareTo(
								o2.getIndex());
					}

				});
				// sort edges by word index of the nodes opposite from current
				edges.sort(new Comparator<Edge>() {

					@Override
					public int compare(Edge e1, Edge e2) {
						NLNodeP other1 = e1.getFrom().equals(e2.getFrom())
								|| e1.getFrom().equals(e2.getTo()) ? (NLNodeP) e1
								.getTo() : (NLNodeP) e1.getFrom();
						NLNodeP other2 = e2.getFrom().equals(e1.getFrom())
								|| e2.getFrom().equals(e1.getTo()) ? (NLNodeP) e2
								.getTo() : (NLNodeP) e2.getFrom();
						return Integer.valueOf(other1.getWordIndex())
								.compareTo(other2.getWordIndex());
					}
				});

				ArrayList<NLNodeP> neighbors;
				// determine all neighboring nodes by extracting them from
				// edges
				neighbors = (ArrayList<NLNodeP>) edges
						.stream()
						.map(q -> q.getFrom().equals(node) ? (NLNodeP) q
								.getTo() : (NLNodeP) q.getFrom())
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
						.collect(Collectors.toList())).contains(f.getLabel()));
				System.out
						.println("Function words left after doubles cleanse: "
								+ fw);
				// create the adjacency matrix
				ArrayList<ArrayList<Integer>> distances = new ArrayList<ArrayList<Integer>>();
				for (NLNodeP neighborNode : neighbors) {
					ArrayList<Integer> distancesFromFWsPerNode = new ArrayList<Integer>();

					for (FunctionWord w : fw) {
						distancesFromFWsPerNode.add(Math.abs(w.getIndex()
								- neighborNode.getWordIndex()));
					}
					distances.add(distancesFromFWsPerNode);
					System.out.println("The associations of node "
							+ neighborNode + " :" + distancesFromFWsPerNode);
				}
				// create map of function word repartition
				HashMap<NLNodeP, ArrayList<FunctionWord>> toMoveMap4All = new HashMap<NLNodeP, ArrayList<FunctionWord>>();
				for (NLNodeP n : neighbors) {
					toMoveMap4All.put(n, new ArrayList<FunctionWord>());
				}
				// calculate vertical minimum in adjancency[dis not a word?]
				// matrix
				for (FunctionWord w : fw) {
					ArrayList<Integer> vertical = new ArrayList<Integer>();
					Integer min = Integer.MAX_VALUE;
					for (NLNodeP neighbor : neighbors) {
						Integer current = distances.get(
								neighbors.indexOf(neighbor)).get(fw.indexOf(w));
						vertical.add(current);
						if (current < min) {
							min = current;
						}
					}
					final Integer minimum = min;
					// leave 1s where the minimum distances are and 0s for the
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
								toMoveMap4All
										.get(neighbors.get(vertical
												.indexOf(integer))).add(w);
								assigned = true;
								continue;
							}
						}
					}
				}

				for (Edge e : edges) {

					NLNodeP currentOppositeNode = e.getFrom().equals(node) ? (NLNodeP) e
							.getTo() : (NLNodeP) e.getFrom();

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
										(a, b) -> a += " : " + b + " ")
								+ e.getLabel();
						System.out.print("Relabeling edge " + e.getFrom()
								+ " -- " + e.getLabel() + "->" + e.getTo()
								+ "  to  ");
						e.setLabel(label);
						System.out.println(label);
						toMoveMap4All.remove(currentOppositeNode);

						System.out
								.println("Function words left after removal of edge "
										+ e);
						toMoveMap4All.values().stream()
								.forEach(w -> System.out.println(w));
					}
				}
			}
		}

		return g;
	}

	public ContextPattern removeDuplicates(ContextPattern g) {
		System.out
				.println("------------------DUPLICATES REMOVAL---------------------");
		GraphOperations goG = new GraphOperations(g);
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

				NLNodeP genericNode = goG.addGenericNode();
				genericNode.setLemma(parasite.getLemma());
				genericNode.setPos(parasite.getPos());
				genericNode.setWordIndex(parasite.getWordIndex());
				goG.moveEdges(parasite, genericNode, false);
				goG.addEdge(genericNode, conceptNode, " ");
				goG.removeNode(parasite);
			}
		}

		for (NLNodeP fromNode : froms) {
			goG.mergeNodes(fromNode, tos.get(froms.indexOf(fromNode)));
		}

		return g;
	}

	public ContextPattern removeDuplicateNN(ContextPattern g) {

		System.out
				.println("------------------DUPLICATE NOUN REMOVAL---------------------");
		GraphOperations goG = new GraphOperations(g);

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
				}
			}

		}

		for (NLNodeP fromNode : froms) {
			goG.mergeNodes(fromNode, tos.get(froms.indexOf(fromNode)));
		}

		return g;
	}

	public ContextPattern processCorefCP(ContextPattern g, Annotation document)
			throws Exception {
		System.out
				.println("------------------COREF RESOLUTION---------------------");
		GraphOperations goCxt = new GraphOperations(g);

		Map<Integer, CorefChain> chains = document
				.get(CorefCoreAnnotations.CorefChainAnnotation.class);
		for (CorefChain chain : chains.values()) {
			// get repr mention
			CorefMention bossMention = chain.getRepresentativeMention();
			NLNodeP headNLNode = goCxt.getByIndex(bossMention.headIndex);
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

							NLNodeP nlNode = goCxt
									.getByIndex(mention.headIndex);
							if (nlNode != null) {

								// NLNodeP generic = (NLNodeP) instantiate(g,
								// nlNode);
								// goCxt.addEdge(generic, headNLNode,
								// nlNode.getLabel());
								// goCxt.removeNode(nlNode);
								goCxt.addEdge(nlNode, headNLNode, "==");

								System.out.println("Mention: " + mention
										+ " head: " + nlNode.getLabel()
										+ " startindex= " + mention.startIndex
										+ " endindex" + mention.endIndex);

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

	public ContextPattern breakDeterminer(ContextPattern g, NLNodeP gov) {

		GraphOperations gocxt = new GraphOperations(g);
		ArrayList<String> dets = new ArrayList<String>();
		for (FunctionWord fw : gov.getAttributes().get("det")) {
			dets.add(fw.getLabel());

		}
		if (!gov.isGeneric()) {
			gov.getAttributes().remove("det");
			for (String string : dets) {
				instantiate(g, gov, string);
			}
		} else
			gov.setLabel(gov.getLabel() + dets.toString());
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

	private HashMap<NLNodeP, Set<NLNodeP>> getRoots(ContextPattern g) {
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
	 *         label
	 */
	public NodeP instantiate(ContextPattern g, NodeP p, String label) {

		System.out.println("Instantiation of node " + p);
		GraphOperations gocxt = new GraphOperations(g);
		NLNodeP genericNode = gocxt.addGenericNode();
		if (p instanceof NLNodeP) {
			NLNodeP pp = (NLNodeP) p;
			genericNode.setLemma(pp.getLemma());
			genericNode.setPos(pp.getPos());
			genericNode.setWordIndex(pp.getWordIndex());// 2 do R 0 2 do
			((NLNodeP) p).setWordIndex(Integer.MAX_VALUE);
			genericNode.getAttributes().putAll(pp.getAttributes());
		}

		gocxt.moveEdges(p, genericNode, false);

		gocxt.addEdge(genericNode, (NLNodeP) p, label);

		return genericNode;
	}

	/**
	 * @param g
	 * @param p
	 * @return The graph with an extra generic node that takes over all its
	 *         edges and connects to the initial node via an "is" edge
	 */
	public NodeP instantiate(ContextPattern g, NodeP p) {
		return instantiate(g, p, "is");
	}

	public ContextPattern breakDeterminerr(ContextPattern g, NLNodeP gov) {
		GraphOperations gocxt = new GraphOperations(g);
		ArrayList<FunctionWord> del = new ArrayList<FunctionWord>();
		for (FunctionWord fw : gov.getAttributes().get("det")) {
			NLNodeP genericNode = gocxt.addGenericNode();
			genericNode.setLemma(gov.getLemma());
			genericNode.setPos(gov.getPos());
			genericNode.setWordIndex(gov.getWordIndex());
			del.add(fw);
			genericNode.getAttributes().putAll(gov.getAttributes());
			gocxt.moveEdges(gov, genericNode, false);
			gocxt.addEdge(genericNode, gov, fw.getLabel());

		}
		gov.getAttributes().get("det").removeAll(del);
		return g;
	}

	public ContextPattern breakPatterns(ContextPattern g) {
		Collection<Edge> edges = g.getEdges();
		HashMap<NodeP, NodeP> caseNodes = new HashMap<NodeP, NodeP>();// key:from
																		// val:to
		HashMap<NodeP, NodeP> detNodes = new HashMap<NodeP, NodeP>();

		GraphOperations goCtx = new GraphOperations(g);

		for (Edge edge : edges) {
			switch (edge.getLabel()) {

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
			goCtx.mergeNodes(From, caseNodes.get(From));
		}

		return g;
	}

}
// .stream().filter(dist -> dist);
// ArrayList<FillerWord> toMovee =
// (ArrayList<FillerWord>) fw
// .stream()
// .filter(w -> neighbors
// .stream()
// .map(NLNodeP::getWordIndex)
// .map(x -> Math.abs(x
// - currentOppositeNode
// .getWordIndex()))
// .reduce(Math.abs(w.getIndex()
// - currentOppositeNode
// .getWordIndex()),
// (a, b) -> a < Math.abs(w
// .getIndex()
// - currentOppositeNode
// .getWordIndex()) ? a
// : Math.abs(w.getIndex()
// - currentOppositeNode
// .getWordIndex())) == Math
// .abs(w.getIndex()
// - currentOppositeNode
// .getWordIndex()))
// .collect(Collectors.toList());