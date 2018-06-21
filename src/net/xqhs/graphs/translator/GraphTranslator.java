package net.xqhs.graphs.translator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.matcher.MatchMaker;
import net.xqhs.graphs.nlp.NLEdgeG;
import net.xqhs.graphs.nlp.NLNodeG;
import net.xqhs.graphs.nlp.NLNodeP;;

public class GraphTranslator {

	private enum Article {
		THE, A, AN
	}

	private static class InEdgesComparator implements Comparator<ComparableNLNodeG> {
		private SimpleGraph g;

		public InEdgesComparator(SimpleGraph g) {
			this.g = g;
		}

		@Override
		public int compare(ComparableNLNodeG n1, ComparableNLNodeG n2) {
			int edgesDiff = g.getInEdges(n1.getNLNodeG()).size() - g.getInEdges(n2.getNLNodeG()).size();
			if (edgesDiff == 0)
				return n1.getNLNodeG().getWordIndex() - n2.getNLNodeG().getWordIndex();
			return edgesDiff;
		}
	}

	private static class OutEdgesComparator implements Comparator<ComparableNLNodeG> {
		private SimpleGraph g;

		public OutEdgesComparator(SimpleGraph g) {
			this.g = g;
		}

		@Override
		public int compare(ComparableNLNodeG n1, ComparableNLNodeG n2) {
			int edgesDiff = g.getOutEdges(n1.getNLNodeG()).size() - g.getOutEdges(n2.getNLNodeG()).size();
			if (edgesDiff == 0)
				return n1.getNLNodeG().getWordIndex() - n2.getNLNodeG().getWordIndex();
			return edgesDiff;
		}
	}

	private class ComparableNLNodeG implements Comparable<ComparableNLNodeG> {
		private NLNodeG node;
		private NLEdgeG edge;

		public ComparableNLNodeG(NLNodeG n) {
			this.node = n;
		}

		public ComparableNLNodeG(NLNodeG n, NLEdgeG e) {
			this.node = n;
			this.edge = e;
		}

		public NLNodeG getNLNodeG() {
			return this.node;
		}

		public NLEdgeG getNLEdgeG() {
			return this.edge;
		}

		@Override
		public int compareTo(ComparableNLNodeG otherNode) {
			return this.node.getWordIndex() - otherNode.getNLNodeG().getWordIndex();
		}
	}

	private Set<Node> getUnvisitedChildren(Node n, SimpleGraph g, Set<Node> visited) {
		Set<Node> children = new HashSet<Node>();

		for (Edge e : g.getOutEdges(n)) {
			if (!visited.contains(e.getTo()))
				children.add(e.getTo());
		}
		return children;
	}

	private Node getLastChainNode(Node n, SimpleGraph g, String dir, Set<Node> visited) throws Exception {
		Collection<Edge> nextNode;
		if (dir.equals("in"))
			nextNode = g.getInEdges(n);
		else if (dir.equals("out"))
			nextNode = g.getOutEdges(n);
		else
			throw new Exception("Unrecognized direction: " + dir);

		if (visited.contains(n)) {
			System.out.println("Cycle found at: " + n.getLabel());
			return n;
		}
		visited.add(n);

		if (nextNode.size() > 1) {
			String neigh = new String();
			for (Edge e : nextNode)
				if (dir.equals("in"))
					neigh += " " + e.getFrom().getLabel();
				else
					neigh += " " + e.getTo().getLabel();
			throw new Exception("Found unflattened node: " + n.getLabel() + " has " + dir + " neighbours: " + neigh);
		}
		if (nextNode.size() == 0)
			return n;

		if (dir.equals("in"))
			return getLastChainNode(nextNode.iterator().next().getFrom(), g, dir, visited);
		return getLastChainNode(nextNode.iterator().next().getTo(), g, dir, visited);
	}

	private Set<Node> topologicalSort(SimpleGraph g) {
		Set<Node> sortedNodes = new HashSet<Node>();
		Set<Node> greyNodes = new HashSet<Node>();
		Set<Node> parents = new HashSet<Node>();
		Set<Node> freshlySorted = new HashSet<Node>();

		for (Node n : g.getNodes()) {
			if (g.getInEdges(n).size() == 0) {
				sortedNodes.add(n);
				greyNodes.addAll(getUnvisitedChildren(n, g, sortedNodes));
			}
		}

		while (!greyNodes.isEmpty()) {
			for (Node n : greyNodes) {
				parents.clear();
				for (Edge e : g.getInEdges(n))
					parents.add(e.getFrom());
				if (sortedNodes.containsAll(parents)) {
					sortedNodes.add(n);
					freshlySorted.add(n);
				}
			}
			greyNodes.removeAll(freshlySorted);
			for (Node n : freshlySorted)
				greyNodes.addAll(getUnvisitedChildren(n, g, sortedNodes));
			freshlySorted.clear();
		}

		return sortedNodes;
	}

