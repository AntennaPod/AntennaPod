package de.podfetcher.adapter;

import java.io.File;
import java.text.DateFormat;
import java.util.List;

import de.podfetcher.R;
import de.podfetcher.feed.Feed;
import android.content.Context;
import android.net.Uri;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.BitmapFactory;
import android.graphics.Color;

public class FeedlistAdapter extends ArrayAdapter<Feed> {

	public FeedlistAdapter(Context context, int textViewResourceId,
			List<Feed> objects) {
		super(context, textViewResourceId, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		Feed feed = getItem(position);

		// Inflate Layout
		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			convertView = inflater.inflate(R.layout.feedlist_item, null);
			holder.title = (TextView) convertView
					.findViewById(R.id.txtvFeedname);
			holder.newEpisodes = (TextView) convertView.findViewById(R.id.txtvNewEpisodes);
			holder.image = (ImageView) convertView
					.findViewById(R.id.imgvFeedimage);
			holder.lastUpdate = (TextView) convertView
					.findViewById(R.id.txtvLastUpdate);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}

		holder.title.setText(feed.getTitle());
		holder.lastUpdate.setText(DateUtils.formatSameDayTime(feed
				.getLastUpdate().getTime(), System.currentTimeMillis(),
				DateFormat.SHORT, DateFormat.SHORT));
		
		int newItems = feed.getNumOfNewItems();
		if (newItems > 0) {
			holder.newEpisodes.setTextColor(Color.parseColor("red"));
			if (newItems == 1) {
				holder.newEpisodes.setText(newItems + " new Episode");
			} else {
				holder.newEpisodes.setText(newItems + " new Episodes");
			}
		} else {
			holder.newEpisodes.setTextColor(Color.parseColor("gray"));
			holder.newEpisodes.setText("No new episodes");
		}
		if (feed.getImage() != null) {
			holder.image.setImageBitmap(feed.getImage().getImageBitmap()); // TODO
																			// select
																			// default
																			// picture
																			// when
																			// no
																			// image
																			// downloaded
		}
		// TODO find new Episodes txtvNewEpisodes.setText(feed)
		return convertView;
	}

	static class Holder {
		TextView title;
		TextView lastUpdate;
		TextView newEpisodes;
		ImageView image;
	}

}
