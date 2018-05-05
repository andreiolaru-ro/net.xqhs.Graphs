package net.xqhs.graphs.nlp.forestation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.nlp.NLEdge;
import net.xqhs.graphs.nlp.NLNode;

public class AdjacencyMatrix {
	SimpleGraph g;

	public NLEdge[][] getMa() {
		return ma;
	}

	public void setMa(NLEdge[][] ma) {
		this.ma = ma;
	}

	public ArrayList<Node> getNodes() {
		return nodes;
	}

	public void setNodes(ArrayList<Node> nodes) {
		this.nodes = nodes;
	}

	NLEdge[][] ma;
	ArrayList<Node> nodes;

	public AdjacencyMatrix(SimpleGraph g) {
		this.g = g;
		if (g == null) {
			System.out
					.println("Cannot build adjacency matrix for nonexistent graph ");
		}
		ma = new NLEdge[g.size()][g.size()];
		for (int i = 0; i < ma.length; i++) {
			for (int j = 0; j < ma.length; j++) {
				ma[i][j] = null;
			}
		}
		nodes = new ArrayList<Node>(g.getNodes());
		for (int i = 0; i < nodes.size(); i++) {
			Collection<Edge> iOutEdges = g.getOutEdges(nodes.get(i));
			for (Edge edg : iOutEdges) {
				NLEdge edge = (NLEdge) edg;
				NLNode j = (NLNode) edge.getTo();
				ma[i][nodes.indexOf(j)] = edge;
			}
		}
	}

	// here should create a tree
	public void bfs(int s) {
		boolean[] viz = new boolean[ma.length];
		for (boolean b : viz) {
			b = false;
		}
		ArrayList<Integer> queue = new ArrayList<Integer>();
		viz[s] = true;
		queue.add(s);
		while (!queue.isEmpty()) {

		}
	}

	public ArrayList<NLNode> getRoots() {
		ArrayList<NLNode> res = new ArrayList<NLNode>();
		for (Node node : nodes) {
			if (g.getOutEdges(node).isEmpty()) {
				res.add((NLNode) node);
			}
		}
		return (ArrayList<NLNode>) res.stream()
				.sorted(new Comparator<NLNode>() {

					@Override
					public int compare(NLNode o1, NLNode o2) {
						if (g.getInEdges(o1).size() == g.getInEdges(o2).size())
							return 0;
						else if (g.getInEdges(o1).size() < g.getInEdges(o2)
								.size())
							return -1;
						return 1;
					}
				}).collect(Collectors.toList());
	}

	public Map<NLNode, Integer> treefy() {
		Map<NLNode, Boolean> visited = new IdentityHashMap<NLNode, Boolean>();
		nodes.stream().forEach(n -> visited.put((NLNode) n, false));

		// key-node value-parent
		Map<NLNode, Integer> hier = new HashMap<NLNode, Integer>();
		ArrayList<NLNode> roots = getRoots();
		System.out.println("Roots are: " + roots);
		// NLNode start = roots != null ? roots.get(roots.size() - 1)
		// : (NLNode) nodes.get(0);
		for (NLNode start : roots) {

			bfs(visited, hier, start);
			while (visited.entrySet().contains(false)) {

				for (NLNode nlNode : visited.keySet()) {
					if (visited.get(nlNode) == false) {
						hier = bfs(visited, hier, nlNode);
					}
				}

			}
			System.out.println("RESULT OF BFS:");
			for (NLNode nlNode : hier.keySet()) {
				System.out.println(nlNode + "  ~  " + hier.get(nlNode));
			}
		}
		return hier;
	}

	public Map<NLNode, Integer> bfs(Map<NLNode, Boolean> visited,
			Map<NLNode, Integer> hier, NLNode start) {
		LinkedList<NLNode> q = new LinkedList<NLNode>();
		int level = 0;
		q.add(start);
		hier.put(start, level);
		visited.replace(start, true);

		while (!q.isEmpty()) {
			NLNode curr = q.remove();
			level++;
			NLNode child = null;
			while ((child = getUnvisitedChildNode(curr, visited)) != null) {
				visited.replace(child, true);
				hier.put(child, level);
				q.add(child);
			}
		}
		return hier;
	}

	private NLNode getUnvisitedChildNode(NLNode node,
			Map<NLNode, Boolean> visited) {
		return (NLNode) g.getInEdges(node).stream().map(e -> e.getFrom())
				.filter(n -> visited.get(n) == false).findFirst().orElse(null);
	}

	// raise to power in order to obtain walks of various lengths
	public int[][] powerToTheMatrix(int pow) {
		// init pow matrix
		int[][] powm = new int[ma.length][ma.length];

		for (int i = 0; i < ma.length; i++) {
			for (int j = 0; j < ma.length; j++) {
				if (ma[i][j] != null)
					powm[i][j] = 1;
				else
					powm[i][j] = 0;

			}
		}
		int[][] temp = powm;
		for (int p = 2; p <= pow; p++) {
			temp = matrixMultiply(temp, powm);
		}
		System.out
				.println("Result of raising adjacency matrix to power " + pow);
		for (int[] element : temp) {
			for (int j = 0; j < temp.length; j++) {
				System.out.print(element[j] + " ");
			}
			System.out.println();
		}
		return temp;
	}

	public int[][] matrixMultiply(int[][] a, int[][] b) {

		int[][] temp = new int[a.length][b.length];
		for (int i = 0; i < ma.length; i++) {
			for (int j = 0; j < ma.length; j++) {
				for (int k = 0; k < ma.length; k++) {
					temp[i][j] += a[i][k] * b[k][j];
				}
			}
		}
		for (int[] element : temp) {
			for (int j = 0; j < temp.length; j++) {
				System.out.print(element[j] + " ");
			}
			System.out.println();
		}
		return temp;
	}

	public int numberOfLoops() {
		int[][] powm = new int[ma.length][ma.length];
		for (int i = 0; i < ma.length; i++) {
			for (int j = 0; j < ma.length; j++) {
				if (ma[i][j] != null)
					powm[i][j] = 1;
				else
					powm[i][j] = 0;

			}
		}
		int no = 0;

		int[][] powm2 = powerToTheMatrix(2);
		int[][] powm3 = powerToTheMatrix(3);

		for (int i = 0; i < powm.length; i++) {
			no += powm[i][i] + powm2[i][i] + powm3[i][i];
		}
		return no;
	}

	public void print() {
		System.out.println("ADJACENCY MATRIX");
		for (Node node : nodes) {
			System.out.print(node.getLabel() + " |");
		}
		System.out.println();
		for (int i = 0; i < ma.length; i++) {
			System.out.print(nodes.get(i) + "  |");
			for (int j = 0; j < ma.length; j++) {
				if (ma[i][j] != null) {
					System.out.print(ma[i][j].getLabel() + " |");
				} else
					System.out.print("       |");
			}
			System.out.println();
		}

	}
}
