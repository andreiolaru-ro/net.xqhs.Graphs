package GSOM;

import java.util.ArrayList;

import net.xqhs.graphs.graph.SimpleGraph;

public class Datapoint {
	public Datapoint(int id, String txt) {
		super();
		this.id = id;
		this.txt = txt;
	}

	int id;
	String txt;
	SimpleGraph g;
	ArrayList<Double> features;

}
