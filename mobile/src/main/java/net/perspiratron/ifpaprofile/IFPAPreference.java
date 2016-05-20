/*
 * Copyright (c) 2015, Brian Dols <brian.dols@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package net.perspiratron.ifpaprofile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class IFPAPreference extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static List<String> fragments = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		addPreferencesFromResource(R.xml.preferences);
		findPreference(getString(R.string.events_key)).setSummary(
				PreferenceManager.getDefaultSharedPreferences(this).getString(
						getString(R.string.events_key), "250") + " " + getString(R.string.units_from_you));
		setLocationSummary(PreferenceManager.getDefaultSharedPreferences(this));
//		if (hasHeaders()) {
//			Button button = new Button(this);
//			button.setText("action");
//			setListFooter(button);
//		}
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		super.onBuildHeaders(target);
//		loadHeadersFromResource(R.xml.preference_headers, target);
		fragments.clear();
		for (Header header : target) {
			fragments.add(header.fragment);
		}
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		// unused
		return fragments.contains(fragmentName);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("distance_preference")) {
			findPreference(key).setSummary(sharedPreferences.getString(key, "250") + " " + getString(R.string.units_from_you));
		}
		if (key.equals("locationMethod")) {
			((IFPAApplication) getApplication()).resetResults();
			setLocationSummary(sharedPreferences);
		}
	}

	private void setLocationSummary(SharedPreferences sharedPreferences) {
		String key = "locationMethod";
		if (sharedPreferences.getBoolean(key, true)) {
			findPreference(key).setSummary(R.string.manual_override);
			String manualQuery = sharedPreferences.getString("manualQuery", "");
			if (manualQuery != "") {
				findPreference("manualQuery").setSummary(manualQuery);
			}
		} else {
			findPreference(key).setSummary(getString(R.string.find_with_location_services));
		}
	}


	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	public static class IFPAPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			PreferenceManager.setDefaultValues(getActivity(),
					R.xml.preferences, false);
			addPreferencesFromResource(R.xml.preferences);
		}
	}
}
