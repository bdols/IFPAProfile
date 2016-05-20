/*
 * Copyright (c) 2016, Brian Dols <brian.dols@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package net.perspiratron.ifpaprofile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class ProfileFragment extends Fragment implements LoaderManager.LoaderCallbacks<JSONObject> {
	private int profile_id;

	private IFPAProfile ifpaProfile;
	private ViewPager viewPager;
	private DetailsPagerAdapter detailsPagerAdapter;

	public ProfileFragment() {
		profile_id = 0;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		if (savedInstanceState == null && getArguments() != null) {
//			Log.d("ifparotate", "oncreate getargs " + ifpaProfile);
//			if (profile_id != getArguments().getInt("profile_id")) {
//				Log.d("ifparotate", "init loader");
//				profile_id = getArguments().getInt("profile_id");
//				this.getLoaderManager().initLoader(0, getArguments(), this);
//			}
//		} // TODO move to onActivityCreated
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null) {
			Log.d("ifparotate", "init restore profile");
			ifpaProfile = (IFPAProfile) savedInstanceState.getSerializable("profile");
			populateView(ifpaProfile);
		} else if (getArguments() != null) {
			Log.d("ifparotate", "oncreate getargs " + ifpaProfile);
			if (profile_id != getArguments().getInt("profile_id")) {
				Log.d("ifparotate", "init loader");
				profile_id = getArguments().getInt("profile_id");
				this.getLoaderManager().initLoader(0, getArguments(), this);
			}
		}

		detailsPagerAdapter = new DetailsPagerAdapter(getChildFragmentManager(),
				profile_id, getActivity());
		viewPager = (ViewPager) getActivity().findViewById(R.id.profile_detail);
		viewPager.setAdapter(detailsPagerAdapter);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("profile", ifpaProfile);
	}

	@Override
	public void onResume() {
		super.onResume();
		IFPAApplication.logAnalyticsHit(this.getClass(), getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.profile, container, false);
		return rootView;
	}

	@Override
	public Loader<JSONObject> onCreateLoader(int i, Bundle bundle) {
		if (bundle != null) {
			profile_id = bundle.getInt("profile_id");
		}

		return new AsyncTaskLoader<JSONObject>(this.getActivity()) {
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
					String urlString = "https://api.ifpapinball.com/v1/player/" + profile_id + "?api_key=" + BuildConfig.IFPA_API_KEY;
					Log.d("ifparequest", urlString);
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
						Log.d("ifparesponse", sb.toString());
						return new JSONObject(sb.toString());
					}
				} catch (IOException | JSONException e) {
					e.printStackTrace();
				}
				return null;
			}
		};

	}

	@Override
	public void onLoadFinished(Loader<JSONObject> objectLoader, JSONObject data) {
		try {
			if (data.has("player")) {
				JSONObject player = data.getJSONObject("player");
				ifpaProfile = IFPAProfile.fromJson(player);
				if (data.has("championshipSeries")) {
					ifpaProfile.addChampionshipSeries(data.getJSONArray("championshipSeries"));
				}
				populateView(ifpaProfile);
				ifpaProfile.addStats(data.getJSONObject("player_stats"));

				((ProfileUpdater) detailsPagerAdapter.getItem(viewPager.getCurrentItem())).setIFPAProfile(ifpaProfile);
			} else {
				throw new NullPointerException();
			}
		} catch (JSONException | NullPointerException e) {
			e.printStackTrace();
			if (getActivity() != null) {
				Toast.makeText(getActivity(), getResources().getString(R.string.server_error), Toast.LENGTH_LONG).show();
				Intent mainIntent = new Intent(getActivity(), MainActivity.class);
				startActivity(mainIntent);
			}
		}
	}

	private void populateView(IFPAProfile player) {
		TextView playerName = (TextView) getActivity().findViewById(R.id.player_name);
		TextView currentRank = (TextView) getActivity().findViewById(R.id.player_current_rank);
		TextView currentRankLabel = (TextView) getActivity().findViewById(R.id.player_current_rank_label);
		TextView playerLocation = (TextView) getActivity().findViewById(R.id.player_location);
		TextView playerInitials = (TextView) getActivity().findViewById(R.id.player_initials);
		TextView playerCountry = (TextView) getActivity().findViewById(R.id.player_country);
		TextView playerId = (TextView) getActivity().findViewById(R.id.ifpa_id);

//		CheckBox playerRegistered = (CheckBox) getActivity().findViewById(R.id.player_registered);
		currentRank.setVisibility(View.INVISIBLE);
		currentRankLabel.setVisibility(View.INVISIBLE);
		playerName.setText(player.getFirstName() + " " + player.getLastName());
		if (TextUtils.isEmpty(player.getCity()) && TextUtils.isEmpty(player.getState())) {
			playerLocation.setVisibility(View.GONE);
		} else {
			if (TextUtils.isEmpty(player.getState())) {
				playerLocation.setText(player.getCity());
			} else {
				playerLocation.setText(player.getCity() + ", " + player.getState());
			}
		}
		playerCountry.setText(player.getCountryName());
		ImageView countryFlag = (ImageView) getActivity().findViewById(R.id.player_country_flag);
		int resId = getResources().getIdentifier(player.getCountryCode().toLowerCase(), "drawable",
				"net.perspiratron.ifpaprofile");
		if (resId != 0) {
			countryFlag.setImageResource(resId);
		}
		playerInitials.setText(player.getInitials());
		playerId.setText(player.getPlayer_id().toString());
//		if (player.isIfpaRegistered()) {
//			playerRegistered.setChecked(true);
//		} else {
//			playerRegistered.setChecked(false);
//		}
	}

	@Override
	public void onLoaderReset(Loader<JSONObject> objectLoader) {
		Log.d("ifparesponse", "got to profile loader reset ");
	}

	public static class DetailsPagerAdapter extends FragmentPagerAdapter {

		private final TournamentResultsFragment tournamentResultsFragment;
		private int profile_id;
		private HistoryFragment historyFragment;
		private StatsFragment statsFragment;
		private Context context;

		public DetailsPagerAdapter(FragmentManager fragmentManager, int profile_id, Context context) {
			super(fragmentManager);
			this.profile_id = profile_id;
			this.context = context;
			statsFragment = new StatsFragment();
			historyFragment = new HistoryFragment();
			Bundle b = new Bundle();
			b.putInt("profile_id", this.profile_id);
			historyFragment.setArguments(b);
			tournamentResultsFragment = new TournamentResultsFragment();
			tournamentResultsFragment.setArguments(b);
		}


		@Override
		public Fragment getItem(int i) {
			if (i == 0) {
				return statsFragment;
			} else if (i == 1) {
				return tournamentResultsFragment;
			} else if (i == 2) {
				return historyFragment;
			}
			return new Fragment();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			if (position == 0) {
				return context.getString(R.string.stats_pager_title);
			} else if (position == 1) {
				return context.getString(R.string.results_pager_title);
			} else if (position == 2) {
				return context.getString(R.string.history_pager_title);
			}
			return super.getPageTitle(position);
		}

		@Override
		public int getCount() {
			return 3;
		}
	}

	public static class StatsFragment extends Fragment implements ProfileUpdater {
		private IFPAProfile ifpaProfile;
		private Activity activity;

		public void setIFPAProfile(IFPAProfile ifpaProfile) {
			this.ifpaProfile = ifpaProfile;
			Log.d("ifparotate", "setifpa");
			populateView();
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			Log.d("ifparotate", "onattach stats" + activity);
			this.activity = activity;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
			Log.d("ifparotate", "oncreate view stats");
			return inflater.inflate(R.layout.stats, container, false);
		}

		@Override
		public void onActivityCreated(@Nullable Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			if (savedInstanceState != null) {
				ifpaProfile = (IFPAProfile) savedInstanceState.getSerializable("ifpaProfile");
			}
			Log.d("ifparotate", "onactivitycreated stats" + ifpaProfile);
			if (ifpaProfile != null) {
				populateView();
			}
		}

		@Override
		public void onResume() {
			super.onResume();
			IFPAApplication.logAnalyticsHit(this.getClass(), this.activity);
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			outState.putSerializable("ifpaProfile", this.ifpaProfile);
		}

		public void populateView() {
			Log.d("ifparotate", "populate view stats" + this.activity);

			TextView currentRank = (TextView) activity.findViewById(R.id.stats_current_rank);
			currentRank.setText(ifpaProfile.getWpprRank().toString());
			TextView currentHighestRank = (TextView) activity.findViewById(R.id.stats_highest_rank);
			currentHighestRank.setText(ifpaProfile.getHighestRank());
			TextView highestRankDate = (TextView) activity.findViewById(R.id.stats_highest_rank_date);
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			DateFormat rankDateFormatter = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
			try {
				highestRankDate.setText(rankDateFormatter.format(dateFormat.parse(ifpaProfile.getHighestRankDate())));
			} catch (ParseException e) {
				e.printStackTrace();
			}

			TextView efficiencyRank = (TextView) activity.findViewById(R.id.stats_efficiency_rank);
			efficiencyRank.setText(ifpaProfile.getEfficiencyRank());
			TextView efficiencyValue = (TextView) activity.findViewById(R.id.stats_efficiency_value);
			if (!TextUtils.isEmpty(ifpaProfile.getEfficiencyValue())) {
				efficiencyValue.setText(String.format(Locale.getDefault(), "%.3f%%", Float.valueOf(ifpaProfile.getEfficiencyValue())));
			}
			TextView ratingsRank = (TextView) activity.findViewById(R.id.stats_ratings_rank);
			ratingsRank.setText(ifpaProfile.getRatingsRank());
			TextView ratingsValue = (TextView) activity.findViewById(R.id.stats_ratings_value);
			if (!TextUtils.isEmpty(ifpaProfile.getRatingsValue())) {
				ratingsValue.setText(String.format(Locale.getDefault(), "%.2f", Float.valueOf(ifpaProfile.getRatingsValue())));
			}
			TextView totalActiveEvents = (TextView) activity.findViewById(R.id.stats_total_active_events);
			totalActiveEvents.setText(ifpaProfile.getTotalActiveEvents());
			TextView totalEvents = (TextView) activity.findViewById(R.id.stats_total_events_all_time);
			totalEvents.setText(ifpaProfile.getTotalEventsAllTime());
			TextView lastMonth = (TextView) activity.findViewById(R.id.stats_last_month_value);
			lastMonth.setText(ifpaProfile.getLastMonthRank());
			TextView lastYear = (TextView) activity.findViewById(R.id.stats_rank_last_year);
			lastYear.setText(ifpaProfile.getLastYearRank());
			TextView wpprValue = (TextView) activity.findViewById(R.id.stats_current_wppr_value);
			if (!TextUtils.isEmpty(ifpaProfile.getCurrentWpprValue())) {
				wpprValue.setText(String.format(Locale.getDefault(), "%.2f", Float.valueOf(ifpaProfile.getCurrentWpprValue())));
			}
			ListView championshipSeries = (ListView) activity.findViewById(R.id.championshipSeries);
			final Map<String, String> series = ifpaProfile.getRegionalChampionships();
			if (series != null) {
				String[] keys = new String[series.size()];
				int i = 0;
				for (String key : series.keySet()) {
					keys[i++] = key;
				}

				if (championshipSeries != null) {
					championshipSeries.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_2,
							android.R.id.text1, keys) {
						@Override
						public View getView(int position, View convertView, ViewGroup parent) {
							View view = super.getView(position, convertView, parent);
							String key = getItem(position);
							TextView text1 = (TextView) view.findViewById(android.R.id.text1);
							TextView text2 = (TextView) view.findViewById(android.R.id.text2);
							text1.setText(key);
							text2.setText(getResources().getString(R.string.championship_rank_label) + series.get(key));
							return view;
						}
					});
				}
			} else {
				String[] keys = new String[1];
				keys[0] = getResources().getString(R.string.no_eligible_events);
				if (championshipSeries != null) {
					championshipSeries.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1,
							android.R.id.text1, keys));
				}
			}
		}
	}
}
