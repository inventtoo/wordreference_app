package com.example.wordreferenceapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.os.Parcel;
import android.os.Parcelable;

public class WordReferenceDictionary extends LanguageDictionary 
implements Parcelable{

	public static final int CATEGORY_PRINCIPAL  = 0;
	public static final int CATEGORY_ADDITIONAL = 1;
	public static final int CATEGORY_COMPOUND   = 2;

	private static final String TAG = WordReferenceDictionary.class.getSimpleName();
	
	private static final String QUERY_URL = 
//		"http://www.wordreference.com/redirect/translation.aspx?w={word}&dict={dict}";
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
	
	public static final Parcelable.Creator<WordReferenceDictionary> CREATOR =
			new Parcelable.Creator<WordReferenceDictionary>() {

				@Override
				public WordReferenceDictionary createFromParcel(Parcel source) {
					try {
						return new WordReferenceDictionary(source);
					} catch (CombinationNotAvailableException cnae) {
						// A dictionary that exists in a Parcel must have 
						// been a valid dictionary
					}
					return null;
				}

				@Override
				public WordReferenceDictionary[] newArray(int size) {
					return null;
				}
			};
			
	private WordReferenceDictionary(Parcel in) 
		throws CombinationNotAvailableException {
			super(Language.fromString(in.readString()), 
					Language.fromString(in.readString()));
	}

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
		DictionaryEntryBlock entry = null;
		int category = LanguageDictionary.CATEGORY_DEFAULT;
		for (Element row : rows) {
			if (row.hasClass("wrtopsection")) {
				// new section
				String text = row.text().toLowerCase();
				if (text.contains("principal")) {
					category = WordReferenceDictionary.CATEGORY_PRINCIPAL;
				} else if (text.contains("additional")) {
					category = WordReferenceDictionary.CATEGORY_ADDITIONAL;
				} else if (text.contains("compound")) {
					category = WordReferenceDictionary.CATEGORY_COMPOUND;
				}
			} 

			Elements cols = row.select("td");
			if (cols.size() == 3 && cols.first().hasClass("FrWrd")) {
				// new entry
				if (entry != null) {
					result.entries.add(entry);
				}
				String term = cols.get(0).select("strong").first().ownText().trim();
				String sense = cols.get(1).ownText().trim();
				Elements elem = cols.get(1).select(".Fr2");
				if (elem.size() > 0) {
					sense = elem.first().text().trim() + " " + sense;
				}
				if (sense.startsWith("(") && sense.endsWith(")")) {
					sense = sense.substring(1, sense.length() - 1);
				}

				entry = new DictionaryEntryBlock(term, sense);

				entry.category = category;
				elem = cols.get(0).select(".POS2");
				if (elem.size() > 0) {
					entry.pos = elem.first().text().trim();
				}
			} 

			if (cols.last().hasClass("ToWrd")) {
				String term = cols.last().ownText().trim();
				DictionaryEntryBlock.DictionaryEntry tran = 
					new DictionaryEntryBlock.DictionaryEntry(term);
				Elements elem = cols.last().select(".POS2");
				if (elem.size() > 0) {
					tran.pos = elem.first().text().trim();
				}
				if (cols.get(1).hasClass("To2")) {
					tran.sense = cols.get(1).ownText().trim();
				} else {
					elem = cols.get(1).select(".To2");
					if (elem.size() > 0) {
						tran.sense = elem.first().ownText().trim();
					}
				}
				if (tran.sense != null && 
						tran.sense.startsWith("(") && tran.sense.endsWith(")")) {
					tran.sense = tran.sense.substring(1, tran.sense.length() - 1);
				}

				entry.translations.add(tran);
			} /*else if (cols.last().hasClass("FrEx") || 
					cols.last().hasClass("ToEx")) {
				entry.usage.add(cols.last().ownText().trim());
			} */
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
			.replace("{word}", URLEncoder.encode(term, "utf-8"));

		Connection conn = Jsoup.connect(url);
		Document doc = conn.get();
		if (doc.select("#noEntryFound").isEmpty()) {
			boolean resultOk = true;
			Elements link = doc.select("div#nav a");
			if (!link.isEmpty()) {
				String dict = link.first().attr("href");
				if (dict.matches("^/[a-z]{2}/[a-z]{2}/.*")) {
					dict = dict.substring(1,3) + dict.substring(4,6);
				} else if (dict.matches("^/[a-z]{4}/$")) {
					dict = dict.substring(1,5);	
				} else {
					dict = null;
				}
				
				if (dict != null) {
					resultOk = mFromLanguage.code().equals(dict.substring(0,2)) ||
							mFromLanguage.code().equals(dict.substring(2,4)) && 
							(flags & LanguageDictionary.REVERSE_SEARCH_IF_POSSIBLE) != 0;
				}
			}
				
			if (resultOk) {
				DictionaryResult result = 
						new DictionaryResult(term, mFromLanguage, mToLanguage);
				parseResult(doc, result);
				result.url = url;
				return result;
			}
		} 


		return null;
	}

	public String toString() {
		return String.format("%s>%s", 
				mFromLanguage.code().toUpperCase(), mToLanguage.code().toUpperCase());
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mFromLanguage.code());
		dest.writeString(mToLanguage.code());
	}
}
