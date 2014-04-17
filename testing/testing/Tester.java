package testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

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
	 * Directory with test files.
	 */
	static String					defaultFileDir		= "playground/";
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
					.setLink(unitName)).readFrom(new FileInputStream(defaultFileDir + filename + patternpart
					+ defaultFileExt));
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
	 * they can be graphs, patterns, etc. In practice, {@link TextGraphRepresentation} is used, so all returns will be
	 * {@link SimpleGraph} instances containing instances of {@link SimpleNode}, {@link NodeP}, and {@link SimpleEdge}.
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
			TextGraphRepresentation repr = (TextGraphRepresentation) new TextGraphRepresentation(g).setUnitName(
					"Greader").setLogLevel((readLevel != null) ? readLevel : Level.OFF);
			repr.readRepresentation(input);
			log.li("graph new pattern: []", repr.toString());
			graphs.put(NAME_GENERAL_GRAPH + "#" + i, g);
			input.set(input.get().trim());
			i++;
		}
		return graphs;
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
}
