package testing;

import java.util.ArrayList;
import java.util.HashMap;

import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.nlp.Parser;
import net.xqhs.graphs.nlp.Tests;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class BookReadingTest extends Tester {

	public static void main(String[] args) {
		new BookReadingTest();
	}

	@Override
	protected void doTesting() {
		HashMap<String, String> lines = Tests.readPrisonRules();
		StanfordCoreNLP pipeline = Parser.init();
		try {
			ArrayList<ContextPattern> pat = Parser.convertContextPatterns(
					new ArrayList<String>(lines.values()), pipeline, "prison");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Done. Check out/prison for results.");
	}
}
