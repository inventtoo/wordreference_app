package com.example.wordreferenceapp;

public enum Language {
	ENGLISH("en"), 
		SPANISH("es"), 
		FRENCH("fr"), 
		ITALIAN("it"), 
		GERMAN("de"), 
		PORTUGUESE("pt");

	private final String language;

	private Language(String language) {
		this.language = language;
	}

	public String code() {
		return this.language;
	}

	public static Language fromString(String text) {
		if (text != null) {
			for (Language lang : Language.values()) {
				if (text.equalsIgnoreCase(lang.code())) {
					return lang;
				}
			}
		}
		return null;
	}
};

