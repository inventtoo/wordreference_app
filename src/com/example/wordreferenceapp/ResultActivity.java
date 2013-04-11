package com.example.wordreferenceapp;

import org.jsoup.HttpStatusException;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
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
	implements AsyncTaskCompleteListener<DictionaryResult>, OnItemSelectedListener,
	DictionaryPickerDialog.DictionaryPickerListener {

	private WordReferenceDictionary mDictionary = null;
	private DictionaryResult mResult = null;
	private String mQuery = null;
	private int mSavedSpinnerPosition;

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

		ActionBar bar = getSupportActionBar();
		bar.setCustomView(getDictionarySpinner());
		bar.setDisplayShowCustomEnabled(true);

		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			setContentView(R.layout.result);
			mQuery = intent.getStringExtra(SearchManager.QUERY);
			doSearch();
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
	
	/* Called when an item is selected on the dictionary spinner */
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		if (pos+1 < parent.getCount()) {
			if (mSavedSpinnerPosition >= 0) {
				WordReferenceDictionary dictionary = 
						(WordReferenceDictionary) parent.getItemAtPosition(pos);
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
			/* the last item acts as a sentinel object to show dictionary picker dialog */
			dictionaries.add(null);

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
		if (connectionAvailable()) {
			if (mDictionary != null) {
				SearchDictionaryTask searchTask = 
					new SearchDictionaryTask(this, mDictionary, 0);
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
	}

	private boolean connectionAvailable() {
		ConnectivityManager cm = 
			(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
	}
	
	/*
	private void showKeyboard(MenuItem menu) {
		menu.setOnActionExpandListener(new OnActionExpandListener() {
	        @Override
	        public boolean onMenuItemActionCollapse(MenuItem item) {
	            // Do something when collapsed
	            return true;  // Return true to collapse action view
	        }

	        @Override
	        public boolean onMenuItemActionExpand(MenuItem item) {
	            //get focus
	            item.getActionView().requestFocus();
	            //get input method
	            InputMethodManager imm = 
	            		(InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
	            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
	            return true;  // Return true to expand action view
	        }
	    });
	}
	*/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.result, menu);

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		MenuItem searchMenu = menu.findItem(R.id.action_search);
		SearchView searchView = (SearchView) searchMenu.getActionView();
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		
		if (Intent.ACTION_MAIN.equals(getIntent().getAction())) {
			if (mDictionary != null && connectionAvailable()) {
				searchMenu.expandActionView();
				showCurrentDictionary();
				//showKeyboard(searchMenu);
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
	
	private void loadResult() {
		TextView term = (TextView) findViewById(R.id.term_searched);
		TextView translations = (TextView) findViewById(R.id.translation_overview);

		term.setText(mResult.term);
		translations.setText(mResult.entries.toString());
	}
	
	public void onTaskComplete(DictionaryResult result, ApplicationError error) {
		mResult = result;

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
}
