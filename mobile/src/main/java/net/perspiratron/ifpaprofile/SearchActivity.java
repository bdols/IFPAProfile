/*
 * Copyright (c) 2016, Brian Dols <brian.dols@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package net.perspiratron.ifpaprofile;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.AsyncTaskLoader;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class SearchActivity extends ListActivity implements LoaderManager.LoaderCallbacks<JSONObject> {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			Bundle bundle = new Bundle();
			bundle.putString("query", query);
			Log.d("ifpaprofile", query);
			this.getLoaderManager().initLoader(0, bundle, this);
		}

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		IFPAProfile ifpaProfile = (IFPAProfile) l.getItemAtPosition(position);
		Intent mainIntent = new Intent(this, MainActivity.class);
		mainIntent.putExtra("profile_id", ifpaProfile.getPlayer_id());
		startActivity(mainIntent);
	}


	@Override
	public Loader<JSONObject> onCreateLoader(int id, Bundle args) {
		if (args != null) {
			final String query = args.getString("query");


			return new AsyncTaskLoader<JSONObject>(this) {
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
						String pairs = new String();
						pairs += "q=" + URLEncoder.encode(query, "UTF-8");
						pairs += "&api_key=" + BuildConfig.IFPA_API_KEY;
						String urlString = "https://api.ifpapinball.com/v1/player/search?" + pairs;
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
		return null;
	}

	@Override
	public void onLoadFinished(Loader<JSONObject> loader, JSONObject data) {
		try {
			JSONArray searchResults = data.getJSONArray("search");
			List<IFPAProfile> profiles = new ArrayList<IFPAProfile>();
			for (int i = 0; i < searchResults.length(); i++) {
				profiles.add(IFPAProfile.fromJson(searchResults.getJSONObject(i)));
			}
			setListAdapter(new IFPAProfileAdapter(this, R.layout.profile, profiles.toArray()));
		} catch (JSONException | NullPointerException e) {
			e.printStackTrace();
			Toast.makeText(this, "No players found", Toast.LENGTH_LONG).show();
			Intent mainIntent = new Intent(this, MainActivity.class);
			startActivity(mainIntent);
		}
	}

	@Override
	public void onLoaderReset(Loader<JSONObject> loader) {

	}
}
