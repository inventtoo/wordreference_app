package com.example.wordreferenceapp;

import org.jsoup.HttpStatusException;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.actionbarsherlock.widget.SearchView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;

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

public class ResultActivity extends SherlockActivity 
	implements AsyncTaskCompleteListener<DictionaryResult> {
	private WordReferenceDictionary mDictionary;
	private DictionaryResult mResult;
	private String mQuery;
	private static final String TAG = ResultActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayShowTitleEnabled(false);
		
		mResult = null;
		mQuery = null;

		try {
			mDictionary = new WordReferenceDictionary(Language.ENGLISH, Language.SPANISH);

			Intent intent = getIntent();
			if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
				mQuery = intent.getStringExtra(SearchManager.QUERY);
				setContentView(R.layout.result);
				doSearch();
			} else if (Intent.ACTION_MAIN.equals(intent.getAction())) {
				setContentView(R.layout.home);
				if (!connectionAvailable()) {
					String text = getString(R.string.msg_conn_unavailable);
					Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
				}
			}
		} catch (CombinationNotAvailableException cnae) {
		}	
	}

	private void doSearch() {
		if (connectionAvailable()) {
			SearchDictionaryTask searchTask = 
				new SearchDictionaryTask(this, mDictionary, 0);
			searchTask.execute(mQuery);
		} else {
			showErrorDisplay(ApplicationError.NET_CONN_UNAVAILABLE);
		}
	}

	private void showErrorDisplay(ApplicationError error) {
		TextView errorMsg = (TextView) findViewById(R.id.error_msg);
		switch (error) {
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
		View errorDisplay = (View) findViewById(R.id.error_display);
		errorDisplay.setVisibility(View.VISIBLE);
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
			if (connectionAvailable()) {
				searchMenu.expandActionView();
				//showKeyboard(searchMenu);
			} else {
			//	searchView.setIconifiedByDefault(false);
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
	
	public void retrySearch(View view) {
		LinearLayout errorDisplay = (LinearLayout) findViewById(R.id.error_display);
		errorDisplay.setVisibility(View.GONE);
		doSearch();	
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
			// TODO: show dialog with closest matches
			Toast.makeText(getApplicationContext(), 
					"No translation found", Toast.LENGTH_SHORT)
				.show();
		} else {
			showErrorDisplay(error);
		}
	}
}
