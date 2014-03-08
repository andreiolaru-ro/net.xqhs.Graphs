package net.xqhs.graphs.matchingPlatform;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.representation.text.TextGraphRepresentation;

/**
 * The class implements a graph that applies changes incrementally, allowing their controlled propagation. Changes are
 * represented as {@link Transaction} instances, representing a single or multiple changes per transaction.
 * <p>
 * There are two uses of the class -- as 'master' and 'shadow':
 * <ul>
 * <li>'Master' instances log every change in queues that are then processed on command by 'shadow' instances.
 * <li>'Shadow' instances process, on command, changes added to their transaction queues by the master instance. Shadow
 * instances can only be constructed by their master, with a call to {@link #createShadow()}.
 * </ul>
 * An instance that is the master of a set of shadows can as well be a shadow itself.
 * <p>
 * A shadow graph can never be modified directly, only by taking transactions from its master's transaction queue.
 * 
 * @author Andrei Olaru
 */
public class TrackingGraph extends SimpleGraph
{
	/**
	 * Types of operations on graphs available.
	 * 
	 * @author Andrei Olaru
	 */
	public static enum Operation {
		/**
		 * Addition of a {@link GraphComponent} to the graph.
		 */
		ADD,
		
		/**
		 * Removal of a {@link GraphComponent} from the graph.
		 */
		REMOVE,
		
		// TODO
		// CHANGE,
	}
	
	/**
	 * The class contains information on one or more changes (operations) to apply to a graph. Each change is a pair of
	 * a {@link GraphComponent} instance and one of {@link Operation}. The changes are not ordered in any guaranteed
	 * manner.
	 * <p>
	 * The implementation is optimized so that single-operation transactions are represented in a more simple manner.
	 * The conversion between single- and multi-operation transactions is done transparently.
	 * <p>
	 * The class implements most operations of {@link Map}, but some are available only if the transaction is
	 * multi-operation.
	 * 
	 * @author Andrei Olaru
	 */
	public static class Transaction implements Map<GraphComponent, Operation>
	{
		/**
		 * <code>true</code> if and inly if it is single-operation (is not true for empty transactions).
		 */
		boolean							singleOperation				= false;
		/**
		 * <code>true</code> if the transaction is empty (no operation contained).
		 */
		boolean							empty						= true;
		/**
		 * For single-operation transactions, the component contained in the operation.
		 */
		GraphComponent					singleOperationComponent	= null;
		/**
		 * For single-operation transactions, the operation to perform.
		 */
		Operation						singleOperationOperation	= null;
		/**
		 * For multi-operation transactions, the map of operations.
		 */
		Map<GraphComponent, Operation>	multipleOperations			= null;
		
		/**
		 * Creates a single-operation transaction.
		 * 
		 * @param component
		 *            - the component contained in the operation.
		 * @param operation
		 *            - the operation to perform.
		 */
		public Transaction(GraphComponent component, Operation operation)
		{
			empty = false;
			singleOperation = true;
			singleOperationComponent = component;
			singleOperationOperation = operation;
		}
		
		/**
		 * Creates an empty transaction.
		 */
		public Transaction()
		{
		}
		
		/**
		 * For single-operation transactions only, retrieves the component contained in the operation.
		 * 
		 * @return the component.
		 * 
		 * @throws UnsupportedOperationException
		 *             if the method is called for an empty or multi-operation transaction.
		 */
		public GraphComponent getComponent()
		{
			if(!singleOperation)
				throw new UnsupportedOperationException("Transaction contains multiple operations");
			return singleOperationComponent;
		}
		
		/**
		 * For single-operation transactions only, retrieves the operation to perform.
		 * 
		 * @return the operation.
		 * 
		 * @throws UnsupportedOperationException
		 *             if the method is called for an empty or multi-operation transaction.
		 */
		public Operation getOperation()
		{
			if(!singleOperation)
				throw new UnsupportedOperationException("Transaction contains multiple operations");
			return singleOperationOperation;
		}
		
