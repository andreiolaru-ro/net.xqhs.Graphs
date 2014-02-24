package net.xqhs.graphs.graph;

import net.xqhs.graphs.representation.text.TextGraphRepresentation;

/**
 * The class is a {@link Node} implementation that represents a node in a hyper graph, i.e. a node that contains a
 * {@link Graph} inside it.
 * <p>
 * The label of a hyper node is optional, therefore the node is initially created with a <code>null</code> label.
 * 
 * @author Andrei Olaru
 */
public class HyperNode extends SimpleNode
{
	/**
	 * The {@link Graph} contained by this node.
	 */
	Graph					nodeContents;
	/**
	 * The representation for the content of the node, which is returned on the call of {@link #toString()}.
	 */
	TextGraphRepresentation	representation;
	
	/**
	 * Creates a new {@link HyperNode} instance, by initializing it with its subordinate graph.
	 * <p>
	 * The label of the hyper node is optional, and it can be set using {@link #setLabel(String)};
	 * 
	 * @param graph
	 *            - the graph contained by this node.
	 */
	public HyperNode(Graph graph)
	{
		super(null);
		if(graph == null)
			throw new IllegalArgumentException("null content graph not allowed");
		nodeContents = graph;
		representation = new TextGraphRepresentation(nodeContents).setLayout("", "", 0);
	}
	
	/**
	 * Sets the label of the node.
	 * 
	 * @param nodeLabel
	 *            - the label.
	 * @return the instance itself.
	 */
	public HyperNode setLabel(String nodeLabel)
	{
		label = nodeLabel;
		return this;
	}
	
	@Override
	public String toString()
	{
		// FIXME: should the output be cashed?
		return representation.update().toString();
	}
}
