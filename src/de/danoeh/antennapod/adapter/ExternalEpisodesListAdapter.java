package de.danoeh.antennapod.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.FeedImageLoader;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.util.EpisodeFilter;

/**
 * Displays unread items and items in the queue in one combined list. The
 * structure of this list is: [header] [queueItems] [header] [unreadItems].
 */
public class ExternalEpisodesListAdapter extends BaseExpandableListAdapter {
	private static final String TAG = "ExternalEpisodesListAdapter";

	public static final int GROUP_POS_QUEUE = 0;
	public static final int GROUP_POS_UNREAD = 1;

	private Context context;

	private List<FeedItem> unreadItems;
	private List<FeedItem> queueItems;

	private ActionButtonCallback feedItemActionCallback;
	private OnGroupActionClicked groupActionCallback;

	public ExternalEpisodesListAdapter(Context context,
			List<FeedItem> unreadItems, List<FeedItem> queueItems,
			ActionButtonCallback callback,
			OnGroupActionClicked groupActionCallback) {
		super();
		this.context = context;
		this.unreadItems = unreadItems;
		this.queueItems = queueItems;
		this.feedItemActionCallback = callback;
		this.groupActionCallback = groupActionCallback;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public FeedItem getChild(int groupPosition, int childPosition) {
		final boolean displayOnlyEpisodes = PodcastApp.getInstance()
				.displayOnlyEpisodes();
		if (groupPosition == GROUP_POS_QUEUE) {
			if (displayOnlyEpisodes) {
				return EpisodeFilter.accessEpisodeByIndex(queueItems,
						childPosition);
			} else {
				return queueItems.get(childPosition);
			}
		} else if (groupPosition == GROUP_POS_UNREAD) {
			if (displayOnlyEpisodes) {
				return EpisodeFilter.accessEpisodeByIndex(unreadItems,
						childPosition);
			} else {
				return unreadItems.get(childPosition);
			}
		}
		return null;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, final int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		Holder holder;
		final FeedItem item = getChild(groupPosition, childPosition);

		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.external_itemlist_item,
					null);
			holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
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
				feedItemActionCallback.onActionButtonPressed(item);
			}
		});

		return convertView;

	}

	static class Holder {
		TextView title;
		ImageView feedImage;
		ImageButton butAction;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		final boolean displayOnlyEpisodes = PodcastApp.getInstance()
				.displayOnlyEpisodes();
		if (groupPosition == GROUP_POS_QUEUE) {
			if (displayOnlyEpisodes) {
				return EpisodeFilter.countItemsWithEpisodes(queueItems);
			} else {
				return queueItems.size();
			}
		} else if (groupPosition == GROUP_POS_UNREAD) {
			if (displayOnlyEpisodes) {
				return EpisodeFilter.countItemsWithEpisodes(unreadItems);
			} else {
				return unreadItems.size();
			}
		}
		return 0;
	}

	@Override
	public int getGroupCount() {
		return 2;
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(final int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		convertView = inflater.inflate(R.layout.feeditemlist_header, null);
		TextView headerTitle = (TextView) convertView
				.findViewById(R.id.txtvHeaderTitle);
		ImageButton actionButton = (ImageButton) convertView
				.findViewById(R.id.butAction);
		String headerString = null;
		if (groupPosition == 0) {
			headerString = context.getString(R.string.queue_label);
			if (!queueItems.isEmpty()) {
				headerString += " (" + queueItems.size() + ")";
			}
		} else {
			headerString = context.getString(R.string.new_label);
			if (!unreadItems.isEmpty()) {
				headerString += " (" + unreadItems.size() + ")";
			}
		}
		headerTitle.setText(headerString);
		actionButton.setFocusable(false);
		actionButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				groupActionCallback.onClick(getGroupId(groupPosition));
			}
		});
		return convertView;
	}

	@Override
	public boolean isEmpty() {
		return unreadItems.isEmpty() && queueItems.isEmpty();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return null;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	public interface OnGroupActionClicked {
		public void onClick(long groupId);
	}

}
