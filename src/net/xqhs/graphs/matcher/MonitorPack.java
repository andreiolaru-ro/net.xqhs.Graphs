package net.xqhs.graphs.matcher;

import java.util.concurrent.atomic.AtomicInteger;

import net.xqhs.util.logging.Debug.DebugItem;
import net.xqhs.util.logging.LoggerSimple;

/**
 * The class works in conjunction with graph matching classes (such as {@link GraphMatcherQuick}) to handle both
 * performance evaluation and visualizing tasks.
 * <p>
 * It offers various performance indicators that are easy to update.
 * <p>
 * It offers the logging methods specified by {@link LoggerSimple}.
 * <p>
 * It maintains a link with an instance of {@link MatchingVisualizer}.
 * 
 * @author Andrei Olaru
 */
public class MonitorPack implements LoggerSimple
{
	/**
	 * Matching visualizer to view the matching process.
	 */
	MatchingVisualizer	visual				= null;
	
	/**
	 * The log to use.
	 */
	LoggerSimple		log					= null;
	
	/**
	 * Measures performance of the algorithm in terms of compared nodes.
	 */
	AtomicInteger		performanceNodes	= new AtomicInteger();
	/**
	 * Measures performance of the algorithm in terms of compared edge labels and references.
	 */
	AtomicInteger		performanceEdges	= new AtomicInteger();
	
	/**
	 * Measures the total number of matches created.
	 */
	AtomicInteger		matchCount			= new AtomicInteger();
	
	/**
	 * Measures the total number of merges between matches.
	 */
	AtomicInteger		mergeCount			= new AtomicInteger();
	
	/**
	 * Sets the log to use by this instance. All logging messages posted to this instance will be posted to the log
	 * specified in the argument.
	 * 
	 * @param logger
	 *            - an instance implementing {@link LoggerSimple}.
	 * @return the instance itself.
	 */
	public MonitorPack setLog(LoggerSimple logger)
	{
		log = logger;
		return this;
	}
	
	/**
	 * Sets the {@link MatchingVisualizer} instance to use to visualize the matching process.
	 * 
	 * @param visualizer
	 *            - the {@link MatchingVisualizer} object to use.
	 * @return the instance itself.
	 */
	public MonitorPack setVisual(MatchingVisualizer visualizer)
	{
		visual = visualizer;
		return this;
	}
	
	/**
	 * @return the {@link MatchingVisualizer} instance in use.
	 */
	public MatchingVisualizer getVisual()
	{
		return visual;
	}
	
	/**
	 * @return the performanceNodes
	 */
	public AtomicInteger getPerformanceNodes()
	{
		return performanceNodes;
	}
	
	/**
	 * @return the performanceEdges
	 */
	public AtomicInteger getPerformanceEdges()
	{
		return performanceEdges;
	}
	
	/**
	 * @return the matchCount
	 */
	public AtomicInteger getMatchCount()
	{
		return matchCount;
	}
	
	/**
	 * @return the mergeCount
	 */
	public AtomicInteger getMergeCount()
	{
		return mergeCount;
	}
	
	/**
	 * Increments the performance indicator.
	 * 
	 * @return the current (updated) value.
	 */
	public int incrementEdgeLabelComparison()
	{
		return performanceEdges.incrementAndGet();
	}
	
	/**
	 * Increments the performance indicator with the specified value.
	 * 
	 * @param increment
	 *            the increment.
	 * @return the current (updated) value.
	 */
	public int incrementEdgeLabelComparison(int increment)
	{
		return performanceEdges.addAndGet(increment);
	}
	
	/**
	 * Increments the performance indicator.
	 * 
	 * @return the current (updated) value.
	 */
	public int incrementEdgeReferenceOperation()
	{
		return performanceEdges.incrementAndGet();
	}
	
	/**
	 * Increments the performance indicator with the specified value.
	 * 
	 * @param increment
	 *            the increment.
	 * @return the current (updated) value.
	 */
	public int incrementEdgeReferenceOperation(int increment)
	{
		return performanceEdges.addAndGet(increment);
	}
	
	/**
	 * Increments the performance indicator.
	 * 
	 * @return the current (updated) value.
	 */
	public int incrementNodeLabelComparison()
	{
		return performanceNodes.incrementAndGet();
	}
	
	/**
	 * Increments the performance indicator with the specified value.
	 * 
	 * @param increment
	 *            the increment.
	 * @return the current (updated) value.
	 */
	public int incrementNodeLabelComparison(int increment)
	{
		return performanceNodes.addAndGet(increment);
	}
	
	/**
	 * Increments the performance indicator.
	 * 
	 * @return the current (updated) value.
	 */
	public int incrementNodeReferenceOperation()
	{
		return performanceNodes.incrementAndGet();
	}
	
	/**
	 * Increments the performance indicator with the specified value.
	 * 
	 * @param increment
	 *            the increment.
	 * @return the current (updated) value.
	 */
	public int incrementNodeReferenceOperation(int increment)
	{
		return performanceNodes.addAndGet(increment);
	}
	
	/**
	 * Increments the performance indicator.
	 * 
	 * @return the current (updated) value.
	 */
	public int incrementMatchCount()
	{
		return matchCount.incrementAndGet();
	}
	
	/**
	 * Increments the performance indicator.
	 * 
	 * @return the current (updated) value.
	 */
	public int incrementMergeCount()
	{
		return mergeCount.incrementAndGet();
	}
	
	/**
	 * Prints a one-line view of the performance indicators, as a log line (if any log exists).
	 * 
	 * @return the same string that was printed.
	 */
	public String printStats()
	{
		String stats = "";
		stats += "nodes Ops|Labels: " + performanceNodes + "; edges Ops|Labels: " + performanceEdges + "; matches: "
				+ matchCount + "; merges: " + mergeCount;
		li(stats);
		return stats;
	}
	
	@Override
	public void le(String message, Object... arguments)
	{
		if(log != null)
			log.le(message, arguments);
	}
	
	@Override
	public void lw(String message, Object... arguments)
	{
		if(log != null)
			log.lw(message, arguments);
	}
	
	@Override
	public void li(String message, Object... arguments)
	{
		if(log != null)
			log.li(message, arguments);
	}
	
	@Override
	public void lf(String message, Object... arguments)
	{
		if(log != null)
			log.lf(message, arguments);
	}
	
	@Override
	public Object lr(Object ret)
	{
		if(log != null)
			return log.lr(ret);
		return ret;
	}
	
	@Override
	public Object lr(Object ret, String message, Object... arguments)
	{
		if(log != null)
			return log.lr(ret, message, arguments);
		return ret;
	}
	
	@Override
	public void dbg(DebugItem debug, String message, Object... arguments)
	{
		if(log != null)
			log.dbg(debug, message, arguments);
	}
}
