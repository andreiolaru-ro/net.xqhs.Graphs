package net.xqhs.graphs.matcher;

import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Collectors;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.nlp.ContextPatternConverter;
import net.xqhs.graphs.nlp.NLEdge;
import net.xqhs.graphs.nlp.NLEdgeG;
import net.xqhs.graphs.nlp.NLNode;
import net.xqhs.graphs.nlp.NLNodeG;
import net.xqhs.graphs.nlp.NLNodeP;
import net.xqhs.graphs.nlp.Parser;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.pattern.NodeP;
import net.xqhs.graphs.representation.text.TextGraphRepresentation;

public class MatchMaker {

	SimpleGraph copyGraph(SimpleGraph g) {
		System.out.println("Copying context graph");
		SimpleGraph newg = new SimpleGraph();
		HashMap<NLNodeG, NLNodeG> iso = new HashMap<NLNodeG, NLNodeG>();

		for (Node n : g.getNodes()) {
			NLNode node = new NLNodeG((NLNodeG) n);
			newg.add(node);
			System.out.println("Created node " + node + " based on " + n);
			iso.put((NLNodeG) n, (NLNodeG) node);
		}

		for (Edge e : g.getEdges()) {
			NLEdge newEdge = new NLEdgeG(iso.get(e.getFrom()), iso.get(e
					.getTo()), e.getLabel(), ((NLEdge) e).getRole());
			newg.add(newEdge);
		}

		TextGraphRepresentation tgr = new TextGraphRepresentation(newg);
		System.out.println(tgr.update().toString());
		return newg;
	}

