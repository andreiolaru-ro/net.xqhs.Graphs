/**
 *
 */
package net.xqhs.graphs.nlp;

import java.util.ArrayList;
import java.util.stream.Collectors;

import net.xqhs.graphs.graph.Node;
import edu.stanford.nlp.ling.IndexedWord;

/**
 *
 *
 */
public class NLNodeG implements NLNode {

	private String label;
	String pos, lemma;
	int wordIndex;
	ArrayList<FunctionWord> attributes;

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public Node setLabel(String label) {
		this.label = label;
		return this;
	}

	@Override
	public String getPos() {
		return pos;

	}

	@Override
	public void setPos(String pos) {
		this.pos = pos;
	}

	@Override
	public int getWordIndex() {
		return wordIndex;
	}

	@Override
	public void setWordIndex(int index) {
		this.wordIndex = index;
	}

	@Override
	public ArrayList<FunctionWord> getAttributes() {
		return attributes;
	}

	@Override
	public ArrayList<FunctionWord> getAttributes(String s) {
		return (ArrayList<FunctionWord>) attributes.stream()
				.filter(a -> a.getTag().equals(s)).collect(Collectors.toList());
	}

	@Override
	public String getLemma() {

		return lemma;
	}

	@Override
	public void setLemma(String lemma) {
		this.lemma = lemma;

	}

	public NLNodeG(IndexedWord w) {
		super();
		this.setLabel(w.word());
		this.setWordIndex(w.index());
		this.setLemma(w.lemma());
		this.pos = w.tag();
		this.attributes = new ArrayList<FunctionWord>();
	}

	@Override
	public String toString() {
		return this.label + "[" + this.getWordIndex() + "]";
	}

}
