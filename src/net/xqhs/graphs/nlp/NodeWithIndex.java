package net.xqhs.graphs.nlp;

import net.xqhs.graphs.graph.Node;

public interface NodeWithIndex extends Node {
	public int getWordIndex();

	public void setWordIndex(int index);

}
