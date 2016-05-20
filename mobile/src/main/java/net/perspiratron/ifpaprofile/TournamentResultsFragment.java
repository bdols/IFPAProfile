/*
 * Copyright (c) 2016, Brian Dols <brian.dols@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package net.perspiratron.ifpaprofile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class TournamentResultsFragment extends ListFragment implements ProfileUpdater, LoaderManager.LoaderCallbacks<JSONObject> {

	private int profile_id;
	private ArrayList<TournamentResultEntry> entriesList;
	private long entriesTimestamp;
	private TournamentEntryArrayAdapter tournamentEntryAdapter;
	private static DateFormat yymmddFormat = new SimpleDateFormat("yyyy-MM-dd");

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("entriesList", entriesList);
		outState.putInt("profile_id", profile_id);
		outState.putLong("entriesTimestamp", entriesTimestamp);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.tournament_results_list, container, false);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		IFPAApplication.logAnalyticsHit(this.getClass(), getActivity());
	}

	@Override
	public void setIFPAProfile(IFPAProfile ifpaProfile) {

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null) {
			entriesList = (ArrayList<TournamentResultEntry>) savedInstanceState.getSerializable("entriesList");
			profile_id = savedInstanceState.getInt("profile_id");
			entriesTimestamp = savedInstanceState.getLong("entriesTimestamp");
			final LayoutInflater inflater = LayoutInflater.from(getActivity());
			tournamentEntryAdapter = new TournamentEntryArrayAdapter(inflater);
			setListAdapter(tournamentEntryAdapter);
		}
		Spinner spinner = (Spinner) getActivity().findViewById(R.id.tournament_results_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.tournament_sort_options,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (tournamentEntryAdapter == null) {
					return;
				}
				CharSequence value = (CharSequence) parent.getItemAtPosition(position);
				if (value.toString().equals(getString(R.string.sort_by_active_recent))) {
					tournamentEntryAdapter.filterThenSort("Active", new Comparator<TournamentResultEntry>() {
						@Override
						public int compare(TournamentResultEntry lhs, TournamentResultEntry rhs) {
							try {
								return yymmddFormat.parse(rhs.getEvent_date()).compareTo(yymmddFormat.parse(lhs.getEvent_date()));
							} catch (ParseException e) {
								e.printStackTrace();
							}
							return 0;
						}
					});
				} else if (value.toString().equals(getString(R.string.sort_by_active_recent))) {
					tournamentEntryAdapter.filterThenSort("Active", new Comparator<TournamentResultEntry>() {
						@Override
						public int compare(TournamentResultEntry lhs, TournamentResultEntry rhs) {
							try {
								return yymmddFormat.parse(rhs.getEvent_date()).compareTo(yymmddFormat.parse(lhs.getEvent_date()));
							} catch (ParseException e) {
								e.printStackTrace();
							}
							return 0;
						}
					});
				} else if (value.toString().equals(getString(R.string.sort_by_most_recent))) {
					tournamentEntryAdapter.filterThenSort(null, new Comparator<TournamentResultEntry>() {
						@Override
						public int compare(TournamentResultEntry lhs, TournamentResultEntry rhs) {
							try {
								return yymmddFormat.parse(rhs.getEvent_date()).compareTo(yymmddFormat.parse(lhs.getEvent_date()));
							} catch (ParseException e) {
								e.printStackTrace();
							}
							return 0;
						}
					});
				} else if (value.toString().equals(getString(R.string.sort_by_historical_order))) {
					tournamentEntryAdapter.filterThenSort(null, new Comparator<TournamentResultEntry>() {
						@Override
						public int compare(TournamentResultEntry lhs, TournamentResultEntry rhs) {
							try {
								return yymmddFormat.parse(lhs.getEvent_date()).compareTo(yymmddFormat.parse(rhs.getEvent_date()));
							} catch (ParseException e) {
								e.printStackTrace();
							}
							return 0;
						}
					});
				} else if (value.toString().equals(getString(R.string.sort_by_active_points))) {
					tournamentEntryAdapter.filterThenSort("Active", new Comparator<TournamentResultEntry>() {
						@Override
						public int compare(TournamentResultEntry lhs, TournamentResultEntry rhs) {
							Float lCurr = Float.valueOf(lhs.getCurrent_points());
							Float rCurr = Float.valueOf(rhs.getCurrent_points());
							return rCurr.compareTo(lCurr);
						}
					});
				} else if (value.toString().equals(getString(R.string.sort_by_current_points))) {
					tournamentEntryAdapter.filterThenSort(null, new Comparator<TournamentResultEntry>() {
						@Override
						public int compare(TournamentResultEntry lhs, TournamentResultEntry rhs) {
							Float lCurr = Float.valueOf(lhs.getCurrent_points());
							Float rCurr = Float.valueOf(rhs.getCurrent_points());
							return rCurr.compareTo(lCurr);
						}
					});
				} else if (value.toString().equals(getString(R.string.sort_by_original_points))) {
					tournamentEntryAdapter.filterThenSort(null, new Comparator<TournamentResultEntry>() {
						@Override
						public int compare(TournamentResultEntry lhs, TournamentResultEntry rhs) {
							Float lOrig = Float.valueOf(lhs.getOriginal_points());
							Float rOrig = Float.valueOf(rhs.getOriginal_points());
							return rOrig.compareTo(lOrig);
						}
					});
				} else if (value.toString().equals(getString(R.string.sort_by_alphabetical))) {

					tournamentEntryAdapter.filterThenSort(null, new Comparator<TournamentResultEntry>() {
						@Override
						public int compare(TournamentResultEntry lhs, TournamentResultEntry rhs) {
							return lhs.getTournament_name().compareTo(rhs.getTournament_name());
						}
					});
				} else {
					Log.e("ifpaspinner", "case not found: " + value.toString());
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		if (savedInstanceState != null) {

		} else {
			if (getArguments() != null) {
				if (profile_id != getArguments().getInt("profile_id")) {
					profile_id = getArguments().getInt("profile_id");
					this.getLoaderManager().initLoader(0, getArguments(), this);
				}
			}
		}
	}

	@Override
	public Loader<JSONObject> onCreateLoader(int id, Bundle args) {
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
				try {
					if (getActivity() != null) {
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								TextView emptyText = (TextView) getActivity().findViewById(android.R.id.empty);
								emptyText.setText(getResources().getString(R.string.loading_tournament_results));
							}
						});

						String urlString = "https://api.ifpapinball.com/v1/player/" + profile_id + "/results?api_key=" + BuildConfig.IFPA_API_KEY;
						URL url = new URL(urlString);
						HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
						connection.setRequestMethod("GET");
						connection.setUseCaches(false);
						connection.setRequestProperty("Content-length", "0");
						connection.setRequestProperty("Content-type", "application/json");
						connection.setRequestProperty("Accept", "application/json");
						connection.setConnectTimeout(30000);
						connection.setReadTimeout(30000);
						Log.d("ifparequest", urlString);
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
							Log.d("ifparesponse", sb.toString());
							return new JSONObject(sb.toString());
						}
					}
					return null;
				} catch (IOException | JSONException e) {
					e.printStackTrace();
				}
				return null;
			}
		};

	}

	@Override
	public void onLoadFinished(Loader<JSONObject> loader, JSONObject jsonObject) {
		try {
			if (jsonObject.has("results_count") && jsonObject.getInt("results_count") > 0) {
				JSONArray tournamentEntries = jsonObject.getJSONArray("results");
				entriesList = new ArrayList<TournamentResultEntry>();
				for (int i = 0; i < tournamentEntries.length(); i++) {
					JSONObject entry = (JSONObject) tournamentEntries.get(i);
					entriesList.add(TournamentResultEntry.fromJSON(entry));
				}
				entriesTimestamp = System.currentTimeMillis();
				final LayoutInflater inflater = LayoutInflater.from(getActivity());
				tournamentEntryAdapter = new TournamentEntryArrayAdapter(inflater);
				setListAdapter(tournamentEntryAdapter);
				TextView emptyText = (TextView) getActivity().findViewById(android.R.id.empty);
				emptyText.setText(getResources().getString(R.string.filtered_to_zero));
			} else {
				throw new NullPointerException(); // heh
			}
		} catch (JSONException | NullPointerException e) {
			if (getActivity() != null) {
				setListAdapter(null);
				TextView emptyText = (TextView) getActivity().findViewById(android.R.id.empty);
				emptyText.setText(getResources().getString(R.string.empty_tournament_results));
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
			e.printStackTrace();
		}

	}

	@Override
	public void onLoaderReset(Loader<JSONObject> loader) {
		Log.d("ifparesponse", "got to profile loader reset ");
	}

	private class TournamentEntryArrayAdapter extends ArrayAdapter<TournamentResultEntry> {
		private final LayoutInflater inflater;
		private final ArrayList<TournamentResultEntry> unfilteredList;
		private Filter filter;

		public TournamentEntryArrayAdapter(LayoutInflater inflater) {
			super(TournamentResultsFragment.this.getActivity(), R.layout.tournament_entry, entriesList);
			this.unfilteredList = new ArrayList<TournamentResultEntry>(entriesList);
			this.inflater = inflater;
			setNotifyOnChange(true);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TournamentResultEntry tournamentResultEntry = getItem(position);
			convertView = inflater.inflate(R.layout.tournament_entry, parent, false);
			populate(convertView, tournamentResultEntry);
			return convertView;
		}

		//		@Override
		public Filter getFilter(Comparator<? super TournamentResultEntry> comparator) {
//			if (filter == null) {
//				filter = new TournamentFilter(comparator);
//			}
			return new TournamentFilter(comparator);
		}

		private void populate(View view, TournamentResultEntry tournamentResultEntry) {
			TextView tournamentName = (TextView) view.findViewById(R.id.tournament_results_name);
			tournamentName.setText(tournamentResultEntry.getTournament_name());
			TextView tournamentDate = (TextView) view.findViewById(R.id.tournament_results_date);
			tournamentDate.setText(tournamentResultEntry.getEvent_date());
			ImageView countryFlag = (ImageView) view.findViewById(R.id.tournament_country);
			int resId = getResources().getIdentifier(tournamentResultEntry.getCountry_code().toLowerCase(), "drawable",
					"net.perspiratron.ifpaprofile");
			if (resId != 0) {
				countryFlag.setImageResource(resId);
			}
			TextView tournamentActive = (TextView) view.findViewById(R.id.tournament_active);
			if (tournamentResultEntry.getWppr_state().equals("Active")) {
				tournamentActive.setText(getResources().getString(R.string.tournament_active));
			} else {
				tournamentActive.setText(getResources().getString(R.string.tournament_inactive));
			}
			TextView tournamentPosition = (TextView) view.findViewById(R.id.tournament_position);
			tournamentPosition.setText("#" + tournamentResultEntry.getPosition());
			TextView tournamentEventName = (TextView) view.findViewById(R.id.tournament_event_name);
			tournamentEventName.setText(tournamentResultEntry.getEvent_name());
			TextView tournamentCurrentPoints = (TextView) view.findViewById(R.id.tournament_current_points);
			tournamentCurrentPoints.setText(tournamentResultEntry.getCurrent_points());
			TextView tournamentOriginalPoints = (TextView) view.findViewById(R.id.tournament_original_points);
			tournamentOriginalPoints.setText(tournamentResultEntry.getOriginal_points());
		}

		public void filterThenSort(String active, Comparator<TournamentResultEntry> comparator) {
			getFilter(comparator).filter(active);
		}

		private class TournamentFilter extends Filter {

			private Comparator<? super TournamentResultEntry> comparator;

			public TournamentFilter(Comparator<? super TournamentResultEntry> comparator) {
				super();
				this.comparator = comparator;
			}

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();
				if (constraint != null && "Active".equals(constraint.toString())) {
					ArrayList<TournamentResultEntry> filterList = new ArrayList<>();
					synchronized (unfilteredList) {
						for (TournamentResultEntry entry : unfilteredList) {
							if ("Active".equals(entry.getWppr_state())) {
								filterList.add(entry);
							}
						}
					}
					results.values = filterList;
					results.count = filterList.size();
				} else {
					synchronized (unfilteredList) {
						results.values = unfilteredList;
						results.count = unfilteredList.size();
					}
				}
				return results;
			}

			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				List<TournamentResultEntry> filtered = (ArrayList<TournamentResultEntry>) results.values;
				notifyDataSetChanged();
				clear();
				if (filtered != null) {
					for (TournamentResultEntry entry : filtered) {
						add(entry);
					}
				}
				notifyDataSetInvalidated();
				sort(comparator);
			}
		}
	}
}
