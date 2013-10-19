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
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.storage.FeedItemStatistics;
import de.danoeh.antennapod.util.ThemeUtils;

public class FeedlistAdapter extends BaseAdapter {
	private static final String TAG = "FeedlistAdapter";

	private Context context;
	protected ItemAccess itemAccess;

	private int selectedItemIndex;
	private ImageLoader imageLoader;
	public static final int SELECTION_NONE = -1;

	public FeedlistAdapter(Context context, ItemAccess itemAccess) {
		super();
		if (context == null) {
			throw new IllegalArgumentException("context must not be null");
		}
		if (itemAccess == null) {
			throw new IllegalArgumentException("itemAccess must not be null");
		}

		this.context = context;
		this.itemAccess = itemAccess;
		selectedItemIndex = SELECTION_NONE;
		imageLoader = ImageLoader.getInstance();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final Holder holder;
		final Feed feed = getItem(position);
        final FeedItemStatistics feedItemStatistics = itemAccess.getFeedItemStatistics(position);

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

        if (feedItemStatistics != null) {
            if (DownloadRequester.getInstance().isDownloadingFile(feed)) {
                holder.lastUpdate.setText(R.string.refreshing_label);
            } else {
                if (feedItemStatistics.lastUpdateKnown()) {
                    holder.lastUpdate.setText(convertView.getResources().getString(
                            R.string.most_recent_prefix)
                            + DateUtils.getRelativeTimeSpanString(
                            feedItemStatistics.getLastUpdate().getTime(),
                            System.currentTimeMillis(), 0, 0));
                } else {
                    holder.lastUpdate.setText("");
                }
            }
            holder.numberOfEpisodes.setText(feedItemStatistics.getNumberOfItems()
                    + convertView.getResources()
                    .getString(R.string.episodes_suffix));

            if (feedItemStatistics.getNumberOfNewItems() > 0) {
                holder.newEpisodes.setText(Integer.toString(feedItemStatistics.getNumberOfNewItems()));
                holder.newEpisodesLabel.setVisibility(View.VISIBLE);
            } else {
                holder.newEpisodesLabel.setVisibility(View.INVISIBLE);
            }

            if (feedItemStatistics.getNumberOfInProgressItems() > 0) {
                holder.inProgressEpisodes
                        .setText(Integer.toString(feedItemStatistics.getNumberOfInProgressItems()));
                holder.inProgressEpisodesLabel.setVisibility(View.VISIBLE);
            } else {
                holder.inProgressEpisodesLabel.setVisibility(View.INVISIBLE);
            }
        }
		final String imageUrl = (feed.getImage() != null) ? feed.getImage()
				.getFile_url() : null;
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
		return itemAccess.getCount();
	}

	@Override
	public Feed getItem(int position) {
		return itemAccess.getItem(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

    public interface ItemAccess {
        int getCount();

        Feed getItem(int position);

        FeedItemStatistics getFeedItemStatistics(int position);
    }
}
