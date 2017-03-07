package testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.xqhs.graphs.context.ContextGraph;
import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.SimpleEdge;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.graph.SimpleNode;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.pattern.NodeP;
import net.xqhs.graphs.representation.text.TextGraphRepresentation;
import net.xqhs.graphs.util.ContentHolder;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.UnitComponent;

/**
 * Parent class for testers.
 *
 * @author Andrei Olaru
 */
public class Tester
{
	/**
	 * Name of the key in a testPack that designates the graph.
	 */
	protected static final String	NAME_GRAPH			= "graph";
	/**
	 * Name of the key in a testPack that designates the pattern, in case only one pattern exists.
	 */
	protected static final String	NAME_PATTERN		= "pattern";
	/**
	 * Name of the prefix for the key in a testPack that designates a graph or pattern (will be visible as a
	 * {@link Graph} instance.
	 */
	protected static final String	NAME_GENERAL_GRAPH	= "graph";
	/**
	 * In a test pack returned by {@link #loadGraphsAndPatterns(String, String, Level)}, the name of the graph (first in
	 * the file).
	 */
	protected static String			testGraphName		= null;
														
	/**
	 * Directory with test files.
	 */
	protected static String			defaultFileDir		= "playground/";
	/**
	 * Extension of graph files.
	 */
	static String					defaultFileExt		= ".txt";
	/**
	 * Whatever is added after the file name to form the filename for the pattern.
	 */
	static String					patternpart			= "P";
														
	/**
	 * Log/unit name
	 */
	protected String				unitName			= "contextTestMain";
	/**
	 * Log
	 */
	protected UnitComponent			log;
									
	/**
	 * Creates a tester and runs it. It calls <code>doTesting()</code>. At the end the log is closed.
	 */
	public Tester()
	{
		log = (UnitComponent) new UnitComponent().setUnitName(unitName).setLogLevel(Level.ALL);
		
		doTesting();
		
		log.doExit();
	}
	
	/**
	 * Method to overload, which must contain the testing code. Call super to perform tests required by the super class.
	 */
	protected void doTesting()
	{
		log.lf("Hello World");
	}
	
	/**
	 * Loads a testPack formed of a graph and a pattern. It uses the specified filename for the graph and the filename
	 * with {@value #patternpart} added for the pattern.
	 * <p>
	 * The representation is in one-edge-per-line fromat.
	 *
	 * @param filename
	 *            - the file name.
	 * @param fileDir
	 *            - directory for the file; if <code>null</code>, {@link #defaultFileDir} will be used.
	 * @param readLevel
	 *            - {@link Level} for the logs involved in reading.
	 * @return the testPack {@link Map} of graph name &rarr; {@link Graph} instance.
	 */
	protected Map<String, Graph> loadTestGraphPattern(String filename, String fileDir, Level readLevel)
	{
		Map<String, Graph> testPack = new HashMap<String, Graph>();
		
		SimpleGraph G;
		try
		{
			G = ((SimpleGraph) new SimpleGraph().setUnitName("G").setLogLevel(readLevel).setLink(unitName))
					.readFrom(new FileInputStream(defaultFileDir + filename + defaultFileExt));
		} catch(FileNotFoundException e)
		{
			e.printStackTrace();
			return null;
		}
		
		GraphPattern GP;
		try
		{
			GP = (GraphPattern) ((SimpleGraph) new GraphPattern().setUnitName("GP").setLogLevel(readLevel)
					.setLink(unitName))
							.readFrom(new FileInputStream(defaultFileDir + filename + patternpart + defaultFileExt));
		} catch(FileNotFoundException e)
		{
			e.printStackTrace();
			return null;
		}
		testPack.put(NAME_GRAPH, G);
		testPack.put(NAME_PATTERN, GP);
		return testPack;
	}
	
