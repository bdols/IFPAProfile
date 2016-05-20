/*
 * Copyright (c) 2016, Brian Dols <brian.dols@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package net.perspiratron.ifpaprofile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class CalendarFragment extends ListFragment implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SharedPreferences.OnSharedPreferenceChangeListener {

	private String distance_in;
	private String lastDistanceIn;
	private ArrayList<CalendarEntry> entriesList = new ArrayList<>();
	private HashMap<String, Long> searchTimestamps;
	private Long entriesTimestamp;
	private Location lastLocation;
	private GoogleApiClient googleApiClient;
	private GoogleApiClient apiClientForPlaces;

	private CalendarLoader calendarLoader;
	private ArrayAdapter<CharSequence> searchRadiusAdapter;
	private AutoCompleteTextView searchLocationAutoComplete;
	private Spinner searchRadiusSpinner;
	private String searchQuery;
	private boolean restoredFromBundle = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		googleApiClient = new GoogleApiClient.Builder(getActivity()).addApi(LocationServices.API)
				.addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
		apiClientForPlaces = new GoogleApiClient.Builder(getActivity())
				.addApi(Places.GEO_DATA_API)
				.addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.calendar_list, container, false);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("entries", entriesList);
		outState.putString("distance_in", distance_in);
		if (searchQuery != null) {
			outState.putString("search_query", searchQuery);
			Log.d("ifpalocation", "saving search_query " + searchQuery);
		} else {
			Log.d("ifpalocation", "null search_query " + searchQuery);
		}
		if (entriesTimestamp != null) {
			outState.putLong("timestamp", entriesTimestamp);
		}
		if (lastLocation != null) {
			outState.putParcelable("lastLocation", lastLocation);
		}
		if (searchTimestamps != null) {
			outState.putSerializable("searchTimestamps", searchTimestamps);
		}
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null) {
			Log.d("ifpalocation", "loading stored data from savedInstanceState");
			entriesList = (ArrayList<CalendarEntry>) savedInstanceState.getSerializable("entries");
			searchTimestamps = (HashMap<String, Long>) savedInstanceState.getSerializable("searchTimestamps");
			distance_in = savedInstanceState.getString("distance_in");
			searchQuery = savedInstanceState.getString("search_query");
			Log.d("ifpalocation", "loaded search_query " + searchQuery);
			setSearchQuery(searchQuery);
			entriesTimestamp = savedInstanceState.getLong("timestamp");
			lastLocation = savedInstanceState.getParcelable("lastLocation");
			restoredFromBundle = true;
		}

		searchRadiusSpinner = (Spinner) getActivity().findViewById(R.id.searchRadiusSpinner);

		searchRadiusAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.distance_label,
				android.R.layout.simple_spinner_item);
		searchRadiusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		searchRadiusSpinner.setAdapter(searchRadiusAdapter);
		final String[] distanceValues = getResources().getStringArray(R.array.distance_values);
		searchRadiusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				CharSequence label = (CharSequence) parent.getItemAtPosition(position);
				Log.d("ifpalocation", "label is " + label);
				Log.d("ifpalocation", "value is " + distanceValues[position]);
				Activity activity = CalendarFragment.this.getActivity();
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString(activity.getString(R.string.events_key), distanceValues[position]);
				editor.apply();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		searchLocationAutoComplete = (AutoCompleteTextView) getActivity().findViewById(R.id.searchLocation);
		LatLngBounds bounds = LatLngBounds.builder().include(new LatLng(-90, -180)).include(new LatLng(90, 180)).build();
		List<Integer> filterTypes = new ArrayList<>();
		filterTypes.add(Place.TYPE_GEOCODE);
		AutocompleteFilter filter = AutocompleteFilter.create(filterTypes);
		searchLocationAutoComplete.setAdapter(new PlaceAutocompleteAdapter(getActivity(), android.R.layout.simple_list_item_1,
				apiClientForPlaces, bounds, filter));
		if (searchQuery != null) {
			searchLocationAutoComplete.setText(searchQuery);
		}

		searchLocationAutoComplete.setSelectAllOnFocus(true);
		searchLocationAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				searchLocationAutoComplete.clearFocus();
				searchLocationAutoComplete.requestFocus();
				InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				PlaceAutocompleteAdapter.PlaceAutocomplete result = (PlaceAutocompleteAdapter.PlaceAutocomplete) parent.getItemAtPosition(position);
				setSearchQuery(result.description.toString());
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CalendarFragment.this.getActivity());
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("manualQuery", result.description.toString());
				editor.putBoolean("locationMethod", true);
				editor.apply();
				getActivity().getSupportLoaderManager().restartLoader(1, buildBundle(getActivity()), calendarLoader);
				Log.d("ifpalocation", "clicked label is " + result.description);
			}
		});
		searchLocationAutoComplete.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				searchLocationAutoComplete.clearFocus();
				searchLocationAutoComplete.requestFocus();
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		IFPAApplication.logAnalyticsHit(this.getClass(), getActivity());
		apiClientForPlaces.connect();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
		prefs.registerOnSharedPreferenceChangeListener(this);
		if (!restoredFromBundle) {
			Log.d("ifpalocation", "loading stored data from application in onResume");
			entriesList = ((IFPAApplication) getActivity().getApplication()).getEntriesList();
			entriesTimestamp = ((IFPAApplication) getActivity().getApplication()).getEntriesTimestamp();
			searchTimestamps = ((IFPAApplication) getActivity().getApplication()).getSearchTimestamps();
			lastLocation = ((IFPAApplication) getActivity().getApplication()).getLastLocation();
			searchQuery = ((IFPAApplication) getActivity().getApplication()).getSearchQuery();
		} else {
			restoredFromBundle = false;
		}
		if (searchTimestamps == null) {
			searchTimestamps = new HashMap<>(5);
		}
		Log.d("ifpalocation", "locationMethod pref is " + prefs.getBoolean("locationMethod", false));
		Log.d("ifpalocation", "lastLocation was " + lastLocation);

		if (!prefs.getBoolean("locationMethod", false)) {
			Log.d("ifpalocation", "connecting api");
			googleApiClient.connect();
		} else {
			Log.d("ifpalocation", "searchQuery was " + searchQuery);
			setSearchQuery(prefs.getString("manualQuery", "Minneapolis, MN"));
			Log.d("ifpalocation", "set query to " + searchQuery);
			searchLocationAutoComplete.setText(searchQuery);
			searchLocationAutoComplete.dismissDropDown();
			lastLocation = null;
			Log.d("ifpalocation", "lastLocation now is null.");
//			entriesTimestamp = null;
//			entriesList = null;
		}
		Log.d("ifpalocation", "distance_in was " + distance_in);
		String last_distance_in = distance_in;
		distance_in = prefs.getString("distance_preference", "250");
		Log.d("ifpalocation", "distance_in now " + distance_in);

		if (calendarLoader == null) {
			calendarLoader = new CalendarLoader();
		}
		Log.d("ifpalocation", "onResume searchQuery is " + searchQuery);
		Long searchTimestamp = null;
		if (searchTimestamps != null) {
			searchTimestamp = searchTimestamps.get(searchTimestampLabel(buildBundle(getActivity())));
		}
		if (entriesTimestamp != null && entriesList != null &&
				System.currentTimeMillis() - entriesTimestamp < 86400000 && entriesList.size() > 0
				&& searchTimestamp != null && searchTimestamp.equals(entriesTimestamp)) {
			LayoutInflater inflater = LayoutInflater.from(getActivity());
			setListAdapter(new CalendarEntryArrayAdapter(inflater));
			Log.d("ifpalocation", "active " + System.currentTimeMillis() + ", " + entriesTimestamp +
					", " + searchTimestampLabel(buildBundle(getActivity())) +
					searchTimestamp);
		} else {
			Log.d("ifpalocation", "calling restartLoader from onResume.");
			getActivity().getSupportLoaderManager().restartLoader(1, buildBundle(getActivity()), calendarLoader);
		}
		String countryCode = Locale.getDefault().getCountry();
		if (countryCode.equals("US") || countryCode.equals("GB") || countryCode.equals("MM")) {
			searchRadiusSpinner.setSelection(searchRadiusAdapter.getPosition(distance_in + " miles"));
		} else {
			searchRadiusSpinner.setSelection(searchRadiusAdapter.getPosition(distance_in + " km"));
		}
	}

	@Override
	public void onPause() {
		googleApiClient.disconnect();
		apiClientForPlaces.disconnect();
		PreferenceManager.getDefaultSharedPreferences(this.getActivity())
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	private void showToast(String message) {
		Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
	}

	public void populate(View view, final CalendarEntry calendarEntry) {
		TextView tournamentName = (TextView) view.findViewById(R.id.tournament_name);
		tournamentName.setText(calendarEntry.getTournamentName());
		TextView tournamentAddress1 = (TextView) view.findViewById(R.id.tournament_address1);
		if (TextUtils.isEmpty(calendarEntry.getAddress1())) {
			tournamentAddress1.setVisibility(View.GONE);
		} else {
			tournamentAddress1.setText(calendarEntry.getAddress1());
		}
		TextView tournamentAddress2 = (TextView) view.findViewById(R.id.tournament_address2);
		if (TextUtils.isEmpty(calendarEntry.getAddress2())) {
			tournamentAddress2.setVisibility(View.GONE);
		} else {
			tournamentAddress2.setText(calendarEntry.getAddress2());
		}
		TextView tournamentCity = (TextView) view.findViewById(R.id.tournament_city);
		tournamentCity.setText(calendarEntry.getCity());
		TextView tournamentState = (TextView) view.findViewById(R.id.tournament_state);
		tournamentState.setText(calendarEntry.getState());
		TextView tournamentDetails = (TextView) view.findViewById(R.id.tournament_details);
		tournamentDetails.setText(calendarEntry.getDetails());
		TextView tournamentZipCode = (TextView) view.findViewById(R.id.tournament_zipcode);
		tournamentZipCode.setText(calendarEntry.getZipCode());
		TextView tournamentStartDate = (TextView) view.findViewById(R.id.tournament_start_date);
		tournamentStartDate.setText(calendarEntry.getStartDate());
		TextView tournamentEndDate = (TextView) view.findViewById(R.id.tournament_end_date);
		if (TextUtils.isEmpty(calendarEntry.getEndDate()) || calendarEntry.getEndDate().equals(calendarEntry.getStartDate())) {
			tournamentEndDate.setVisibility(View.GONE);
			view.findViewById(R.id.date_to_label).setVisibility(View.GONE);
		} else {
			tournamentEndDate.setText(calendarEntry.getEndDate());
		}
		ImageButton mapButton = (ImageButton) view.findViewById(R.id.calendar_map_button);
		if (calendarEntry.hasLocation()) {
			final String latitude = calendarEntry.getLatitude();
			final String longitude = calendarEntry.getLongitude();
			mapButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent mapIntent = new Intent(Intent.ACTION_VIEW);
					String mapStr = "geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude + "(pinball!)";
					Uri mapUri = Uri.parse(mapStr);
					Log.d("ifpamap", mapUri.toString());
					mapIntent.setData(mapUri);
					if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
						startActivity(mapIntent);
					}
				}
			});
		} else {
			mapButton.setVisibility(View.GONE);
		}
		ImageButton webButton = (ImageButton) view.findViewById(R.id.calendar_web_button);
		if (calendarEntry.getWebsite() != null) {
			final String webLink = calendarEntry.getWebsite();
			webButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Uri webUri = Uri.parse(webLink);
					Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
					if (webIntent.resolveActivity(getActivity().getPackageManager()) != null) {
						startActivity(webIntent);
					}
				}
			});
		} else {
			webButton.setVisibility(View.GONE);
		}
		ImageButton calendarButton = (ImageButton) view.findViewById(R.id.calendar_add_to_mine_button);
		calendarButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent calendarIntent = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI);
				calendarIntent.putExtra(CalendarContract.Events.TITLE, calendarEntry.getTournamentName());
				calendarIntent.putExtra(CalendarContract.Events.DESCRIPTION, calendarEntry.getDetails());
				calendarIntent.putExtra(CalendarContract.Events.EVENT_LOCATION, calendarEntry.getAddress1() + '\n' + calendarEntry.getAddress2() + '\n' + calendarEntry.getCity() + ' '
						+ calendarEntry.getState() + " " + calendarEntry.getCountry());
				@SuppressLint("SimpleDateFormat") DateFormat yymmddFormat = new SimpleDateFormat("yyyy-MM-dd");
				if (TextUtils.isEmpty(calendarEntry.getEndDate()) || calendarEntry.getEndDate().equals(calendarEntry.getStartDate())) {
					try {
						calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, yymmddFormat.parse(calendarEntry.getStartDate()).getTime());
						calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, yymmddFormat.parse(calendarEntry.getStartDate()).getTime());
						calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true);

					} catch (ParseException e) {
						e.printStackTrace();
					}
				} else {
					try {
						calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, yymmddFormat.parse(calendarEntry.getEndDate()).getTime());
						calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, yymmddFormat.parse(calendarEntry.getStartDate()).getTime());
					} catch (ParseException e) {
						e.printStackTrace();
					}
					calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true);
				}
				startActivity(calendarIntent);
			}
		});
	}

	public String milesOrKilometers() {
		String countryCode = Locale.getDefault().getCountry();
		if (countryCode.equals("US") || countryCode.equals("GB") || countryCode.equals("MM")) {
			return "distance_in_miles";
		} else {
			return "distance_in_kilometers";
		}
	}

	public Bundle buildBundle(Activity activity) {
		Bundle bundleCalendar = new Bundle();
		lastDistanceIn = distance_in;
		distance_in = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext())
				.getString(activity.getString(R.string.events_key), "250");
		bundleCalendar.putString(milesOrKilometers(), distance_in);
		bundleCalendar.putString("search_query", getSearchQuery());
		return bundleCalendar;
	}

	private void setLocation(Location location) {
		if (location == null) {
			return;
		}
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
		if (prefs.getBoolean("locationMethod", true)) {
			return;
		}
		if (lastLocation == null || location.distanceTo(lastLocation) > 3000) {
			lastLocation = location;
			googleApiFillSearchQueryFromLocation(location);
			if (lastLocation == null) {
				Log.d("ifpalocation", "lastLocation is null");
				Log.d("ifpalocation", "calling restartLoader from setLocation new location");
			} else {
				Log.d("ifpalocation", "distance to last " + location.distanceTo(lastLocation));
				Log.d("ifpalocation", "calling restartLoader from setLocation long distance");
			}
			if (getActivity() != null) {
				getActivity().getSupportLoaderManager().restartLoader(1, buildBundle(getActivity()), calendarLoader);
			}
		} else {
			if (getActivity() != null) {
				Long searchTimestamp = searchTimestamps.get(searchTimestampLabel(buildBundle(getActivity())));
				if (entriesTimestamp == null || searchTimestamp == null ||
						(searchTimestamp != null && searchTimestamp.compareTo(entriesTimestamp) < 0) ||
						System.currentTimeMillis() - entriesTimestamp >= 86400000) {
					entriesTimestamp = System.currentTimeMillis();
					Log.d("ifpalocation", "calling restartLoader from setLocation parameter change");
					getActivity().getSupportLoaderManager().restartLoader(1, buildBundle(getActivity()), calendarLoader);
				}
			} else {
				Log.d("ifpalocation", "activity is null?");
			}
		}
		lastLocation = location;
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.d("ifpalocation", "onConnected");
		LocationRequest locationRequest = LocationRequest.create();
		if (Build.MODEL.equals("sdk") || Build.MODEL.contains("Genymotion") || Build.FINGERPRINT.contains("generic")) {
			locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
			locationRequest.setInterval(5000);
		} else {
			locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
			locationRequest.setInterval(50000);
			locationRequest.setFastestInterval(10000);
		}
		Log.d("ifpalocation", "update interval " + locationRequest.getInterval());
		if (googleApiClient.isConnected()) {
			Log.d("ifpalocation", "is connected");
			setLocation(LocationServices.FusedLocationApi.getLastLocation(googleApiClient));
			LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,
					locationRequest, this);
		}
	}

	@Override
	public void onConnectionSuspended(int i) {
		Log.d("ifpalocation", "onConnectionSuspended");  //  When called, all requests have been canceled
		// and no outstanding listeners will be executed. GoogleApiClient will automatically attempt
		// to restore the connection. Applications should disable UI components that require the
		// service, and wait for a call to onConnected(Bundle) to re-enable them
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.d("ifpalocation", "onConnectionFailed");
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				searchLocationAutoComplete.setText("");
			}
		});
	}

	@Override
	public void onLocationChanged(Location location) {
		setLocation(location);
	}

	public String getSearchQuery() {
		return searchQuery;
	}

	public void setSearchQuery(String searchQuery) {
		this.searchQuery = searchQuery;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(this.getActivity().getString(R.string.events_key))) {
			Log.d("ifpalocation2", "caught change");
			getActivity().getSupportLoaderManager().restartLoader(1, buildBundle(getActivity()), calendarLoader);
			if (searchQuery != null && !searchQuery.equals(searchLocationAutoComplete.getText())) {
				searchLocationAutoComplete.setText(searchQuery);
				searchLocationAutoComplete.dismissDropDown();
			}
		}
	}

	private class CalendarLoader implements LoaderManager.LoaderCallbacks<JSONObject> {
		private Bundle bundle;

		@Override
		public Loader<JSONObject> onCreateLoader(int i, final Bundle bundle) {
			this.bundle = bundle;
			return new AsyncTaskLoader<JSONObject>(getActivity()) {
				JSONObject data;

				@Override
				protected void onStartLoading() {
					super.onStartLoading();
					if (data != null) {
						deliverResult(data);
					} else {
						forceLoad();
					}
				}

				@Override
				public void deliverResult(JSONObject data) {
					super.deliverResult(data);
					this.data = data;
				}

				@Override
				public JSONObject loadInBackground() {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (getListAdapter() != null) {
								((ArrayAdapter) getListAdapter()).clear();
							}
							if (getActivity() != null) {
								TextView emptyText = (TextView) getActivity().findViewById(android.R.id.empty);
								emptyText.setText(getResources().getString(R.string.loading_calendar_search_text));
							}
						}
					});
					Log.d("ifpalocation", "in loadInBackground");
					return searchIFPA(bundle);
				}
			};

		}

		@Override
		public void onLoadFinished(Loader<JSONObject> jsonObjectLoader, JSONObject jsonObject) {
			Log.d("ifpalocation", "in onLoadFinished");
			processCalendarResults(jsonObject, bundle);
		}

		@Override
		public void onLoaderReset(Loader<JSONObject> jsonObjectLoader) {
			Log.d("ifparesponse", "got to calendar loader reset");
		}
	}

	public String searchTimestampLabel(Bundle bundle) {
		return bundle.getString("search_query", "noop") +
				bundle.getString(milesOrKilometers(), "250") + milesOrKilometers();
	}

	private void processCalendarResults(JSONObject jsonObject, Bundle bundle) {
		entriesList = new ArrayList<>();
		try {
			if (jsonObject.has("total_entries")) { // && jsonObject.getInt("total_entries") > 0) {
				JSONArray calendarEntries = jsonObject.getJSONArray("calendar");
				for (int i = 0; i < calendarEntries.length(); i++) {
					JSONObject entry = (JSONObject) calendarEntries.get(i);
					entriesList.add(CalendarEntry.fromJSON(entry));
				}
				final LayoutInflater inflater = LayoutInflater.from(getActivity());
				setListAdapter(new CalendarEntryArrayAdapter(inflater));
				((ArrayAdapter) getListAdapter()).notifyDataSetChanged();
			}
		} catch (JSONException | NullPointerException e) {
//			e.printStackTrace();
			if (getActivity() != null) {
				if (getListAdapter() != null) {
					((ArrayAdapter) getListAdapter()).clear();
				}
				TextView emptyText = (TextView) getActivity().findViewById(android.R.id.empty);
				emptyText.setText(getResources().getString(R.string.empty_calendar_search_text));
				emptyText.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Uri ifpaUri = Uri.parse("http://www.ifpapinball.com/");
						Intent webIntent = new Intent(Intent.ACTION_VIEW, ifpaUri);
						if (getActivity() != null && webIntent.resolveActivity(getActivity().getPackageManager()) != null) {
							startActivity(webIntent);
						}
					}
				});
			}
		}
		Log.d("ifpalocation", "before save calendarentries");
		if (getActivity() != null) {
			((IFPAApplication) getActivity().getApplication()).saveCalendarEntries(searchQuery,
					entriesList, lastLocation, entriesTimestamp, searchTimestamps);
			Log.d("ifpalocation", "called save calendarentries with " + lastLocation);
		}
	}

	private String googleApiFillSearchQueryFromLocation(Location lastLocation) {
		final String searchLocation;
		try {
			if (googleApiClient.isConnected()) {
				if (lastLocation != null) {
					Geocoder geocoder = new Geocoder(getActivity(), Locale.ENGLISH);
					Log.d("ifpalocation", "looking for addresses for lat: " +
							lastLocation.getLatitude() + " long: " + lastLocation.getLongitude());
					List<Address> addresses = geocoder.getFromLocation(lastLocation.getLatitude(),
							lastLocation.getLongitude(), 5);
					if (addresses.size() > 0) {
						Address a = addresses.get(0);
						String assembled = "";
						if (a.getLocality() != null) {
							assembled += a.getLocality() + " ";
						}
						if (a.getAdminArea() != null) {
							assembled += a.getAdminArea() + " ";
						}
						if (a.getCountryName() != null) {
							assembled += a.getCountryName();
						}
						searchLocation = assembled;
						setSearchQuery(searchLocation);
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								searchLocationAutoComplete.setText(searchLocation);
								searchLocationAutoComplete.dismissDropDown();
							}
						});

						Log.d("ifpalocation", searchLocation);
					} else {
						Log.d("ifpalocation", "addresses are empty");
						searchLocation = null;
					}
				} else {
					Log.d("ifpalocation", "lastLocation is null");
					searchLocation = null;
				}
			} else {
				Log.d("ifpalocation", "googleapiclient is not connected");

				if (entriesList != null && entriesList.size() > 0 && (lastLocation == null
						&& (lastDistanceIn != null && distance_in != null && lastDistanceIn.equals(distance_in)))) {
					// don't call the server for the same info
					Log.d("ifpalocation", "destroying loader lastLocation " + lastLocation + " lastDistanceIn"
							+ lastDistanceIn + " distance_in " + distance_in);
					getActivity().getSupportLoaderManager().destroyLoader(1);
					searchLocation = null;
				} else if (googleApiClient.isConnecting()) {
					Log.d("ifpalocation", "destroying loader, googleapiclient is connecting");
					searchLocation = null;
					getActivity().getSupportLoaderManager().destroyLoader(1);
				} else {
					Log.d("ifpalocation", "not destroying loader " + googleApiClient.isConnecting() + ", " + googleApiClient.isConnected());
					searchLocation = "2001 Lyndale Avenue South\n" + "Minneapolis MN";
					lastLocation = null;
				}
				Log.d("ifpalocation", "googleapiclient is not connected lastlocation is null");
			}
			setSearchQuery(searchLocation);
			return searchLocation;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private JSONObject searchIFPA(Bundle bundle) {
		try {
			entriesTimestamp = System.currentTimeMillis();
			searchTimestamps.put(searchTimestampLabel(bundle), entriesTimestamp);
			String searchLocation = bundle.getString("search_query");
			String pairs = "";
			pairs += "address=" + URLEncoder.encode(searchLocation, "UTF-8");
			pairs += "&";
			if (bundle.getString("distance_in_miles") != null) {
				pairs += "m=" + bundle.getString("distance_in_miles");
			} else if (bundle.getString("distance_in_kilometers") != null) {
				pairs += "k=" + bundle.getString("distance_in_kilometers");
			} else {
				pairs += "m=250";
			}
			pairs += "&api_key=" + BuildConfig.IFPA_API_KEY;
			String urlString = "https://api.ifpapinball.com/v1/calendar/search?" + pairs;
			Log.d("ifpalocation", urlString);
			URL url = new URL(urlString);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setUseCaches(true);
			connection.setRequestProperty("Content-length", "0");
			connection.setRequestProperty("Content-type", "application/json");
			connection.setRequestProperty("Accept", "application/json");
			connection.setConnectTimeout(30000);
			connection.setReadTimeout(30000);
			connection.connect();
			int status = connection.getResponseCode();
			if (status == 200 || status == 201) {
				BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line).append("\n");
				}
				br.close();
				Log.d("ifpalocation", sb.toString());
				return new JSONObject(sb.toString());
			}
			return null;
		} catch (JSONException | IOException | NullPointerException e) {
			e.printStackTrace();
		}
		return null;
	}

	static class CalendarEntryHolder {
		TextView name;
		TextView address1;
		TextView address2;
		TextView city;
		TextView state;
		TextView zipCode;
		TextView details;
		TextView startDate;
		TextView endDate;
		TextView to;
		ImageButton calendarWeb;
		ImageButton calendarMap;
		int position;
	}

	private class CalendarEntryArrayAdapter extends ArrayAdapter<CalendarEntry> {
		private final LayoutInflater inflater;

		public CalendarEntryArrayAdapter(LayoutInflater inflater) {
			super(CalendarFragment.this.getActivity(), R.layout.calendar_entry, entriesList);
			this.inflater = inflater;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
//			CalendarEntryHolder holder = new CalendarEntryHolder();
//			holder.name = (TextView) convertView.findViewById(R.id.tournament_name);
//			holder.address1 = (TextView) convertView.findViewById(R.id.tournament_address1);
//			holder.address2 = (TextView) convertView.findViewById(R.id.tournament_address2);
//			holder.city = (TextView) convertView.findViewById(R.id.tournament_city);
//			holder.state = (TextView) convertView.findViewById(R.id.tournament_state);
//			holder.zipCode = (TextView) convertView.findViewById(R.id.tournament_zipcode);
//			holder.details = (TextView) convertView.findViewById(R.id.tournament_details);
//			holder.startDate = (TextView) convertView.findViewById(R.id.tournament_start_date);
//			holder.endDate = (TextView) convertView.findViewById(R.id.tournament_end_date);
//			holder.to = (TextView) convertView.findViewById(R.id.date_to_label);
//			holder.calendarMap = (ImageButton) convertView.findViewById(R.id.calendar_map_button);
//			holder.calendarWeb= (ImageButton) convertView.findViewById(R.id.calendar_web_button);
//			holder.position = position;
//			convertView.setTag(holder);

			CalendarEntry calendarEntry = getItem(position);
			convertView = inflater.inflate(R.layout.calendar_entry, parent, false);
			populate(convertView, calendarEntry);
			return convertView;
		}
	}
//	void updateLocationProvider() {
//		boolean isGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//		boolean isNet = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
//		locationProvider = null;
//		if (isNet) {
//			locationProvider = LocationManager.NETWORK_PROVIDER;
//			Log.d("ifpalocation", "provider is " + locationProvider);
//		} else if (isGPS) {
//			locationProvider = LocationManager.GPS_PROVIDER;
//		}
//		Log.d("ifpalocation", "provider is " + locationProvider + " gps is " + isGPS + " net is " + isNet);
//	}

}

