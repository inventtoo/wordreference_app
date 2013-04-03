package com.example.wordreferenceapp;

import java.util.ArrayList;

public class DictionaryResult {
	String term;
	String pronunciation;
	Language fromLanguage;
	Language toLanguage;
	ArrayList<DictionaryEntry> entries;
	String url;

	public DictionaryResult(String term, Language fromLanguage, Language toLanguage) {
		this.term = term;
		this.fromLanguage = fromLanguage;
		this.toLanguage = toLanguage;
		this.pronunciation = "";
		entries = new ArrayList<DictionaryEntry>();
	}
};
