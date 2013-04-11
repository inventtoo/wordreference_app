package com.example.wordreferenceapp;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class WordReferenceDictionary extends LanguageDictionary {

	private static final String QUERY_URL = 
		"http://www.wordreference.com/{dict}/{word}";
	private static final String AUTOCOMPLETE_URL = 
		"http://www.wordreference.com/2012/autocomplete.aspx?dict={dict}&query={word}";
	private static final String SPELL_URL = 
		"http://spell.wordreference.com/spell/spelljs.php?dict={dict}&w={word}";
	
	private static final Language[][] AVAILABLE_COMBINATIONS = {
		/* SPANISH */
		{ Language.ENGLISH, Language.SPANISH },
		{ Language.SPANISH, Language.ENGLISH },
		{ Language.FRENCH, Language.SPANISH },
		{ Language.SPANISH, Language.FRENCH },
		{ Language.PORTUGUESE, Language.SPANISH },
		{ Language.SPANISH, Language.PORTUGUESE },
		/* FRENCH */
		{ Language.FRENCH, Language.ENGLISH },
		{ Language.ENGLISH, Language.FRENCH },
		/* ITALIAN */
		{ Language.ITALIAN, Language.ENGLISH },
		{ Language.ENGLISH, Language.ITALIAN },
		/* GERMAN */
		{ Language.GERMAN, Language.ENGLISH },
		{ Language.ENGLISH, Language.GERMAN },
		/* RUSSIAN */
		{ Language.RUSSIAN, Language.ENGLISH },
		{ Language.ENGLISH, Language.RUSSIAN },
		/* PORTUGUESE */
		{ Language.PORTUGUESE, Language.ENGLISH },
		{ Language.ENGLISH, Language.PORTUGUESE },
		/* POLISH */
		{ Language.POLISH, Language.ENGLISH },
		{ Language.ENGLISH, Language.POLISH },
		/* ROMANIAN */
		{ Language.ROMANIAN, Language.ENGLISH },
		{ Language.ENGLISH, Language.ROMANIAN },
		/* CZECH */
		{ Language.CZECH, Language.ENGLISH },
		{ Language.ENGLISH, Language.CZECH },
		/* GREEK */
		{ Language.GREEK, Language.ENGLISH },
		{ Language.ENGLISH, Language.GREEK },
		/* TURKISH */
		{ Language.TURKISH, Language.ENGLISH },
		{ Language.ENGLISH, Language.TURKISH },
		/* CHINESE */
		{ Language.CHINESE, Language.ENGLISH },
		{ Language.ENGLISH, Language.CHINESE },
		/* JAPANESE */
		{ Language.JAPANESE, Language.ENGLISH },
		{ Language.ENGLISH, Language.JAPANESE },
		/* KOREAN */
		{ Language.KOREAN, Language.ENGLISH },
		{ Language.ENGLISH, Language.KOREAN },
		/* ARABIC */
		{ Language.ARABIC, Language.ENGLISH },
		{ Language.ENGLISH, Language.ARABIC },
	};

	public WordReferenceDictionary(Language fromLanguage, Language toLanguage) 
		throws CombinationNotAvailableException  {
		super(fromLanguage, toLanguage);
/*
		WordReferenceDictionary[] dictionaries = 
			WordReferenceDictionary.AVAILABLE_DICTIONARIES;

		for (WordReferenceDictionary dictionary : dictionaries) {
			if (dictionary.equals(this)) {
				return;
			}
		}

		throw new CombinationNotAvailableException();
		*/
	}
	
	public static List<Language> availableCombinationsWithSource(Language language) {
		List<Language> languages = new ArrayList<Language>();
		
		Language[][] combinations = AVAILABLE_COMBINATIONS;
		int numComb = combinations.length;
		for (int n = 0; n < numComb; ++n) {
			if (combinations[n][0] == language) {
				languages.add(combinations[n][1]);
			}
		}
		
		return languages;
	}

	@Override
	public List<DictionaryTerm> getHints(String query) throws IOException {
		String url = WordReferenceDictionary.AUTOCOMPLETE_URL
			.replace("{dict}", mFromLanguage.code() + mToLanguage.code())
			.replace("{word}", query);

		List<DictionaryTerm> hints = new ArrayList<DictionaryTerm>();

		try {
			URL autocomplete = new URL(url);
			BufferedReader in = new BufferedReader(
					new InputStreamReader(autocomplete.openStream()));

			String line;
			while ((line = in.readLine()) != null) {
				String[] items = line.split(" ");
				if (items.length >= 3) {
					String term = items[0];
					Language lang = Language.fromString(items[1]);
					int freq = Integer.valueOf(items[2]);
					if (lang != null) {
						DictionaryTerm dt = new DictionaryTerm(term, lang, freq);
						hints.add(dt);
					}
				}
			}
		} catch (PatternSyntaxException pse) {
		}

		return hints;
	}

	@Override
	public List<DictionaryTerm> getAlternatives(String term) throws IOException {
		String url = WordReferenceDictionary.SPELL_URL
			.replace("{dict}", mFromLanguage.code() + mToLanguage.code())
			.replace("{word}", term);

		List<DictionaryTerm> alternatives = new ArrayList<DictionaryTerm>();
		Document doc = Jsoup.connect(url).get();
		Elements choices = doc.select("td a");
		for (Element choice : choices) {
			/* WordReference does not return alternatives in both languages */
			DictionaryTerm dt = new DictionaryTerm(choice.text().trim(), mFromLanguage);
			alternatives.add(dt);
		}	

		return alternatives;
	}

	private void parseResult(Document doc, DictionaryResult result) {
		Elements rows = doc.select("table.WRD tr");
		DictionaryEntry entry = null;
		for (Element row : rows) {
			Elements cols = row.select("td");
			if (cols.size() == 3 && cols.first().classNames().contains("FrWrd")) {
				// new entry
				if (entry != null) {
					result.entries.add(entry);
				}
				entry = new DictionaryEntry(
						cols.get(0).select("strong").first().text().trim(),
						cols.get(1).text().trim()
						);
			} 

			if (cols.last().classNames().contains("ToWrd")) {
				entry.translations.add(cols.last().ownText().trim());
			} else if (cols.last().classNames().contains("FrEx") || 
					cols.last().classNames().contains("ToEx")) {
				entry.usage.add(cols.last().ownText().trim());
			}
		}
		if (entry != null) {
			result.entries.add(entry);
		}

		Elements pron = doc.select("#pronWR");
		if (pron.size() > 0) {
			result.pronunciation = pron.first().text().trim();
		}
	}

	@Override
	public DictionaryResult searchTerm(String term, int flags) throws IOException {
		String url = WordReferenceDictionary.QUERY_URL
			.replace("{dict}", mFromLanguage.code() + mToLanguage.code())
			.replace("{word}", term);

		Document doc = Jsoup.connect(url).get();
		if (doc.select("#noEntryFound").isEmpty()) {
			DictionaryResult result = 
				new DictionaryResult(term, mFromLanguage, mToLanguage);
			parseResult(doc, result);
			result.url = url;
			return result;
		} 

		return null;
	}

	public String toString() {
		return String.format("%s>%s", 
				mFromLanguage.code().toUpperCase(), mToLanguage.code().toUpperCase());
	}
}
