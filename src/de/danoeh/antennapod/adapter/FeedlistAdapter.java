package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.ImageLoader;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.ThemeUtils;

public class FeedlistAdapter extends BaseAdapter {
	private static final String TAG = "FeedlistAdapter";

	private Context context;
	private FeedManager manager = FeedManager.getInstance();

	private int selectedItemIndex;
	private ImageLoader imageLoader;
	public static final int SELECTION_NONE = -1;

	public FeedlistAdapter(Context context) {
		super();
		this.context = context;
		selectedItemIndex = SELECTION_NONE;
		imageLoader = ImageLoader.getInstance();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final Holder holder;
		final Feed feed = getItem(position);

		// Inflate Layout
		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) context
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
					ThemeUtils.getSelectionBackgroundColor()));
		} else {
			convertView.setBackgroundResource(0);
		}

		holder.title.setText(feed.getTitle());
		int numOfItems = feed.getNumOfItems(true);
		if (DownloadRequester.getInstance().isDownloadingFile(feed)) {
			holder.lastUpdate.setText(R.string.refreshing_label);
		} else {
			if (numOfItems > 0) {
				holder.lastUpdate.setText(convertView.getResources().getString(
						R.string.most_recent_prefix)
						+ DateUtils.getRelativeTimeSpanString(
								feed.getItemAtIndex(true, 0).getPubDate().getTime(),
								System.currentTimeMillis(), 0, 0));
			}
		}
		holder.numberOfEpisodes.setText(numOfItems
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

		final String imageUrl = (feed.getImage() != null) ? feed.getImage()
				.getFile_url() : null;
		holder.image.setTag(imageUrl);
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

	@Override
	public int getCount() {
		return manager.getFeedsSize();
	}

	@Override
	public Feed getItem(int position) {
		return manager.getFeedAtIndex(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

}
