package net.xqhs.graphs.nlp;

import net.xqhs.graphs.context.ContextGraph;
import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.graph.SimpleGraph;
import testing.ContextGraphsTest.IntTimeKeeper;

public class NLGraphFactory {
	public static SimpleGraph makeGraph(NLGraphType t) {
		switch (t) {
		case GRAPH:
			System.out.println("Creating context graph...");
			ContextGraph g = new ContextGraph();
			g.setTimeKeeper(new IntTimeKeeper());
			return g;
		case PATTERN:
			System.out.println("Creating context pattern...");
			return new ContextPattern();
		default:
			return null;
		}
	}

}
