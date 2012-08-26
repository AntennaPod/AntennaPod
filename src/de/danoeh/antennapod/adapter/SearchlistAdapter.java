package de.danoeh.antennapod.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.FeedImageLoader;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedComponent;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.SearchResult;

/** List adapter for search activity. */
public class SearchlistAdapter extends ArrayAdapter<SearchResult> {

	public SearchlistAdapter(Context context, int textViewResourceId,
			List<SearchResult> objects) {
		super(context, textViewResourceId, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final Holder holder;
		SearchResult result = getItem(position);
		FeedComponent component = result.getComponent();

		// Inflate Layout
		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			convertView = inflater.inflate(R.layout.searchlist_item, null);
			holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
			holder.cover = (ImageView) convertView
					.findViewById(R.id.imgvFeedimage);
			holder.subtitle = (TextView) convertView
					.findViewById(R.id.txtvSubtitle);

			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}
		if (component.getClass() == Feed.class) {
			final Feed feed = (Feed) component;
			holder.title.setText(feed.getTitle());
			holder.subtitle.setVisibility(View.GONE);
			holder.cover.post(new Runnable() {

				@Override
				public void run() {
					FeedImageLoader.getInstance().loadThumbnailBitmap(
							feed.getImage(), holder.cover);
				}
			});

		} else if (component.getClass() == FeedItem.class) {
			final FeedItem item = (FeedItem) component;
			holder.title.setText(item.getTitle());
			if (result.getSubtitle() != null) {
				holder.subtitle.setVisibility(View.VISIBLE);
				holder.subtitle.setText(result.getSubtitle());
			}
			holder.cover.post(new Runnable() {

				@Override
				public void run() {
					FeedImageLoader.getInstance().loadThumbnailBitmap(
							item.getFeed().getImage(), holder.cover);
				}
			});

		}

		return convertView;
	}

	static class Holder {
		ImageView cover;
		TextView title;
		TextView subtitle;
	}

}
