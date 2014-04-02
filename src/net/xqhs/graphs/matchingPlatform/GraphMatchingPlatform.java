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
import net.xqhs.graphs.matchingPlatform.TrackingGraph.Transaction;
import net.xqhs.graphs.pattern.GraphPattern;

/**
 * Implementations of this interface serve as platforms that handle on-demand matching of various graphs and patterns
 * against a designated, 'principal' graph.
 * <p>
 * In order to not lose matches in case the graph changes too quickly, the matching works on a 'shadow' of the principal
 * graph. Individual (or, if requested, sets of multiple) changes to the principal graph are recorded in individual
 * {@link Transaction} instances (see {@link TrackingGraph}). At each call of {@link #incrementSequence()} (or similar
 * methods) the shadow graph applies the next transactions and gets closer to the current state of the principal graph.
 * In this way, if an edge is added to the graph and then removed, at subsequent sequence increments, the list of
 * matches will first not contain the matches of the edge, then it will, and then it will not.
 * 
 * @author Andrei Olaru
 */
public interface GraphMatchingPlatform
{
	/**
	 * Interface for graphs that are principal graphs of a platform.
	 * 
	 * @author Andrei Olaru
	 */
	public interface PlatformPrincipalGraph extends Graph
	{
		/**
		 * Creates a new shadow graph of this graph, based on the current state of the graph.
		 * 
		 * @return the newly created shadow graph.
		 */
		public PlatformShadowGraph createShadowGraph();
		
		/**
		 * @return the current sequence number of the graph.
		 */
		public int getSequence();
	}
	
	/**
	 * Interface for graphs that are shadows of a master graph (most likely the platform principal graph).
	 * 
	 * @author Andrei Olaru
	 */
	public interface PlatformShadowGraph extends Graph
	{
		/**
		 * @return the current sequence number of the shadow graph.
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
	
	/**
	 * Sets the principal graph of the platform.
	 * <p>
	 * If another graph was previously set, all old data is cleared and patterns are re-added to match the new graph.
	 * 
	 * @param graph
	 *            - the graph, as a {@link PlatformPrincipalGraph}.
	 * @return the platform itself.
	 */
	public GraphMatchingPlatform setPrincipalGraph(PlatformPrincipalGraph graph);
	
	/**
	 * Retrieves the principal graph of the platform.
	 * 
	 * @return the graph.
	 */
	public PlatformPrincipalGraph getPrincipalGraph();
	
	/**
	 * Adds a new pattern to the platform. Further match retrievals will also contain matches between this new pattern
	 * and the principal graph.
	 * 
	 * @param pattern
	 *            - the {@link GraphPattern} to add.
	 * @return the platform itself.
	 */
	public GraphMatchingPlatform addPattern(GraphPattern pattern);
	
	/**
	 * Removes a pattern from the platform. All data related to the pattern is removed.
	 * 
	 * @param pattern
	 *            - the {@link GraphPattern} to remove.
	 * @return the platform itself.
	 */
	public GraphMatchingPlatform removePattern(GraphPattern pattern);
	
	/**
	 * @return a {@link Collection} of existing patterns in the platform. May be an unmodifiable or immutable list.
	 */
	public Collection<GraphPattern> getPatterns();
	
	/**
	 * Builds and returns the full matches between the patterns and the principal graph with the next set of operations
	 * (transaction) applied, moving the matching sequence one step closer to the graph sequence.
	 * <p>
	 * If the matching process was already synchronized with the principal graph, <code>null</code> is returned.
	 * 
	 * @return full matches for the newly incremented sequence, or <code>null</code> if the sequence cannot be
	 *         incremented.
	 */
	public Set<Match> incrementSequence();
	
	/**
	 * Applies {@link #incrementSequence()} until the matching process (matching sequence) reaches desired sequence of
	 * the principal graph. For each incremented sequence, the corresponding complete list of full matches is returned.
	 * <p>
	 * If the matching process was already synchronized with the principal graph, an empty list is returned.
	 * 
	 * @param targetSequence
	 *            - the desired sequence to reach.
	 * @return a list of pairs with the sequence as key and the list of full matches existing in that sequence, as
	 *         value; the empty list if there is nowhere to increment.
	 */
	public List<Entry<Integer, Set<Match>>> incrementSequence(int targetSequence);
	
	/**
	 * Applies {@link #incrementSequence()} until the matching process is synchronized with the principal graph.
	 * <p>
	 * If the matching process was already synchronized with the principal graph, an empty list is returned.
	 * 
	 * @return a list of pairs with the sequence as key and the list of full matches existing in that sequence, as
	 *         value; the empty list if there is nowhere to increment.
	 */
	public List<Entry<Integer, Set<Match>>> incrementSequenceFastForward();
	
	/**
	 * Builds and returns the matches between the specified pattern and the principal graph (as it is in the current
	 * matching sequence). Only the matches with <i>k</i> between 0 and <code>maxK</code> (inclusive) are returned.
	 * 
	 * @param pattern
	 *            - the pattern for which to get the matches.
	 * @param maxK
	 *            - the maximum k for retrieved matches.
	 * @return the set of matches between the pattern and the current matching sequence of the graph.
	 * 
	 * @throws IllegalArgumentException
	 *             if the pattern has not been added to the platform.
	 */
	public Set<Match> getMatches(GraphPattern pattern, int maxK);
	
	/**
	 * Retrieves the current 'matching sequence', which is incremented by {@link #incrementSequence()}.
	 * 
	 * @return the matching sequence.
	 */
	public int getMathingSequence();
	
	/**
	 * Retrieves the sequence of the last change to the principal graph.
	 * 
	 * @return the graph sequence.
	 */
	public int getGraphSequence();
	
	/**
	 * Retrieves a {@link GraphMatchingPlatform} for the specified pattern and the <i>current</i> sequence of the
	 * principal graph (not the matching sequence). Matching is done against a shadow of the principal graph. Any
	 * changes to the principal graph will not be visible in the matching process.
	 * <p>
	 * Except for the snapshot of the principal graph, this method uses no resources of the platform. The return
	 * matching process is independent of the platform's patterns and matching processes.
	 * 
	 * @param pattern
	 *            - the pattern to match.
	 * @return the matching process.
	 */
	public GraphMatchingProcess getMatcherAgainstGraph(GraphPattern pattern);
	
	// public void printindexes();
	
}
