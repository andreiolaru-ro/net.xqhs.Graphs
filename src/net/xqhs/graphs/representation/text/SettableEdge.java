package net.xqhs.graphs.representation.text;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleEdge;

/**
 * A utility class that allows the <i>from</i> and <i>to</i> nodes to be set separately. It is meant to be converted to
 * a {@link SimpleEdge} after setting both ends.
 * <p>
 * IMPORTANT: this edge should not be added to a graph before setting both adjacent nodes (or better, it should always
 * be converted through the <code>toSimpleEdge()</code> method).
 * 
 * @author Andrei Olaru
 * 
 */
class SettableEdge extends SimpleEdge
{
	/**
	 * Constructs an {@link Edge} instance with the adjacent nodes not set.
	 * 
	 * @param label
	 *            : the label of the edge.
	 */
	protected SettableEdge(String label)
	{
		super(null, null, label);
	}
	
	/**
	 * @param sourceNode
	 *            : the source node.
	 * @return the instance itself.
	 */
	protected SettableEdge setFrom(Node sourceNode)
	{
		from = sourceNode;
		return this;
	}
	
	/**
	 * @param destinationNode
	 *            : the destination node.
	 * @return the instance itself.
	 */
	protected SettableEdge setTo(Node destinationNode)
	{
		to = destinationNode;
		return this;
	}
	
	/**
	 * Transfers the parameters of the current instance to a new {@link SimpleEdge} instance.
	 * 
	 * @return the newly created {@link SimpleEdge} instance, with the same label and from and to nodes.
	 */
	protected SimpleEdge toSimpleEdge()
	{
		return new SimpleEdge(from, to, label);
	}
}
