package net.xqhs.graphs.graph;

/**
 * An instance of the class contains a description of the graph that is specific to the arrangement of the nodes and
 * edges and is not related to the nodes and edges themselves.
 * <p>
 * The most intuitive element of such a description is the name of the graph.
 * <p>
 * WARNING: To be extended in the future.
 * 
 * @author Andrei Olaru
 */
public class GraphDescription
{
	/**
	 * Description of the graph, as a string. For instance, this can be the name of the graph.
	 */
	String	descriptionString;
	
	/**
	 * Retrieves the name of the graph. It is identical to its description or, if the description is more complex, it is
	 * a part of the description.
	 * 
	 * @return the name of the graph.
	 */
	public String getName()
	{
		return descriptionString;
	}
	
	/**
	 * Sets the description of the graph.
	 * 
	 * @param description
	 *            - the description.
	 * @return the instance itself.
	 */
	public GraphDescription setDescription(String description)
	{
		descriptionString = description;
		return this;
	}
	
	@Override
	public String toString()
	{
		return descriptionString;
	}
}
