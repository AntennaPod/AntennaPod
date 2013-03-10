package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.ImageLoader;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.Converter;

/**
 * Displays unread items and items in the queue in one combined list. The
 * structure of this list is: [header] [queueItems] [header] [unreadItems].
 */
public class ExternalEpisodesListAdapter extends BaseExpandableListAdapter {
	private static final String TAG = "ExternalEpisodesListAdapter";

	public static final int GROUP_POS_QUEUE = 0;
	public static final int GROUP_POS_UNREAD = 1;

	private Context context;
	private FeedManager manager = FeedManager.getInstance();

	private ActionButtonCallback feedItemActionCallback;
	private OnGroupActionClicked groupActionCallback;

	public ExternalEpisodesListAdapter(Context context,
			ActionButtonCallback callback,
			OnGroupActionClicked groupActionCallback) {
		super();
		this.context = context;

		this.feedItemActionCallback = callback;
		this.groupActionCallback = groupActionCallback;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public FeedItem getChild(int groupPosition, int childPosition) {
		if (groupPosition == GROUP_POS_QUEUE) {
			return manager.getQueueItemAtIndex(childPosition, true);
		} else if (groupPosition == GROUP_POS_UNREAD) {
			return manager.getUnreadItemAtIndex(childPosition, true);
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
			holder.feedTitle = (TextView) convertView
					.findViewById(R.id.txtvFeedname);
			holder.lenSize = (TextView) convertView
					.findViewById(R.id.txtvLenSize);
			holder.downloadStatus = (ImageView) convertView
					.findViewById(R.id.imgvDownloadStatus);
			holder.feedImage = (ImageView) convertView
					.findViewById(R.id.imgvFeedimage);
			holder.butAction = (ImageButton) convertView
					.findViewById(R.id.butAction);
			holder.statusPlaying = (View) convertView
					.findViewById(R.id.statusPlaying);
			holder.episodeProgress = (ProgressBar) convertView
					.findViewById(R.id.pbar_episode_progress);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}

		holder.title.setText(item.getTitle());
		holder.feedTitle.setText(item.getFeed().getTitle());
		FeedItem.State state = item.getState();

		if (groupPosition == GROUP_POS_QUEUE) {
			switch (state) {
			case PLAYING:
				holder.statusPlaying.setVisibility(View.VISIBLE);
				holder.episodeProgress.setVisibility(View.VISIBLE);
				break;
			case IN_PROGRESS:
				holder.statusPlaying.setVisibility(View.GONE);
				holder.episodeProgress.setVisibility(View.VISIBLE);
				break;
			case NEW:
				holder.statusPlaying.setVisibility(View.GONE);
				holder.episodeProgress.setVisibility(View.GONE);
				break;
			default:
				holder.statusPlaying.setVisibility(View.GONE);
				holder.episodeProgress.setVisibility(View.GONE);
				break;
			}
		} else {
			holder.statusPlaying.setVisibility(View.GONE);
			holder.episodeProgress.setVisibility(View.GONE);
		}

		FeedMedia media = item.getMedia();
		if (media != null) {

			if (state == FeedItem.State.PLAYING
					|| state == FeedItem.State.IN_PROGRESS) {
				if (media.getDuration() > 0) {
					holder.episodeProgress.setProgress((int) (((double) media
							.getPosition()) / media.getDuration() * 100));
					holder.lenSize.setText(Converter
							.getDurationStringLong(media.getDuration()
									- media.getPosition()));
				}
			} else if (!media.isDownloaded()) {
				holder.lenSize.setText(context.getString(R.string.size_prefix)
						+ Converter.byteToString(media.getSize()));
			} else {
				holder.lenSize.setText(context
						.getString(R.string.length_prefix)
						+ Converter.getDurationStringLong(media.getDuration()));
			}

			TypedArray drawables = context.obtainStyledAttributes(new int[] {
					R.attr.av_download, R.attr.navigation_refresh });
			holder.lenSize.setVisibility(View.VISIBLE);
			if (!media.isDownloaded()) {
				if (DownloadRequester.getInstance().isDownloadingFile(media)) {
					holder.downloadStatus.setVisibility(View.VISIBLE);
					holder.downloadStatus.setImageDrawable(drawables
							.getDrawable(1));
				} else {
					holder.downloadStatus.setVisibility(View.INVISIBLE);
				}
			} else {
				holder.downloadStatus.setVisibility(View.VISIBLE);
				holder.downloadStatus
						.setImageDrawable(drawables.getDrawable(0));
			}
		} else {
			holder.downloadStatus.setVisibility(View.INVISIBLE);
			holder.lenSize.setVisibility(View.INVISIBLE);
		}

		holder.feedImage.setTag((item.getFeed().getImage() != null) ? item
				.getFeed().getImage().getFile_url() : null);
		ImageLoader.getInstance().loadThumbnailBitmap(
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
		TextView feedTitle;
		TextView lenSize;
		ImageView downloadStatus;
		ImageView feedImage;
		ImageButton butAction;
		View statusPlaying;
		ProgressBar episodeProgress;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		if (groupPosition == GROUP_POS_QUEUE) {
			return manager.getQueueSize(true);
		} else if (groupPosition == GROUP_POS_UNREAD) {
			return manager.getUnreadItemsSize(true);
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
			if (manager.getQueueSize(true) > 0) {
				headerString += " (" + getChildrenCount(GROUP_POS_QUEUE) + ")";
			}
		} else {
			headerString = context.getString(R.string.waiting_list_label);
			if (manager.getUnreadItemsSize(true) > 0) {
				headerString += " (" + getChildrenCount(GROUP_POS_UNREAD) + ")";
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
		return manager.getUnreadItemsSize(true) == 0
				&& manager.getQueueSize(true) == 0;
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
