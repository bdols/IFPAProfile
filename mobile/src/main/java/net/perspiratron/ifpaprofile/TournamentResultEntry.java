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

public class TournamentResultEntry implements Serializable {
	private String tournament_name;
	private String tournament_id;
	private String country_code;
	private String periodic_flag;
	private String wppr_state;
	private String event_name;
	private String event_date;
	private String position;
	private String original_points;
	private String current_points;


	public static TournamentResultEntry fromJSON(JSONObject jsonObject) {
		TournamentResultEntry tournamentResultEntry = new TournamentResultEntry();
		try {
			tournamentResultEntry.tournament_name = jsonObject.getString("tournament_name");
			tournamentResultEntry.tournament_id = jsonObject.getString("tournament_id");
			tournamentResultEntry.country_code = jsonObject.getString("country_code");
			tournamentResultEntry.periodic_flag = jsonObject.getString("periodic_flag");
			tournamentResultEntry.wppr_state = jsonObject.getString("wppr_state");
			tournamentResultEntry.event_name = jsonObject.getString("event_name");
			tournamentResultEntry.event_date = jsonObject.getString("event_date");
			tournamentResultEntry.position = jsonObject.getString("position");
			tournamentResultEntry.original_points = jsonObject.getString("original_points");
			tournamentResultEntry.current_points = jsonObject.getString("current_points");
			return tournamentResultEntry;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getTournament_name() {
		return tournament_name;
	}

	public String getTournament_id() {
		return tournament_id;
	}

	public String getCountry_code() {
		return country_code;
	}

	public String getPeriodic_flag() {
		return periodic_flag;
	}

	public String getWppr_state() {
		return wppr_state;
	}

	public String getEvent_name() {
		return event_name;
	}

	public String getEvent_date() {
		return event_date;
	}

	public String getPosition() {
		return position;
	}

	public String getOriginal_points() {
		return original_points;
	}

	public String getCurrent_points() {
		return current_points;
	}

}