	private Edge getNodesLink(Node from, Node to, SimpleGraph g) {
		Collection<Edge> edges = g.getOutEdges(from);

		for (Edge e : edges)
			if (e.getTo() == to)
				return e;
		return null;
	}

	private Node findPreviousCycleNode(Node n, Node cycleNode, SimpleGraph g) {
		Collection<Edge> outEdges = g.getOutEdges(n);
		Node node;

		if (outEdges.isEmpty())
			return null; // wrong way

		for (Edge e : outEdges)
			if (e.getTo() == cycleNode) { // found it
				return n;
			}
		for (Edge e : outEdges) {
			node = findPreviousCycleNode(e.getTo(), cycleNode, g);
			if (node != null) // found it
				return node;
		}
		return null;
	}

	private Node getOtherEnd(Node current, Node end, SimpleGraph g) {
		Collection<Edge> edges = g.getOutEdges(current);
		Node n;

		if (current == end)
			return null; // wrong way

		if (edges.isEmpty())
			return current;

		for (Edge e : edges) {
			n = getOtherEnd(e.getTo(), end, g);
			if (n != null)
				return n;
		}
		return null;
	}

	private void solveCycles(SimpleGraph g) throws Exception {
		Collection<Node> nodes = g.getNodes();
		List<ComparableNLNodeG> InSortedNodes = new LinkedList<ComparableNLNodeG>();
		List<ComparableNLNodeG> OutSortedNodes = new LinkedList<ComparableNLNodeG>();
		NLNodeG cycleNode, pCycleNode;
		NLEdgeG edge;

		for (Node n : nodes) {
			InSortedNodes.add(new ComparableNLNodeG((NLNodeG) n));
			OutSortedNodes.add(new ComparableNLNodeG((NLNodeG) n));
		}
		Collections.sort(InSortedNodes, new InEdgesComparator(g)); // nodes sorted by in degree, then by word index
		Collections.sort(OutSortedNodes, new OutEdgesComparator(g)); // nodes sorted by out degree, then by word index

		int firstIn = g.getInEdges(InSortedNodes.iterator().next().getNLNodeG()).size();
		int lastIn = g.getInEdges(InSortedNodes.listIterator(InSortedNodes.size()).previous().getNLNodeG()).size();
		int firstOut = g.getInEdges(OutSortedNodes.iterator().next().getNLNodeG()).size();
		int lastOut = g.getInEdges(OutSortedNodes.listIterator(OutSortedNodes.size()).previous().getNLNodeG()).size();

		if (firstIn == 1 && lastIn == 1 && firstOut == 1 && lastOut == 1) { // whole graph is a cycle
			cycleNode = InSortedNodes.iterator().next().getNLNodeG();
			g.removeEdge(g.getInEdges(cycleNode).iterator().next()); // remove edge from last node to first node (sorted
																		// by word index)
			return;

		} else if (firstIn == 1 && lastIn == 1 && firstOut == 0 && lastOut > 1) { // graph starts with cycle
			cycleNode = OutSortedNodes.listIterator(OutSortedNodes.size()).previous().getNLNodeG();
			cycleNode = (NLNodeG) findPreviousCycleNode(cycleNode, cycleNode, g);
			pCycleNode = (NLNodeG) findPreviousCycleNode(cycleNode, cycleNode, g);
			edge = (NLEdgeG) getNodesLink(pCycleNode, cycleNode, g);
			g.removeEdge(edge); // remove edge pCycleNode --> cycleNode
			// add edge pCycleNode --> end of graph
			g.addEdge(new NLEdgeG(pCycleNode, (NLNodeG) getOtherEnd(cycleNode, pCycleNode, g), edge.getLabel(),
					edge.getRole()));
		} else if (firstIn == 0 && lastIn > 1 && firstOut == 1 && lastOut == 1) { // graph ends with cycle
			cycleNode = InSortedNodes.listIterator(InSortedNodes.size()).previous().getNLNodeG();
			pCycleNode = (NLNodeG) findPreviousCycleNode(cycleNode, cycleNode, g);
			edge = (NLEdgeG) getNodesLink(pCycleNode, cycleNode, g);
			g.removeEdge(edge); // remove edge pCycleNode --> cycleNode
		} else if (firstIn == 0 && lastIn > 1 && firstOut == 0 && lastOut > 1) { // graph has inner cycle
			cycleNode = InSortedNodes.listIterator(InSortedNodes.size()).previous().getNLNodeG();
			pCycleNode = (NLNodeG) findPreviousCycleNode(cycleNode, cycleNode, g);
			edge = (NLEdgeG) getNodesLink(pCycleNode, cycleNode, g);
			g.removeEdge(edge); // remove edge pCycleNode --> cycleNode
			// add edge pCycleNode --> end of graph
			g.addEdge(new NLEdgeG(pCycleNode, (NLNodeG) getOtherEnd(cycleNode, pCycleNode, g), edge.getLabel(),
					edge.getRole()));
		}
	}

