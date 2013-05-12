package com.example.wordreferenceapp;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DictionaryEntryListAdapter extends BaseAdapter {
	private LayoutInflater mInflater;
	private List<DictionaryEntryBlock> mData;
	private String mTerm;
	private Spannable[] mText;
	private Context mContext;
	private int mOffset;
	private int mAdditionalItems;
	
	public DictionaryEntryListAdapter(Context context, DictionaryResult result) {
		mData = result.entries;
		mTerm = result.term;
		
		init(context);
	}
	
	public DictionaryEntryListAdapter(Context context, String term, 
			Iterator<DictionaryEntryBlock> entries) {
		mData = new ArrayList<DictionaryEntryBlock>();
		while (entries.hasNext()) {
			mData.add(entries.next());
		}
		mTerm = term;
		
		init(context);
	}
	
	// general initialization stuff
	private void init(Context context) {
		mText = new Spannable[mData.size()];
		mInflater = LayoutInflater.from(context);
		mContext = context;
		setOffset(0);
		setAdditionalItems(0);
	}

	/** Set the start position number for the entries handled by the adapter. */
	public void setOffset(int offset) {
		mOffset = offset;
	}

	/** Set the number of additional items in case the adapter is combined with others. */
	public void setAdditionalItems(int additionalItems) {
		mAdditionalItems = additionalItems;
	}
	
	private int getThemeColors(Context context, int attr) {
		return context.getResources().getColor(getThemeResourceId(context, attr));
	}

	private int getThemeResourceId(Context context, int attr) {
		TypedValue typedvalueattr = new TypedValue();
		context.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView textView;
		if (convertView == null) {
			convertView = mInflater.inflate(android.R.layout.simple_list_item_1, null);
			convertView.setPadding(12, 6, 12, 6);
			textView = (TextView) convertView.findViewById(android.R.id.text1);
			textView.setTextAppearance(mContext, android.R.style.TextAppearance_Small);
			convertView.setTag(textView);
		} else {
			textView = (TextView) convertView.getTag();
		}
		
		Spannable styledText;
		if (mText[position] == null) {
			DictionaryEntryBlock entry = mData.get(position);

			int lenPos;
			int numItems = mData.size() + mAdditionalItems;
			if (numItems < 10) {
				lenPos = 1;
			} else if (numItems < 100) {
				lenPos = 2;
			} else {
				lenPos = 3;
			} // more than 999 entries?
			SpannableStringBuilder text = 
				new SpannableStringBuilder(String.format("%"+lenPos+"d.", position+mOffset+1));
			text.setSpan(new TypefaceSpan("monospace"), 0, text.length()-1, 0);

			if (entry.pos != null && entry.pos.length() > 0) {
				int length = text.length();
				text.append(" ").append(entry.pos).append("  ");

				PreferencesManager prefManager = PreferencesManager.getInstance(mContext);
				text.setSpan(new BackgroundColorSpan(prefManager.getTagColor(entry.pos)),
						length, length+entry.pos.length()+2, 0);
				text.setSpan(new ForegroundColorSpan(Color.WHITE),
						length, length+entry.pos.length()+2, 0);
			}

			boolean collapse = true;
			if (!mTerm.equals(entry.term)) {
				int length = text.length();
				text.append(entry.term).append(" ");
				text.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
						length, length+entry.term.length(), 0);

				collapse = false;
			}
			if (entry.sense != null && entry.sense.length() > 0) {
				int length = text.length();
				text.append("| ").append(entry.sense);
				text.setSpan(new ForegroundColorSpan( 
						getThemeColors(mContext, android.R.attr.textColorTertiary)), 
						length, length+2+entry.sense.length(), 0);
				collapse = false;
			}

			if (collapse) {
				int numTrans = entry.translations.size();
				for (int i = 0; i < numTrans && collapse; ++i) {
					DictionaryEntryBlock.DictionaryEntry tran = entry.translations.get(i);
					if (tran.sense != null && tran.sense.length() > 0) {
						collapse = false;
					}
				}
			}
			if (!collapse) {
				text.append("\n\t");
			}

			if (!entry.translations.isEmpty()) {
				newEntryLine(text, entry.translations.get(0));
			}
			int numTrans = entry.translations.size();
			for (int i = 1; i < numTrans; ++i) {
				DictionaryEntryBlock.DictionaryEntry prev = entry.translations.get(i-1);
				DictionaryEntryBlock.DictionaryEntry tran = entry.translations.get(i);
				String separator = 
					(prev.sense != null && prev.sense.length() > 0) ? "\n\t" : ", ";
				text.append(separator);
				newEntryLine(text, tran);
			} 

			styledText = text;
			mText[position] = styledText;
		} else {
			styledText = mText[position];
		}
		
		textView.setText(styledText, BufferType.SPANNABLE);

		return convertView;
	}
	
	private void newEntryLine(SpannableStringBuilder text, 
			DictionaryEntryBlock.DictionaryEntry tran) {
		text.append(tran.term);
		if (tran.sense != null && tran.sense.length() > 0) {
			int length = text.length();
			text.append(" | ").append(tran.sense);
			text.setSpan(new ForegroundColorSpan( 
					getThemeColors(mContext, android.R.attr.textColorTertiary)), 
					length, length+3+tran.sense.length(), 0);
		}
	}
	
	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public Object getItem(int position) {
		return mData.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
}
