package testing;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import net.xqhs.graphs.translator.Merlin;

public class MerlinTest extends Tester {

	public static void main(String[] args) {
		new MerlinTest();
	}

	@Override
	protected void doTesting() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		Merlin m = new Merlin("/home/alexandra/sertaras/licenta/graphs.txt");
		String query = new String();
		String reply;
		
		System.out.print("> ");
		
		try {
			query = reader.readLine();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		while (!query.equals("bye")) {
			try {
				reply = m.askMerlin(query); // get Merlin's reply
				System.out.println(reply);
				System.out.print("> ");
				query = reader.readLine(); //get next query
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}

}
