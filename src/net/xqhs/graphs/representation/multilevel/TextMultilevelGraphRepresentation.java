package net.xqhs.graphs.representation.multilevel;

import java.util.List;
import java.util.Map;

import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.representation.RepresentationElement;

/**
 * Text representation for multilevel graphs. See {@link MultilevelGraphRepresentation}, that this class extends.
 * 
 * @author Andrei Olaru
 */
public class TextMultilevelGraphRepresentation extends MultilevelGraphRepresentation
{
	/**
	 * Creates a new representation.
	 * 
	 * @param graph
	 *            - the graph.
	 * @param nodeLevels
	 *            - the membership function. See
	 *            {@link MultilevelGraphRepresentation#MultilevelGraphRepresentation(Graph, List)}.
	 */
	public TextMultilevelGraphRepresentation(Graph graph, List<Map<Node, Node>> nodeLevels)
	{
		super(graph, nodeLevels);
	}
	
	@Override
	protected void processGraph()
	{
		super.processGraph();
		
	}
	
	@Override
	public Object displayRepresentation()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public RepresentationElement getRepresentation()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
}