		/**
		 * @return <code>true</code> if the transaction contains exactly one operation.
		 */
		public boolean isSingleOperation()
		{
			return singleOperation;
		}
		
		/**
		 * Compacts a multi-operation transaction in case it is in fact empty or single-operation.
		 */
		protected void compact()
		{
			if(empty || singleOperation || (multipleOperations == null))
				throw new IllegalStateException("Should not have called compact in this state");
			empty = multipleOperations.isEmpty();
			singleOperation = (multipleOperations.size() == 1);
			if(empty)
				multipleOperations = null;
			if(singleOperation)
			{
				Entry<GraphComponent, Operation> e = multipleOperations.entrySet().iterator().next();
				singleOperationComponent = e.getKey();
				singleOperationOperation = e.getValue();
				multipleOperations = null;
			}
		}
		
		/**
		 * Converts an empty or single-operation transaction into a multi-operation transaction.
		 */
		protected void toMultipleOperations()
		{
			if(!(singleOperation || empty))
				return;
			empty = false;
			multipleOperations = new HashMap<GraphComponent, TrackingGraph.Operation>();
			if(singleOperation)
				multipleOperations.put(singleOperationComponent, singleOperationOperation);
			singleOperation = false;
			singleOperationComponent = null;
			singleOperationOperation = null;
		}
		
		/**
		 * Adds a new operation to the transaction.
		 * <p>
		 * If the transaction was empty, it is now single-operation. If it was single-operation, it is now
		 * multi-operation.
		 */
		@Override
		public Operation put(GraphComponent component, Operation operation)
		{
			if(empty)
			{
				empty = false;
				singleOperation = true;
				singleOperationComponent = component;
				singleOperationOperation = operation;
				return singleOperationOperation;
			}
			if(singleOperation)
			{
				if(component == singleOperationComponent)
				{ // replace existing operation
					Operation ret = singleOperationOperation;
					singleOperationOperation = operation;
					return ret;
				}
				// otherwise, convert to multiple operation
				toMultipleOperations();
			}
			return multipleOperations.put(component, operation);
		}
		
		/**
		 * The method is identical to {@link #put(GraphComponent, Operation)}, with the exception that it returns the
		 * instance itself.
		 * 
		 * @param component
		 *            - the component contained in the operation.
		 * @param operation
		 *            - the operation to perform.
		 * @return the instance itself.
		 */
		public Transaction putR(GraphComponent component, Operation operation)
		{
			multipleOperations.put(component, operation);
			return this;
		}
		
		@Override
		public void putAll(Map<? extends GraphComponent, ? extends Operation> operations)
		{
			toMultipleOperations();
			multipleOperations.putAll(operations);
			compact();
		}
		
		@Override
		public Operation get(Object component)
		{
			if(empty)
				return null;
			if(singleOperation)
			{
				if(component == singleOperationComponent)
					return singleOperationOperation;
				return null;
			}
			return multipleOperations.get(component);
		}
		
		@Override
		public boolean containsKey(Object key)
		{
			if(empty)
				return false;
			return singleOperation ? (singleOperationComponent == key) : multipleOperations.containsKey(key);
		}
		
		/**
		 * The method is unsupported.
		 */
		@Override
		public boolean containsValue(Object value)
		{
			throw new UnsupportedOperationException("Operation is unsupported");
		}
		
		/**
		 * The method is unsupported.
		 */
		@Override
		public Collection<Operation> values()
		{
			throw new UnsupportedOperationException("Operation is unsupported");
		}
		
		/**
		 * Resets the transaction to an empty transaction.
		 */
		@Override
		public void clear()
		{
			empty = true;
			singleOperation = false;
			singleOperationComponent = null;
			singleOperationOperation = null;
			multipleOperations.clear();
			multipleOperations = null;
		}
		
		@Override
		public boolean isEmpty()
		{
			return empty;
		}
		
		@Override
		public int size()
		{
			return empty ? 0 : (singleOperation ? 1 : multipleOperations.size());
		}
		
