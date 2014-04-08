package testing;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.pattern.GraphPattern;
import net.xqhs.graphs.representation.text.TextGraphRepresentation;
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
	protected static final String	NAME_GRAPH		= "graph";
	/**
	 * Name of the key in a testPack that designates the pattern, in case only one pattern exists.
	 */
	protected static final String	NAME_PATTERN	= "pattern";
	
	/**
	 * Directory with test files.
	 */
	static String					defaultFileDir	= "playground/";
	/**
	 * Extension of graph files.
	 */
	static String					defaultFileExt	= ".txt";
	/**
	 * Whatever is added after the file name to form the filename for the pattern.
	 */
	static String					patternpart		= "P";
	
	/**
	 * Log/unit name
	 */
	protected String				unitName		= "contextTestMain";
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
