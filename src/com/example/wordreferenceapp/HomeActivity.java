package com.example.wordreferenceapp;

import android.os.Bundle;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.app.ActionBar;

public class HomeActivity extends SherlockActivity {
	private WordReferenceDictionary dictionary;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		try {
			this.dictionary = new WordReferenceDictionary(Language.ENGLISH, Language.SPANISH);
		} catch (CombinationNotAvailableException e) {
		}
		
		ActionBar bar = getSupportActionBar();
		bar.setDisplayShowTitleEnabled(false);
		
		setContentView(R.layout.home);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.home, menu);
		return true;
	}

}