	private void flattenNode(Node n, SimpleGraph g, String dir) throws Exception {
		Collection<Edge> edges;

		if (dir.equals("in"))
			edges = g.getInEdges(n);
		else if (dir.equals("out"))
			edges = g.getOutEdges(n);
		else
			throw new Exception("Unrecognized direction: " + dir);

		if (edges.size() < 2) {
			// node has at most one parent(in)/child(out) => no flattening needed
			return;
		}

		List<ComparableNLNodeG> nextNodes = new LinkedList<ComparableNLNodeG>();
		ListIterator<ComparableNLNodeG> it;
		ComparableNLNodeG entry;
		NLNodeG nn1, nn2;
		NLEdgeG e1, e2;
		Set<Edge> toRemove = new HashSet<Edge>();

		if (dir.equals("in")) {
			for (Edge e : edges)
				nextNodes.add(new ComparableNLNodeG((NLNodeG) e.getFrom(), (NLEdgeG) e));
		} else { // dir == "out"
			for (Edge e : edges)
				nextNodes.add(new ComparableNLNodeG((NLNodeG) e.getTo(), (NLEdgeG) e));
		}
		Collections.sort(nextNodes);
		it = nextNodes.listIterator();

		entry = it.next();
		nn1 = entry.getNLNodeG();
		e1 = entry.getNLEdgeG();

		while (it.hasNext()) {
			entry = it.next();
			nn2 = entry.getNLNodeG();
			e2 = entry.getNLEdgeG();
			if (dir.equals("in")) {
				toRemove.add(e1);
				g.addEdge(new NLEdgeG(nn1, (NLNodeG) getLastChainNode(nn2, g, dir, new HashSet<Node>()), e1.getLabel(),
						e1.getRole()));
				e1 = e2;
				nn1 = nn2;
			} else { // dir == "out"
				toRemove.add(e2);
				g.addEdge(new NLEdgeG((NLNodeG) getLastChainNode(nn1, g, dir, new HashSet<Node>()), nn2, e2.getLabel(),
						e2.getRole()));
			}
		}
		g.removeAll(toRemove);
	}

	public String sortGraphToString(SimpleGraph g) {
		List<ComparableNLNodeG> nodes = new LinkedList<ComparableNLNodeG>();
		String sentence = new String();

		for (Node n : g.getNodes())
			nodes.add(new ComparableNLNodeG((NLNodeG) n));
		Collections.sort(nodes);

		for (ComparableNLNodeG n : nodes) {
			sentence += n.getNLNodeG().getLabel() + " ";
		}

		return sentence;
	}

	private String edgeToWord(Edge e) {
		String word = new String();
		String prev = new String();

		for (String w : e.getLabel().split(":|\\s+"))
			if (w.length() > 0) {
				if (prev.equals(w))
					continue;
				word += " " + w;
				prev = w;
			}

		return word + " ";
	}

	public String graphToString(SimpleGraph g) {
		String edgeLabel, sentence = new String();
		Set<Node> visited = new HashSet<Node>();
		Collection<Node> nextNodes;
		Node prevRoot, root = null;

		try {
			root = getLastChainNode(g.getNodes().iterator().next(), g, "in", new HashSet<Node>());
		} catch (Exception err) {
			err.printStackTrace();
		}

		visited.add(root);
		sentence += root.getLabel();
		nextNodes = getUnvisitedChildren(root, g, visited);
		prevRoot = root;
		while (!nextNodes.isEmpty()) {
			root = nextNodes.iterator().next();
			edgeLabel = edgeToWord(getNodesLink(prevRoot, root, g));
			visited.add(root);
			nextNodes = getUnvisitedChildren(root, g, visited);
			sentence += edgeLabel + root.getLabel();
			prevRoot = root;

		}

		return (sentence.substring(0, 1).toUpperCase() + sentence.substring(1) + ".").trim();
	}

