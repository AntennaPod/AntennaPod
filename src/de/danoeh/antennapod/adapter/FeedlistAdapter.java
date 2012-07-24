package de.danoeh.antennapod.adapter;

import java.io.File;
import java.text.DateFormat;
import java.util.List;

import de.danoeh.antennapod.asynctask.FeedImageLoader;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.R;
import android.content.Context;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;
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
	private static final String TAG = "FeedlistAdapter";

	private int selectedItemIndex;
	private FeedImageLoader imageLoader;
	public static final int SELECTION_NONE = -1;

	public FeedlistAdapter(Context context, int textViewResourceId,
			List<Feed> objects) {
		super(context, textViewResourceId, objects);
		selectedItemIndex = SELECTION_NONE;
		imageLoader = FeedImageLoader.getInstance();
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

			holder.newEpisodes = (TextView) convertView
					.findViewById(R.id.txtvNewEps);
			holder.image = (ImageView) convertView
					.findViewById(R.id.imgvFeedimage);
			holder.lastUpdate = (TextView) convertView
					.findViewById(R.id.txtvLastUpdate);
			holder.numberOfEpisodes = (TextView) convertView
					.findViewById(R.id.txtvNumEpisodes);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();

		}

		if (position == selectedItemIndex) {
			convertView.setBackgroundColor(convertView.getResources().getColor(
					R.color.selection_background));
		} else {
			convertView.setBackgroundResource(0);
		}

		holder.title.setText(feed.getTitle());
		if (DownloadRequester.getInstance().isDownloadingFile(feed)) {
			holder.lastUpdate.setText(R.string.refreshing_label);
		} else {
			holder.lastUpdate.setText(convertView.getResources().getString(
					R.string.last_update_prefix)
					+ DateUtils.formatSameDayTime(feed.getLastUpdate()
							.getTime(), System.currentTimeMillis(),
							DateFormat.SHORT, DateFormat.SHORT));
		}
		holder.numberOfEpisodes.setText(feed.getItems().size()
				+ convertView.getResources()
						.getString(R.string.episodes_suffix));
		int newItems = feed.getNumOfNewItems();
		if (newItems > 0) {
			holder.newEpisodes.setText(Integer.toString(newItems));
			holder.newEpisodes.setVisibility(View.VISIBLE);
		} else {
			holder.newEpisodes.setVisibility(View.INVISIBLE);
		}
		holder.image.setTag(feed.getImage());
		imageLoader.loadThumbnailBitmap(feed.getImage(), holder.image);
		
		// TODO find new Episodes txtvNewEpisodes.setText(feed)
		return convertView;
	}

	static class Holder {
		TextView title;
		TextView lastUpdate;
		TextView numberOfEpisodes;
		TextView newEpisodes;
		ImageView image;
	}

	public int getSelectedItemIndex() {
		return selectedItemIndex;
	}

	public void setSelectedItemIndex(int selectedItemIndex) {
		this.selectedItemIndex = selectedItemIndex;
		notifyDataSetChanged();
	}

}
