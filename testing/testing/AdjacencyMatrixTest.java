package testing;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.xqhs.graphical.GCanvas;
import net.xqhs.graphs.context.ContextGraph;
import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.graph.Graph;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.graphs.matcher.MatchingVisualizer;
import net.xqhs.graphs.matcher.MonitorPack;
import net.xqhs.graphs.nlp.ContextPatternConverter;
import net.xqhs.graphs.nlp.GraphConverter;
import net.xqhs.graphs.nlp.NLNodeP;
import net.xqhs.graphs.nlp.Parser;
import net.xqhs.graphs.nlp.forestation.AdjacencyMatrix;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.UnitComponent;

import org.graphstream.ui.view.Viewer;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class AdjacencyMatrixTest extends GraphMatcherTest {
	public static void main(String[] args) {
		// calls constructor of superclass tester which calls doTesting duh
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
			// testing adjMatrix
			AdjacencyMatrix adj = new AdjacencyMatrix(
					(SimpleGraph) testPack.get(NAME_PATTERN));
			adj.print();
			adj.treefy();
			adj.powerToTheMatrix(3);
			// JList<JList<String>> mat = new JList<JList<String>>();
			// NLEdge[][] ma = adj.getMa();
			// for (NLEdge[] element : ma) {
			//
			// ArrayList<String> labels = new ArrayList<String>();
			// for (int j = 0; j < ma.length; j++) {
			// labels.add(element[j] == null ? " - " : element[j]
			// .getLabel());
			// }
			// JList<String> labelList = new JList(labels.toArray());
			// labelList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
			// // labelList.setVisibleRowCount(labels.size());
			// labelList.setAlignmentY(JList.LEFT_ALIGNMENT);
			// labelList.setPreferredSize(new Dimension(250, 350));
			// mat.add(labelList);
			// }
			// mat.setLayoutOrientation(JList.VERTICAL);
			JTable table = new JTable(adj.getMa(), adj.getNodes().toArray());

			GCanvas canvas = null;
			if (visual) {
				JFrame frame = new JFrame(unitName);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

				canvas = new GCanvas();
				canvas.setZoom(2);
				canvas.resetLook();

				table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

				JScrollPane scrollPane = new JScrollPane(table);
				table.setFillsViewportHeight(true);
				JList list = new JList(adj.getNodes().toArray());
				list.setFixedCellWidth(50);

				list.setFixedCellHeight(table.getRowHeight()
						+ table.getRowMargin());
				// list.setCellRenderer(new RowHeaderRenderer(table));
				scrollPane.setRowHeaderView(list);
				frame.add(scrollPane);
				// frame.setLocation(10, 30);

				// JPanel contentPane = (JPanel) frame.getContentPane();
				// contentPane.setLayout(new BorderLayout(8, 8));
				// contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
				// contentPane.setSize(1100, 700);
				// contentPane.add(mat, BorderLayout.NORTH);
				// frame.add(canvas, BorderLayout.SOUTH);
				// contentPane.setVisible(true);
				frame.add(canvas);
				frame.pack();
				frame.setVisible(true);
			}

			MonitorPack monitoring = new MonitorPack()
					.setLog((LoggerSimple) new UnitComponent().setUnitName(
							"matcher").setLogLevel(Level.INFO));
			// dis iz where u change canvas to display on other surface
			if (visual)
				monitoring.setVisual(new MatchingVisualizer().setCanvas(canvas)
						.setTopLeft(new Point(400, 100)));// -400,0

			testMatchingProcess(testPack, monitoring);
		} else
			System.out.println("Something's fishy in Wisconsin.");
	}

	protected Map<String, Graph> getDefaultGraphsAndPatterns() throws Exception {
		Map<String, Graph> result = new HashMap<String, Graph>();
		ArrayList<String> patterns = new ArrayList<String>();
		patterns.add("When Emily is leaving the house through the front door, she should be restrained.");
		StanfordCoreNLP pipeline = Parser.init();
		// TODO:Atentie carpeala
		ContextPattern pat = Parser.convertContextPatterns(patterns, pipeline)
				.get(0);
		Viewer v = Parser.displayContextPattern(pat, true);
		System.out.println("Adjacency matrix:");

		ContextGraph cgh = new GraphConverter(
				Arrays.asList("Emily is leaving the house through the front door."),
				pipeline).getG();
		if (pat != null && cgh != null) {
			result.put(NAME_GRAPH, cgh);
			result.put(NAME_PATTERN, pat);
		}

		return result;
	}
}
