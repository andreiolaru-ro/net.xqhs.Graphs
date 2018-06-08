package testing;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import org.graphstream.ui.view.Viewer;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
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
import net.xqhs.graphs.nlp.NLComponentReader;
import net.xqhs.graphs.nlp.NLNodeP;
import net.xqhs.graphs.nlp.Parser;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.representation.text.TextGraphRepresentation;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.UnitComponent;

public class MatcherTestG<E> extends GraphMatcherTest
{
	public static void main(String[] args)
	{
		new MatcherTestG();
	}

	@Override
	protected void doTesting()
	{
		// super.doTesting();

		boolean visual = true;

		Map<String, Graph> testPack = null;
		try
		{
			testPack = getDefaultGraphsAndPatterns();

		} catch(Exception e)
		{

			e.printStackTrace();
		}

		if(testPack != null)
		{
			printTestPack(testPack, true, "\n", "\t", 2, log);
			// print tree structure
			HashMap<NLNodeP, Set<NLNodeP>> roots = ContextPatternConverter
					.getRoots((ContextPattern) testPack.get(NAME_PATTERN));
			for(NLNodeP key : roots.keySet())
			{
				System.out.println("Chiildren of: " + key);
				String rootstr = "";
				for(NLNodeP root : roots.get(key))
				{
					rootstr += " " + root + ", ";
				}
				if(!rootstr.equals(""))
					System.out.println(rootstr);
			}

			GCanvas canvas = null;
			if(visual)
			{
				JFrame frame = new JFrame(unitName);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

				canvas = new GCanvas();
				canvas.setZoom(2);
				canvas.resetLook();
				MonitorPack monitoring = new MonitorPack()
						.setLog((LoggerSimple) new UnitComponent().setUnitName("matcher").setLogLevel(Level.INFO));

				monitoring.setVisual(new MatchingVisualizer().setCanvas(canvas).setTopLeft(new Point(-400, 0)));// -400,0

				// testMatchingProcess(testPack, monitoring);

				Graph G = testPack.get(NAME_GRAPH);
				GraphPattern GP = (GraphPattern) testPack.get(NAME_PATTERN);

				GraphMatchingProcess GMQ = GraphMatcherQuick.getMatcher(G, GP, monitoring);
				if(!GMQ.getBestMatches().isEmpty())
				{
					printSeparator(0, "best Matches[" + GMQ.getBestMatches().get(0).getK() + "] : ");
					Match bestMatch = GMQ.getBestMatches().get(0);
					SimpleGraph gph = (SimpleGraph) bestMatch.getGraph();
					GraphPattern uns = bestMatch.getUnsolvedPart();
					MatchMaker mm = new MatchMaker();
					SimpleGraph g = mm.nowKiss(gph, uns);
					TextGraphRepresentation GR = new TextGraphRepresentation(g).setLayout("\n", "\t", 15);
					GR.update();
					System.out.println(GR.displayRepresentation());

					try
					{
						Parser.displayContextPattern(g, true);
						SimpleGraph sg = new SimpleGraph();
						sg.addAll(bestMatch.getMatchedGraph().getComponents());
						Parser.displayContextPattern(sg, true);
					} catch(IOException | InterruptedException e)
					{

						e.printStackTrace();
					}
				}
				else
					System.out.println("NO match :( ");
				frame.add(canvas);
				frame.pack();
				frame.setVisible(true);

			}
		}
		else
			System.out.println("Something's fishy in Wisconsin.");

	}

	protected Map<String, Graph> getDefaultGraphsAndPatterns() throws Exception
	{
		Map<String, Graph> result = new HashMap<String, Graph>();
		ArrayList<String> patterns = new ArrayList<String>();
		// patterns.add("When Emily is leaving the house through the front door, she should be restrained.");
		// patterns.add("Emily needs keys when leaving the house.");
		// patterns.add("If Emily is at the front door, she must be leaving the house.");
		patterns.add("Recommend me a restaurant which serves fries.");
		// patterns.add("If rain is forecast, bring along an umbrella.");

		String graphString = null;
		graphString = "McDonalds is a fastfood restaurant and it serves french fries.";
		// graphString = "rain is forecast.";

		StanfordCoreNLP pipeline = null;
		ArrayList<String> patternsToTransform = new ArrayList<String>();
		ArrayList<ContextPattern> patternsCached = new ArrayList<ContextPattern>();
		for(String pattern : patterns)
		{
			Graph g = cacheGet(pattern, new ContextPattern(), log);
			if(g != null)
				patternsCached.add((ContextPattern) g);
			else
				patternsToTransform.add(pattern);
		}

		ArrayList<ContextPattern> pat = new ArrayList<>();
		if(!patternsToTransform.isEmpty())
		{
			pipeline = Parser.init();
			pat.addAll(Parser.convertContextPatterns(patternsToTransform, pipeline));
			System.out.println("Pattern: " + pat);
			// !! ASSUMES ENTRIES ARE IN THE SAME ORDER IN pat AND IN patternsToTransform.
			for(int i = 0; i < pat.size(); i++)
				cacheSave(patternsToTransform.get(i), pat.get(i), log);
		}
		pat.addAll(patternsCached);

		for(ContextPattern contextPattern : pat)
		{
			Viewer v = Parser.displayContextPattern(contextPattern, true);
		}

		ContextGraph cgh = (ContextGraph) cacheGet(graphString, new ContextGraph(), log);
		if(cgh == null)
		{
			if(pipeline == null)
				pipeline = Parser.init();
			cgh = new GraphConverter(Arrays.asList(graphString), pipeline).getG();
			cacheSave(graphString, cgh, log);
		}
		if(pat != null && cgh != null)
		{
			result.put(NAME_GRAPH, cgh);
			result.put(NAME_PATTERN, pat.get(0));
		}

		return result;
	}

