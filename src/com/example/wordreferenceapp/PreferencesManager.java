package com.example.wordreferenceapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.util.Locale;
import java.util.regex.PatternSyntaxException;

/** Singleton object to handle preferences */
public class PreferencesManager {
	private static final String LAST_COLOR = "color._last_";
	private static final String COLOR_PREFIX = "color.";
	private static final String DEFAULT_DICTIONARY = "dictionary";
	private static final String PREF_FILE = "settings";

	private static PreferencesManager INSTANCE = null;
	private final SharedPreferences mPreferences;
	private WordReferenceDictionary mDictionary;
	private int[] mColors;

	private PreferencesManager(Context context) {
		mPreferences = context.getSharedPreferences(PREF_FILE, 0);
		mDictionary = null;

		mColors = context.getResources().getIntArray(R.array.colors);
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
	 * Return a dictionary based on the default language set on the device. 
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

	/** 
	 * Assign a color to a char sequence and return that color. 
	 *
	 * @return always the same color for a certain tag 
	 **/
	public int getTagColor(String tag) {
		int color;

		String key = COLOR_PREFIX + tag.replace(' ', '_');
		if (mPreferences.contains(key)) {
			color = mPreferences.getInt(key, 0);
		} else {
			int lastColor = mPreferences.getInt(LAST_COLOR, -1);
			int colorIndex = (lastColor+1) % mColors.length; 

			color = mColors[colorIndex];

			SharedPreferences.Editor editor = mPreferences.edit();
			editor.putInt(key, color); 
			editor.putInt(LAST_COLOR, colorIndex);
			editor.commit();
		}

		return color;
	}
}
