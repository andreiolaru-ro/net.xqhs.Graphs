package net.xqhs.graphs.graph;

public class SimpleEdge extends GraphComponent implements Edge
{
	protected String	label	= null; // FIXME: support null labels throughout the source
	protected Node		from	= null;
	protected Node		to		= null;
	
	/**
	 * Constructs a new edge. WARNING: this also changes the from and to nodes, by adding the newly constructed edge to
	 * their respective out and in lists.
	 * 
	 * @param fromNode
	 *            : the from node; this edge is added to the node's outEdges list
	 * @param toNode
	 *            : the to node; this edge is added to the node's inEdges list
	 * @param edgeLabel
	 *            : the label of the edge
	 */
	public SimpleEdge(Node fromNode, Node toNode, String edgeLabel)
	{
		this.from = fromNode;
		this.to = toNode;
		this.label = edgeLabel;
		if(this.to instanceof ConnectedNode && this.to != null)
			((ConnectedNode)this.to).getInEdges().add(this);
		if(this.from instanceof ConnectedNode && this.from != null)
			((ConnectedNode)this.to).getOutEdges().add(this);
	}
	
	public String getLabel()
	{
		return label;
	}
	
	public Node getFrom()
	{
		return from;
	}
	
	public Node getTo()
	{
		return to;
	}
	
	@Override
	public String toString()
	{
		return from + toStringShort() + to;
	}
	
	public String toStringShort()
	{
		return toStringShort(false);
	}
	
	public String toStringShort(boolean isBackward)
	{
		return (isBackward ? "<" : "") + (this.label != null ? ("-" + this.label + "-") : "-")
				+ (isBackward ? "" : ">");
	}
	
}