package net.xqhs.graphs.matcher;

import java.util.List;

/**
 * An implementation of this interface abstracts the matching process between a graph and a pattern (or two graph).
 * <p>
 * The graph and the pattern remain the same for the entire process. They are not expected to change over time, and if
 * they change, the process may need to be reinitialized by using {@link #clearData()}.
 * <p>
 * The interface offers methods to enable incremental matching, moving between matches of a specified <i>k</i> (number
 * of edges in the pattern missing from the matched part of the pattern). The <i>k</i> is specified by a call to
 * {@link #resetIterator(int)} and is 0 by default.
 * <p>
 * The implementation should act as an iterator, using {@link #getNextMatch()} to advance to the next match of <i>k</i>
 * equal to the threshold or lower.
 * <p>
 * The two additional methods that retrieve all matches reset the iterator as well.
 * <p>
 * It is recommended that implementations keep any information related to the matching process (such as intermediate
 * matches) that could help improve performance of future queries. Memory occupied by this information should be clear
 * if {@link #clearData()} is invoked.
 * 
 * @author Andrei Olaru
 */
public interface GraphMatchingProcess
{
	/**
	 * Resets the iterator. The next call to {@link #getNextMatch()} will return the first match conforming to the
	 * current <i>k</i> threshold.
	 * 
	 * @return the instance itself.
	 */
	public GraphMatchingProcess resetIterator();
	
	/**
	 * Resets the iterator (see {@link #resetIterator()}) and specifies a new <i>k</i> threshold.
	 * 
	 * @param k
	 *            - the new threshold.
	 * @return the instance itself.
	 */
	public GraphMatchingProcess resetIterator(int k);
	
	/**
	 * Clears all data related to the matching process.
	 * <p>
	 * The call to this method results in freeing all memory occupied by information related to the matching process
	 * that was retained.
	 * 
	 * @return the instance itself.
	 */
	public GraphMatchingProcess clearData();
	
	/**
	 * Searches for the next match with a <i>k</i> lower than or equal to the current threshold. The match returned is
	 * the first match that was not previously returned after the last call to {@link #resetIterator()}.
	 * <p>
	 * Depending on implementation, if no initialization has been done prior to this call (after constructing the
	 * instance or after a call to {@link #clearData()}), the first call to this method should also make all
	 * initializations in the matching process.
	 * <p>
	 * If intermediate matches are kept during the matching process, and {@link #clearData()} was not invoked, this
	 * method may return faster, if the desired match has already been generated partially or entirely.
	 * <p>
	 * As the matching process is a complex one, a call to this method is expected to be time consuming, depending on
	 * implementation and existing matching information.
	 * 
	 * @return the first match that was not previously returned after the last call to {@link #resetIterator()}.
	 */
	public Match getNextMatch();
	
	/**
	 * The method returns all matches with a <i>k</i> lower than or equal to the argument.
	 * <p>
	 * Performance of a call will be influenced by existing matching information.
	 * 
	 * @param k
	 *            - the threshold.
	 * @return a {@link List} of matches complying with the threshold.
	 */
	public List<Match> getAllMatches(int k);
	
	/**
	 * The method returns all complete matches.
	 * <p>
	 * Performance of a call will be influenced by existing matching information.
	 * 
	 * @return a {@link List} of complete matches of the pattern in the graph.
	 */
	public List<Match> getAllCompleteMatches();
}
