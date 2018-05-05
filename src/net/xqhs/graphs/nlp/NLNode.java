package net.xqhs.graphs.nlp;

import java.util.ArrayList;

public interface NLNode extends NodeWithIndex {

	public String getPos();

	public void setPos(String pos);

	public ArrayList<FunctionWord> getAttributes();

	public ArrayList<FunctionWord> getAttributes(String s);

	public String getLemma();

	public void setLemma(String lemma);
}
