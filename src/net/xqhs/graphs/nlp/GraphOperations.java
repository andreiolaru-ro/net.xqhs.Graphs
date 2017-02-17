package net.xqhs.graphs.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.pattern.EdgeP;
import net.xqhs.graphs.pattern.NodeP;

public class GraphOperations {
	ContextPattern cxt;
	int maxIndex = 0, indexCount = 0;
	ArrayList<GraphComponent> createables, deletables;

	public GraphOperations(ContextPattern cxt) {
		this.cxt = cxt;
		for (Node node : cxt.getNodes()) {
			if (((NodeP) node).isGeneric()) {
				indexCount++;
				maxIndex = Math.max(maxIndex, ((NodeP) node).genericIndex());
			}
		}
		createables = new ArrayList<GraphComponent>();
		deletables = new ArrayList<GraphComponent>();
	}

	public NLEdgeP addEdge(NLNodeP from, NLNodeP to, String edgeLabel) {
		NLEdgeP edgeAlreadyThere = (NLEdgeP) containsEdge(from, to);
		if (edgeAlreadyThere == null) {
			NLEdgeP edge = new NLEdgeP(from, to, edgeLabel);
			cxt.addEdge(edge);
			System.out.println("Adding edge " + from + " -" + edgeLabel + "-> "
					+ to);
			return edge;
		}

		edgeAlreadyThere
				.setLabel(edgeAlreadyThere.getLabel() + ":" + edgeLabel);
		return edgeAlreadyThere;

	}

	private Edge containsEdge(NLNodeP from, NLNodeP to) {
		Collection<Edge> edges = new ArrayList<Edge>();
		// edges.addAll(cxt.getInEdges(from));
		edges.addAll(cxt.getOutEdges(from));
		List<Edge> es = edges.stream().filter(e -> e.getTo() == to)
				.collect(Collectors.toList());
		if (es != null && !es.isEmpty())
			return es.get(0);
		return null;

	}

	public void removeEdge(EdgeP edge) {
		if (cxt.contains(edge)) {
			System.out.println("Removing edge " + edge.getFrom() + " -"
					+ edge.getLabel() + "-> " + edge.getTo());
			cxt.removeEdge(edge);
		} else
			System.out.println("Edge already removed");
	}

	public NodeP addNode(String label) {
		NodeP n = new NodeP(label);
		cxt.addNode(n);
		System.out.println("Adding node " + label);
		return n;
	}

	public void removeNode(NodeP node) {
		if (cxt.contains(node)) {
			System.out.println("Removing node " + node);
			if (node.isGeneric()) {
				genericNodeDeleted(node.genericIndex());
			}
			cxt.removeNode(node);
		}

	}

	private void genericNodeDeleted(int index) {
		indexCount--;

	}

	public NodeP addNode(NodeP node) {
		cxt.addNode(node);
		System.out.println("Adding node " + node);
		return node;
	}

	// badBadDontUse
	public NLNodeP addGenericNode() {
		NLNodeP genericNode = new NLNodeP();
		indexCount++;
		System.out.println("Added generic node:" + genericNode);
		cxt.addNode(genericNode);
		return genericNode;
	}

	public NLNodeP getNLNodeByWordIndex(int index) {
		for (Node node : cxt.getNodes()) {
			NLNodeP nlNode = (NLNodeP) node;
			if (nlNode.getWordIndex() == index)
				return nlNode;
		}
		return null;
	}

	public void mergeNodes(NodeP fromDep, NodeP toGov) {
		if (cxt.contains(fromDep) && cxt.contains(toGov))
			moveEdges(fromDep, toGov, true);
		removeNode(fromDep);
	}

	public void apply() {
		if (!deletables.isEmpty()) {
			cxt.removeAll(deletables);
			deletables.clear();
		}
		if (!createables.isEmpty()) {
			cxt.addAll(createables);
			createables.clear();
		}
	}

	public NLNodeP getByIndex(int index) {
		NLNodeP nodeNL = null;
		for (Node node : cxt.getNodes()) {
			nodeNL = (NLNodeP) node;
			if (nodeNL.getWordIndex() == index) {
				return nodeNL;
			}
		}
		return nodeNL;
	}

	public void moveEdges(NodeP from, NodeP to, boolean andRelabel) {
		if (cxt.contains(from)) {
			for (Edge outEdgeP : cxt.getOutEdges(from)) {
				if (outEdgeP.getTo().equals(to)) {
					removeEdge((EdgeP) outEdgeP);
				} else {
					String label = outEdgeP.getLabel();
					if (andRelabel) {
						label = from.getLabel() + " :" + label;
					}
					createables.add(new NLEdgeP((NLNodeP) to,
							(NLNodeP) outEdgeP.getTo(), label));
					deletables.add(outEdgeP);
					System.out.println("Modifying edge:" + outEdgeP.getFrom()
							+ " --" + outEdgeP.getLabel() + "-> "
							+ outEdgeP.getTo() + " to " + to.getLabel() + " -"
							+ from.getLabel() + " :" + outEdgeP.getLabel()
							+ "-> " + outEdgeP.getTo());
				}
			}
			for (Edge inEdgeP : cxt.getInEdges(from)) {
				if (inEdgeP.getFrom().equals(to)) {
					removeEdge((EdgeP) inEdgeP);
				} else {
					String label = inEdgeP.getLabel();
					if (andRelabel) {
						label += " :" + from.getLabel();
					}
					createables.add(new NLEdgeP((NLNodeP) inEdgeP.getFrom(),
							(NLNodeP) to, label));
					deletables.add(inEdgeP);
					System.out.println("Modifying edge:" + inEdgeP.getFrom()
							+ " --" + inEdgeP.getLabel() + "-> "
							+ inEdgeP.getTo() + " to " + inEdgeP.getFrom()
							+ " -" + inEdgeP.getLabel() + " :"
							+ from.getLabel() + to.getLabel() + "-> " + from);
				}
			}

			apply();

		} else
			System.out.println("Node " + from + "not in graph");
	}

}
