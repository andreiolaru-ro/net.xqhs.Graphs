package net.xqhs.graphs.nlp;

import net.xqhs.graphs.context.ContextGraph.ContextEdge;
import net.xqhs.graphs.context.Instant.Offset;

public class NLEdgeG extends ContextEdge implements NLEdge {
	public NLEdgeG(NLNodeG fromNode, NLNodeG toNode, String edgeLabel,
			String role, Offset edgeValidity) {
		super(fromNode, toNode, edgeLabel, edgeValidity);
		this.role = role;

	}

	// while time is not an issue
	public NLEdgeG(NLNodeG fromNode, NLNodeG toNode, String edgeLabel,
			String role) {
		super(fromNode, toNode, edgeLabel, new Offset(1));
		// TODO:Investigate wtf is this
		// role = label.contains(":") ? label.split(":")[0] : label;
		this.role = role == null ? label.contains(":") ? label.split(":")[0]
				: label : role;

	}

	public NLEdgeG(NLNodeG fromNode, NLNodeG toNode, String edgeLabel) {
		super(fromNode, toNode, edgeLabel, new Offset(1));

		role = label.contains(":") ? label.split(":")[0] : label;

	}

	public NLEdgeG(NLEdge ee) {

		super(new NLNodeG((NLNode) ee.getFrom()), new NLNodeG(
				(NLNode) ee.getTo()), ee.getLabel(), new Offset(1));
		role = ee.getRole();
	}

	private String role;

	@Override
	public String getRole() {
		return role;
	}

	@Override
	public String setRole(String role) {
		this.role = role;
		return role;
	}

}