package net.xqhs.graphs.context;

import java.util.IdentityHashMap;
import java.util.Map;

import net.xqhs.graphs.context.Instant.Offset;
import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.pattern.EdgeP;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.pattern.NodeP;

public class ContextPattern extends GraphPattern {
	Offset persistence;

	public ContextPattern() {
		super();
	}

	public ContextPattern(ContextPattern other) {
		super();
		this.maxIndex = other.maxIndex;
		Map<NodeP, NodeP> isomorphism = new IdentityHashMap<NodeP, NodeP>();
		for (Node node : other.getNodes()) {

			NodeP disnNodeP = (NodeP) node;
			NodeP newNode;
			if (disnNodeP.isGeneric()) {
				newNode = new NodeP(disnNodeP.genericIndex());
			} else
				newNode = new NodeP(disnNodeP.getLabel());
			isomorphism.put(disnNodeP, newNode);
			this.add(newNode);
		}
		for (Edge edge : other.edges) {

			EdgeP newEdge = new EdgeP(isomorphism.get(edge.getFrom()),
					isomorphism.get(edge.getTo()), edge.getLabel());
			this.edges.add(newEdge);
		}
	}

	public ContextPattern setPersistence(Offset patternPersistence) {
		try {
			locked();
		} catch (ConfigLockedException e) {
			throw new IllegalStateException(e);
		}
		persistence = patternPersistence;
		return this;
	}

	@Override
	public ContextPattern add(GraphComponent component) {
		try {
			locked();
		} catch (ConfigLockedException e) {
			throw new IllegalStateException(e);
		}
		return (ContextPattern) super.add(component);
	}

	@Override
	public ContextPattern remove(GraphComponent component) {
		try {
			locked();
		} catch (ConfigLockedException e) {
			throw new IllegalStateException(e);
		}
		return (ContextPattern) super.remove(component);
	}
}
