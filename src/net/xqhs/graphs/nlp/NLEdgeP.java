package net.xqhs.graphs.nlp;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.pattern.EdgeP;

public class NLEdgeP extends EdgeP implements NLEdge {
	private String role;

	public String getRole() {
		return role;
	}

	public String setRole(String role) {
		this.role = role;
		return role;
	}

	public NLEdgeP(Edge e) {
		super((NLNodeP) e.getFrom(), (NLNodeP) e.getTo(), " ");
		role = e.getLabel();

	}

	public NLEdgeP(NLNodeP from, NLNodeP to, String label) {
		super(from, to, label);
		role = label.contains(":") ? label.split(":")[0] : label;
	}

	public NLEdgeP(NLNodeP from, NLNodeP to, String label, String role) {
		super(from, to, label);
		this.role = role;
	}

	/**
	 *
	 */
	private static final long serialVersionUID = -16040555855922315L;

}
