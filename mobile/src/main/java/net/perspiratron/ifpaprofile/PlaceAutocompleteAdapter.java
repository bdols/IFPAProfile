/*
 * Copyright (c) 2015, Brian Dols <brian.dols@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package net.perspiratron.ifpaprofile;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class PlaceAutocompleteAdapter extends ArrayAdapter<PlaceAutocompleteAdapter.PlaceAutocomplete> implements Filterable {
	private Context context;
	private GoogleApiClient googleApiClient;
	private LatLngBounds bounds;
	private AutocompleteFilter filter;
	private ArrayList<PlaceAutocomplete> resultList;

	public PlaceAutocompleteAdapter(Context context, int resource, GoogleApiClient googleApiClient, LatLngBounds bounds, AutocompleteFilter filter) {
		super(context, resource);
		this.context = context;
		this.googleApiClient = googleApiClient;
		this.bounds = bounds;
		this.filter = filter;
	}

	@Override
	public int getCount() {
		return resultList.size();
	}

	@Override
	public PlaceAutocomplete getItem(int position) {
		return resultList.get(position);
	}

	@Override
	public Filter getFilter() {
		Filter filter = new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();
				// Skip the autocomplete query if no constraints are given.
				if (constraint != null) {
					// Query the autocomplete API for the (constraint) search string.
					resultList = getAutocomplete(constraint);
					if (resultList != null) {
						// The API successfully returned results.
						results.values = resultList;
						results.count = resultList.size();
					}
				}
				return results;
			}

			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				if (results != null && results.count > 0) {
					// The API returned at least one result, update the data.
					notifyDataSetChanged();
				} else {
					// The API did not return any results, invalidate the data set.
					notifyDataSetInvalidated();
				}
			}
		};
		return filter;
	}

	private ArrayList<PlaceAutocomplete> getAutocomplete(CharSequence constraint) {
		if (googleApiClient.isConnected()) {
			Log.d("ifpalocation", "Starting autocomplete query for: " + constraint);

			// Submit the query to the autocomplete API and retrieve a PendingResult that will
			// contain the results when the query completes.
			PendingResult<AutocompletePredictionBuffer> results =
					Places.GeoDataApi
							.getAutocompletePredictions(googleApiClient, constraint.toString(),
									bounds, filter);

			// This method should have been called off the main UI thread. Block and wait for at most 60s
			// for a result from the API.
			AutocompletePredictionBuffer autocompletePredictions = results
					.await(60, TimeUnit.SECONDS);

			final Status status = autocompletePredictions.getStatus();
			if (!status.isSuccess()) {
				Toast.makeText(getContext(), "Error contacting API: " + status.toString(),
						Toast.LENGTH_SHORT).show();
				Log.e("ifpalocation", "Error getting autocomplete prediction API call: " + status.toString());
				autocompletePredictions.release();
				return null;
			}

			Log.d("ifpalocation", "Query completed. Received " + autocompletePredictions.getCount()
					+ " predictions");

			// Copy the results into our own data structure, because we can't hold onto the buffer.
			// AutocompletePrediction objects encapsulate the API response (place ID and description).

			Iterator<AutocompletePrediction> iterator = autocompletePredictions.iterator();
			ArrayList<PlaceAutocomplete> resultList = new ArrayList<>(autocompletePredictions.getCount());
			while (iterator.hasNext()) {
				AutocompletePrediction prediction = iterator.next();
				// Get the details of this prediction and copy it into a new PlaceAutocomplete object.
				resultList.add(new PlaceAutocomplete(prediction.getPlaceId(),
						prediction.getDescription()));
			}

			// Release the buffer now that all data has been copied.
			autocompletePredictions.release();

			return resultList;
		}
		Log.e("ifpalocation", "Google API client is not connected for autocomplete query.");
		return null;
	}

	class PlaceAutocomplete {

		public CharSequence placeId;
		public CharSequence description;

		PlaceAutocomplete(CharSequence placeId, CharSequence description) {
			this.placeId = placeId;
			this.description = description;
		}

		@Override
		public String toString() {
			return description.toString();
		}
	}

}
