package com.example.wordreferenceapp;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DictionarySpinnerAdapter extends BaseAdapter {
	private LayoutInflater mInflater;
	private List<LanguageDictionary> mData;
	private AssetManager mAssetManager;
	
	public DictionarySpinnerAdapter(Context context, List<LanguageDictionary> objects) {
		mInflater = LayoutInflater.from(context);
		mAssetManager = context.getAssets();
		mData = objects;
	}
	
	private View getFooterView(View convertView) {
		if (convertView == null) {
			convertView = mInflater.inflate(android.R.layout.simple_list_item_1, null);
		}
		TextView tv = (TextView) convertView.findViewById(android.R.id.text1);
		tv.setText(R.string.more);
		
		return convertView;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position+1 == getCount()) {
			return getFooterView(convertView);
		}

		if (convertView == null) {
			convertView = mInflater.inflate(android.R.layout.simple_list_item_1, null);
		} 
		
		TextView tv = (TextView) convertView.findViewById(android.R.id.text1);
		tv.setText(mData.get(position).toString());

		return convertView;
	}
	
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return getView(position, convertView, parent);
	}

	@Override
	public int getCount() {
		return mData.size()+1;
	}

	@Override
	public Object getItem(int position) {
		return (position < mData.size()) ? mData.get(position) : null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
}
