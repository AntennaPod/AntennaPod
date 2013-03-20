package de.danoeh.antennapod.adapter;

import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
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
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.Converter;
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
				int sz=unreadItems.size();
				if(sz>0) {
					if(childPosition>=sz) {
						childPosition=sz-1;
					}
					return unreadItems.get(childPosition);
				}
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
		
		if(item==null) {
			return(convertView);
		}
		
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
			holder.statusUnread = (View) convertView
					.findViewById(R.id.statusUnread);
			holder.statusInProgress = (TextView) convertView
					.findViewById(R.id.statusInProgress);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}

		holder.title.setText(item.getTitle());
		holder.feedTitle.setText(item.getFeed().getTitle());

		if (groupPosition == GROUP_POS_QUEUE) {
			FeedItem.State state = item.getState();
			switch (state) {
			case PLAYING:
				holder.statusPlaying.setVisibility(View.VISIBLE);
				holder.statusUnread.setVisibility(View.GONE);
				holder.statusInProgress.setVisibility(View.GONE);
				break;
			case IN_PROGRESS:
				holder.statusPlaying.setVisibility(View.GONE);
				holder.statusUnread.setVisibility(View.GONE);
				holder.statusInProgress.setVisibility(View.VISIBLE);
				holder.statusInProgress.setText(Converter
						.getDurationStringLong(item.getMedia().getPosition()));
				break;
			case NEW:
				holder.statusPlaying.setVisibility(View.GONE);
				holder.statusUnread.setVisibility(View.VISIBLE);
				holder.statusInProgress.setVisibility(View.GONE);
				break;
			default:
				holder.statusPlaying.setVisibility(View.GONE);
				holder.statusUnread.setVisibility(View.GONE);
				holder.statusInProgress.setVisibility(View.GONE);
				break;
			}
		} else {
			holder.statusPlaying.setVisibility(View.GONE);
			holder.statusUnread.setVisibility(View.GONE);
			holder.statusInProgress.setVisibility(View.GONE);
		}

		FeedMedia media = item.getMedia();
		if (media != null) {
			TypedArray drawables = context.obtainStyledAttributes(new int[] {
					R.attr.av_download, R.attr.navigation_refresh });
			holder.lenSize.setVisibility(View.VISIBLE);
			if (!media.isDownloaded()) {
				if (DownloadRequester.getInstance().isDownloadingFile(media)) {
					holder.downloadStatus.setVisibility(View.VISIBLE);
					holder.downloadStatus.setImageDrawable(drawables
							.getDrawable(1));
				} else {
					holder.downloadStatus.setVisibility(View.GONE);
				}
				holder.lenSize.setText(context.getString(R.string.size_prefix)
						+ Converter.byteToString(media.getSize()));
			} else {
				holder.downloadStatus.setVisibility(View.VISIBLE);
				holder.downloadStatus
						.setImageDrawable(drawables.getDrawable(0));
				holder.lenSize.setText(context
						.getString(R.string.length_prefix)
						+ Converter.getDurationStringLong(media.getDuration()));
			}
		} else {
			holder.downloadStatus.setVisibility(View.GONE);
			holder.lenSize.setVisibility(View.INVISIBLE);
		}

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
		TextView feedTitle;
		TextView lenSize;
		ImageView downloadStatus;
		ImageView feedImage;
		ImageButton butAction;
		View statusUnread;
		View statusPlaying;
		TextView statusInProgress;
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
				headerString += " (" + getChildrenCount(GROUP_POS_QUEUE) + ")";
			}
		} else {
			headerString = context.getString(R.string.new_label);
			if (!unreadItems.isEmpty()) {
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
