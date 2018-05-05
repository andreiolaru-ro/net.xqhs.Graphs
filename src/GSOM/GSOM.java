package GSOM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class GSOM {
	public static ArrayList<SomNode> somNodes;
	public static Map<String, ArrayList<Double>> dataset;
	public static double spreadFactor = 0.5;
	public static double growthThreshold;
	public static int dimensions, presentToNetwork;
	public static int num_iterations = 1000;
	public static int currIter, maxIter;
	public static double initLearningRate = 0.1;
	public static double alpha = 0.1;
	public static String outFile;
	public static Random r;

	public void train() {
		int rand = r.nextInt(dataset.keySet().size());
		ArrayList<Double> v = dataset.get(dataset.keySet().toArray()[rand]);
		// http://www.ijmo.org/vol6/504-M08.pdf There are many
		// learning rate functions while linear, inverse of time and
		// power series are mostly used in SOM [11].
		double learnRate = initLearningRate * alpha
				* (1 - 1.5 / somNodes.size());

		// present input & find bmu
		ArrayList<SomNode> recalculatables = new ArrayList<SomNode>();
		// for (int i = 0; i < presentToNetwork; i++) {
		SomNode bmu = bmu(v);
		bmu.lastIteration = currIter;
		// adjust neighborhood of bmu->Should include more than just neighbors
		ArrayList<SomNode> neighs = new ArrayList<SomNode>();
		neighs.add(bmu);
		neighs.addAll(bmu.neighbors.values().stream().filter(n -> n != null)
				.collect(Collectors.toList()));
		if (!recalculatables.contains(bmu)) {
			recalculatables.add(bmu);
		}

		for (SomNode somNode : neighs) {
			somNode.weightAdjust(v, learnRate);
			if (!recalculatables.contains(somNode)) {
				recalculatables.add(somNode);
			}
			// }
			// calc err
			double err = euclid(bmu.weights, v);
			// add err to node/ distribute
			ArrayList<SomNode> newNodes = addErrToNode(bmu, err);
			if (newNodes != null && !newNodes.isEmpty()) {
				recalculatables.addAll(newNodes);
			}
		}
		// increment iteration
		currIter++;
		// recalculate representative data elements for changed nodes
		Map<String, ArrayList<Double>> usedData = new HashMap<String, ArrayList<Double>>();
		for (SomNode somNode : somNodes) {
			usedData.putIfAbsent(somNode.dataStr, somNode.data);
		}
		for (SomNode node : recalculatables) {
			double d = Double.MAX_VALUE;

			String winr = "";
			ArrayList<Double> winrW = null;

			for (java.util.Map.Entry<String, ArrayList<Double>> datapoint : dataset
					.entrySet()) {

				if (!usedData.containsKey(datapoint.getKey())) {
					ArrayList<Double> point = datapoint.getValue();
					double ed = euclid(point, node.weights);
					if (ed < d) {
						d = ed;
						winrW = point;
						winr = datapoint.getKey();
					}
				}
			}

			if (!node.dataStr.equals(winr)) {
				node.data = winrW;
				node.dataStr = winr;
				node.lastChanged = currIter;

			}
			System.out.println(node.data + " , " + node.x + " , " + node.y
					+ " change according to bmu=" + bmu.weights);
			usedData.put(winr, winrW);

		}

		// removeUnusedNodes();

	}

	private void removeUnusedNodes() {
		ArrayList<SomNode> removables = new ArrayList<SomNode>();
		for (SomNode node : somNodes) {
			int fails = currIter - node.lastIteration;
			// if node hasn't won in ages
			if (fails > somNodes.size() * 4
					* (1 + currIter / dataset.keySet().size())) {
				// disconnect
				if (node.neighbors.get(SomNode.LEFT) != null)
					node.neighbors.get(SomNode.LEFT).neighbors.replace(
							SomNode.RIGHT, null);

				if (node.neighbors.get(SomNode.RIGHT) != null)
					node.neighbors.get(SomNode.RIGHT).neighbors.replace(
							SomNode.LEFT, null);

				if (node.neighbors.get(SomNode.UP) != null)
					node.neighbors.get(SomNode.UP).neighbors.replace(
							SomNode.DOWN, null);

				if (node.neighbors.get(SomNode.DOWN) != null)
					node.neighbors.get(SomNode.DOWN).neighbors.replace(
							SomNode.UP, null);
				removables.add(node);

			}
		}
		for (SomNode n : removables) {
			System.out.println("Removing node (" + n.x + " , " + n.y
					+ ")  at iteration " + currIter
					+ " after it hasn't won since " + n.lastIteration);
			somNodes.remove(n);
		}

	}

	// add err to node &grow map if needs & distribute err if needs
	public ArrayList<SomNode> addErrToNode(SomNode node, Double err) {
		node.error += err;
		if (node.error > growthThreshold) {
			if (!node.isEdge()) {
				node = findEdgeNodeLike(node);// either this or distribute error
												// to neighbors
				if (node == null)
					System.out.println("No free edge node found");
				else {
					ArrayList<SomNode> newNodes = grow(node);
					return newNodes;
				}

			}

		}
		return null;

	}

	private SomNode insert(int x, int y, SomNode initNode) {
		SomNode newNode = new SomNode(GSOM.dimensions, currIter, x, y);
		somNodes.add(newNode);

		newNode.lastIteration = currIter;// needed for pruning

		for (SomNode preNode : somNodes) {
			if (preNode.x == x - 1 && preNode.y == y) {
				newNode.neighbors.replace(SomNode.LEFT, preNode);
				preNode.neighbors.replace(SomNode.RIGHT, newNode);
			}

			if (preNode.x == x + 1 && preNode.y == y) {
				newNode.neighbors.replace(SomNode.RIGHT, preNode);
				preNode.neighbors.replace(SomNode.LEFT, newNode);
			}

			if (preNode.x == x && preNode.y == y + 1) {
				newNode.neighbors.replace(SomNode.UP, preNode);
				preNode.neighbors.replace(SomNode.DOWN, newNode);
			}
			if (preNode.x == x && preNode.y == y - 1) {
				newNode.neighbors.replace(SomNode.DOWN, preNode);
				preNode.neighbors.replace(SomNode.UP, newNode);
			}
		}
		// init new node weights according to neighbors and initNode
		SomNode nei = newNode.neighbors.values().stream()
				.filter(n -> n != null).findFirst().orElse(null);
		if (nei == null)
			System.out
					.println("Panic at the disco. Newly inserted node has no neighbors");
		else {
			for (int i = 0; i < nei.weights.size(); i++) {
				newNode.weights.set(i,
						2 * (initNode.weights.get(i) - nei.weights.get(i)));
			}
		}
		return newNode;

	}

	// grow net in all possible directions
	private ArrayList<SomNode> grow(SomNode node) {
		ArrayList<SomNode> newNodes = new ArrayList<SomNode>();
		if (node.neighbors.get(SomNode.LEFT) == null) {
			SomNode newNode = insert(node.x - 1, node.y, node);
			newNodes.add(newNode);
			System.out.println("Growing left with (" + newNode.x + " , "
					+ newNode.y + ")");
		}
		if (node.neighbors.get(SomNode.RIGHT) == null) {
			SomNode newNode = insert(node.x + 1, node.y, node);
			newNodes.add(newNode);
			System.out.println("Growing right with (" + newNode.x + " , "
					+ newNode.y + ")");
		}
		if (node.neighbors.get(SomNode.UP) == null) {
			SomNode newNode = insert(node.x, node.y + 1, node);
			newNodes.add(newNode);
			System.out.println("Growing left with (" + newNode.x + " , "
					+ newNode.y + ")");
		}
		if (node.neighbors.get(SomNode.DOWN) == null) {
			SomNode newNode = insert(node.x, node.y - 1, node);
			newNodes.add(newNode);
			System.out.println("Growing left with (" + newNode.x + " , "
					+ newNode.y + ")");
		}
		return newNodes;
	}

	public double euclid(ArrayList<Double> x, ArrayList<Double> y) {
		double dist = 0;
		for (int i = 0; i < x.size(); i++) {

			dist += Math.pow(x.get(i) - y.get(i), 2);
		}

		return dist;
	}

	public SomNode bmu(ArrayList<Double> v) {
		double bestD = Double.MAX_VALUE;
		SomNode winr = null;
		for (SomNode somNode : somNodes) {
			double ed = euclid(v, somNode.weights);
			if (ed < bestD) {
				bestD = ed;
				winr = somNode;
			}
		}
		return winr;

	}

	public SomNode findEdgeNodeLike(SomNode nod) {
		double bestD = Double.MAX_VALUE;
		SomNode winr = null;
		for (SomNode somNode : somNodes) {
			if (somNode.isEdge() && !somNode.equals(nod)) {
				double ed = euclid(nod.weights, somNode.weights);
				if (ed < bestD) {
					bestD = ed;
					winr = somNode;
				}
			}
		}
		return winr;

	}

	public static void init(Map<String, ArrayList<Double>> dataset) {
		GSOM.dataset = dataset;
		GSOM.dimensions = dataset.values().iterator().next().size();

		// GSOM.growthThreshold = -GSOM.dimensions
		// * (Math.log(GSOM.spreadFactor) / Math.log(2));
		GSOM.growthThreshold = -GSOM.dimensions * Math.log(GSOM.spreadFactor);
		GSOM.somNodes = new ArrayList<SomNode>();
		GSOM.currIter = 0;
		GSOM.maxIter = dataset.keySet().size();
		GSOM.r = new Random();
		GSOM.presentToNetwork = 20;
		// makenodez
		SomNode n00 = new SomNode(GSOM.dimensions, 0, 0, 0);
		SomNode n01 = new SomNode(GSOM.dimensions, 0, 0, 1);
		SomNode n10 = new SomNode(GSOM.dimensions, 0, 1, 0);
		SomNode n11 = new SomNode(GSOM.dimensions, 0, 1, 1);
		GSOM.somNodes.addAll(Arrays.asList(n00, n01, n10, n11));
		n00.neighbors.replace(SomNode.RIGHT, n10);
		n00.neighbors.replace(SomNode.UP, n01);
		n01.neighbors.replace(SomNode.RIGHT, n11);
		n01.neighbors.replace(SomNode.DOWN, n00);
		n10.neighbors.replace(SomNode.UP, n11);
		n10.neighbors.replace(SomNode.LEFT, n00);
		n11.neighbors.replace(SomNode.LEFT, n01);
		n11.neighbors.replace(SomNode.DOWN, n10);

	}

}
