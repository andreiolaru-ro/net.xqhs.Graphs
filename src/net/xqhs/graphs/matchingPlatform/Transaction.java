package net.xqhs.graphs.matchingPlatform;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.matchingPlatform.Transaction.Operation;

/**
 * The class contains information on one or more changes (operations) to apply to a graph. Each change is a pair of a
 * {@link GraphComponent} instance and one of {@link Operation}. The changes are not ordered in any guaranteed manner.
 * <p>
 * The implementation is optimized so that single-operation transactions are represented in a more simple manner. The
 * conversion between single- and multi-operation transactions is done transparently.
 * <p>
 * The class implements most operations of {@link Map}, but some are available only if the transaction is
 * multi-operation.
 *
 * @author Andrei Olaru
 */
public class Transaction implements Map<GraphComponent, Operation>
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
		multipleOperations = new HashMap<GraphComponent, Operation>();
		if(singleOperation)
			multipleOperations.put(singleOperationComponent, singleOperationOperation);
		singleOperation = false;
		singleOperationComponent = null;
		singleOperationOperation = null;
	}
	
	/**
	 * Adds a new operation to the transaction.
	 * <p>
	 * If the transaction was empty, it is now single-operation. If it was single-operation, it is now multi-operation.
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
		put(component, operation);
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