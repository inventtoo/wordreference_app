package com.example.wordreferenceapp;

import java.io.IOException;
import java.util.List;

public abstract class LanguageDictionary {

	/** Perform the search in the opposite direction if no results are found */
	public static final int REVERSE_SEARCH_IF_POSSIBLE = 1;

	protected Language mFromLanguage;
	protected Language mToLanguage;

	public LanguageDictionary(Language fromLanguage, Language toLanguage) 
		throws CombinationNotAvailableException  {
		mFromLanguage = fromLanguage;
		mToLanguage = toLanguage;
	}

	/** Returns possible candidate terms in the dictionary for the query specified. */
	public abstract List<DictionaryTerm> getHints(String query) throws IOException;

	/** Returns alternative terms in the dictionary for a possible non-existent term. */
	public abstract List<DictionaryTerm> getAlternatives(String term) throws IOException;

	/** 
	 * Search operation.
	 *
	 * @param term the term to look up
	 * @param flags currently only REVERSE_SEARCH_IF_POSSIBLE
	 * @return the dictionary entries for the term searched or null if not found
	 */
	public abstract DictionaryResult searchTerm(String term, int flags) throws IOException;
}
