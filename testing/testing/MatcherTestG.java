package testing;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import net.xqhs.graphical.GCanvas;
import net.xqhs.graphs.context.ContextGraph;
import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.matcher.GraphMatcherQuick;
import net.xqhs.graphs.matcher.GraphMatchingProcess;
import net.xqhs.graphs.matcher.Match;
import net.xqhs.graphs.matcher.MatchMaker;
import net.xqhs.graphs.matcher.MatchingVisualizer;
import net.xqhs.graphs.matcher.MonitorPack;
import net.xqhs.graphs.nlp.ContextPatternConverter;
import net.xqhs.graphs.nlp.GraphConverter;
import net.xqhs.graphs.nlp.NLNodeP;
import net.xqhs.graphs.nlp.Parser;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.UnitComponent;

import org.graphstream.ui.view.Viewer;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class MatcherTestG<E> extends GraphMatcherTest {
	public static void main(String[] args) {
		new MatcherTestG();
	}

	@Override
	protected void doTesting() {
		// super.doTesting();

		boolean visual = true;

		Map<String, Graph> testPack = null;
		try {
			testPack = getDefaultGraphsAndPatterns();

		} catch (Exception e) {

			e.printStackTrace();
		}

		if (testPack != null) {
			printTestPack(testPack, true, "\n", "\t", 2, log);
			// print tree structure
			HashMap<NLNodeP, Set<NLNodeP>> roots = ContextPatternConverter
					.getRoots((ContextPattern) testPack.get(NAME_PATTERN));
			for (NLNodeP key : roots.keySet()) {
				System.out.println("Chiildren of: " + key);
				String rootstr = "";
				for (NLNodeP root : roots.get(key)) {
					rootstr += " " + root + ", ";
				}
				if (!rootstr.equals(""))
					System.out.println(rootstr);
			}

			GCanvas canvas = null;
			if (visual) {
				JFrame frame = new JFrame(unitName);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

				canvas = new GCanvas();
				canvas.setZoom(2);
				canvas.resetLook();
				MonitorPack monitoring = new MonitorPack()
						.setLog((LoggerSimple) new UnitComponent().setUnitName(
								"matcher").setLogLevel(Level.INFO));

				monitoring.setVisual(new MatchingVisualizer().setCanvas(canvas)
						.setTopLeft(new Point(-400, 0)));// -400,0

				// testMatchingProcess(testPack, monitoring);

				Graph G = testPack.get(NAME_GRAPH);
				GraphPattern GP = (GraphPattern) testPack.get(NAME_PATTERN);

				GraphMatchingProcess GMQ = GraphMatcherQuick.getMatcher(G, GP,
						monitoring);

				printSeparator(0, "best Matches["
						+ GMQ.getBestMatches().get(0).getK() + "] : ");
				Match bestMatch = GMQ.getBestMatches().get(0);
				SimpleGraph gph = (SimpleGraph) bestMatch.getGraph();
				GraphPattern uns = bestMatch.getUnsolvedPart();
				MatchMaker mm = new MatchMaker();
				SimpleGraph g = mm.nowKiss(gph, uns);
				try {
					Parser.contextPatternVisualize(g, true);
				} catch (IOException | InterruptedException e) {

					e.printStackTrace();
				}

				frame.add(canvas);
				frame.pack();
				frame.setVisible(true);

			}
		} else
			System.out.println("Something's fishy in Wisconsin.");

	}

	protected Map<String, Graph> getDefaultGraphsAndPatterns() throws Exception {
		Map<String, Graph> result = new HashMap<String, Graph>();
		ArrayList<String> patterns = new ArrayList<String>();
		// patterns.add("When Emily is leaving the house through the front door, she should be restrained.");
		// patterns.add("Emily needs keys when leaving the house.");
		patterns.add("If Emily is at the front door, she is leaving the house.");

		StanfordCoreNLP pipeline = Parser.init();
		ArrayList<ContextPattern> pat = Parser.convertContextPatterns(patterns,
				pipeline);
		for (ContextPattern contextPattern : pat) {
			Viewer v = Parser.contextPatternVisualize(contextPattern, true);
		}

		ContextGraph cgh = new GraphConverter(
				Arrays.asList("Emily is at the front door."), pipeline).getG();
		if (pat != null && cgh != null) {
			result.put(NAME_GRAPH, cgh);
			result.put(NAME_PATTERN, pat.get(0));
		}

		return result;
	}
}
