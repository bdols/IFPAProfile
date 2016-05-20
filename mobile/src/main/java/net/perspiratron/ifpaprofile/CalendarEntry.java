/*
 * Copyright (c) 2015, Brian Dols <brian.dols@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package net.perspiratron.ifpaprofile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class CalendarEntry implements Serializable {

	private String tournamentId;
	private String tournamentName;
	private String address1;
	private String address2;
	private String city;
	private String state;
	private String zipCode;
	private String country;
	private String website;
	private boolean euroChamp;
	private boolean papaCircuit;
	private String directorName;
	private String latitude;
	private String longitude;
	private String details;
	private boolean eventPrivate;
	private int distance;
	private String startDate;
	private String endDate;

	public static CalendarEntry fromJSON(JSONObject jsonObject) {
		CalendarEntry calendarEntry = new CalendarEntry();
		try {

			calendarEntry.tournamentId = jsonObject.getString("tournament_id");
			calendarEntry.tournamentName = jsonObject.getString("tournament_name");
			calendarEntry.address1 = jsonObject.getString("address1");
			calendarEntry.address2 = jsonObject.getString("address2");
			calendarEntry.city = jsonObject.getString("city");
			calendarEntry.state = jsonObject.getString("state");
			if (jsonObject.has("zipcode")) {
				calendarEntry.zipCode = jsonObject.getString("zipcode");
				if (calendarEntry.zipCode.equals("null")) {
					calendarEntry.zipCode = "";
				}
			}
			calendarEntry.country = jsonObject.getString("country_name");
			if (jsonObject.has("website")) {
				calendarEntry.website = jsonObject.getString("website");
			}
			if (jsonObject.has("euro_champ_flag")) {
				calendarEntry.euroChamp = jsonObject.getString("euro_champ_flag").equals("Y");
			}
			if (jsonObject.has("papa_circuit_flag")) {
				calendarEntry.papaCircuit = jsonObject.getString("papa_circuit_flag").equals("Y");
			}
			if (jsonObject.has("director_name")) {
				calendarEntry.directorName = jsonObject.getString("director_name");
			}
			if (jsonObject.has("latitude")) {
				calendarEntry.latitude = jsonObject.getString("latitude");
			}
			if (jsonObject.has("longitude")) {
				calendarEntry.longitude = jsonObject.getString("longitude");
			}
			if (jsonObject.has("details")) {
				calendarEntry.details = jsonObject.getString("details");
			}
			if (jsonObject.has("private_flag")) {
				calendarEntry.eventPrivate = jsonObject.getString("private_flag").equals("Y");
			}
			if (jsonObject.has("distance")) {
				calendarEntry.distance = jsonObject.getInt("distance");
			}
			if (jsonObject.has("start_date")) {
				calendarEntry.startDate = jsonObject.getString("start_date");
			}
			if (jsonObject.has("end_date")) {
				calendarEntry.endDate = jsonObject.getString("end_date");
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return calendarEntry;
	}


	public String getTournamentId() {
		return tournamentId;
	}

	public String getTournamentName() {
		return tournamentName;
	}

	public String getAddress1() {
		return address1;
	}

	public String getAddress2() {
		return address2;
	}

	public String getCity() {
		return city;
	}

	public String getState() {
		return state;
	}

	public String getZipCode() {
		return zipCode;
	}

	public String getCountry() {
		return country;
	}

	public String getWebsite() {
		return website;
	}

	public boolean isEuroChamp() {
		return euroChamp;
	}

	public boolean isPapaCircuit() {
		return papaCircuit;
	}

	public String getDirectorName() {
		return directorName;
	}

	public String getLatitude() {
		return latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public String getDetails() {
		return details;
	}

	public boolean isEventPrivate() {
		return eventPrivate;
	}

	public int getDistance() {
		return distance;
	}

	public boolean hasLocation() {
		return latitude != null && longitude != null;
	}

	public String getStartDate() {
		return startDate;
	}

	public String getEndDate() {
		return endDate;
	}
}