	SimpleGraph convertUnsolvedPartOfPatternToGraph(GraphPattern gp) {
		System.out.println("Coverting pattern to graph");

		SimpleGraph newg = new SimpleGraph();
		HashMap<NLNodeP, NLNodeG> iso = convertPatternNodesToGraphNodes(gp);
		// connect generic edges whose generics are part of the match
		for (Edge e : gp
				.getEdges()
				.stream()
				.filter(ee -> ((NLEdge) ee).getRole().equals(
						ContextPatternConverter.determinerRole))
				.collect(Collectors.toList())) {

			if (!iso.containsKey(e.getFrom())) {
				if (iso.containsKey(e.getTo())) {
					iso.put((NLNodeP) e.getFrom(), iso.get(e.getTo()));
					System.out.println("Made association " + e.getFrom()
							+ " == " + iso.get(e.getFrom()));
				}
			}
		}
		newg.addAll(iso.values().stream().distinct()
				.collect(Collectors.toList()));
		System.out.println("Node correspondences:" + iso.toString());
		for (Edge e : gp.getEdges()) {
			// if edge ain't an is
			if (!((NLEdge) e).getRole().equals(
					ContextPatternConverter.determinerRole))

			{
				NLNodeG from = iso.get(e.getFrom());
				NLNodeG to = iso.get(e.getTo());
				if (from != null && to != null) {

					NLEdgeG eG = new NLEdgeG(from, to, e.getLabel(),
							((NLEdge) e).getRole());
					newg.add(eG);
				} else {
					// create copies of the missing nodes and add them to the
					// graph
					System.out.println("Free floating edge " + e);
					NLNodeG newFrom = null, newTo = null;

					if (from == null) {
						if (!((NLNodeP) e.getFrom()).isGeneric())
							newFrom = new NLNodeG((NLNodeP) e.getFrom());
						else {
							// try to find type in iso- doesnt work & needs
							// replacing if case ever pops
							from = iso
									.get(gp.getEdges()
											.stream()
											.filter(ee -> ((NLEdge) ee)
													.getRole()
													.equals(ContextPatternConverter.determinerRole)
													&& ee.getFrom().equals(
															ee.getFrom()))
											.findFirst().get().getTo());
							if (from == null)
								newFrom = new NLNodeG(
										(NLNodeP) gp
												.getEdges()
												.stream()
												.filter(ee -> ((NLEdge) ee)
														.getRole()
														.equals(ContextPatternConverter.determinerRole)
														&& ee.getFrom().equals(
																ee.getFrom()))
												.findFirst().get().getTo());
							// newFrom.setLabel(newFrom.getLabel() + "**");
							System.out.println("Created node " + newFrom);

						}
					}
					if (to == null) {
						if (!((NLNodeP) e.getTo()).isGeneric())
							newTo = new NLNodeG((NLNodeP) e.getTo());
						else {
							to = iso.get(gp
									.getEdges()
									.stream()
									.filter(ee -> ((NLEdge) ee)
											.getRole()
											.equals(ContextPatternConverter.determinerRole)
											&& ee.getFrom().equals(ee.getTo()))
									.findFirst().get().getTo());
							if (to == null)
								newTo = new NLNodeG(
										(NLNodeP) gp
												.getEdges()
												.stream()
												.filter(ee -> ((NLEdge) ee)
														.getRole()
														.equals(ContextPatternConverter.determinerRole)
														&& ee.getFrom().equals(
																ee.getTo()))
												.findFirst().get().getTo());
							// newTo.setLabel(newTo.getLabel() + "**");
							System.out.println("Created node" + newTo);

						}
					}

					NLEdgeG eG = new NLEdgeG(from == null ? newFrom : from,
							to == null ? newTo : to, e.getLabel(),
							((NLEdge) e).getRole());
					newg.add(eG);
					if (from == null)
						newg.add(newFrom);
					if (to == null)
						newg.add(newTo);

				}
			} else {
				System.out.println("Found determiner edge");

			}

		}
		try {
			Parser.displayContextPattern(newg, true);

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// TextGraphRepresentation tgr = new TextGraphRepresentation(newg);
		// System.out.println(tgr.update().toString());
		return newg;

	}

	public HashMap<NLNodeP, NLNodeG> convertPatternNodesToGraphNodes(
			GraphPattern gp) {
		HashMap<NLNodeP, NLNodeG> iso = new HashMap<NLNodeP, NLNodeG>();
		System.out.println("Nodes in unsolved part of pattern: "
				+ gp.getNodes().toString());
		for (Node nodep : gp.getNodes()) {
			NLNodeP node = (NLNodeP) nodep;
			if (!iso.containsKey(node)) {
				if (node.isGeneric()) {
					// get determiner edge
					NLNodeP genericNode = node;
					NLNodeP type = findTypeOfGenericNode(gp, genericNode);
					// copy type node
					NLNodeG newTypeG = new NLNodeG(type);
					// add 2 stars to label to make sure we know this is alien
					// if (!gp.getNodes().contains(type))
					// newTypeG.setLabel(newTypeG.getLabel() + "**");
					// add reference from generic to type in iso
					iso.put(genericNode, newTypeG);
					// add reference from original type node to its copy
					iso.put(type, newTypeG);
					System.out
							.println("Found generic node of type " + newTypeG);
				} else {
					// copy node
					NLNodeG nodeg = new NLNodeG(node);
					// add correspondence from original pattern node to copy
					iso.put(node, nodeg);
				}
			}
		}
		return iso;
	}

	public static NLNodeP findTypeOfGenericNode(GraphPattern gp,
			NodeP genericNode) {
		Edge detEdge = gp
				.getOutEdges(genericNode)
				.stream()
				.filter(e -> ((NLEdge) e).getRole().equals(
						ContextPatternConverter.determinerRole)
						&& e.getFrom().equals(genericNode)).findFirst().get();
		// find actual type of generic node
		NLNodeP type = (NLNodeP) detEdge.getTo();
		return type;
	}

	public SimpleGraph nowKiss(SimpleGraph gg, GraphPattern unsolvedPart) {
		SimpleGraph g = copyGraph(gg);
		SimpleGraph u = convertUnsolvedPartOfPatternToGraph(unsolvedPart);

		for (Edge ee : u.getEdges()) {
			NLEdge e = (NLEdge) ee;
			// deal with nodes at both ends
			// if the sub is in the graph there is a question over whether the
			// higher part of the pattern should follow into the graph.

			// case from, to not in g
			if (g.getNodesNamed(e.getFrom().getLabel()).isEmpty()
					&& g.getNodesNamed(e.getTo().getLabel()).isEmpty()) {

				if (u.contains(e.getFrom()) && u.contains(e.getTo())) {
					g.add(e);
					g.add(e.getFrom());
					g.add(e.getTo());

				}
			}
			// from in g, to in u
			// if (u.contains(e.getTo()) && !u.contains(e.getFrom()))
			// if the graph contains the missing node
			else if (!g.getNodesNamed(e.getFrom().getLabel()).isEmpty()
					&& g.getNodesNamed(e.getTo().getLabel()).isEmpty()) {
				// add to node to graph
				g.add(e.getTo());

				NLEdgeG edge = new NLEdgeG((NLNodeG) g
						.getNodesNamed(e.getFrom().getLabel()).iterator()
						.next(), (NLNodeG) e.getTo(), e.getLabel(), e.getRole());
				System.out.println("Created edge " + e + "using node "
						+ e.getFrom());
				// &add edge to connect to existing from node
				g.add(edge);
			}
			// from in u, to in g
			// if (u.contains(e.getFrom()) && !u.contains(e.getTo()))
			else if (g.getNodesNamed(e.getFrom().getLabel()).isEmpty()
					&& !g.getNodesNamed(e.getTo().getLabel()).isEmpty()) {
				// add from to g
				g.add(e.getFrom());
				NLEdgeG edge = new NLEdgeG((NLNodeG) e.getFrom(), (NLNodeG) g
						.getNodesNamed(e.getTo().getLabel()).iterator().next(),
						e.getLabel(), e.getRole());
				g.add(edge);// &connect to existing node to

			}
			// if both nodes are in graph & edge is in pattern
			else {
				g.add(new NLEdgeG((NLNodeG) g
						.getNodesNamed(e.getFrom().getLabel()).iterator()
						.next(), (NLNodeG) g
						.getNodesNamed(e.getTo().getLabel()).iterator().next(),
						e.getLabel(), e.getRole()));
			}
		}

		for (Node node : u.getNodes()) {
			if (g.getNodesNamed(node.getLabel()).isEmpty()) {
				g.add(node);
			}
		}

		TextGraphRepresentation tgr = new TextGraphRepresentation(g);
		System.out.println(tgr.displayRepresentation());
		return g;
	}

}