		/**
		 * Unsupported operation for empty or single-operation transactions.
		 */
		@Override
		public Set<Entry<GraphComponent, Operation>> entrySet()
		{
			if(empty || singleOperation)
				throw new UnsupportedOperationException("Cannot iterate over empty or single-operation transactions.");
			return multipleOperations.entrySet();
		}
		
		/**
		 * Unsupported operation for empty or single-operation transactions.
		 */
		@Override
		public Set<GraphComponent> keySet()
		{
			if(empty || singleOperation)
				throw new UnsupportedOperationException("Cannot iterate over empty or single-operation transactions.");
			return multipleOperations.keySet();
		}
		
		@Override
		public Operation remove(Object component)
		{
			if(empty)
				return null;
			if(singleOperation)
			{
				if(component == singleOperationComponent)
				{
					Operation ret = singleOperationOperation;
					singleOperationComponent = null;
					singleOperationOperation = null;
					empty = true;
					singleOperation = false;
					return ret;
				}
				return null;
			}
			Operation ret = multipleOperations.remove(component);
			compact();
			return ret;
		}
		
		@Override
		public String toString()
		{
			if(empty)
				return "[-]";
			if(singleOperation)
				return singleOperationComponent + ": " + singleOperationOperation.toString();
			return multipleOperations.toString();
		}
		
		/**
		 * Retrieves the operations in this transaction as a (copy) {@link Map} of {@link GraphComponent} &rarr;
		 * {@link Operation}.
		 * 
		 * @return the operations.
		 */
		public Map<GraphComponent, Operation> toOperationMap()
		{
			toMultipleOperations();
			Map<GraphComponent, Operation> ret = new HashMap<GraphComponent, Operation>(multipleOperations);
			compact();
			return ret;
		}
	}
	
	/**
	 * The current sequence of the graph. The sequence is incremented after each transaction. For shadow graphs, it
	 * should mirror the master's sequence, but it is not the same instance.
	 */
	protected AtomicInteger				sequence		= new AtomicInteger(0);
	
	/**
	 * <code>true</code> if the graph is a shadow graph, <code>false</code> if it is not the shadow of any other graph.
	 */
	protected boolean					isShadow;
	
	/**
	 * Only for shadow graphs, the {@link Queue} of transactions to perform. The queue is created by the master graph.
	 */
	protected Queue<Transaction>		transactionQueue;
	
	/**
	 * The set of transaction queues for the shadow graphs of this graph. There is one for each shadow graph.
	 */
	protected List<Queue<Transaction>>	shadowQueues	= null;
	
	/**
	 * <code>true</code> if a history should be kept of all performed transactions.
	 */
	protected boolean					keepHistory		= false;
	
	/**
	 * If required by {@link #keepHistory}, the history of all transactions.
	 */
	protected List<Transaction>			history			= null;
	
	/**
	 * Creates a new graph that is not the shadow of any other graph.
	 */
	public TrackingGraph()
	{
		super();
		isShadow = false;
	}
	
	/**
	 * Creates a shadow graph, based on a transactions queue and, optionally, an initial sequence and an initial graph.
	 * 
	 * @param transactionsLink
	 *            - the queue of transactions to apply to this graph.
	 * @param initialSequence
	 *            - the initial sequence number.
	 * @param initialGraph
	 *            - the graph containing the nodes and edges to add initially to this graph. Although the node and edge
	 *            instances will be the same, there will exist no other relation to the specified graph.
	 */
	public TrackingGraph(Queue<Transaction> transactionsLink, int initialSequence, Graph initialGraph)
	{
		super();
		if(transactionsLink == null)
			throw new IllegalArgumentException("A shadow graph must be linked to an existing transaction queue");
		addAll(initialGraph.getComponents());
		sequence = new AtomicInteger(initialSequence);
		transactionQueue = transactionsLink;
		isShadow = true;
	}
	
	/**
	 * @return <code>true</code> if the graph is the shadow of another graph.
	 */
	public boolean isShadow()
	{
		return isShadow;
	}
	
