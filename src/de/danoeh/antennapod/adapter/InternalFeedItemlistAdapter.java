package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.feed.MediaType;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.Converter;
import de.danoeh.antennapod.util.ThemeUtils;

/** List adapter for items of feeds that the user has already subscribed to. */
public class InternalFeedItemlistAdapter extends DefaultFeedItemlistAdapter {

	private ActionButtonCallback callback;
	private boolean showFeedtitle;
	private int selectedItemIndex;

	public static final int SELECTION_NONE = -1;

	public InternalFeedItemlistAdapter(Context context,
			DefaultFeedItemlistAdapter.ItemAccess itemAccess,
			ActionButtonCallback callback, boolean showFeedtitle) {
		super(context, itemAccess);
		this.callback = callback;
		this.showFeedtitle = showFeedtitle;
		this.selectedItemIndex = SELECTION_NONE;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		Holder holder;
		final FeedItem item = getItem(position);

		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.feeditemlist_item, null);
			holder.title = (TextView) convertView
					.findViewById(R.id.txtvItemname);
			holder.lenSize = (TextView) convertView
					.findViewById(R.id.txtvLenSize);
			holder.butAction = (ImageButton) convertView
					.findViewById(R.id.butAction);
			holder.published = (TextView) convertView
					.findViewById(R.id.txtvPublished);
			holder.inPlaylist = (ImageView) convertView
					.findViewById(R.id.imgvInPlaylist);
			holder.downloaded = (ImageView) convertView
					.findViewById(R.id.imgvDownloaded);
			holder.type = (ImageView) convertView.findViewById(R.id.imgvType);
			holder.downloading = (ImageView) convertView
					.findViewById(R.id.imgvDownloading);
			if (showFeedtitle) {
				holder.feedtitle = (TextView) convertView
						.findViewById(R.id.txtvFeedname);
			}
			holder.statusPlaying = (View) convertView
					.findViewById(R.id.statusPlaying);
			holder.statusUnread = (View) convertView
					.findViewById(R.id.statusUnread);
			holder.episodeProgress = (ProgressBar) convertView
					.findViewById(R.id.pbar_episode_progress);

			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}
		if (!(getItemViewType(position) == Adapter.IGNORE_ITEM_VIEW_TYPE)) {
			convertView.setVisibility(View.VISIBLE);
			if (position == selectedItemIndex) {
				convertView.setBackgroundColor(convertView.getResources()
						.getColor(ThemeUtils.getSelectionBackgroundColor()));
			} else {
				convertView.setBackgroundResource(0);
			}

			holder.title.setText(item.getTitle());
			if (showFeedtitle) {
				holder.feedtitle.setVisibility(View.VISIBLE);
				holder.feedtitle.setText(item.getFeed().getTitle());
			}

			FeedItem.State state = item.getState();
			switch (state) {
			case PLAYING:
				holder.statusPlaying.setVisibility(View.VISIBLE);
				holder.statusUnread.setVisibility(View.GONE);
				holder.episodeProgress.setVisibility(View.VISIBLE);
				break;
			case IN_PROGRESS:
				holder.statusPlaying.setVisibility(View.GONE);
				holder.statusUnread.setVisibility(View.GONE);
				holder.episodeProgress.setVisibility(View.VISIBLE);
				break;
			case NEW:
				holder.statusPlaying.setVisibility(View.GONE);
				holder.statusUnread.setVisibility(View.VISIBLE);
				holder.episodeProgress.setVisibility(View.GONE);
				break;
			default:
				holder.statusPlaying.setVisibility(View.GONE);
				holder.statusUnread.setVisibility(View.GONE);
				holder.episodeProgress.setVisibility(View.GONE);
				break;
			}

			holder.published.setText(convertView.getResources().getString(
					R.string.published_prefix)
					+ Converter.getRelativeTimeSpanString(getContext(),
							item.getPubDate().getTime()));

			FeedMedia media = item.getMedia();
			if (media == null) {
				holder.downloaded.setVisibility(View.GONE);
				holder.downloading.setVisibility(View.GONE);
				holder.inPlaylist.setVisibility(View.GONE);
				holder.type.setVisibility(View.GONE);
				holder.lenSize.setVisibility(View.GONE);
			} else {

				if (state == FeedItem.State.PLAYING
						|| state == FeedItem.State.IN_PROGRESS) {
					if (media.getDuration() > 0) {
						holder.episodeProgress
								.setProgress((int) (((double) media
										.getPosition()) / media.getDuration() * 100));
						holder.lenSize.setText(Converter
								.getDurationStringLong(media.getDuration()
										- media.getPosition()));
					}
				} else if (!media.isDownloaded()) {
					holder.lenSize.setText(getContext().getString(
							R.string.size_prefix)
							+ Converter.byteToString(media.getSize()));
				} else {
					holder.lenSize.setText(getContext().getString(
							R.string.length_prefix)
							+ Converter.getDurationStringLong(media
									.getDuration()));
				}

				holder.lenSize.setVisibility(View.VISIBLE);
				if (FeedManager.getInstance().isInQueue(item)) {
					holder.inPlaylist.setVisibility(View.VISIBLE);
				} else {
					holder.inPlaylist.setVisibility(View.GONE);
				}
				if (item.getMedia().isDownloaded()) {
					holder.downloaded.setVisibility(View.VISIBLE);
				} else {
					holder.downloaded.setVisibility(View.GONE);
				}

				if (DownloadRequester.getInstance().isDownloadingFile(
						item.getMedia())) {
					holder.downloading.setVisibility(View.VISIBLE);
				} else {
					holder.downloading.setVisibility(View.GONE);
				}

				TypedArray typeDrawables = getContext().obtainStyledAttributes(
						new int[] { R.attr.type_audio, R.attr.type_video });
				MediaType mediaType = item.getMedia().getMediaType();
				if (mediaType == MediaType.AUDIO) {
					holder.type.setImageDrawable(typeDrawables.getDrawable(0));
					holder.type.setVisibility(View.VISIBLE);
				} else if (mediaType == MediaType.VIDEO) {
					holder.type.setImageDrawable(typeDrawables.getDrawable(1));
					holder.type.setVisibility(View.VISIBLE);
				} else {
					holder.type.setImageBitmap(null);
					holder.type.setVisibility(View.GONE);
				}
			}

			holder.butAction.setFocusable(false);
			holder.butAction.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					callback.onActionButtonPressed(item);
				}
			});

		} else {
			convertView.setVisibility(View.GONE);
		}
		return convertView;

	}

	static class Holder extends DefaultFeedItemlistAdapter.Holder {
		TextView feedtitle;
		ImageView inPlaylist;
		ImageView downloaded;
		ImageView downloading;
		ImageButton butAction;
		View statusUnread;
		View statusPlaying;
		ProgressBar episodeProgress;
	}

	public int getSelectedItemIndex() {
		return selectedItemIndex;
	}

	public void setSelectedItemIndex(int selectedItemIndex) {
		this.selectedItemIndex = selectedItemIndex;
		notifyDataSetChanged();
	}

}
