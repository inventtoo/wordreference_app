package com.example.wordreferenceapp;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class DictionaryEntryBlock implements Parcelable {
	String term;
	String pos;  /* Part of sentence: grammatical function */
	String sense;
	int category;
	List<DictionaryEntry> translations;

	public static final Parcelable.Creator<DictionaryEntryBlock> CREATOR = 
		new Parcelable.Creator<DictionaryEntryBlock>() {
			public DictionaryEntryBlock createFromParcel(Parcel in) {
				return new DictionaryEntryBlock(in);
			}
			public DictionaryEntryBlock[] newArray(int size) {
				return null;
			}
		};

	public static class DictionaryEntry implements Parcelable {
		String term;
		String pos;  /* Part of sentence: grammatical function */
		String sense;

		public static final Parcelable.Creator<DictionaryEntry> CREATOR = 
			new Parcelable.Creator<DictionaryEntry>() {
			public DictionaryEntry createFromParcel(Parcel in) {
				return new DictionaryEntry(in);
			}
			public DictionaryEntry[] newArray(int size) {
				return null;
			}
		};

		private DictionaryEntry(Parcel in) {
			readFromParcel(in);
		}

		public DictionaryEntry(String term) {
			this.term = term;
			this.pos = null;
			this.sense = null;
		}

		private void readFromParcel(Parcel in) {
			this.term = in.readString();
			this.pos = in.readString();
			this.sense = in.readString();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeString(this.term);
			out.writeString(this.pos);
			out.writeString(this.sense);
		}
	}

	private DictionaryEntryBlock(Parcel in) {
		readFromParcel(in);
	}

	public DictionaryEntryBlock(String term) {
		this(term, null);
	}

	public DictionaryEntryBlock(String term, String sense) {
		this.term = term;
		this.sense = sense;
		this.pos = null;
		this.category = LanguageDictionary.CATEGORY_DEFAULT;
		this.translations = new ArrayList<DictionaryEntry>();
	}

	private void readFromParcel(Parcel in) {
		this.term = in.readString();
		this.pos = in.readString(); 
		this.sense = in.readString();
		this.category = in.readInt();
		this.translations = new ArrayList<DictionaryEntry>();
		in.readTypedList(this.translations, DictionaryEntry.CREATOR);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.term);
		out.writeString(this.pos);
		out.writeString(this.sense);
		out.writeInt(this.category);
		out.writeTypedList(this.translations);
	}
}
