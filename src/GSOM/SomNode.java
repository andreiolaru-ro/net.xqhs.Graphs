package GSOM;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

public class SomNode {
	public static final int LEFT = 0, RIGHT = 1, UP = 2, DOWN = 3;
	Random r;
	ArrayList<Double> weights;
	double error;
	int startIteration;// iteration at node insert
	int lastIteration;// where the node won

	// best matching data
	String dataStr;
	ArrayList<Double> data;

	int lastChanged;

	Map<Integer, SomNode> neighbors;
	int x, y;

	public SomNode(int dim, int it, int x, int y) {
		r = new Random();

		weights = new ArrayList<Double>();
		for (int i = 0; i < dim; i++) {
			weights.add(r.nextDouble());
		}

		this.error = 0;
		this.startIteration = it;
		this.lastIteration = 0;
		this.dataStr = " ";
		this.data = null;
		this.lastChanged = 0;
		this.x = x;
		this.y = y;
		neighbors = new IdentityHashMap<Integer, SomNode>();
		neighbors.put(LEFT, null);
		neighbors.put(RIGHT, null);
		neighbors.put(UP, null);
		neighbors.put(DOWN, null);
	}

	public void weightAdjust(ArrayList<Double> target, double learningRate) {
		for (int w = 0; w < weights.size(); w++) {
			this.weights.set(w, weights.get(w) + learningRate
					* (target.get(w) - weights.get(w)));
		}
	}

	public boolean isEdge() {
		return neighbors.values().contains(null) ? true : false;
	}

}
