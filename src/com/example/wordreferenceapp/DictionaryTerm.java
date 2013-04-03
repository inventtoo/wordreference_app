package com.example.wordreferenceapp;

public class DictionaryTerm {
	String term;
	Language language;
	int relevance;

	public DictionaryTerm(String term, Language language) {
		this(term, language, 0);
	}

	public DictionaryTerm(String term, Language language, int relevance) {
		this.term = term;
		this.language = language;
		this.relevance = relevance;
	}

	public String toString() {
		return String.format("%s <%s>", this.term, this.language.code().toUpperCase());
	}
}
