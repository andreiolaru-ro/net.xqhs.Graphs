package net.xqhs.graphs.representation.multilevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.representation.GraphRepresentationImplementation;

/**
 * 
 * @author Andrei Olaru
 */
public abstract class MultilevelGraphRepresentation extends GraphRepresentationImplementation
{
	/**
	 * A {@link List} in which each item corresponds to a level. Levels go bottom &rarr; top. An item in the list is a
	 * {@link Map} that represents the member function for nodes of the current level, as (node-on-the-current-level,
	 * node-on-the-next-level-this-node-belongs-to).
	 * <p>
	 * Though not necessary, an element exists for the last level, with all values <code>null</code>.
	 */
	protected List<Map<Node, Node>>		levelNodes	= null;
	/**
	 * A {@link List} in which each item corresponds to a level. Levels go bottom &rarr; top. Each item contains the
	 * graphs at the current level, having their corresponding nodes in the upper level as key.
	 * <p>
	 * An item is a {@link Map} that links a {@link Node} from the level above to a subgraph at the current level which
	 * corresponds to that Node.
	 * <p>
	 * Though not necessary, an element exists for the bottom level and is is an empty {@link Map} (for easier reading
	 * of code).
	 * <p>
	 * This member is built when {@link #processGraph()} is called, based on {@link #levelNodes}, by gathering up nodes
	 * belonging to the same node in the level above into the same {@link Graph} instance.
	 */
	protected List<Map<Node, Graph>>	theLevels	= null;
	
	/**
	 * Creates a new multi-level representation, based on the graph containing all nodes and the membership function
	 * between levels.
	 * 
	 * @param graph
	 *            - the {@link Graph} reuniting all nodes in the representation.
	 * @param nodeLevels
	 *            - a {@link List} in which each item corresponds to a level, going from bottom level to top level. An
	 *            item is a {@link Map} that represents the member function for nodes of the current level, as (node,
	 *            node-on-the-next-level-this-node-belongs-to). An item for the last level of the graph should exist and
	 *            have all values <code>null</code>.
	 */
	public MultilevelGraphRepresentation(Graph graph, List<Map<Node, Node>> nodeLevels)
	{
		super(graph);
		
		levelNodes = new Vector<Map<Node, Node>>(nodeLevels);
	}
	
	/**
	 * Creates the {@link #theLevels} member, based on {@link #levelNodes}.
	 */
	@Override
	protected void processGraph()
	{
		theLevels = new ArrayList<Map<Node, Graph>>(levelNodes.size());
		for(Map<Node, Node> level : levelNodes)
		{
			// create the level
			Map<Node, Graph> graphs = new HashMap<Node, Graph>();
			theLevels.add(graphs);
			
			// put the nodes in corresponding graphs
			for(Map.Entry<Node, Node> memberShip : level.entrySet())
			{
				if(!graphs.containsKey(memberShip.getValue()))
					// new parent node -> new subordinate graph
					// TODO log name: unitName + ":" + belongsTo.getValue(), unitName));
					graphs.put(memberShip.getValue(), new SimpleGraph());
				graphs.get(memberShip.getValue()).addNode(memberShip.getKey());
			}
			// put in-graph edges in the graphs
			for(Graph graph : graphs.values())
				for(Edge edge : theGraph.getEdges())
					if(graph.contains(edge.getTo()) && graph.contains(edge.getFrom()))
						graph.addEdge(edge);
		}
		
	}
}
