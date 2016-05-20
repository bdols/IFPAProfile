/*
 * Copyright (c) 2015, Brian Dols <brian.dols@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package net.perspiratron.ifpaprofile;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class IFPAProfileAdapter extends ArrayAdapter<Object> {
	private final LayoutInflater inflater;

	public IFPAProfileAdapter(SearchActivity searchActivity, int profile, Object[] ifpaProfiles) {
		super(searchActivity, profile, ifpaProfiles);
		inflater = LayoutInflater.from(searchActivity);
		setNotifyOnChange(false);
	}

	@Override
	public long getItemId(int position) {
		return ((IFPAProfile) getItem(position)).getPlayer_id();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		IFPAProfile ifpaProfile = (IFPAProfile) getItem(position);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.profile, parent, false);
		}
		TextView playerLocation = (TextView) convertView.findViewById(R.id.player_location);
		TextView playerName = (TextView) convertView.findViewById(R.id.player_name);
		TextView playerCountry = (TextView) convertView.findViewById(R.id.player_country);
		TextView playerCurrentRank = (TextView) convertView.findViewById(R.id.player_current_rank);
		TextView playerInitials = (TextView) convertView.findViewById(R.id.player_initials);
		TextView playerId = (TextView) convertView.findViewById(R.id.ifpa_id);
//		CheckBox playerRegistered = (CheckBox) convertView.findViewById(R.id.player_registered);
		if (TextUtils.isEmpty(ifpaProfile.getCity()) && TextUtils.isEmpty(ifpaProfile.getState())) {
			playerLocation.setVisibility(View.GONE);
		} else {
			if (TextUtils.isEmpty(ifpaProfile.getState())) {
				playerLocation.setText(ifpaProfile.getCity());
			} else {
				playerLocation.setText(ifpaProfile.getCity() + ", " + ifpaProfile.getState());
			}
		}
		playerCountry.setText(ifpaProfile.getCountryName());
		ImageView countryFlag = (ImageView) convertView.findViewById(R.id.player_country_flag);
		int resId = getContext().getResources().getIdentifier(ifpaProfile.getCountryCode().toLowerCase(), "drawable", "net.perspiratron.ifpaprofile");
		if (resId != 0) {
			countryFlag.setImageResource(resId);
		}
		playerCurrentRank.setText("#" + ifpaProfile.getWpprRank().toString());
		playerName.setText(ifpaProfile.getFirstName() + " " + ifpaProfile.getLastName());
		playerInitials.setVisibility(View.INVISIBLE);
		playerId.setText(ifpaProfile.getPlayer_id().toString());
//		playerRegistered.setVisibility(View.INVISIBLE);
		View detailView = convertView.findViewById(R.id.profile_detail);
		detailView.setVisibility(View.GONE);
		return convertView;
	}
}
