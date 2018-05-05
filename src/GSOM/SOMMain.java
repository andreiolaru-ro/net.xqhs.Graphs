package GSOM;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class SOMMain {
	public static HashMap<String, ArrayList<Double>> readIris() {
		ArrayList<String> lines = new ArrayList<String>();
		HashMap<String, ArrayList<Double>> hashlines = new HashMap<String, ArrayList<Double>>();
		try {
			FileReader f = new FileReader("src//GSOM//iris");

			BufferedReader br = new BufferedReader(f);

			String sCurrentLine;

			while ((sCurrentLine = br.readLine()) != null) {

				if (sCurrentLine != null && !sCurrentLine.isEmpty()) {

					lines.add(sCurrentLine);
					sCurrentLine = null;
				}
			}
			br.close();
			int p = 0;
			for (String line : lines) {
				ArrayList<Double> values = new ArrayList<Double>();

				String[] vals = line.split(",");
				values.add(Double.parseDouble(vals[0]));
				values.add(Double.parseDouble(vals[1]));
				values.add(Double.parseDouble(vals[2]));
				values.add(Double.parseDouble(vals[3]));
				values.add(vals[4].equals("Iris-setosa") ? 1.0 : vals[4]
						.equals("Iris-versicolor") ? 2.0 : 3.0);
				hashlines.put(String.valueOf(p), values);
				p++;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return hashlines;
	}

	public static void main(String[] args) {
		GSOM g = new GSOM();
		HashMap<String, ArrayList<Double>> data = readIris();
		HashMap<String, Double> result = new HashMap<String, Double>();
		ArrayList<Datapoint> points = new ArrayList<Datapoint>();

		for (Entry<String, ArrayList<Double>> entry : data.entrySet()) {
			result.put(entry.getKey(), entry.getValue().get(4));
			entry.getValue().remove(4);
			Datapoint d = new Datapoint(Integer.parseInt(entry.getKey()), " ");
			d.features = entry.getValue();
			points.add(d);
		}

		points = FeatureXtractor.rescale(points);
		for (Datapoint datapoint : points) {
			data.replace(String.valueOf(datapoint.id), datapoint.features);
		}
		GSOM.init(data);
		while (GSOM.currIter < GSOM.num_iterations) {
			g.train();
		}
		for (int i = 0; i < GSOM.somNodes.size(); i++) {
			SomNode nod = GSOM.somNodes.get(i);
			System.out.println("Node (" + nod.x + "," + nod.y + ") ="
					+ result.get(nod.dataStr));
		}

	}
}
