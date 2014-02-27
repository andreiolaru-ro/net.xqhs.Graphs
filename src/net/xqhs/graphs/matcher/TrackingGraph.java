package net.xqhs.graphs.matcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.graph.SimpleGraph;

public class TrackingGraph extends SimpleGraph
{
	enum Operation {
		ADD, REMOVE, CHANGE,
	}
	
	class Transaction extends HashMap<GraphComponent, Operation>
	{
		// nothing to extends
	}
	
	AtomicInteger					sequence;
	
	protected Queue<Transaction>	transactionQueue;
	
	protected boolean				isShadow;
	
	protected boolean				keepHistory	= false;
	
	protected List<Transaction>		history		= null;
	
	public TrackingGraph()
	{
		this(false, null);
	}
	
	public TrackingGraph(boolean shadowGraph, Queue<Transaction> transactionsLink)
	{
		super();
		if(shadowGraph && (transactionsLink == null))
			throw new IllegalArgumentException("A shadow graph must be linked to an existing transaction queue");
		isShadow = shadowGraph;
		if(transactionsLink != null)
			transactionQueue = transactionsLink;
		else
			transactionQueue = new LinkedList<TrackingGraph.Transaction>();
	}
	
	public TrackingGraph keepHistory(boolean keep, boolean clearHistory)
	{
		if(clearHistory)
			// history should be cleared
			history.clear();
		if(keep && (history == null))
			// history was not kept before
			history = new ArrayList<TrackingGraph.Transaction>();
		keepHistory = keep;
		return this;
	}
	
	@Override
	public SimpleGraph addEdge(Edge edge)
	{
		// TODO Auto-generated method stub
		return super.addEdge(edge);
	}
	
}
