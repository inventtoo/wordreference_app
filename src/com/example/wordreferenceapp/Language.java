package com.example.wordreferenceapp;

import java.util.Locale;

public enum Language {
		ARABIC("ar"),
		CHINESE("zh"),
		CZECH("cz"),
		ENGLISH("en"), 
		FRENCH("fr"), 
		GERMAN("de"), 
		GREEK("gr"),
		ITALIAN("it"), 
		JAPANESE("ja"),
		KOREAN("ko"),
		POLISH("pl"),
		PORTUGUESE("pt"),
		ROMANIAN("ro"),
		RUSSIAN("ru"),
		SPANISH("es"), 
		TURKISH("tr");

	private final String mLanguage;

	private Language(String language) {
		mLanguage = language;
	}

	public String code() {
		return mLanguage;
	}
	
	public String isocode() {
		if (this == GREEK) {
			return "el";
		} else if (this == CZECH) {
			return "cs";
		} else {
			return code();
		}
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
	
	public String toString() {
		return (new Locale(isocode())).getDisplayLanguage(Locale.getDefault());
	}
};

