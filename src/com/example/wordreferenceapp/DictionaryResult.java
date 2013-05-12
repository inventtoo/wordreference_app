package com.example.wordreferenceapp;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


public class DictionaryResult implements Parcelable {
	String term;
	String pronunciation;
	Language fromLanguage;
	Language toLanguage;
	List<DictionaryEntryBlock> entries;
	String url;

	public static final Parcelable.Creator<DictionaryResult> CREATOR = 
		new Parcelable.Creator<DictionaryResult>() {
		public DictionaryResult createFromParcel(Parcel in) {
			return new DictionaryResult(in);
		}
		public DictionaryResult[] newArray(int size) {
			return null;
		}
	};
	
	private class DictionaryEntryBlockIterator implements Iterator<DictionaryEntryBlock> {
		private Iterator<DictionaryEntryBlock> mIterator;
		private int[] mCategories;
		private DictionaryEntryBlock mNext;
		
		public DictionaryEntryBlockIterator(List<DictionaryEntryBlock> list, int[] categories) {
			mIterator = list.iterator();
			mCategories = categories.clone();
			Arrays.sort(mCategories);
			mNext = null;
		}
		
		@Override
		public boolean hasNext() {
			if (mNext == null) {
				boolean matchCategory = false;
				while (!matchCategory && mIterator.hasNext()) {
					mNext = mIterator.next();
					matchCategory = (Arrays.binarySearch(mCategories, mNext.category) >= 0);
					
				}
				if (!matchCategory) {
					mNext = null;
				}
				return matchCategory;
			}

			return true;
		}

		@Override
		public DictionaryEntryBlock next() {
			if (mNext == null && !hasNext()) {
				throw new NoSuchElementException();
			} 
			
			DictionaryEntryBlock next = mNext;
			mNext = null;
			
			return next;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private DictionaryResult(Parcel in) {
		readFromParcel(in);
	}

	public DictionaryResult(String term, Language fromLanguage, Language toLanguage) {
		this.term = term;
		this.fromLanguage = fromLanguage;
		this.toLanguage = toLanguage;
		this.pronunciation = "";
		this.entries = new ArrayList<DictionaryEntryBlock>();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.term);
		out.writeString(this.pronunciation);
		out.writeString(this.fromLanguage.code());
		out.writeString(this.toLanguage.code());
		out.writeTypedList(this.entries);
		out.writeString(this.url);
	}

	private void readFromParcel(Parcel in) {
		this.term = in.readString();
		this.pronunciation = in.readString();
		this.fromLanguage = Language.fromString(in.readString());
		this.toLanguage = Language.fromString(in.readString());
		this.entries = new ArrayList<DictionaryEntryBlock>();
		in.readTypedList(this.entries, DictionaryEntryBlock.CREATOR);
		this.url = in.readString();
	}
		
	public Iterator<DictionaryEntryBlock> iterator(int... categories) {
		return new DictionaryEntryBlockIterator(entries, categories);
	}
};