	/**
	 * Creates a new shadow graph of this graph, based on the current state of the graph.
	 * <p>
	 * While overriding this method may not be useful because a {@link TrackingGraph} is returned, extending classes may
	 * use <code>createShadowQueue()</code> to get the shadow queue and create the graph themselves.
	 * 
	 * @return the newly created shadow graph.
	 */
	public TrackingGraph createShadow()
	{
		return new TrackingGraph(createShadowQueue(), sequence.get(), this);
	}
	
	/**
	 * Creates a new shadow queue to be used by a shadow graph. The queue is also added to the list of shadow queues.
	 * 
	 * @return the queue.
	 */
	protected Queue<Transaction> createShadowQueue()
	{
		if(shadowQueues == null)
			shadowQueues = new ArrayList<Queue<Transaction>>();
		Queue<Transaction> newQueue = new LinkedList<TrackingGraph.Transaction>();
		shadowQueues.add(newQueue);
		return newQueue;
	}
	
	/**
	 * Sets history keeping. Clearing the history is controlled by the second parameter and is independent of the value
	 * of the first.
	 * <p>
	 * Turning off history keeping without clearing the history keeps the history so it can be continued with a new call
	 * instructing to keep the history without clearing it.
	 * 
	 * @param keep
	 *            - <code>true</code> if history should be kept as of this call; <code>false</code> if history keeping
	 *            should be halted.
	 * @param clearHistory
	 *            - <code>true</code> if the history should be cleared with this call.
	 * @return the graph itself.
	 */
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
	
	/**
	 * Internal method for performing one operation upon the current state of the graph.
	 * <p>
	 * If <code>externalCall</code> is <code>true</code>, the method also creates a new transaction with the operation
	 * and adds it to shadow queues and to the history. Otherwise, these operations are handled elsewhere.
	 * <p>
	 * This method should be overridden by any extending classes needing to do anything with the newly added components.
	 * If it is the case, the overriding method should also check if they should throw the exception.
	 * 
	 * @param component
	 *            - the component contained in the operation.
	 * @param operation
	 *            - the operation to perform.
	 * @param externalCall
	 *            - <code>true</code> if the method is called by an add or remove method, and the transaction should be
	 *            added.
	 * @return the graph itself.
	 * 
	 * @throws UnsupportedOperationException
	 *             if the operation is applied from the exterior, to a shadow graph.
	 */
	protected TrackingGraph performOperation(GraphComponent component, Operation operation, boolean externalCall)
	{
		if(isShadow && externalCall)
			throw new UnsupportedOperationException(
					"A shadow graph only takes modifications from its transaction queue.");
		if(externalCall)
			if(((operation == Operation.ADD) && !contains(component))
					|| ((operation == Operation.REMOVE) && contains(component)))
				addTransaction(new Transaction(component, operation));
		switch(operation)
		{
		case ADD:
			super.add(component);
			break;
		case REMOVE:
			super.remove(component);
			break;
		}
		return this;
	}
	
	/**
	 * Handles adding of new transactions to the history and to shadow graphs queues.
	 * 
	 * @param t
	 *            - the transaction to add.
	 */
	protected void addTransaction(Transaction t)
	{
		sequence.incrementAndGet();
		if(shadowQueues != null)
			for(Queue<Transaction> queue : shadowQueues)
				queue.add(t);
		if(keepHistory)
			history.add(t);
	}
	
	/**
	 * Public method allowing the application of an already created transaction to the graph.
	 * 
	 * @param t
	 *            - the transaction to apply.
	 * @return the graph itself.
	 * 
	 * @throws UnsupportedOperationException
	 *             if the method is called for a shadow graph.
	 */
	public TrackingGraph applyTransaction(Transaction t)
	{
		if(isShadow)
			throw new UnsupportedOperationException(
					"A shadow graph only takes modifications from its transaction queue.");
		applyTransactionInternal(t);
		return this;
	}
	
