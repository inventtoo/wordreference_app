package com.example.wordreferenceapp;

import org.jsoup.HttpStatusException;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.commonsware.cwac.merge.MergeAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class SearchDictionaryTask extends AsyncTask<String, Void, DictionaryResult> {
	private static final String TAG = SearchDictionaryTask.class.getSimpleName();

	private WeakReference<AsyncTaskCompleteListener<DictionaryResult>> mCallback;
	private LanguageDictionary mDictionary;
	private int mFlags;
	private ApplicationError mError;

	public SearchDictionaryTask(
			AsyncTaskCompleteListener<DictionaryResult> callback,
			LanguageDictionary dictionary, 
			int flags) {
		mCallback = new WeakReference<AsyncTaskCompleteListener<DictionaryResult>>(callback);
		mDictionary = dictionary;
		mFlags = flags;
		mError = ApplicationError.OK;
	}

	@Override
	protected void onPreExecute() {
		if (mCallback.get() != null) {
			Activity context = (Activity) mCallback.get();
			ProgressBar progressCircle = (ProgressBar) context.findViewById(R.id.progress_circle);
			progressCircle.setVisibility(View.VISIBLE);
		} else {
			Log.d(TAG, "Reference is null");
		}
	}

	@Override
	protected DictionaryResult doInBackground(String... querys) {
		try {
			return mDictionary.searchTerm(querys[0], mFlags);
		} catch (HttpStatusException hse) {
			mError = ApplicationError.NET_SERVER_RESPONSE;
		} catch (SocketTimeoutException ste) {
			mError = ApplicationError.NET_SOCKET_TIMEOUT;
		} catch (IOException ioe) {
			mError = ApplicationError.NOT_DEFINED;
		}
		return null;
	}

	@Override
	protected void onPostExecute(DictionaryResult result) {
		if (mCallback.get() != null) {
			Activity context = (Activity) mCallback.get();
			ProgressBar progressCircle = (ProgressBar) context.findViewById(R.id.progress_circle);
			progressCircle.setVisibility(View.GONE);

			mCallback.get().onTaskComplete(result, mError);
		} else {
			Log.d(TAG, "Reference is null");
		}
	}
}

