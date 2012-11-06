package de.danoeh.antennapod.adapter;

import java.text.DateFormat;
import java.util.List;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.FeedImageLoader;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.storage.DownloadRequester;

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
		final Holder holder;
		final Feed feed = getItem(position);

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
			holder.inProgressEpisodes = (TextView) convertView
					.findViewById(R.id.txtvProgressEps);
			holder.newEpisodesLabel = (View) convertView
					.findViewById(R.id.lNewStatusLabel);
			holder.inProgressEpisodesLabel = (View) convertView
					.findViewById(R.id.lProgressStatusLabel);
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
							DateFormat.MEDIUM, DateFormat.SHORT));
		}
		holder.numberOfEpisodes.setText(feed.getNumOfItems()
				+ convertView.getResources()
						.getString(R.string.episodes_suffix));

		int newItems = feed.getNumOfNewItems();
		int inProgressItems = feed.getNumOfStartedItems();

		if (newItems > 0) {
			holder.newEpisodes.setText(Integer.toString(newItems));
			holder.newEpisodesLabel.setVisibility(View.VISIBLE);
		} else {
			holder.newEpisodesLabel.setVisibility(View.INVISIBLE);
		}

		if (inProgressItems > 0) {
			holder.inProgressEpisodes
					.setText(Integer.toString(inProgressItems));
			holder.inProgressEpisodesLabel.setVisibility(View.VISIBLE);
		} else {
			holder.inProgressEpisodesLabel.setVisibility(View.INVISIBLE);
		}

		holder.image.setTag(feed.getImage());

		imageLoader.loadThumbnailBitmap(
				feed.getImage(),
				holder.image,
				(int) convertView.getResources().getDimension(
						R.dimen.thumbnail_length));

		return convertView;
	}

	static class Holder {
		TextView title;
		TextView lastUpdate;
		TextView numberOfEpisodes;
		TextView newEpisodes;
		TextView inProgressEpisodes;
		ImageView image;
		View newEpisodesLabel;
		View inProgressEpisodesLabel;
	}

	public int getSelectedItemIndex() {
		return selectedItemIndex;
	}

	public void setSelectedItemIndex(int selectedItemIndex) {
		this.selectedItemIndex = selectedItemIndex;
		notifyDataSetChanged();
	}

}