	/**
	 * Internal method for applying a transaction to the graph. The operation is performed and the transaction is added
	 * to the history and shadow graph queues.
	 * 
	 * @param t
	 *            - the transaction to apply.
	 */
	protected void applyTransactionInternal(Transaction t)
	{
		if(t.isEmpty())
			lw("Transaction is void.");
		else if(t.isSingleOperation())
			performOperation(t.getComponent(), t.getOperation(), false);
		else
			for(Entry<GraphComponent, Operation> e : t.entrySet())
				performOperation(e.getKey(), e.getValue(), false);
		addTransaction(t);
	}
	
	@Override
	public TrackingGraph add(GraphComponent component)
	{
		if(!contains(component))
			return performOperation(component, Operation.ADD, true);
		lw("component [] already present. Not re-added.", component);
		return this;
	}
	
	/**
	 * Adds all the nodes and edges in the argument to the current graph, all in one transaction.
	 * 
	 * @param components
	 *            - the {@link GraphComponent} instances to add.
	 * @return the graph itself.
	 */
	@Override
	public TrackingGraph addAll(Collection<? extends GraphComponent> components)
	{
		if(isShadow)
			throw new UnsupportedOperationException(
					"A shadow graph only takes modifications from its transaction queue.");
		Transaction t = new Transaction();
		for(GraphComponent comp : components)
			if(!contains(comp))
				t.put(comp, Operation.ADD);
			else
				lw("node [" + comp.toString() + "] already present. Not re-added.");
		if(!t.isEmpty())
			applyTransactionInternal(t);
		return this;
	}
	
	@Override
	public TrackingGraph remove(GraphComponent component)
	{
		if(contains(component))
			return performOperation(component, Operation.REMOVE, true);
		lw("component [] not contained", component);
		return this;
	}
	
	/**
	 * Removes all the nodes and edges in the argument from the current graph, all in one transaction.
	 * 
	 * @param components
	 *            - the {@link GraphComponent} instances to remove.
	 * @return the graph itself.
	 */
	@Override
	public TrackingGraph removeAll(Collection<? extends GraphComponent> components)
	{
		if(isShadow)
			throw new UnsupportedOperationException(
					"A shadow graph only takes modifications from its transaction queue.");
		Transaction t = new Transaction();
		for(GraphComponent comp : components)
			if(contains(comp))
				t.put(comp, Operation.REMOVE);
			else
				lw("node [" + comp.toString() + "] not present.");
		if(!t.isEmpty())
			applyTransactionInternal(t);
		return this;
	}
	
	/**
	 * @return the current sequence number.
	 */
	public int getSequence()
	{
		return sequence.get();
	}
	
	/**
	 * @return <code>true</code> if there are transactions in the transaction queue that can be applied to the current
	 *         state of the graph.
	 * 
	 * @throws IllegalStateException
	 *             if the method is called on a graph that is not a shadow graph.
	 */
	public boolean canIncrement()
	{
		if(!isShadow)
			throw new IllegalStateException("Non-shadow graphs do not support this operation");
		return !transactionQueue.isEmpty();
	}
	
	/**
	 * Retrieves the operations that will be applied at the next sequence increment.
	 * 
	 * @return the operations to be performed, or <code>null</code> if the shadow is synchronized with its master.
	 */
	public Map<GraphComponent, Operation> getNextSequenceOperations()
	{
		if(!isShadow)
			throw new IllegalStateException("Non-shadow graphs do not support this operation");
		if(transactionQueue.isEmpty())
			// nowhere to increment
			return null;
		return transactionQueue.peek().toOperationMap();
	}
	
	/**
	 * Internal method that applies one transaction to the graph.
	 */
	protected void incrementSequenceInternal()
	{
		if(!isShadow || transactionQueue.isEmpty())
			throw new IllegalStateException("Illegal state reached.");
		applyTransactionInternal(transactionQueue.poll());
	}
	
