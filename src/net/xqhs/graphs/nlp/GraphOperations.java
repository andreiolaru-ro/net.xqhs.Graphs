package net.xqhs.graphs.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleGraph;

public class GraphOperations {
	// ContextPattern cxt;
	SimpleGraph cxt;
	NLGraphType t;
	int maxIndex = 0, indexCount = 0;
	ArrayList<GraphComponent> createables, deletables;

	public GraphOperations(NLGraphType t, SimpleGraph cxt) {
		this.cxt = cxt;
		this.t = t;
		for (Node node : cxt.getNodes()) {
			if (node instanceof NLNodeP) {
				if (((NLNodeP) node).isGeneric()) {
					indexCount++;
					maxIndex = Math.max(maxIndex,
							((NLNodeP) node).genericIndex());
				}
			}
		}
		createables = new ArrayList<GraphComponent>();
		deletables = new ArrayList<GraphComponent>();
	}

	public NLEdge addEdge(NLNode from, NLNode to, String edgeLabel, String role) {
		NLEdge edgeAlreadyThere = (NLEdge) containsEdge(from, to);
		if (edgeAlreadyThere == null) {
//			System.out.println("Adding edge " + from + " -" + edgeLabel + "-> "
//					+ to);
			NLEdge edge = NLEdgeFactory
					.makeNLEdge(t, from, to, edgeLabel, role);
			// new NLEdgeP(from, to, edgeLabel);

			cxt.addEdge(edge);

			return edge;
		}

		edgeAlreadyThere
				.setLabel(edgeAlreadyThere.getLabel() + ":" + edgeLabel);
		return edgeAlreadyThere;

	}

	private Edge containsEdge(NLNode from, NLNode to) {
		Collection<Edge> edges = new ArrayList<Edge>();
		// edges.addAll(cxt.getInEdges(from));
		edges.addAll(cxt.getOutEdges(from));
		List<Edge> es = edges.stream().filter(e -> e.getTo() == to)
				.collect(Collectors.toList());
		if (es != null && !es.isEmpty())
			return es.get(0);
		return null;

	}

	public void removeEdge(NLEdge edge) {
		if (cxt.contains(edge)) {
//			System.out.println("Removing edge " + edge.getFrom() + " -"
//					+ edge.getLabel() + "-> " + edge.getTo());
			cxt.removeEdge(edge);
		} //else
//			System.out.println("Edge already removed");
	}

	// public NLNode addNode(String label) {
	// NLNode n =NLNodeFactory.makeNode(t, w) new NodeP(label);
	// cxt.addNode(n);
	// System.out.println("Adding node " + label);
	// return n;
	// }

	public void removeNode(NLNode node) {
		if (cxt.contains(node)) {
//			System.out.println("Removing node " + node);
			// prolly useless
			if (node instanceof NLNodeP) {
				if (((NLNodeP) node).isGeneric()) {
					genericNodeDeleted(((NLNodeP) node).genericIndex());
				}
			}
			cxt.removeNode(node);
		}

	}

	// neverused
	private void genericNodeDeleted(int index) {
		indexCount--;

	}

	public NLNode addNode(NLNode node) {
		cxt.addNode(node);
//		System.out.println("Adding node " + node);
		return node;
	}

	// badBadDontUse
	// public NLNodeP addGenericNode() {
	// NLNodeP genericNode = new NLNodeP();
	// indexCount++;
	// System.out.println("Added generic node:" + genericNode);
	// cxt.addNode(genericNode);
	// return genericNode;
	// }

	public NLNode getNLNodeByWordIndex(int index) {
		for (Node node : cxt.getNodes()) {
			NLNode nlNode = (NLNode) node;
			if (nlNode.getWordIndex() == index)
				return nlNode;
		}
		return null;
	}

	public void mergeNodes(NLNode fromDep, NLNode toGov) {
		if (cxt.contains(fromDep) && cxt.contains(toGov)) {
			moveEdges(fromDep, toGov, false);
			removeNode(fromDep);
		}
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

	public NLNode getByIndex(int index) {
		NLNode nodeNL = null;
		for (Node node : cxt.getNodes()) {
			nodeNL = (NLNode) node;
			if (nodeNL.getWordIndex() == index) {
				return nodeNL;
			}
		}
		return nodeNL;
	}

	public void moveEdges(NLNode from, NLNode to, boolean andRelabel) {
		if (cxt.contains(from) && cxt.contains(to) && !from.equals(to)) {
			for (Edge outEdgeP : cxt.getOutEdges(from)) {
				if (outEdgeP.getTo().equals(to)) {

					deletables.add(outEdgeP);
				} else {
					String label = outEdgeP.getLabel();
					String role = ((NLEdge) outEdgeP).getRole();
					if (andRelabel) {
						label = from.getLabel() + " :" + label;
					}
					createables.add(NLEdgeFactory.makeNLEdge(t, to,
							(NLNode) outEdgeP.getTo(), label, role));

					// new NLEdgeP((NLNodeP) to,
					// (NLNodeP) outEdgeP.getTo(), label, role));
					deletables.add(outEdgeP);
//					System.out.println("Modifying outEdge:"
//							+ outEdgeP.getFrom() + " --" + outEdgeP.getLabel()
//							+ "-> " + outEdgeP.getTo() + " [role]:"
//							+ ((NLEdge) outEdgeP).getRole() + " to \n" + to
//							+ " -" + label + "-> " + outEdgeP.getTo());
				}
			}
			for (Edge inEdgeP : cxt.getInEdges(from)) {
				// no reflexives pls
				if (inEdgeP.getFrom().equals(to)) {
					deletables.add(inEdgeP);
				} else {
					String label = inEdgeP.getLabel();
					String role = ((NLEdge) inEdgeP).getRole();
					if (andRelabel) {
						label += " :" + from.getLabel();
					}
					createables.add(NLEdgeFactory.makeNLEdge(t,
							(NLNode) inEdgeP.getFrom(), to, label, role));
					// new NLEdgeP((NLNodeP) inEdgeP.getFrom(),
					// (NLNodeP) to, label, role));
					deletables.add(inEdgeP);
//					System.out.println("Modifying inEdge:" + inEdgeP.getFrom()
//							+ " --" + inEdgeP.getLabel() + "-> "
//							+ inEdgeP.getTo() + " [role]:"
//							+ ((NLEdge) inEdgeP).getRole() + "  to \n"
//							+ inEdgeP.getFrom() + " -" + label + "-> " + to);
				}
			}

			apply();

		} //else
//			System.out.println("Node " + from + " or " + to
//					+ "not in graph or " + from + " == " + to);
	}

}
