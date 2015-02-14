package net.xqhs.graphs.context;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;

import net.xqhs.graphs.context.Instant.Offset;
import net.xqhs.graphs.context.Instant.TickReceiver;
import net.xqhs.graphs.context.Instant.TimeKeeper;
import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleEdge;
import net.xqhs.graphs.matchingPlatform.GMPImplementation.PrincipalGraph;
import net.xqhs.graphs.matchingPlatform.TrackingGraph;
import net.xqhs.graphs.matchingPlatform.Transaction.Operation;
import net.xqhs.graphs.pattern.NodeP;

/**
 * When {@link ContextEdge} instances are added to a the graph, the behavior is different from when using underlying
 * classes. That is, the edge is attached to the graph correctly even if the nodes adjacent to it are new instances (are
 * not the ones from the graph). Moreover, if any nodes adjacent to it are not already in the graph (as found by label
 * comparison), they will be added to the graph automatically.
 * <p>
 * When a {@link ContextEdge} instance is added to, or removed from the graph, it will also be added to / removed from
 * the validity queue. WHen the validity of an edge expires, it is removed.
 *
 * @author Andrei Olaru
 */
// TODO: add canPerformOperation throughout the various Graph classes to be sure that the operation will be performed.
public class ContextGraph extends PrincipalGraph implements TickReceiver
{
	/**
	 * A context edge features, beside what is offered by {@link SimpleEdge}, a validity setting, so that it is able to
	 * expire at a given time after being added to a {@link ContextGraph}.
	 *
	 * @author Andrei Olaru
	 */
	public static class ContextEdge extends SimpleEdge
	{
		/*
		 * in milliseconds.
		 */
		Offset	initialValidity	= null;

		public ContextEdge(Node fromNode, Node toNode, String edgeLabel, Offset edgeValidity)
		{
			super(fromNode, toNode, edgeLabel);
			if(edgeValidity == null)
				throw new IllegalArgumentException("Edge validity must be an instantiated object.");
			initialValidity = edgeValidity;
		}

		/**
		 * This method is available internally for use by {@link ContextGraph}.
		 *
		 * @param sourceNode
		 *            : the source node.
		 * @return the instance itself.
		 */
		ContextEdge setFrom(Node sourceNode)
		{
			from = sourceNode;
			return this;
		}
		
		/**
		 * This method is available internally for use by {@link ContextGraph}.
		 *
		 * @param destinationNode
		 *            : the destination node.
		 * @return the instance itself.
		 */
		ContextEdge setTo(Node destinationNode)
		{
			to = destinationNode;
			return this;
		}
	}

	TimeKeeper									theTime			= null;
	PriorityQueue<Entry<Instant, ContextEdge>>	validityQueue	= new PriorityQueue<Entry<Instant, ContextEdge>>(1,
																		new Comparator<Entry<Instant, ContextEdge>>() {
																			@Override
																			public int compare(
																					Entry<Instant, ContextEdge> o1,
																					Entry<Instant, ContextEdge> o2)
																			{
																				return (int) Math.signum(o1.getKey().time
																						- o2.getKey().time);
																			}
																		});

	protected ContextGraph setTimeKeeper(TimeKeeper time)
	{
		theTime = time;
		theTime.registerTickReceiver(this, null);
		return this;
	}

	@Override
	protected ContextGraph performOperation(GraphComponent component, Operation operation, boolean externalCall)
	{
		// TODO check if modification will be done (see top of file)
		if(isShadow && externalCall)
			throw new UnsupportedOperationException(
					"A shadow graph only takes modifications from its transaction queue.");
		if(component instanceof ContextEdge)
			switch(operation)
			{
			case ADD:
				// check nodes and edge (see class documentation)
				ContextEdge e = (ContextEdge) component;
				Node from = e.getFrom();
				Node to = e.getTo();
				Collection<Node> fromExisting = getNodesNamed(from.getLabel());
				Collection<Node> toExisting = getNodesNamed(to.getLabel());

				if(!fromExisting.isEmpty())
					e.setFrom(fromExisting.iterator().next());
				else
					super.performOperation(e.getFrom(), Operation.ADD, externalCall);

				if(!toExisting.isEmpty())
					e.setTo(toExisting.iterator().next());
				else
					super.performOperation(e.getTo(), Operation.ADD, externalCall);

				if(!fromExisting.isEmpty() && !toExisting.isEmpty())
					for(Edge existing : nodes.get(e.getFrom()).getOutEdges())
						if((existing.getTo() == e.getTo()) && (existing.getLabel().equals(e.getLabel())))
							// edge is existing
							return this;
				
				validityQueue.add(new AbstractMap.SimpleEntry<Instant, ContextEdge>(theTime.now().offsetInstant(
						e.initialValidity), e));
				break;
			case REMOVE:
				for(Iterator<Entry<Instant, ContextEdge>> it = validityQueue.iterator(); it.hasNext();)
					if(it.next().getValue() == component)
						it.remove();
				break;
			}
		super.performOperation(component, operation, externalCall);
		return this;
	}

	/**
	 * Overrides {@link TrackingGraph#add(GraphComponent)} to not allow nodes with the same label (as per the theory of
	 * context graphs) or generic nodes (having a label beginning with {@link NodeP#NODEP_LABEL}.
	 */
	@Override
	public ContextGraph add(GraphComponent component)
	{
		if((component instanceof NodeP) && ((NodeP) component).isGeneric())
			throw new IllegalArgumentException("Generic nodes are not allowed");
		if((component instanceof Node) && ((Node) component).getLabel().startsWith(NodeP.NODEP_LABEL))
			throw new IllegalArgumentException("Generic nodes are not allowed");
		// TODO this is not efficient -- a index of labels should be used
		if((component instanceof Node) && !getNodesNamed(((Node) component).getLabel()).isEmpty())
			throw new IllegalArgumentException("Multiple nodes with the same name are not allowed");

		super.add(component);
		return this;
	}

	@Override
	public void tick(TimeKeeper ticker, Instant now)
	{
		Set<Edge> removals = new HashSet<Edge>();
		while(!validityQueue.isEmpty() && validityQueue.peek().getKey().before(now))
			removals.add(validityQueue.poll().getValue());
		removeAll(removals);
	}
}
