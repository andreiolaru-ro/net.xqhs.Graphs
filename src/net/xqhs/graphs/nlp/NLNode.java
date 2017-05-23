package net.xqhs.graphs.nlp;

import java.util.ArrayList;

import net.xqhs.graphs.graph.Node;

public interface NLNode extends Node {

	public String getPos();

	public void setPos(String pos);

	public int getWordIndex();

	public void setWordIndex(int index);

	public ArrayList<FunctionWord> getAttributes();

	public ArrayList<FunctionWord> getAttributes(String s);

	public String getLemma();

	public void setLemma(String lemma);
}
