package testing;

import java.util.Random;
import java.util.Vector;

import net.xqhs.graphs.context.CCMImplementation;
import net.xqhs.graphs.context.ContextGraph;
import net.xqhs.graphs.context.ContinuousContextMatchingPlatform;
import net.xqhs.graphs.context.Instant.TimeKeeper;
import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleEdge;
import net.xqhs.graphs.graph.SimpleNode;
import net.xqhs.graphs.matcher.MonitorPack;
import net.xqhs.graphs.matchingPlatform.Transaction;
import net.xqhs.graphs.matchingPlatform.Transaction.Operation;
import testing.scenario_based.ContextGraphsTest.IntTimeKeeper;

public class ParallelLoadTester extends Tester
{
	protected static final int	N_NODES				= 10;
	protected static final int	MAX_EDGES			= N_NODES * (N_NODES - 1) / 2;
	protected static final int	MAX_EDGES_PER_CYCLE	= 4;
	protected static final int	N_CYCLES			= 100;
													
	@Override
	protected void doTesting()
	{
		super.doTesting();
		
		long seedPre = -1;
		long seed = System.currentTimeMillis();
		if(seedPre >= 0)
			seed = seedPre;
		log.lf("seed was " + seed);
		Random rand = new Random(seed);
		
		TimeKeeper ticker = new IntTimeKeeper();
		MonitorPack monitor = new MonitorPack(); // .setLog(log);
		ContinuousContextMatchingPlatform CCM = new CCMImplementation(ticker, monitor);
		ContextGraph CG = new ContextGraph();
		CCM.setContextGraph(CG);
		
		Vector<Node> nodes = new Vector<Node>();
		for(int i = 0; i < N_NODES; i++)
		{
			Node n = new SimpleNode(new Integer(i).toString());
			CG.add(n);
			nodes.add(n);
		}
		Edge[][] adjacency = new Edge[N_NODES][N_NODES];
		log.li("CG", CG);
		
		printSeparator(0, "start testing");
		int cycles = N_CYCLES;
		while(cycles > 0)
		{
			Transaction t = new Transaction();
			int nRem = CG.getEdges().isEmpty() ? 0 : rand.nextInt(Math.min(MAX_EDGES_PER_CYCLE, CG.m()));
			int nAdd = CG.m() >= MAX_EDGES ? 0 : rand.nextInt(Math.min(MAX_EDGES_PER_CYCLE, MAX_EDGES - CG.m()));
			
			log.li("rem:" + nRem);
			// deletions
			while(t.size() < nRem)
			{
				int src = rand.nextInt(N_NODES);
				int dest = rand.nextInt(N_NODES);
				if((adjacency[src][dest] != null) && !t.keySet().contains(adjacency[src][dest]))
					t.put(adjacency[src][dest], Operation.REMOVE);
			}
			
			log.li("add:" + nAdd);
			// additions
			while(t.size() < nRem + nAdd)
			{
				int src = rand.nextInt(N_NODES);
				int dest = rand.nextInt(N_NODES);
				if(!t.keySet().contains(adjacency[src][dest]))
				{
					adjacency[src][dest] = new SimpleEdge(nodes.get(dest), nodes.get(src), null);
					t.put(adjacency[src][dest], Operation.ADD);
				}
			}
			
			CG.applyTransaction(t);
			cycles--;
			log.li("Transaction", t);
			log.li("REM/ADD: []/[]; CG", nRem, nAdd, CG);
		}
	}
	
	public static void main(String[] args)
	{
		new ParallelLoadTester();
	}
	
}
