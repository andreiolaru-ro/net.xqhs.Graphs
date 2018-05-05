package net.xqhs.graphs.matcher;

import java.util.ArrayList;
import java.util.HashMap;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.nlp.ContextPatternConverter;
import net.xqhs.graphs.nlp.NLEdge;
import net.xqhs.graphs.nlp.NLEdgeG;
import net.xqhs.graphs.nlp.NLNode;
import net.xqhs.graphs.nlp.NLNodeG;
import net.xqhs.graphs.nlp.NLNodeP;
import net.xqhs.graphs.pattern.GraphPattern;
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
		ArrayList<NLNodeG> nodes = new ArrayList<NLNodeG>();
		HashMap<NLNodeP, NLNodeG> iso = new HashMap<NLNodeP, NLNodeG>();
		for (Node nodep : gp.getNodes()) {
			NLNodeP node = (NLNodeP) nodep;
			if (!iso.containsKey(nodep)) {
				if (node.isGeneric()) {
					Edge detEdge = gp
							.getOutEdges(node)
							.stream()
							.filter(e -> ((NLEdge) e).getRole().equals(
									ContextPatternConverter.determinerRole))
							.findFirst().get();
					Node determinedType = detEdge.getTo();
					NLNodeG nodeg = new NLNodeG((NLNode) determinedType);
					nodes.add(nodeg);
					iso.put(node, nodeg);
					iso.put((NLNodeP) determinedType, nodeg);
				} else {
					NLNodeG nodeg = new NLNodeG(node);
					nodes.add(nodeg);
					iso.put(node, nodeg);
				}
			}
		}
		newg.addAll(nodes);
		System.out.println("Node correspondences:" + iso.toString());
		for (Edge e : gp.getEdges()) {
			if (!((NLEdge) e).getRole().equals(
					ContextPatternConverter.determinerRole))
			// if edge ain't an is
			{
				NLNodeG from = iso.get(e.getFrom());
				NLNodeG to = iso.get(e.getTo());
				if (from != null && to != null) {

					NLEdgeG eG = new NLEdgeG(from, to, e.getLabel(),
							((NLEdge) e).getRole());
					newg.add(eG);
				} else {
					// System.out.println("Cannot add edge" + e +
					// " because node "
					// + from + " or " + to + " missing");
					NLNodeG newNode;
					newNode = from == null ? new NLNodeG((NLNodeP) e.getFrom())
							: new NLNodeG((NLNodeP) e.getTo());
					// TODO: treat case in which both to and from are null
					NLEdgeG eG = new NLEdgeG(from == null ? newNode : from,
							to == null ? newNode : to, e.getLabel(),
							((NLEdge) e).getRole());
					newg.add(eG);
					// newg.add(newNode);

				}

			}
		}
		// try {
		// Parser.contextPatternVisualize(newg, true);
		//
		// } catch (IOException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// } catch (InterruptedException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		TextGraphRepresentation tgr = new TextGraphRepresentation(newg);
		System.out.println(tgr.update().toString());
		return newg;

	}

	public SimpleGraph nowKiss(SimpleGraph gg, GraphPattern unsolvedPart) {
		SimpleGraph g = copyGraph(gg);
		SimpleGraph u = convertUnsolvedPartOfPatternToGraph(unsolvedPart);

		for (Edge ee : u.getEdges()) {
			NLEdge e = (NLEdge) ee;
			// deal with nodes at both ends
			// if the sub is in the graph there is a question over whether
			// the
			// higher part of the pattern( the one it contributes to) should
			// follow into the graph.
			if (u.contains(e.getFrom()) && u.contains(e.getTo())) {// from, to
																	// in u
				g.add(e);
				g.add(e.getFrom());
				g.add(e.getTo());

			}
			if (u.contains(e.getTo()) && !u.contains(e.getFrom()))// from in g,
																	// to in u
				if (!g.getNodesNamed(e.getFrom().getLabel()).isEmpty()) {
					g.add(e.getTo());

					NLEdgeG edge = new NLEdgeG((NLNodeG) g
							.getNodesNamed(e.getFrom().getLabel()).iterator()
							.next(), (NLNodeG) e.getTo(), e.getLabel(),
							e.getRole());
					g.add(edge);// & connect to existing node
				} else
					// from nowhere to b found
					System.out.println("Error node " + e.getFrom()
							+ " not found ");

			if (u.contains(e.getFrom()) && !u.contains(e.getTo()))// from in u,
																	// to in g
				if (!g.getNodesNamed(e.getTo().getLabel()).isEmpty()) {
					g.add(e.getFrom());
					NLEdgeG edge = new NLEdgeG((NLNodeG) e.getFrom(),
							(NLNodeG) g.getNodesNamed(e.getTo().getLabel())
									.iterator().next(), e.getLabel(),
							e.getRole());
					g.add(edge);// &connect to existing node

				} else
					System.out.println("Error " + e.getTo() + " not found");

			if (!u.contains(e.getTo()) && !u.contains(e.getFrom())) {
				g.add(new NLEdgeG((NLNodeG) g.getNodesNamed(e.getFrom()
						.getLabel()), (NLNodeG) g.getNodesNamed(e.getTo()
						.getLabel()), e.getLabel(), e.getRole()));
			}
		}
		for (Node node : u.getNodes()) {
			if (!g.contains(node)) {
				g.add(node);
			}
		}
		TextGraphRepresentation tgr = new TextGraphRepresentation(g);
		System.out.println(tgr.displayRepresentation());
		return g;
	}
}
