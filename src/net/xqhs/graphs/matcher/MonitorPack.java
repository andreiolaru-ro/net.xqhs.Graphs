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
	MatchingVisualizer	visual					= null;
	
	/**
	 * The log to use.
	 */
	LoggerSimple		log						= null;
	
	/**
	 * Measures performance of the algorithm in terms of compared node references.
	 */
	AtomicInteger		performanceNodes		= new AtomicInteger();
	/**
	 * Measures performance of the algorithm in terms of compared node labels.
	 */
	AtomicInteger		performanceNodesLabels	= new AtomicInteger();
	/**
	 * Measures performance of the algorithm in terms of compared edge references.
	 */
	AtomicInteger		performanceEdges		= new AtomicInteger();
	/**
	 * Measures performance of the algorithm in terms of compared edge labels.
	 */
	AtomicInteger		performanceEdgesLabels	= new AtomicInteger();
	
	/**
	 * Measures the total number of matches created.
	 */
	AtomicInteger		matchCount				= new AtomicInteger();
	
	/**
	 * Measures the total number of merges between matches.
	 */
	AtomicInteger		mergeCount				= new AtomicInteger();
	/**
	 * Measures the amount of memory, as declared by the caller.
	 */
	AtomicInteger		memory					= new AtomicInteger();
	/**
	 * Measures the number of matches currently stored.
	 */
	AtomicInteger		storedMatches			= new AtomicInteger();
	
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
	public int getPerformanceNodes()
	{
		return performanceNodes.get();
	}
	
	/**
	 * @return the performanceNodesLabels
	 */
	public int getPerformanceNodesLabels()
	{
		return performanceNodesLabels.get();
	}
	
	/**
	 * @return the performanceEdges
	 */
	public int getPerformanceEdges()
	{
		return performanceEdges.get();
	}
	
	/**
	 * @return the performanceEdgesLabels
	 */
	public int getPerformanceEdgesLabels()
	{
		return performanceEdgesLabels.get();
	}
	
	/**
	 * @return the matchCount
	 */
	public int getMatchCount()
	{
		return matchCount.get();
	}
	
	/**
	 * @return the mergeCount
	 */
	public int getMergeCount()
	{
		return mergeCount.get();
	}
	
	/**
	 * @return the memory indication.
	 */
	public int getMemoryIndication()
	{
		return memory.get();
	}
	
	/**
	 * Increments the performance indicator.
	 *
	 * @return the current (updated) value.
	 */
	public int incrementEdgeLabelComparison()
	{
		return performanceEdgesLabels.incrementAndGet();
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
		return performanceEdgesLabels.addAndGet(increment);
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
		return performanceNodesLabels.incrementAndGet();
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
		return performanceNodesLabels.addAndGet(increment);
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
	 * Sets the current memory consumption.
	 *
	 * @param indication
	 *            - the indication on the memory consumption.
	 * @return the previously stored indication.
	 */
	public int setMemoryIndication(int indication)
	{
		int ret = memory.get();
		memory.set(indication);
		return ret;
	}
	
	/**
	 * Sets the current number of stored matches.
	 *
	 * @param matches
	 *            - the number of stored matches.
	 * @return the previously stored number.
	 */
	public int setStoredMatches(int matches)
	{
		int ret = storedMatches.get();
		storedMatches.set(matches);
		return ret;
	}
	
	/**
	 * Prints a one-line view of the performance indicators, as a log line (if any log exists).
	 *
	 * @return the same string that was printed.
	 */
	public String printStats()
	{
		String stats = "";
		stats += "nodes Ops|Labels: " + performanceNodes + "|" + performanceNodesLabels + "; edges Ops|Labels: "
				+ performanceEdges + "|" + performanceEdgesLabels + "; matches: " + matchCount + "; merges: "
				+ mergeCount + "; stored/memory: " + storedMatches + "/" + memory;
		stats += " $$> " + performanceNodes + ", " + performanceNodesLabels + ", " + performanceEdges + ", "
				+ performanceEdgesLabels + ", " + matchCount + ", " + mergeCount + ", " + storedMatches + ", " + memory
				+ "<$$";
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
