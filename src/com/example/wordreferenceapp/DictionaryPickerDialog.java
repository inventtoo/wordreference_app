package com.example.wordreferenceapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class DictionaryPickerDialog extends DialogFragment
	implements OnItemClickListener {

	private LanguageAdapter mAdapter;
	private DictionaryPickerListener mListener;

	private class LanguageAdapter extends BaseAdapter {
		class LanguageItem {
			Language language;
			Bitmap flag;
			boolean selected;
			boolean enabled;
		}
		private LanguageItem[] mLanguageItems;
		private int mNumberSelectedItems;
		private LanguageItem mSourceLanguage;
		private LanguageItem mTargetLanguage;

		private LayoutInflater mInflater;
		private AssetManager mAssetManager;

		public LanguageAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
			mAssetManager = context.getAssets();

			Language[] languages = Language.values();
			mLanguageItems = new LanguageItem[languages.length];
			for (int i = 0; i < languages.length; ++i) {
				LanguageItem item = new LanguageItem();
				String code = languages[i].code();
				String isocode = languages[i].isocode();
				try {
					InputStream stream = 
						mAssetManager.open("flags/32/" + code + ".png");
					item.flag = BitmapFactory.decodeStream(stream);
				} catch (IOException ioe) {
					item.flag = null;
				}
				item.language = languages[i];
				
				mLanguageItems[i] = item;
			}
			clear();
		}
		
		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}
		
		@Override
		public boolean isEnabled(int position) {
			return mLanguageItems[position].enabled;
		}

		@Override
		public int getCount() {
			return mLanguageItems.length;
		}

		private void clear() {
			mNumberSelectedItems = 0;
			mSourceLanguage = mTargetLanguage = null;
			for (LanguageItem item : mLanguageItems) {
				item.enabled = true;
				item.selected = false;
			}
		}

		private void disableIncompatibleLanguages(Language withLanguage) {
			List<Language> compatible = 
				WordReferenceDictionary.availableCombinationsWithSource(withLanguage);
			compatible.add(withLanguage);

			for (LanguageItem item : mLanguageItems) {
				item.enabled = compatible.contains(item.language);
			}
		}

		/** Select a maximum of two items and return true if the item is actually toggled. */
		public boolean toggleSelected(int position) {
			boolean toggled = false;
			LanguageItem item = mLanguageItems[position];

			if (item.selected) {
				/* deselect target language */
				if (mNumberSelectedItems == 2 && item == mTargetLanguage) {
					mTargetLanguage = null;
					item.selected = false;
					mNumberSelectedItems--;
					toggled = true;
					/* deselect source language -> restart */
				} else if (mNumberSelectedItems == 1) {
					clear();
					toggled = true;
				}
			} else {
				item.selected = true;
				/* select source language */
				if (mNumberSelectedItems == 0) {
					mSourceLanguage = item;
					mNumberSelectedItems = 1;
					disableIncompatibleLanguages(item.language);
					toggled = true;
				} else {
					/* select target language */
					if (mTargetLanguage != null) {
						/* deselect previous one */
						mTargetLanguage.selected = false;
					}
					mTargetLanguage = item;
					mNumberSelectedItems = 2;
					toggled = true;
				}
			} 

			if (toggled) {
				notifyDataSetChanged();
			}

			return toggled;
		}

		public Language getSourceLanguage() {
			return (mSourceLanguage != null) ? mSourceLanguage.language : null;
		}

		public Language getTargetLanguage() {
			return (mTargetLanguage != null) ? mTargetLanguage.language : null;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.country_grid_item, null);
				holder = new ViewHolder();
				holder.flag = (ImageView) convertView.findViewById(R.id.country_flag);
				holder.language = (TextView) convertView.findViewById(R.id.language_name);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			LanguageItem item = mLanguageItems[position];
			holder.language.setText(item.language.toString());
			holder.flag.setImageBitmap(item.flag);
			if (item.selected) {
				convertView.setBackgroundResource(android.R.color.holo_blue_light);
			} else {
				convertView.setBackgroundResource(0);
			}
			holder.language.setEnabled(item.enabled);
			if (item.enabled) {
				holder.flag.getDrawable().setAlpha(255);
			} else {
				holder.flag.getDrawable().setAlpha(55);
			}

			return convertView;
		}

		class ViewHolder {
			ImageView flag;
			TextView language;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
	}

	public interface DictionaryPickerListener {
		public void onDialogPositiveClick(DialogInterface dialog);
		public void onDialogNegativeClick(DialogInterface dialog);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (DictionaryPickerListener) activity;
		} catch (ClassCastException cce) {
			throw new ClassCastException(activity.toString() 
					+ " must implement DictionaryPickerListener");
		}
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();

		GridView gridView = (GridView) inflater.inflate(R.layout.country_grid, null);
		gridView.setOnItemClickListener(this);
		mAdapter = new LanguageAdapter(getActivity());
		gridView.setAdapter(mAdapter);

		builder.setView(gridView)
			.setTitle(R.string.translate_from)
			.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					Language sourceLanguage = mAdapter.getSourceLanguage();
					Language targetLanguage = mAdapter.getTargetLanguage();
					if (sourceLanguage != null && targetLanguage != null) {
						try {

							WordReferenceDictionary dictionary = 
									new WordReferenceDictionary(sourceLanguage, targetLanguage);
							PreferencesManager prefManager = 
									PreferencesManager.getInstance(getActivity());
							prefManager.setDefaultDictionary(dictionary);
						} catch (CombinationNotAvailableException cnae) {
							
						}
					}
					
					mListener.onDialogPositiveClick(dialog);
				}
			})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					mListener.onDialogNegativeClick(dialog);
				}
			});	

		AlertDialog dialog = builder.create();
		dialog.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				((AlertDialog) dialog).getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
			}
		});
		
		return dialog;
	}

	public void onCancel(DialogInterface dialog) {
		mListener.onDialogNegativeClick(dialog);
	}

	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		if (mAdapter.toggleSelected(position)) {
			/* the item was actually selected/deselected */
			Language sourceLanguage = mAdapter.getSourceLanguage();
			Language targetLanguage = mAdapter.getTargetLanguage();
			AlertDialog dialog = (AlertDialog) getDialog();

			if (targetLanguage != null) {
				String titleHolder = 
					getResources().getString(R.string.translate_from_lang_to_lang);
				dialog.setTitle(String.format(titleHolder, sourceLanguage.toString(), 
						targetLanguage.toString()));
				dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
			} else if (sourceLanguage != null) {
				String titleHolder = 
					getResources().getString(R.string.translate_from_lang_to);
				dialog.setTitle(String.format(titleHolder, sourceLanguage.toString()));
				dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
			} else {
				String title = getResources().getString(R.string.translate_from);
				dialog.setTitle(title);
				dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
			}
		}
	}
}
