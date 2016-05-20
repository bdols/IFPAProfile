/*
 * Copyright (c) 2015, Brian Dols <brian.dols@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package net.perspiratron.ifpaprofile;


import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.HashMap;

enum TrackerName {
	APP_TRACKER, // Tracker used only in this app.
	GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
	ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
}

public class IFPAApplication extends Application {
	static HashMap<TrackerName, Tracker> mTrackers = new HashMap<>();
	private String searchQuery;
	private ArrayList<CalendarEntry> entriesList;
	private HashMap<String, Long> searchTimestamps;
	private Location lastLocation;
	private Long entriesTimestamp;

	static synchronized Tracker getTracker(Context context) {
		if (!mTrackers.containsKey(TrackerName.APP_TRACKER)) {
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
			Tracker t = analytics.newTracker(R.xml.app_tracker);
			mTrackers.put(TrackerName.APP_TRACKER, t);
		}

		return mTrackers.get(TrackerName.APP_TRACKER);
	}

	public static void logAnalyticsHit(Class clazz, Context context) {
		Tracker tracker = getTracker(context);
		tracker.setScreenName(clazz.getCanonicalName());
		tracker.send(new HitBuilders.AppViewBuilder().build());
	}

	public void saveCalendarEntries(String searchQuery, ArrayList<CalendarEntry> entriesList,
	                                Location lastLocation, long entriesTimestamp,
	                                HashMap<String, Long> searchTimestamps) {
		this.searchQuery = searchQuery;
		this.entriesList = entriesList;
		this.lastLocation = lastLocation;
		this.entriesTimestamp = entriesTimestamp;
		this.searchTimestamps = searchTimestamps;
	}

	public ArrayList<CalendarEntry> getEntriesList() {
		return entriesList;
	}

	public Location getLastLocation() {
		return lastLocation;
	}

	public Long getEntriesTimestamp() {
		return entriesTimestamp;
	}

	public String getSearchQuery() {
		return searchQuery;
	}

	public void resetResults() {
		this.lastLocation = null;
		Log.d("ifpalocation", "resetresults");
//		this.entriesList = null;
//		this.entriesTimestamp = null;
	}

	public HashMap<String, Long> getSearchTimestamps() {
		return searchTimestamps;
	}
}
