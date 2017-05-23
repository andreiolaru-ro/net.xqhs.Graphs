package net.xqhs.graphs.nlp;

import edu.stanford.nlp.ling.IndexedWord;

public class NLNodeFactory {
	public static NLNode makeNode(NLGraphType t, IndexedWord w) {
		switch (t) {
		case GRAPH:
			return new NLNodeG(w);
		case PATTERN:
			return new NLNodeP(w);
		}
		return null;
	}

	// public static NLNode makeNode(NLGraphType t, String label) {
	// switch (t) {
	// case GRAPH:
	// return new NLNodeG(label);
	// case PATTERN:
	// return new NLNodeP(label);
	// }
	// return null;
	// }

}
