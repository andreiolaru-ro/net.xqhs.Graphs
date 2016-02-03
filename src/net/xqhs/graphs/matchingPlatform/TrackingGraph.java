package net.xqhs.graphs.matchingPlatform;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.graph.GraphDescription;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.matchingPlatform.Transaction.Operation;
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
 * <p>
 * For extending classes, all changes to the graph are made through {@link #performOperation} (to which all calls to
 * add, addAll, remove, removeAll are redirected). Therefore extending classes only need to handle changes to the graph
 * through {@link #performOperation}. The third argument controls whether the operation will also be added as a
 * transaction or not.
 *
 * @author Andrei Olaru
 */
public class TrackingGraph extends SimpleGraph
{
	/**
	 * Interface to be implemented by any class that can be notified of changes to the graph.
	 *
	 * @author Andrei Olaru
	 */
	public interface ChangeNotificationReceiver
	{
		/**
		 * Method that will be called when a change to the graph is recorded. More precisely, when a transaction has
		 * been applied.
		 */
		public void notifyChange();
	}

	/**
	 * The current sequence of the graph. The sequence is incremented after each transaction. For shadow graphs, it
	 * should mirror the master's sequence, but it is not the same instance.
	 */
	protected AtomicInteger						sequence				= new AtomicInteger(0);

	/**
	 * <code>true</code> if the graph is a shadow graph, <code>false</code> if it is not the shadow of any other graph.
	 */
	protected boolean							isShadow;

	/**
	 * Only for shadow graphs, the {@link Queue} of transactions to perform. The queue is created by the master graph.
	 */
	protected Queue<Transaction>				transactionQueue;

	/**
	 * The set of transaction queues for the shadow graphs of this graph. There is one for each shadow graph.
	 */
	protected List<Queue<Transaction>>			shadowQueues			= null;

	/**
	 * The set of entities that must receive notifications when transactions are applied to the graph.
	 */
	protected Set<ChangeNotificationReceiver>	notificationReceivers	= null;

	/**
	 * <code>true</code> if a history should be kept of all performed transactions.
	 */
	protected boolean							keepHistory				= false;

	/**
	 * If required by {@link #keepHistory}, the history of all transactions.
	 */
	protected List<Transaction>					history					= null;

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
	protected TrackingGraph(Queue<Transaction> transactionsLink, int initialSequence, Graph initialGraph)
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
		Queue<Transaction> newQueue = new LinkedList<Transaction>();
		shadowQueues.add(newQueue);
		return newQueue;
	}
	
	/**
	 * Enables an outside entity to both access a shadow queue and receive notifications when changes are performed.
	 *
	 * @param receiver
	 *            -- the receiver for the notifications
	 * @return a newly created shadow queue.
	 */
	public Queue<Transaction> getShadowAndNotifications(ChangeNotificationReceiver receiver)
	{
		if(receiver != null)
			registerChangeNotificationReceiver(receiver);
		return createShadowQueue();
	}

	/**
	 * The method registers a new receiver for change notifications.
	 *
	 * @param receiver
	 *            - the receiver to notify.
	 */
	public void registerChangeNotificationReceiver(ChangeNotificationReceiver receiver)
	{
		if(notificationReceivers == null)
			notificationReceivers = new HashSet<ChangeNotificationReceiver>();
		notificationReceivers.add(receiver);
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
			history = new ArrayList<Transaction>();
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
	 *            - <code>true</code> if the method is called by an add or remove method, and a transaction should be
	 *            added for the operation. <code>false</code> if this call is the result of applying a transaction.
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
		if(notificationReceivers != null)
			for(ChangeNotificationReceiver receiver : notificationReceivers)
				receiver.notifyChange();
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
	 * FIXME: if there are no elements in the queue, the sequence is not incremented. should check for
	 * desynchronization; is it possible?
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
	 * be added with {@link #addAll(Collection)}. The description can be added with
	 * {@link #setDescription(GraphDescription)}.
	 */
	@Override
	public SimpleGraph readFrom(InputStream input)
	{
		throw new UnsupportedOperationException("Reading graphs is not supported. Use method addAll().");
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
		return detail + new TextGraphRepresentation(this).setLayout(branchSeparator, separatorIncrement, limit).update()
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
