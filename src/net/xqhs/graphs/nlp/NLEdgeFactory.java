package net.xqhs.graphs.nlp;

public class NLEdgeFactory {
	public static NLEdge makeNLEdge(NLGraphType t, NLNode from, NLNode to,
			String label, String role) {
		switch (t) {
		case GRAPH:
			return new NLEdgeG((NLNodeG) from, (NLNodeG) to, label, role);
		case PATTERN:
			return new NLEdgeP((NLNodeP) from, (NLNodeP) to, label, role);
		default:
			return null;
		}
	}

	public static NLEdge makeNLEdge(NLGraphType t, NLNode from, NLNode to,
			String label) {
		switch (t) {
		case GRAPH:
			return new NLEdgeG((NLNodeG) from, (NLNodeG) to, label);
		case PATTERN:
			return new NLEdgeP((NLNodeP) from, (NLNodeP) to, label);
		default:
			return null;
		}
	}

}