	public void redirectEdges(SimpleGraph g) {
		Set<Edge> toRemove = new HashSet<Edge>();
		Set<Edge> toAdd = new HashSet<Edge>();
		NLNodeG to, from;

		for (Edge e : g.getEdges()) {
			from = (NLNodeG) e.getFrom();
			to = (NLNodeG) e.getTo();
			if (from.getWordIndex() > to.getWordIndex()) {
				toRemove.add(e);
				toAdd.add(new NLEdgeG(to, from, e.getLabel()));
			}
		}

		g.removeAll(toRemove);
		g.addAll(toAdd);

	}

	private Edge getEdgeToGenericNode(Node n, ContextPattern cp) {
		Collection<Node> nodes;

		nodes = cp.getNodesNamed(n.getLabel());
		if (nodes.isEmpty())
			return null;

		Node nn = nodes.iterator().next();

		for (Edge e : cp.getInEdges(nn)) {
			NLNodeP np = (NLNodeP) e.getFrom();
			if (np.isGeneric())
				return e;
		}

		for (Edge e : cp.getOutEdges(nn)) {
			NLNodeP np = (NLNodeP) e.getTo();
			if (np.isGeneric())
				return e;
		}

		return null;
	}

	private String refineArticle(String article, String word) {
		article = article.toLowerCase();
		Boolean isArticle = false;
		for (Article a : Article.values())
			if (a.toString().toLowerCase().equals(article)) {
				isArticle = true;
				break;
			}
		if (!isArticle)
			return "";
		if (article.equals(Article.THE.toString().toLowerCase()))
			return article;

		String vowels = "aeiou";
		if (vowels.indexOf(Character.toLowerCase(word.charAt(0))) != -1)
			// word starts with vowel
			return Article.AN.toString().toLowerCase();

		return article;
	}

	private void setNNArticle(Node n, String article, SimpleGraph g) {
		Collection<Edge> edges = g.getInEdges(n);

		// if n is first node, place article in its label
		if (edges.isEmpty()) {
			n.setLabel(refineArticle(article, n.getLabel()) + " " + n.getLabel());
			return;
		}

		// if has adjective or noun companion, place article in front of them
		for (Edge e : edges) {
			NLNodeG nn = (NLNodeG) e.getFrom();
			if (nn.getPos().startsWith("JJ") || (e.getLabel().equals("") && nn.getPos().startsWith("NN"))) {
				setNNArticle(nn, article, g);
				return;
			}
		}

		// if no more adjectives, place article on in edge
		Edge e = edges.iterator().next();
		e.setLabel(e.getLabel() + " " + refineArticle(article, n.getLabel()));
	}

	public void computeArticles(ContextPattern cp, SimpleGraph g) {
		Edge e;

		for (Node n : g.getNodes()) {
			if (!((NLNodeG) n).getPos().startsWith("NN")) // if it's not a noun, it does not need article
				continue;
			e = getEdgeToGenericNode(n, cp);
			if (e == null)
				continue;
			setNNArticle(n, e.getLabel(), g);
		}
	}

	public SimpleGraph flattenGraph(SimpleGraph g) {
		SimpleGraph newg = new MatchMaker().copyGraph(g);

		redirectEdges(newg);

		for (Node n : topologicalSort(newg)) {
			if (newg.getInEdges(n).size() > 1) {
				try {
					flattenNode(n, newg, "in");
				} catch (Exception err) {
					err.printStackTrace();
				}
			}
		}

		List<Node> nodes = new ArrayList<Node>(topologicalSort(newg));
		ListIterator<Node> it = nodes.listIterator(nodes.size());
		Node n;
		// Iterate in reverse.
		while (it.hasPrevious()) {
			n = it.previous();
			if (newg.getOutEdges(n).size() > 1) {
				try {
					flattenNode(n, newg, "out"); // first flatten in then flatten out starting from end
				} catch (Exception err) {
					err.printStackTrace();
				}
			}
		}

		// try {
		// solveCycles(newg);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		return newg;
	}

	public void printGraph(SimpleGraph g) {
		System.out.println("================================");
		for (Edge e : g.getEdges()) {
			System.out.println(e.toString());
		}
		System.out.println("================================");

	}

}