	/**
	 * Takes one transaction from the graph's transaction queue and applies it to the current state of the graph.
	 * <p>
	 * FIXME: if there are no elements in the queue, the sequence is not incremented. should check for dsynchronization;
	 * is it possible?
	 * 
	 * @return the new current sequence number.
	 * 
	 * @throws IllegalStateException
	 *             if the method is called on a graph that is not a shadow graph.
	 */
	public int incrementSequence()
	{
		if(!isShadow)
			throw new IllegalStateException("Non-shadow graphs do not support this operation");
		if(transactionQueue.isEmpty())
			// nowhere to increment
			return -1;
		incrementSequenceInternal();
		return sequence.get();
	}
	
	/**
	 * Takes several transactions from the graph's transaction queue and applies them to the current state of the graph,
	 * in order to reach the target sequence number.
	 * 
	 * @param targetSequence
	 *            - the sequence number to reach before stopping.
	 * 
	 * @return the new current sequence number. If there are enough transactions in the queue, it is equal to the
	 *         <code>targetSequence</code>.
	 * 
	 * @throws IllegalStateException
	 *             if the method is called on a graph that is not a shadow graph.
	 */
	public int incrementSequence(int targetSequence)
	{
		if(!isShadow)
			throw new IllegalStateException("Non-shadow graphs do not support this operation");
		while(!transactionQueue.isEmpty() && (sequence.get() < targetSequence))
			incrementSequenceInternal();
		if(sequence.get() < targetSequence)
			lw("Target sequence not reached.");
		return sequence.get();
	}
	
	/**
	 * Brings the graph up to date with its master graph, transaction by transaction.
	 * 
	 * @return the new current sequence number.
	 * 
	 * @throws IllegalStateException
	 *             if the method is called on a graph that is not a shadow graph.
	 */
	public int incrementSequenceFastForward()
	{
		if(!isShadow)
			throw new IllegalStateException("Non-shadow graphs do not support this operation");
		while(!transactionQueue.isEmpty())
			incrementSequenceInternal();
		return sequence.get();
	}
	
	/**
	 * The current implementation does not support reading nodes and edges, but all the edges and nodes from a graph can
	 * be added with {@link #addAll(Collection)}.
	 */
	@Override
	public SimpleGraph readFrom(InputStream input)
	{
		throw new UnsupportedOperationException("Reading graphs is not supported. Use method addGraph(Graph).");
	}
	
	/**
	 * The method returns a string representation of the graph as rendered by {@link TextGraphRepresentation} with
	 * default layout parameters.
	 * <p>
	 * A customized representation can be obtained by calling {@link #toString(String, String, int)}.
	 * <p>
	 * The basic string representation of the graph can be obtained by calling {@link #toStringBasic()}.
	 */
	@Override
	public String toString()
	{
		return toString(TextGraphRepresentation.DEFAULT_BRANCH_SEPARATOR,
				TextGraphRepresentation.DEFAULT_SEPARATOR_INCREMENT, TextGraphRepresentation.DEFAULT_INCREMENT_LIMIT);
	}
	
	/**
	 * The method returns a string representation of the graph as rendered by {@link TextGraphRepresentation} with the
	 * specified parameters.
	 * 
	 * @param branchSeparator
	 *            - see {@link TextGraphRepresentation#setLayout(String, String, int)}.
	 * @param separatorIncrement
	 *            - see {@link TextGraphRepresentation#setLayout(String, String, int)}.
	 * @param limit
	 *            - see {@link TextGraphRepresentation#setLayout(String, String, int)}.
	 * @return the string representation.
	 */
	public String toString(String branchSeparator, String separatorIncrement, int limit)
	{
		String detail = "[" + sequence + "|";
		boolean first = true;
		if(shadowQueues != null)
			for(Queue<Transaction> q : shadowQueues)
			{
				detail += (first ? "" : "/") + q.size();
				first = false;
			}
		else
			detail += "-";
		detail += "]";
		return detail
				+ new TextGraphRepresentation(this).setLayout(branchSeparator, separatorIncrement, limit).update()
						.toString();
	}
	
	/**
	 * Returns a basic string representation of the graph, as rendered by {@link SimpleGraph#toString()}.
	 * 
	 * @return the string representation.
	 */
	public String toStringBasic()
	{
		return super.toString();
	}
	
}
