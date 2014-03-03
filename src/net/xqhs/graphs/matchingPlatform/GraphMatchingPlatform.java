package net.xqhs.graphs.matchingPlatform;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.matcher.GraphMatchingProcess;
import net.xqhs.graphs.matcher.Match;
import net.xqhs.graphs.matchingPlatform.TrackingGraph.Operation;
import net.xqhs.graphs.pattern.GraphPattern;

/**
 * Implementations of this interface serve as platforms that handle on-demand matching of various graphs and patterns
 * against a designated, 'principal' graph.
 * 
 * @author Andrei Olaru
 */
public interface GraphMatchingPlatform
{
	public interface PlatformPrincipalGraph extends Graph
	{
		/**
		 * Creates a new shadow graph of this graph, based on the current state of the graph.
		 * 
		 * @return the newly created shadow graph.
		 */
		public PlatformShadowGraph createShadowGraph();
		
		/**
		 * @return the current sequence number.
		 */
		public int getSequence();
	}
	
	public interface PlatformShadowGraph extends Graph
	{
		/**
		 * @return the current sequence number.
		 */
		public int getSequence();
		
		/**
		 * Retrieves the operations that will be applied at the next sequence increment.
		 * 
		 * @return the operations to be performed, or <code>null</code> if the shadow is synchronized with its master.
		 */
		public Map<GraphComponent, Operation> getNextSequenceOperations();
		
		/**
		 * @return <code>true</code> if there are transactions in the transaction queue that can be applied to the
		 *         current state of the graph.
		 * 
		 * @throws IllegalStateException
		 *             if the method is called on a graph that is not a shadow graph.
		 */
		public boolean canIncrement();
		
		/**
		 * Takes one transaction from the graph's transaction queue and applies it to the current state of the graph.
		 * 
		 * @return the new current sequence number.
		 * 
		 * @throws IllegalStateException
		 *             if the method is called on a graph that is not a shadow graph.
		 */
		public int incrementSequence();
		
		/**
		 * Takes several transactions from the graph's transaction queue and applies them to the current state of the
		 * graph, in order to reach the target sequence number.
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
		public int incrementSequence(int targetSequence);
		
		/**
		 * Brings the graph up to date with its master graph, transaction by transaction.
		 * 
		 * @return the new current sequence number.
		 * 
		 * @throws IllegalStateException
		 *             if the method is called on a graph that is not a shadow graph.
		 */
		public int incrementSequenceFastForward();
	}
	
	public GraphMatchingPlatform setPrincipalGraph(PlatformPrincipalGraph graph);
	
	public PlatformPrincipalGraph getPrincipalGraph();
	
	public GraphMatchingPlatform addPattern(GraphPattern pattern);
	
	public GraphMatchingPlatform removePattern(GraphPattern pattern);
	
	public Collection<GraphPattern> getPatterns();
	
	public Set<Match> incrementSequence();
	
	public List<Entry<Integer, Set<Match>>> incrementSequence(int targetSequence);
	
	public List<Entry<Integer, Set<Match>>> incrementSequenceFastForward();
	
	public int getMathingSequence();
	
	public int getGraphSequence();
	
	public GraphMatchingProcess getMatcherAgainstGraph(GraphPattern pattern);
	
}