	/**
	 * Loads a group of graphs and/or patterns from a file. Graphs are returned as {@link Graph} implementations, so
	 * they can be graphs, patterns, etc. In practice, {@link TextGraphRepresentation} is used. All returns will be
	 * {@link SimpleGraph} or {@link GraphPattern} instances containing instances of {@link SimpleNode}, {@link NodeP},
	 * and {@link SimpleEdge}.
	 * <p>
	 * Other implementations of graphs can use {@link Graph#addAll(java.util.Collection)} to import the read edges and
	 * nodes.
	 * <p>
	 * TODO: include names in the files.
	 *
	 * @param filename
	 *            - the name of the file (no extension).
	 * @param fileDir
	 *            - the directory of the file. If <code>null</code>, the default is used. It will be assembled as a
	 *            prefix to the file name.
	 * @param readLevel
	 *            - the log {@link Level} to use for reading the graphs. If <code>null</code>, no logging is output.
	 * @return a {@link Map} of names for the graphs and read graphs. Names will have the prefix
	 *         {@value #NAME_GENERAL_GRAPH} followed by # and the 0-based index of the graph in the file.
	 * @throws IOException
	 *             - if reading the file fails.
	 */
	protected Map<String, Graph> loadGraphsAndPatterns(String filename, String fileDir, Level readLevel)
			throws IOException
	{
		String file = ((fileDir != null) ? fileDir : defaultFileDir) + filename + defaultFileExt;
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(file))));
		StringBuilder builder = new StringBuilder();
		String line;
		while((line = reader.readLine()) != null)
		{
			builder.append(line);
			builder.append('\n');
		}
		ContentHolder<String> input = new ContentHolder<String>(builder.toString());
		reader.close();
		
		Map<String, Graph> graphs = new HashMap<String, Graph>();
		int i = 0;
		while(input.get().length() > 0)
		{
			Graph g = new SimpleGraph();
			TextGraphRepresentation repr;
			try
			{
				repr = (TextGraphRepresentation) new TextGraphRepresentation(g).setUnitName("Greader")
						.setLogLevel((readLevel != null) ? readLevel : Level.OFF);
				repr.readRepresentation(input);
			} catch(IllegalArgumentException e)
			{
				g = new GraphPattern();
				repr = (TextGraphRepresentation) new TextGraphRepresentation(g).setUnitName("Greader")
						.setLogLevel((readLevel != null) ? readLevel : Level.OFF);
				repr.readRepresentation(input);
			}
			log.li("new graph /  pattern: []", repr.toString());
			graphs.put(NAME_GENERAL_GRAPH + "#" + i, g);
			input.set(input.get().trim());
			i++;
		}
		return graphs;
	}
	
	/**
	 * Extracts the graph from the test pack. It is identified by the key {@link #testGraphName}.
	 * <p>
	 * It does not return a {@link ContextGraph} because it is expected that the context graph will add the components
	 * in the returned graph in some test-specific order.
	 *
	 * @param testPack
	 *            - the test pack, presumably created by
	 *            {@link #loadGraphsAndPatterns(String, String, net.xqhs.util.logging.LoggerSimple.Level)}.
	 * @return the graph.
	 */
	protected static Graph getTestGraph(Map<String, Graph> testPack)
	{
		return testPack.get(testGraphName);
	}
	
	/**
	 * Loads the list of graphs to serve as patterns, from a test pack. All graphs are considered, except for the one
	 * named {@link #testGraphName}.
	 *
	 * @param testPack
	 *            - the test pack, presumably created by
	 *            {@link #loadGraphsAndPatterns(String, String, net.xqhs.util.logging.LoggerSimple.Level)}.
	 * @return a {@link List} of {@link Graph} instances.
	 */
	protected static List<Graph> getTestGraphs(Map<String, Graph> testPack)
	{
		List<Graph> ret = new ArrayList<Graph>();
		for(String name : testPack.keySet())
			if(!name.equals(testGraphName))
				ret.add(testPack.get(name));
		return ret;
	}
	
	/**
	 * Loads the list of patterns from a test pack. All graphs are considered, except for the one named
	 * {@link #testGraphName}.
	 *
	 * @param testPack
	 *            - the test pack, presumably created by
	 *            {@link #loadGraphsAndPatterns(String, String, net.xqhs.util.logging.LoggerSimple.Level)}.
	 * @return a {@link List} of {@link GraphPattern} instances.
	 */
	protected static List<GraphPattern> getGraphPatterns(Map<String, Graph> testPack)
	{
		List<GraphPattern> ret = new ArrayList<GraphPattern>();
		for(Graph gp : getTestGraphs(testPack))
			ret.add((GraphPattern) new GraphPattern().addAll(gp.getComponents()).setDescription(gp.getDescription()));
		return ret;
	}
	
	/**
	 * Gets a list of {@link ContextPattern} instances based on the patterns in a test pack. All patterns are
	 * considered, except for the one named {@link #testGraphName}.
	 *
	 * @param testPack
	 *            - the test pack, presumably created by
	 *            {@link #loadGraphsAndPatterns(String, String, net.xqhs.util.logging.LoggerSimple.Level)}.
	 * @return a {@link List} of context patterns, in the order in which they were defined in the file.
	 */
	protected static List<ContextPattern> getContextPatterns(Map<String, Graph> testPack)
	{
		List<ContextPattern> ret = new ArrayList<ContextPattern>();
		for(Graph gp : getTestGraphs(testPack))
			ret.add((ContextPattern) new ContextPattern().addAll(gp.getComponents())
					.setDescription(gp.getDescription()));
		return ret;
	}
	
	/**
	 * Prints all the graphs in a testPack.
	 *
	 * @param testPack
	 *            - the testPack {@link Map} of graph name &rarr; {@link Graph} instance.
	 * @param printSimple
	 *            - if <code>true</code>, the {@link #toString()} version of the graph will be printed before the text
	 *            representation.
	 * @param separator
	 *            - see {@link TextGraphRepresentation#setLayout(String, String, int)}.
	 * @param separatorIncrement
	 *            - see {@link TextGraphRepresentation#setLayout(String, String, int)}.
	 * @param incrementLimit
	 *            - see {@link TextGraphRepresentation#setLayout(String, String, int)}.
	 * @param log
	 *            - the {@link LoggerSimple} instance to use to display the graph.
	 */
	protected static void printTestPack(Map<String, Graph> testPack, boolean printSimple, String separator,
			String separatorIncrement, int incrementLimit, LoggerSimple log)
	{
		for(Map.Entry<String, Graph> entry : testPack.entrySet())
		{
			log.li("[]: []", entry.getKey(), entry.getValue().toString());
			TextGraphRepresentation GR = new TextGraphRepresentation(entry.getValue()).setLayout(separator,
					separatorIncrement, incrementLimit);
			GR.update();
			log.li(GR.displayRepresentation());
		}
	}
	
	/**
	 * Prints a separator.
	 *
	 * @param progress
	 *            - negative for beginning of section; positive for ending of section; <code>0</code> for intermediate
	 *            separator; Absolute value gives number of beginning/ending symbols.
	 * @param section
	 *            - name of the separated section.
	 */
	protected void printSeparator(int progress, String section)
	{
		String arrows = "---";
		if(progress != 0)
		{
			String arrow = (progress < 0) ? "v" : ((progress > 0) ? "^" : "-");
			arrows = "";
			for(int i = 0; i < Math.abs(progress); i++)
				arrows += arrow;
		}
		log.li("================================= " + arrows + " [] =================", section);
	}
	
	/**
	 * @return the log.
	 */
	protected LoggerSimple getLog()
	{
		return log;
	}
}
