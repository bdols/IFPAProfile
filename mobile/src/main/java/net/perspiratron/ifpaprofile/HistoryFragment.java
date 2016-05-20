/*
 * Copyright (c) 2016, Brian Dols <brian.dols@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package net.perspiratron.ifpaprofile;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;

public class HistoryFragment extends Fragment implements LoaderManager.LoaderCallbacks<JSONObject>, ProfileUpdater {
	private int profile_id;
	private float maxWppr;
	private TreeMap<Date, Integer> rankPosition;
	private TreeMap<Date, Float> wpprHistory;

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null && rankPosition != null && wpprHistory != null) {
			plotData();
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
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.graph, container, false);
		if (savedInstanceState != null) {
			maxWppr = savedInstanceState.getFloat("maxWppr");
			Serializable rankPositionObject = savedInstanceState.getSerializable("rankPosition");
			Serializable wpprHistoryObject = savedInstanceState.getSerializable("wpprHistory");
			try {
				if (rankPositionObject != null) {
					rankPosition = new TreeMap<Date, Integer>((HashMap<Date, Integer>) rankPositionObject);
				}
				if (wpprHistoryObject != null) {
					wpprHistory = new TreeMap<Date, Float>((HashMap<Date, Float>) wpprHistoryObject);
				}
			} catch (ClassCastException exc) {
				rankPosition = (TreeMap<Date, Integer>) rankPositionObject;
				wpprHistory = (TreeMap<Date, Float>) wpprHistoryObject;
			}
		}
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		IFPAApplication.logAnalyticsHit(this.getClass(), getActivity());
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putFloat("maxWppr", maxWppr);
		outState.putSerializable("rankPosition", rankPosition);
		outState.putSerializable("wpprHistory", wpprHistory);
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
					String urlString = "https://api.ifpapinball.com/v1/player/" + profile_id + "/history?api_key=" + BuildConfig.IFPA_API_KEY;
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
	public void onLoadFinished(Loader<JSONObject> jsonObjectLoader, JSONObject data) {
		try {
			JSONArray rank_history = data.getJSONArray("rank_history");
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			rankPosition = new TreeMap<Date, Integer>();
			wpprHistory = new TreeMap<Date, Float>();
			maxWppr = 0;
			for (int i = 0; i < rank_history.length(); i++) {
				JSONObject point = (JSONObject) rank_history.get(i);
				Log.d("ifpapoint", point.getString("rank_date"));
				try {
					rankPosition.put(dateFormat.parse(point.getString("rank_date")), Integer.valueOf(point.getString("rank_position")));
					wpprHistory.put(dateFormat.parse(point.getString("rank_date")), Float.valueOf(point.getString("wppr_points")));
					if (maxWppr < Float.valueOf(point.getString("wppr_points"))) {
						maxWppr = Float.valueOf(point.getString("wppr_points"));
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			plotData();
		} catch (JSONException | NullPointerException e) {
			e.printStackTrace();
		}

	}

	private void plotData() {
		DataPoint[] points = new DataPoint[rankPosition.size()];
		DataPoint[] secondPoints = new DataPoint[wpprHistory.size()];
		int counter = 0;
		for (Date dateKey : rankPosition.keySet()) {
			points[counter] = new DataPoint(dateKey, rankPosition.get(dateKey));
			secondPoints[counter++] = new DataPoint(dateKey, wpprHistory.get(dateKey));
		}
		GraphView graph = (GraphView) getActivity().findViewById(R.id.history_graph);
		LineGraphSeries<DataPoint> lineGraphSeries = new LineGraphSeries<DataPoint>(points);
		LineGraphSeries<DataPoint> secondGraphSeries = new LineGraphSeries<DataPoint>(secondPoints);
		lineGraphSeries.setColor(Color.BLUE);
		lineGraphSeries.setTitle(getResources().getString(R.string.stats_current_rank_label));
		secondGraphSeries.setTitle(getResources().getString(R.string.stats_current_wppr_value_label));
		secondGraphSeries.setColor(Color.RED);

		graph.addSeries(lineGraphSeries);
		graph.getSecondScale().addSeries(secondGraphSeries);
		graph.getSecondScale().setMinY(0);
		graph.getSecondScale().setMaxY(maxWppr);
		graph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.RED);

		int numLabels = 2;
		int orientation = getActivity().getResources().getConfiguration().orientation;

		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			graph.getGridLabelRenderer().setNumHorizontalLabels(points.length / 12 + 2);
			if (points.length / 12 > 22) {
				numLabels += points.length / 24;
				Log.d("ifpapoint2", "div24 " + numLabels);
			} else {
				numLabels += points.length / 12;
				Log.d("ifpapoint2", "div12 " + numLabels);
			}
		} else {
			if (points.length / 12 > 20) {
				numLabels += points.length / 48;
				Log.d("ifpapoint2", "portrait div48 " + numLabels);
			} else if (points.length / 12 > 10) {
				numLabels += points.length / 24;
				Log.d("ifpapoint2", "portrait div24 " + numLabels);
			} else {
				numLabels += points.length / 12;
				Log.d("ifpapoint2", "portrait div12 " + numLabels);
			}
		}
		Log.d("ifpapoint2", "numLabels " + numLabels);
		DateFormat dateFormat1 = new SimpleDateFormat("yy", Locale.getDefault());
		if (numLabels < 5) {
			dateFormat1 = new SimpleDateFormat("yyyy", Locale.getDefault());
		}
		graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity(), dateFormat1));
		graph.getGridLabelRenderer().setNumHorizontalLabels(numLabels);
		graph.getLegendRenderer().setVisible(true);
		graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
		graph.getLegendRenderer().setMargin(100);

		graph.getViewport().setXAxisBoundsManual(true);
		graph.getViewport().setMinX(points[0].getX());
		graph.getViewport().setMaxX(points[points.length - 1].getX());
		graph.getViewport().setScalable(true);
		graph.getViewport().setScrollable(true);
	}

	@Override
	public void onLoaderReset(Loader<JSONObject> jsonObjectLoader) {
		Log.d("ifparesponse", "got to history loader reset");
	}

	@Override
	public void setIFPAProfile(IFPAProfile ifpaProfile) {

	}
}
