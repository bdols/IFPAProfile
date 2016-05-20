/*
 * Copyright (c) 2015, Brian Dols <brian.dols@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package net.perspiratron.ifpaprofile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

public class IFPAProfile implements Serializable {

	private Integer player_id;
	private String firstName;
	private String lastName;
	private String countryName;
	private String countryCode;
	private String city;
	private String state;
	private String age;
	private Integer wpprRank;
	private String initials;
	private boolean ifpaRegistered;
	private String highestRank;
	private String highestRankDate;
	private String currentWpprValue;
	private String lastMonthRank;
	private String lastYearRank;
	private String wpprPointsAllTime;
	private String totalEventsAllTime;
	private String totalActiveEvents;
	private String ratingsRank;
	private String ratingsValue;
	private String efficiencyRank;
	private String efficiencyValue;
	private Map<String, String> regionalChampionships;

	public Map<String, String> getRegionalChampionships() {
		return regionalChampionships;
	}

	public void addStats(JSONObject jsonObject) {
		try {
			if (jsonObject.has("current_wppr_rank")) {
				wpprRank = jsonObject.getInt("current_wppr_rank");
			}
			if (jsonObject.has("highest_rank")) {
				highestRank = jsonObject.getString("highest_rank");
			}
			if (jsonObject.has("highest_rank_date")) {
				highestRankDate = jsonObject.getString("highest_rank_date");
			}
			if (jsonObject.has("current_wppr_value")) {
				currentWpprValue = jsonObject.getString("current_wppr_value");
			}
			if (jsonObject.has("last_month_rank")) {
				lastMonthRank = jsonObject.getString("last_month_rank");
			}
			if (jsonObject.has("last_year_rank")) {
				lastYearRank = jsonObject.getString("last_year_rank");
			}
			if (jsonObject.has("wppr_points_all_time")) {
				wpprPointsAllTime = jsonObject.getString("wppr_points_all_time");
			}
			if (jsonObject.has("total_events_all_time")) {
				totalEventsAllTime = jsonObject.getString("total_events_all_time");
			}
			if (jsonObject.has("total_active_events")) {
				totalActiveEvents = jsonObject.getString("total_active_events");
			}
			if (jsonObject.has("ratings_rank")) {
				ratingsRank = jsonObject.getString("ratings_rank");
			}
			if (jsonObject.has("ratings_value")) {
				ratingsValue = jsonObject.getString("ratings_value");
			}
			if (jsonObject.has("efficiency_rank")) {
				efficiencyRank = jsonObject.getString("efficiency_rank");
			}
			if (jsonObject.has("efficiency_value")) {
				efficiencyValue = jsonObject.getString("efficiency_value");
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void addChampionshipSeries(JSONArray championshipSeries) {
		this.regionalChampionships = new TreeMap<>();
		for (int i = 0; i < championshipSeries.length(); i++) {
			try {
				JSONObject seriesData = (JSONObject) championshipSeries.get(i);
				this.regionalChampionships.put(seriesData.getString("group_name"), seriesData.getString("rank"));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public static IFPAProfile fromJson(JSONObject jsonObject) {
		IFPAProfile ifpaProfile = new IFPAProfile();
		try {
			ifpaProfile.player_id = jsonObject.getInt("player_id");
			ifpaProfile.firstName = jsonObject.getString("first_name");
			ifpaProfile.lastName = jsonObject.getString("last_name");
			ifpaProfile.countryName = jsonObject.getString("country_name");
			ifpaProfile.countryCode = jsonObject.getString("country_code");
			ifpaProfile.city = jsonObject.getString("city");
			ifpaProfile.state = jsonObject.getString("state");
			if (jsonObject.has("wppr_rank")) {
				ifpaProfile.wpprRank = jsonObject.getInt("wppr_rank");
			}
			if (jsonObject.has("current_wppr_rank")) {
				ifpaProfile.wpprRank = jsonObject.getInt("current_wppr_rank");
			}
			if (jsonObject.has("age")) {
				ifpaProfile.age = jsonObject.getString("age");
			}
			if (jsonObject.has("initials")) {
				ifpaProfile.initials = jsonObject.getString("initials");
			}
			if (jsonObject.has("ifpa_registered")) {
				ifpaProfile.ifpaRegistered = jsonObject.getString("ifpa_registered").equals("Y");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return ifpaProfile;
	}

	public Integer getPlayer_id() {
		return player_id;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getCountryName() {
		return countryName;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public String getCity() {
		return city;
	}

	public String getState() {
		return state;
	}

	public Integer getWpprRank() {
		return wpprRank;
	}

	public String getAge() {
		return age;
	}

	public String getInitials() {
		return initials;
	}

	public boolean isIfpaRegistered() {
		return ifpaRegistered;
	}

	public String getHighestRank() {
		return highestRank;
	}

	public String getHighestRankDate() {
		return highestRankDate;
	}

	public String getCurrentWpprValue() {
		return currentWpprValue;
	}

	public String getLastMonthRank() {
		return lastMonthRank;
	}

	public String getLastYearRank() {
		return lastYearRank;
	}

	public String getWpprPointsAllTime() {
		return wpprPointsAllTime;
	}

	public String getTotalEventsAllTime() {
		return totalEventsAllTime;
	}

	public String getTotalActiveEvents() {
		return totalActiveEvents;
	}

	public String getRatingsRank() {
		return ratingsRank;
	}

	public String getRatingsValue() {
		return ratingsValue;
	}

	public String getEfficiencyRank() {
		if (efficiencyRank.equals("Not Ranked")) {
			return "---";
		}
		return efficiencyRank;
	}

	public String getEfficiencyValue() {
		return efficiencyValue;
	}
}
