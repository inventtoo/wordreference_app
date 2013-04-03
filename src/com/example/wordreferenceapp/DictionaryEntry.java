package com.example.wordreferenceapp;

import java.util.ArrayList;

public class DictionaryEntry {
	String term;
	String sense;
	ArrayList<String> translations;
	ArrayList<String> usage;

	public DictionaryEntry(String term) {
		this(term, "");
	}

	public DictionaryEntry(String term, String sense) {
		this.term = term;
		this.sense = sense;
		this.translations = new ArrayList<String>();
		this.usage = new ArrayList<String>();
	}

	public String toString() {
		String s = this.term;
		if (this.sense.length() > 0) {
			if (this.sense.startsWith("(")) {
				s += " " + this.sense;
			} else {
				s += " (" + this.sense + ")";
			}
		} 
		s += ":\n\t" + this.translations;
		if (this.usage.size() > 0) {
			s += "\n\t" + this.usage;
		} 
		return s;
	}
};
