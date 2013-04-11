package com.example.wordreferenceapp;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;
import java.util.regex.PatternSyntaxException;

/** Singleton object to handle preferences */
public class PreferencesManager {
	private static final String DEFAULT_DICTIONARY = "dictionary";
	private static final String PREF_FILE = "settings";

	private static PreferencesManager INSTANCE = null;
	private final SharedPreferences mPreferences;
	private WordReferenceDictionary mDictionary;

	private PreferencesManager(Context context) {
		mPreferences = context.getSharedPreferences(PREF_FILE, 0);
		mDictionary = null;
	}

	public synchronized static void createInstance(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new PreferencesManager(context);
		}
	}

	public static PreferencesManager getInstance(Context context) {
		createInstance(context);
		return INSTANCE;
	}

	public WordReferenceDictionary getDefaultDictionary() {
		if (mDictionary == null) {
			String dictionary = mPreferences.getString(DEFAULT_DICTIONARY, "");

			if (dictionary != "") {
				try {
					String[] languages = dictionary.split(">");
					if (languages.length == 2) {
						Language fromLanguage = Language.fromString(languages[0]);
						Language toLanguage = Language.fromString(languages[1]);
						if (fromLanguage != null && toLanguage != null) {
							mDictionary = new WordReferenceDictionary(fromLanguage, toLanguage);
						}
					}
				} catch (PatternSyntaxException pse) {
				} catch (CombinationNotAvailableException cnae) {
				}
			}
		}

		return mDictionary;
	}

	public void setDefaultDictionary(WordReferenceDictionary dictionary) {
		if (dictionary != mDictionary) {
			mDictionary = dictionary;
			SharedPreferences.Editor editor = mPreferences.edit();
			editor.putString(DEFAULT_DICTIONARY, mDictionary.toString());
			editor.commit();
		}
	}

	/** 
	 * Returns a dictionary based on the default language set on the device. 
	 *
	 * @param setDefault set the dictionary as default unless null is returned
	 * @return English-to-SystemLanguage if SystemLanguage is other than English, null otherwise.
	 **/
	public WordReferenceDictionary getDictionaryFromSystemLanguage(boolean setDefault) {
		Language systemLanguage = Language.fromString(Locale.getDefault().getLanguage());
		if (systemLanguage != null && systemLanguage != Language.ENGLISH) {
			try {
				WordReferenceDictionary dictionary = 
					new WordReferenceDictionary(Language.ENGLISH, systemLanguage);
				if (setDefault) {
					setDefaultDictionary(dictionary);
				}
				return dictionary;
			} catch (CombinationNotAvailableException cnae) {
			}
		}

		return null;
	}
}