	/**
	 * The file storing the cahce of recent graphs and patterns.
	 */
	protected static String	RECENTS_FILE	= "recent/recent.txt";
	/**
	 * In the cache file, the separator between the raw text and the graph representation.
	 */
	protected static String	SEPARATOR		= "###";

	/**
	 * Tries to find a line in the cache file which has the given text as raw text.
	 *
	 * @param text
	 *            -- the raw text to search.
	 * @param g
	 *            -- the {@link Graph} instance in which to read the representation.
	 * @param log
	 *            -- the log to use.
	 * @return the graph found, if any.
	 */
	protected static Graph cacheGet(String text, Graph g, LoggerSimple log)
	{
		try (BufferedReader reader = new BufferedReader(new FileReader(RECENTS_FILE)))
		{
			String line = null;
			String lastRepr = null;
			while((line = reader.readLine()) != null)
			{
				String[] parts = line.split(SEPARATOR);
				if(parts.length > 1 && parts[0].equals(text))
				{
					lastRepr = parts[1];
				}
			}
			if(lastRepr == null)
			{
				log.li("No graph found for text [].", text);
				return null;
			}
			log.li("Reading representation for [] from cache: []", text, lastRepr);
			new TextGraphRepresentation(g).setComponentReader(new NLComponentReader()).readRepresentation(lastRepr);
			log.li("Result: ", g.toString());
			return g;
		} catch(IOException e)
		{
			log.le("Reading file [] failed:", RECENTS_FILE, e);
			return null;
		}
	}

	/**
	 * Adds a new entry to the cache file.
	 *
	 * @param text
	 *            -- the raw text.
	 * @param graph
	 *            -- the graph corresponding to the raw text.
	 * @param log
	 *            -- the log to use.
	 */
	protected static void cacheSave(String text, Graph graph, LoggerSimple log)
	{
		String line = text + SEPARATOR + new TextGraphRepresentation(graph).update().toString() + "\n";
		try
		{
			Files.write(Paths.get(RECENTS_FILE), line.getBytes(), StandardOpenOption.APPEND);
			log.li("Added line: []", line);
		} catch(IOException e)
		{
			log.le("Appending to file [] failed: ", RECENTS_FILE, e);
		}
	}
	
	// /**
	// * Removes from the cache file the line for the given raw text.
	// *
	// * @param text
	// * -- the raw text to search.
	// * @param log
	// * -- the log to use.
	// * @return <code>true</code> if any line was found with the given raw text, <code>false</code> otherwise.
	// */
	// protected static boolean cacheRemove(String text, LoggerSimple log)
	// {
	// File tmp;
	// boolean found = false;
	// try
	// {
	// tmp = File.createTempFile("tmp", "");
	// try (BufferedReader reader = new BufferedReader(new FileReader(RECENTS_FILE));
	// BufferedWriter writer = new BufferedWriter(new FileWriter(tmp));)
	// {
	//
	// String line;
	// while((line = reader.readLine()) != null)
	// {
	// String[] parts = line.split(SEPARATOR);
	// if(parts.length > 1 && parts[0].equals(text))
	// {
	// found = true;
	// log.li("Removing found line: ", line);
	// }
	// else
	// writer.write(String.format("%s%n", line));
	// }
	// } catch(IOException e)
	// {
	// log.le("Something went wrong: ", e);
	// }
	//
	// File oldFile = new File(RECENTS_FILE);
	// if(oldFile.delete())
	// tmp.renameTo(oldFile);
	// } catch(IOException e)
	// {
	// log.le("Failed to create / manage temporary file: ", e);
	// }
	// return found;
	// }
}
