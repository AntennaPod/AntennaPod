package de.danoeh.antennapod.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.FeedImageLoader;
import de.danoeh.antennapod.feed.FeedItem;

/**
 * Array adapter that should be used to display a list of FeedItems from several
 * different feeds.
 */
public class ExternalFeedItemlistAdapter extends AbstractFeedItemlistAdapter {

	ActionButtonCallback callback;

	public ExternalFeedItemlistAdapter(Context context, int textViewResourceId,
			List<FeedItem> objects,
			ActionButtonCallback callback) {
		super(context, textViewResourceId, objects);
		this.callback = callback;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		Holder holder;
		FeedItem item = getItem(position);

		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.external_itemlist_item, null);
			holder.title = (TextView) convertView
					.findViewById(R.id.txtvTitle);
			holder.feedImage = (ImageView) convertView
					.findViewById(R.id.imgvFeedimage);
			holder.butAction = (ImageButton) convertView
					.findViewById(R.id.butAction);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}

		holder.title.setText(item.getTitle());
		holder.feedImage.setTag(item.getFeed().getImage());
		FeedImageLoader.getInstance().loadThumbnailBitmap(
				item.getFeed().getImage(),
				holder.feedImage,
				(int) convertView.getResources().getDimension(
						R.dimen.thumbnail_length));
		holder.butAction.setFocusable(false);
		holder.butAction.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//callback.onActionButtonPressed(position);
			}
		});

		return convertView;

	}

	static class Holder {
		TextView title;
		ImageView feedImage;
		ImageButton butAction;
	}

	
}
