package net.xqhs.graphs.nlp;

import net.xqhs.graphs.graph.Edge;

public interface NLEdge extends Edge {
	public String getRole();

	public String setRole(String role);

}
