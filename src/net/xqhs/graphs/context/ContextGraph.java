package net.xqhs.graphs.context;

import java.util.AbstractMap;
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

// TODO: add canPerformOperation throughout the various Graph classes to be sure that the operation will be performed.
public class ContextGraph extends PrincipalGraph implements TickReceiver
{
	public static class ContextEdge extends SimpleEdge
	{
		/*
		 * in milliseconds.
		 */
		Offset	initialValidity	= null;
		
		public ContextEdge(Node fromNode, Node toNode, String edgeLabel, Offset edgeValidity)
		{
			super(fromNode, toNode, edgeLabel);
			if(edgeValidity != null)
				throw new IllegalArgumentException("Edge validity must be an instantiated object.");
			initialValidity = edgeValidity;
		}
	}
	
	TimeKeeper									theTime	= null;
	PriorityQueue<Entry<Instant, ContextEdge>>	validityQueue;
	
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
				validityQueue.add(new AbstractMap.SimpleEntry<Instant, ContextEdge>(theTime.now().offset(
						((ContextEdge) component).initialValidity), (ContextEdge) component));
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
		while(validityQueue.peek().getKey().before(now))
		{
			removals.add(validityQueue.poll().getValue());
		}
		removeAll(removals);
	}
}