public class ResultActivity extends SherlockFragmentActivity 
	implements 
	AsyncTaskCompleteListener<DictionaryResult>, 
	DictionaryPickerDialog.DictionaryPickerListener,
	OnItemSelectedListener,
	ActionBar.TabListener {

	private WordReferenceDictionary mDictionary = null;
	private DictionaryResult mResult = null;
	private String mQuery = null;
	private ApplicationError mError = ApplicationError.OK;
	private int mSavedSpinnerPosition = -1;
	private ListAdapter[] mAdapters;
	private ListFragment[] mFragments;
	private int mCurSelectedTab = -1;

	private static final String KEY_RESULT = "com.example.wordreferenceapp.RESULT";
	private static final String KEY_TAB_SELECTED = "com.example.wordreferenceapp.TAB_SELECTED";
	private static final String KEY_ERROR = "com.example.wordreferenceapp.ERROR";
	private static final String KEY_QUERY = "com.example.wordreferenceapp.QUERY";
	private static final String KEY_DICTIONARY = "com.example.wordreferenceapp.DICTIONARY";

	private static final String ACTION_BUTTON_RETRY = "retry";
	private static final String ACTION_BUTTON_PICK_DICT = "pick_dict";
	private static final String TAG = ResultActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mDictionary = getDictionary();
		if (mDictionary == null) {
			DialogFragment dialog = new DictionaryPickerDialog();
			dialog.show(getSupportFragmentManager(), "DictionaryPickerDialog");
		}

		ActionBar actionBar = getSupportActionBar();
		actionBar.setCustomView(getDictionarySpinner());
		actionBar.setDisplayShowCustomEnabled(true);

		Intent intent = getIntent();
		if (savedInstanceState != null && 
				savedInstanceState.containsKey(KEY_RESULT)) {
			setContentView(R.layout.result);
			mResult = savedInstanceState.getParcelable(KEY_RESULT);
			loadResult();
			int selectedTab = savedInstanceState.getInt(KEY_TAB_SELECTED, -1);
			if (selectedTab != -1) {
				actionBar.setSelectedNavigationItem(selectedTab);
			}
		} else if (savedInstanceState != null && 
				savedInstanceState.containsKey(KEY_ERROR)) {
			setContentView(R.layout.result);
			mQuery = savedInstanceState.getString(KEY_QUERY);
			ApplicationError error = savedInstanceState.getParcelable(KEY_ERROR);
			showErrorDisplay(error);
		} else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			setContentView(R.layout.result);
			mQuery = intent.getStringExtra(SearchManager.QUERY);
			if (intent.hasExtra(KEY_DICTIONARY)) {
				doSearch((LanguageDictionary) intent.getParcelableExtra(KEY_DICTIONARY));
			} else {
				doSearch();
			}
		} else if (Intent.ACTION_MAIN.equals(intent.getAction())) {
			setContentView(R.layout.home);
			if (!connectionAvailable()) {
				String text = getString(R.string.msg_conn_unavailable);
				Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
			} 
		}
	}
	
	private void showCurrentDictionary() {
		if (mDictionary != null) {
			Toast.makeText(getApplicationContext(), 
					mDictionary.toString(), Toast.LENGTH_SHORT).show();
		}
	}
	
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		if (parent.getId() == R.id.dictionaries) {
			
			WordReferenceDictionary dictionary = 
					(WordReferenceDictionary) parent.getItemAtPosition(pos);
			
			if (dictionary != null) {
				if (mSavedSpinnerPosition >= 0) {
					PreferencesManager prefManager = 
							PreferencesManager.getInstance(getApplicationContext());
					prefManager.setDefaultDictionary(dictionary);

					showCurrentDictionary();
				}
				mSavedSpinnerPosition = pos;
			} else {
				int savedPosition = mSavedSpinnerPosition;
				mSavedSpinnerPosition = -1;
				parent.setSelection(savedPosition);
				/* More... */
				DialogFragment dialog = new DictionaryPickerDialog();
				dialog.show(getSupportFragmentManager(), "DictionaryPickerDialog");
			}
		} else if (parent.getId() == R.id.languages) {
			
			Language toLanguage = (Language) parent.getItemAtPosition(pos);
			if (toLanguage != mResult.toLanguage) {
				try {
					WordReferenceDictionary newDict = 
							new WordReferenceDictionary(mResult.fromLanguage, toLanguage);

					Intent intent = new Intent(this, ResultActivity.class);
					intent.setAction(Intent.ACTION_SEARCH);
					intent.putExtra(SearchManager.QUERY, mResult.term);
					intent.putExtra(KEY_DICTIONARY, newDict);
					startActivity(intent);
				} catch (CombinationNotAvailableException cnae) {
				}
			} 
		}
	}

	public void onNothingSelected(AdapterView<?> parent) {
		// Another interface callback
	}	
	
	private Spinner getDictionarySpinner() {
		Spinner dictionarySpinner = 
				(Spinner) LayoutInflater.from(this).inflate(R.layout.dictionary_spinner, null);
		
		if (mDictionary != null) {
			List<LanguageDictionary> dictionaries = new ArrayList<LanguageDictionary>();
			dictionaries.add(mDictionary);

			mSavedSpinnerPosition = -1;

			DictionarySpinnerAdapter adapter = 
					new DictionarySpinnerAdapter(this, dictionaries);
			//		Spinner dictionarySelector = new Spinner(this);
			dictionarySpinner.setAdapter(adapter);
			dictionarySpinner.setOnItemSelectedListener(this);
		}
		
		return dictionarySpinner;
	}
	
	private WordReferenceDictionary getDictionary() {
		PreferencesManager prefManager = 
				PreferencesManager.getInstance(getApplicationContext());
		WordReferenceDictionary dictionary = prefManager.getDefaultDictionary();
		
		if (dictionary == null) {
			dictionary = prefManager.getDictionaryFromSystemLanguage(false);
		}

		return dictionary;
	}
	
	private void doSearch() {
		doSearch(mDictionary);
	}
	
	private void doSearch(LanguageDictionary dictionary) {
		if (connectionAvailable()) {
			if (dictionary != null) {
				SearchDictionaryTask searchTask = 
					new SearchDictionaryTask(this, dictionary, 0);
				searchTask.execute(mQuery);
			} else {
				showErrorDisplay(ApplicationError.DICT_NOT_SPECIFIED);
			}
		} else {
			showErrorDisplay(ApplicationError.NET_CONN_UNAVAILABLE);
		}
	}

	private void showErrorDisplay(ApplicationError error) {
		View errorDisplay = (View) findViewById(R.id.error_display);

		if (errorDisplay.getVisibility() != View.VISIBLE) {
			TextView welcomeText = (TextView) (TextView) findViewById(R.id.welcome_text);
			if (welcomeText != null) {
				welcomeText.setVisibility(View.GONE);
			}
			
			Button errorButton = (Button) errorDisplay.findViewById(R.id.action_button);
			errorButton.setText(R.string.retry_button_title);
			errorButton.setTag(ACTION_BUTTON_RETRY);

			TextView errorMsg = (TextView) errorDisplay.findViewById(R.id.error_msg);

			switch (error) {
				case DICT_NOT_SPECIFIED:
					errorMsg.setText(R.string.msg_dict_not_specified);
					errorButton.setText(R.string.pick_dict_button_title);
					errorButton.setTag(ACTION_BUTTON_PICK_DICT);
					break;
				case NET_SOCKET_TIMEOUT:
					errorMsg.setText(R.string.msg_network_timeout);
					break;
				case NET_SERVER_RESPONSE:
					errorMsg.setText(R.string.msg_server_error);
					break;
				case NET_CONN_UNAVAILABLE:
					errorMsg.setText(R.string.msg_conn_unavailable);
					break;
				case NOT_DEFINED:
					errorMsg.setText(R.string.msg_error_unknown);
					break;
			}

			errorDisplay.setVisibility(View.VISIBLE);
		}

		mError = error;
	}

	private boolean connectionAvailable() {
		ConnectivityManager cm = 
			(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.result, menu);

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		MenuItem searchMenu = menu.findItem(R.id.action_search);
		SearchView searchView = (SearchView) searchMenu.getActionView();
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		searchView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS 
				| InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
		
		if (Intent.ACTION_MAIN.equals(getIntent().getAction())) {
			if (mDictionary != null && connectionAvailable()) {
				searchMenu.expandActionView();
				showCurrentDictionary();
			} 
		}

		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mResult != null) {
			MenuItem viewExternal = (MenuItem) menu.findItem(R.id.action_view_external);
			viewExternal.setVisible(true);
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_view_external:
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mResult.url));
				startActivity(intent);
				break;
		}
		return true;
	}
	
	public void actionOnError(View view) {
		LinearLayout errorDisplay = (LinearLayout) findViewById(R.id.error_display);
		errorDisplay.setVisibility(View.GONE);

		String tag = (String) view.getTag();
		if (tag.equals(ACTION_BUTTON_RETRY)) {
			doSearch();	
		} else if (tag.equals(ACTION_BUTTON_PICK_DICT)) {
			/* More... */
			DialogFragment dialog = new DictionaryPickerDialog();
			dialog.show(getSupportFragmentManager(), "DictionaryPickerDialog");
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		if (mError != ApplicationError.OK) {
			outState.putParcelable(KEY_ERROR, mError);
			if (mQuery != null) {
				outState.putString(KEY_QUERY, mQuery);
			}
		} else if (mResult != null) {
			outState.putParcelable(KEY_RESULT, mResult);
			if (mCurSelectedTab != -1) {
				outState.putInt(KEY_TAB_SELECTED, mCurSelectedTab);
			}
		}
	}
	
	private void loadResult() {
		class LanguageListAdapter extends ArrayAdapter<Language> {
			private int mFixedPosition;
			
			public LanguageListAdapter(Context context, int textViewResourceId, 
					List<Language> objects, int fixedPosition) {
				super(context, textViewResourceId, objects);
				mFixedPosition = fixedPosition;
			}
			
			private View getCustomView(int position, View convertView, ViewGroup parent) {
				TextView tv = (TextView) super.getView(position, convertView, parent);
				
				AssetManager am = getContext().getAssets();
				Language language = getItem(position);
				InputStream stream = null;
				try {
					stream = am.open("flags/32/" + language.code() + ".png");
					BitmapDrawable drawable = new BitmapDrawable(getContext().getResources(), stream);
					tv.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
					tv.setCompoundDrawablePadding(6);
				} catch (IOException ioe) {
				} finally {
					if (stream != null){
						try {
							stream.close();
						} catch (IOException e) {
						}
					}
				}
				return tv;
			}
			
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				return getCustomView(mFixedPosition, convertView, parent);
			}
			
			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				return getCustomView(position, convertView, parent);
			}
		}
		TextView tv = (TextView) findViewById(R.id.term);
		tv.setText(mResult.term);
		List<Language> languages = 
				WordReferenceDictionary.availableCombinationsWithSource(mResult.fromLanguage);
		
		Spinner sp = (Spinner) findViewById(R.id.languages);
		int languageSelected = languages.indexOf(mResult.toLanguage);
		sp.setAdapter(new LanguageListAdapter(this, R.layout.language_spinner_item, 
				languages, languageSelected));	
		sp.setOnItemSelectedListener(this);
		sp.setSelection(languageSelected);
		sp.setEnabled(languages.size() > 1);
		
		Iterator<DictionaryEntryBlock> principal = 
				mResult.iterator(WordReferenceDictionary.CATEGORY_PRINCIPAL);
		Iterator<DictionaryEntryBlock> additional = 
				mResult.iterator(WordReferenceDictionary.CATEGORY_ADDITIONAL);
		Iterator<DictionaryEntryBlock> compounds = 
				mResult.iterator(WordReferenceDictionary.CATEGORY_COMPOUND);

		mAdapters = new ListAdapter[2];
		mFragments = new ListFragment[2];
		
		if (principal.hasNext() && additional.hasNext()) {
			MergeAdapter adapter = new MergeAdapter();
			DictionaryEntryListAdapter adapter1 = 
				new DictionaryEntryListAdapter(this, mResult.term, principal);
			DictionaryEntryListAdapter adapter2 = 
				new DictionaryEntryListAdapter(this, mResult.term, additional);

			adapter1.setAdditionalItems(adapter2.getCount());
			adapter2.setAdditionalItems(adapter1.getCount());
			adapter2.setOffset(adapter1.getCount());
			
			TextView header = (TextView) LayoutInflater.from(this)
				.inflate(R.layout.section_header, null);
			header.setText(R.string.category_principal_header);
			adapter.addView(header);
			adapter.addAdapter(adapter1);
			
			header = (TextView) LayoutInflater.from(this)
				.inflate(R.layout.section_header, null);
			header.setText(R.string.category_additional_header);
			adapter.addView(header);
			adapter.addAdapter(adapter2);
			
			mAdapters[0] = adapter;
		} else if (principal.hasNext() || additional.hasNext()) {
			if (!compounds.hasNext()) {
				mAdapters[0] = new DictionaryEntryListAdapter(this, mResult);
			} else if (principal.hasNext()) {
				mAdapters[0] = new DictionaryEntryListAdapter(this, mResult.term, principal);
			} else {
				// very rare situation, but...
				mAdapters[0] = new DictionaryEntryListAdapter(this, mResult.term, additional);
			}
		} else {
			mAdapters[0] = new DictionaryEntryListAdapter(this, mResult.term, 
					mResult.iterator(LanguageDictionary.CATEGORY_DEFAULT));
		}
		
		if (compounds.hasNext()) {
			mAdapters[1] = new DictionaryEntryListAdapter(this, mResult.term, compounds);
			if (mAdapters[0] != null) {
				DictionaryEntryListAdapter adapter = (DictionaryEntryListAdapter) mAdapters[1];
				adapter.setOffset(mAdapters[0].getCount());
				adapter.setAdditionalItems(mAdapters[0].getCount());
			}
		}

		if (mAdapters[0] != null && mAdapters[1] != null) {
			ActionBar actionBar = getSupportActionBar();
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			ActionBar.Tab tab1 = actionBar
					.newTab()
					.setText(R.string.category_single_header)
					.setTabListener(this);
			ActionBar.Tab tab2 = actionBar
					.newTab()
					.setText(R.string.category_compound_header)
					.setTabListener(this);
			actionBar.addTab(tab1);
			actionBar.addTab(tab2);
		} else {
			ListView list = (ListView) findViewById(R.id.dictionary_entries);
			list.setAdapter(mAdapters[0]);
		}
	}
	
	public void onTaskComplete(DictionaryResult result, ApplicationError error) {
		mResult = result;
		mError = error;

		if (result != null) {
			loadResult();
			ActivityCompat.invalidateOptionsMenu(this);
		} else if (error == ApplicationError.OK) {
			// TODO: show dialog with alternatives
			Toast.makeText(getApplicationContext(), 
					"No translation found", Toast.LENGTH_SHORT)
				.show();
		} else {
			showErrorDisplay(error);
		}
	}

	@Override
	public void onDialogPositiveClick(DialogInterface dialog) {
		PreferencesManager prefManager = 
				PreferencesManager.getInstance(getApplicationContext());
		mDictionary = prefManager.getDefaultDictionary();
		getSupportActionBar().setCustomView(getDictionarySpinner());
		
		showCurrentDictionary();
	}

	@Override
	public void onDialogNegativeClick(DialogInterface dialog) {
		if (mDictionary == null) {
			showErrorDisplay(ApplicationError.DICT_NOT_SPECIFIED);
		}
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		// When the given tab is selected, show the tab contents in the
	    // container view.
		int position = tab.getPosition();
		if (mFragments[position] == null) {
			mFragments[position] = new ListFragment();
			mFragments[position].setListAdapter(mAdapters[position]);
			ft.add(R.id.content, mFragments[position]);
		} 
		if (mCurSelectedTab != -1) {
			ft.hide(mFragments[mCurSelectedTab]);
		}
		ft.show(mFragments[position]);
	//	ft.commit();
		mCurSelectedTab = position;
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		
	}
}
