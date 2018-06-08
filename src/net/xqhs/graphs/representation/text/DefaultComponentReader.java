package net.xqhs.graphs.representation.text;

import net.xqhs.graphs.graph.SimpleEdge;
import net.xqhs.graphs.graph.SimpleNode;

/**
 * {@link GraphComponentReader} implementation for the default represenation of nodes and edges.
 * <ul>
 * <li>nodes are represented by their label.
 * <li>edges are also represented by their label.
 * </ul>
 *
 * @author andreiolaru
 */
public class DefaultComponentReader implements GraphComponentReader
{
	
	@Override
	public SimpleNode readNode(String rawInput)
	{
		return new SimpleNode(rawInput);
	}
	
	@Override
	public SettableEdge readEdge(String rawInput)
	{
		return new SettableEdge(rawInput);
	}
	
	@Override
	public SimpleEdge compileEdge(SettableEdge settableEdge)
	{
		return new SimpleEdge(settableEdge.getFrom(), settableEdge.getTo(), settableEdge.getLabel());
	}

}
