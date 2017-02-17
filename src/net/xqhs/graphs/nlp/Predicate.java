package net.xqhs.graphs.nlp;

import java.util.ArrayList;
import java.util.HashMap;

public class Predicate {
	public Predicate(String name, HashMap<String, ArrayList<String>> arguments) {
		super();
		this.name = name;
		this.arguments = arguments;
	}

	@Override
	public String toString() {

		String s = "P: " + this.getName() + " ( ";
		if (!arguments.isEmpty())
			for (String role : arguments.keySet()) {
				for (String arg : arguments.get(role)) {
					s += " " + arg + " ,";
				}
				s += ")";
			}
		return s;

	}

	String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public HashMap<String, ArrayList<String>> getArguments() {
		return arguments;
	}

	public void setArguments(HashMap<String, ArrayList<String>> arguments) {
		this.arguments = arguments;
	}

	public boolean getValue() {
		return value;
	}

	public void setValue(boolean value) {
		this.value = value;
	}

	HashMap<String, ArrayList<String>> arguments;
	boolean value;

}
