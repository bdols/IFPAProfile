/*
 * Copyright (c) 2015, Brian Dols <brian.dols@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package net.perspiratron.ifpaprofile;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

	private ProfileFragment profileFragment;
	private CalendarFragment calendarFragment;
	private SearchView searchView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		if (savedInstanceState == null) {
			Log.d("ifpacreate", "savedInstanceState is null");

			Intent intent = getIntent();
			int profile_id = 0;
			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				String[] segments = intent.getDataString().split("\\?");
				if (segments.length > 0) {
					Log.d("ifpaurl", intent.getDataString() + " : " + segments[0] + " : " + segments[1]);
					String phpish = segments[1];
					profile_id = Integer.valueOf(phpish.replace("p=", ""));
				}
			} else {
				profile_id = intent.getIntExtra("profile_id", 0);
			}
			profileFragment = new ProfileFragment();
			calendarFragment = new CalendarFragment();
			if (profile_id > 0) {
				Bundle bundleProfile = new Bundle();
				bundleProfile.putInt("profile_id", profile_id);
				profileFragment.setArguments(bundleProfile);
				getSupportFragmentManager().beginTransaction()
						.add(R.id.container, profileFragment)
						.commit();
			} else {
				getSupportFragmentManager().beginTransaction()
						.add(R.id.container, calendarFragment)
						.commit();
			}
		} else {
			Log.d("ifpacreate", "savedInstanceState is not null");

			profileFragment = (ProfileFragment) getSupportFragmentManager().getFragment(
					savedInstanceState, ProfileFragment.class.getName());
			if (profileFragment == null) {
				profileFragment = new ProfileFragment();
				Log.d("ifpacreate", "new profile fragment" + profileFragment.isVisible());
			} else {
				Log.d("ifpacreate", "restored profile fragment" + profileFragment.isVisible());
			}
			calendarFragment = (CalendarFragment) getSupportFragmentManager().getFragment(
					savedInstanceState, CalendarFragment.class.getName());
			if (calendarFragment == null) {
				calendarFragment = new CalendarFragment();
				Log.d("ifpacreate", "new calendar fragment" + calendarFragment.isVisible());
			} else {
				Log.d("ifpacreate", "restored calendar fragment" + calendarFragment.isVisible());
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		for (Fragment f : getSupportFragmentManager().getFragments()) {
//			if (f.isVisible()) {}
			if (f.getClass() == ProfileFragment.class) {
				getSupportFragmentManager().putFragment(outState, ProfileFragment.class.getName(),
						profileFragment);
				Log.d("ifpacreate", "saving profileFragment" + profileFragment);
			} else if (f.getClass() == CalendarFragment.class) {
				getSupportFragmentManager().putFragment(outState, CalendarFragment.class.getName(),
						calendarFragment);
				Log.d("ifpacreate", "saving calendarFragment" + calendarFragment);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
		if (searchView != null) {
			SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
			SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
			searchView.setSearchableInfo(info);
			searchView.setIconifiedByDefault(true);
			searchView.setIconified(true);
//			searchView.setSubmitButtonEnabled(true);
			searchView.setOnQueryTextListener(this);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		switch (id) {
			case R.id.action_settings:
				Intent myIntent = new Intent();
				myIntent.setClass(this, IFPAPreference.class);
				myIntent.addCategory(Intent.CATEGORY_PREFERENCE);
				startActivityForResult(myIntent, 1);
				return true;

			case R.id.action_calendar:
				FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
				Log.d("ifpacreate", "calendar req profile" + profileFragment);
				if (profileFragment != null) {

					Log.d("ifpacreate", "calendar req " + profileFragment.isVisible() + profileFragment.isAdded());
				}
				if (profileFragment != null && profileFragment.isVisible()) { // TODO: test isAdded()
					fragmentTransaction.remove(profileFragment);
				}
				Log.d("ifpacreate", "calendar req calfrag" + calendarFragment);
				if (calendarFragment != null) {
					Log.d("ifpacreate", "calendar req calvis" + calendarFragment.isVisible() + calendarFragment.isAdded());
				}

				if (calendarFragment != null && calendarFragment.isAdded()) {
					fragmentTransaction.show(calendarFragment);
				} else {
					fragmentTransaction.add(R.id.container, calendarFragment);
				}
				fragmentTransaction.commit();
				Log.d("ifpacreate", "calendar req postcommit " + profileFragment.isVisible() + profileFragment.isAdded());
				break;
			case R.id.action_search:
				searchView.setIconified(false);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onQueryTextSubmit(String s) {
		return false;
	}

	@Override
	public boolean onQueryTextChange(String s) {
		return false;
	}


}
