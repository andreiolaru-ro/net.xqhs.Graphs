package gui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import javax.swing.text.Document;

import net.miginfocom.swing.MigLayout;
import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.nlp.Parser;

import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class PatternInsertGUI {
	private static final JLabel lbInsertText = new JLabel("Pattern text");
	private static final PatternInsertGUI pig = new PatternInsertGUI();

	public static void main(String[] args) {
		StanfordCoreNLP pipeline = Parser.init();
		System.setProperty("org.graphstream.ui.renderer",
				"org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		JFrame f = new JFrame("Context Pattern Converter & Graph Matcher");

		f.setSize(1075, 607);
		f.setLocation(300, 200);
		f.getContentPane().setLayout(
				new MigLayout("", "[82.00px][964px,grow][][]",
						"[22px][95.00][grow][540px,grow][grow]"));

		f.getContentPane().add(lbInsertText, "cell 0 1,alignx left,growy");
		f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		JTextArea txtPatternTxtInsert = new JTextArea();
		txtPatternTxtInsert.setBackground(Color.PINK);
		txtPatternTxtInsert.setLineWrap(true);
		txtPatternTxtInsert.setWrapStyleWord(true);
		txtPatternTxtInsert.setEditable(true);
		JScrollPane scrollPane = new JScrollPane(txtPatternTxtInsert);
		// how to capture input
		f.getContentPane().add(scrollPane, "cell 1 1, growx,aligny center");
		// f.getContentPane().add(txtPatternTxtInsert,
		// "cell 1 1,growx,aligny center");

		JButton btnConvertPattern = new JButton("Convert");
		// btnConvert.setEnabled(false);
		// make button active only if there's txt in txtPatternTxtInsert
		ButtonModel model = btnConvertPattern.getModel();
		Document doc = txtPatternTxtInsert.getDocument();
		ButtonEnablement buttonEnablement = new ButtonEnablement(model);
		buttonEnablement.addDocument(doc);
		f.getContentPane().add(btnConvertPattern, "cell 2 1");

		JLabel lblCurrentContext = new JLabel("Current context");
		f.getContentPane().add(lblCurrentContext, "cell 0 2");

		JTextArea txtGraphInsert = new JTextArea();
		txtGraphInsert.setBackground(Color.MAGENTA);
		lblCurrentContext.setLabelFor(txtGraphInsert);

		f.getContentPane().add(txtGraphInsert, "cell 1 2,grow");

		JButton btnConvertGraph = new JButton("Convert");
		btnConvertGraph.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {

				// convert text to context graph, set current context graph and
				// display in frame
			}
		});
		f.getContentPane().add(btnConvertGraph, "cell 2 2");

		JPanel panel = new JPanel();
		panel.setBorder(new LineBorder(new Color(0, 0, 0), 3, true));
		panel.setBackground(Color.PINK);
		FlowLayout flowLayout = (FlowLayout) panel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		f.getContentPane().add(panel, "cell 1 3,alignx left,grow");

		JTextArea txtConversionResult = new JTextArea();
		txtConversionResult.setBackground(Color.GREEN);
		txtConversionResult.setWrapStyleWord(true);
		// f.getContentPane().add(txtConversionResult,
		// "cell 1 3,growx,aligny bottom");
		JScrollPane scrollPane_1 = new JScrollPane(txtConversionResult);
		f.getContentPane().add(scrollPane_1, "cell 1 4,growx,aligny bottom");
		JLabel lblConversionResult = new JLabel("Conversion Result:");
		lblConversionResult.setLabelFor(txtConversionResult);
		f.getContentPane().add(lblConversionResult, "cell 0 4");

		lbInsertText.setLabelFor(txtPatternTxtInsert);

		btnConvertPattern.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				Toolkit.getDefaultToolkit().beep();
				ArrayList<String> sentences = new ArrayList<String>();
				// break into paragtraphs or sentences &
				sentences.add(txtPatternTxtInsert.getText());
				try {
					// TODO: Atentie Carpeala
					ContextPattern pat = Parser.convertContextPatterns(
							sentences, pipeline).get(0);
					Viewer view = Parser.displayContextPattern(pat, false);
					ViewPanel v = view.getDefaultView();
					v.setBounds(panel.getBounds());
					panel.add(v);
					if (view.getDefaultView() == null) {
						System.out.println("AIN'T NO DEFAULT VIEW YO");
					}
					panel.repaint();
					txtConversionResult.append(Parser.cxtToStr(pat));

					txtConversionResult.setCaretPosition(txtConversionResult
							.getDocument().getLength());

				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				// convert sentence to pattern and match with current graph then
				// translate all of it to sentence and display extended graph
				// and sentence
			}
		});

		f.setVisible(true);
	}
}
