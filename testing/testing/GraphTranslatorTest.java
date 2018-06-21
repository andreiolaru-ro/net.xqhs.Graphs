package testing;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import net.xqhs.graphical.GCanvas;
import net.xqhs.graphs.context.ContextGraph;
import net.xqhs.graphs.matcher.MatchingVisualizer;
import net.xqhs.graphs.matcher.MonitorPack;
import net.xqhs.graphs.nlp.GraphConverter;
import net.xqhs.graphs.nlp.Parser;
import net.xqhs.graphs.translator.UserRequest;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.UnitComponent;

public class GraphTranslatorTest extends Tester {

	public static void main(String[] args) {
		new GraphTranslatorTest();
	}

	@Override
	protected void doTesting() {
		ArrayList<String> knowledge = null;
		ArrayList<String> patterns = null;
		Map<String, ContextGraph> gs = new TreeMap<String, ContextGraph>();
		StanfordCoreNLP pipeline = Parser.init();
		String type = "test_";
		try {
			knowledge = readFile(new File("/home/alexandra/sertaras/licenta/" + type + "graphs.txt"));
			patterns = readFile(new File("/home/alexandra/sertaras/licenta/" + type + "patterns.txt"));

			for (String s : knowledge)
				gs.put(s, new GraphConverter(Arrays.asList(s), pipeline).getG());
					
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		GCanvas canvas = new GCanvas();
		canvas.setZoom(2);
		canvas.resetLook();
		MonitorPack monitoring = new MonitorPack()
				.setLog((LoggerSimple) new UnitComponent().setUnitName("matcher").setLogLevel(Level.INFO));

		monitoring.setVisual(new MatchingVisualizer().setCanvas(canvas).setTopLeft(new Point(-400, 0)));

		UserRequest ur = null;
		ArrayList<String> responses = new ArrayList<String>();

		for (String p : patterns) {
			try {
				ur = new UserRequest(p, pipeline);
				responses.add(ur.getStringReply(gs, monitoring));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			writeFile("/home/alexandra/sertaras/licenta/" + type + "matched_graphs_"
					+ sdf.format(Calendar.getInstance().getTime()) + ".txt", responses);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static ArrayList<String> readFile(File fin) throws IOException {
		// Construct BufferedReader from FileReader
		BufferedReader br = new BufferedReader(new FileReader(fin));
		ArrayList<String> sentences = new ArrayList<String>();

		String line = null;
		while ((line = br.readLine()) != null) {
			sentences.add(line);
		}
		br.close();

		return sentences;
	}

	public static void writeFile(String fileName, ArrayList<String> sentences) throws IOException {
		FileWriter fw = new FileWriter(fileName);

		for (String s : sentences) {
			fw.write(s + "\n");
		}

		fw.close();
	}

}
