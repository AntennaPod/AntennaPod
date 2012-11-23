package de.danoeh.antennapod.adapter;

import java.text.DateFormat;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.MediaType;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.Converter;
import de.danoeh.antennapod.util.EpisodeFilter;
import de.danoeh.antennapod.util.ThemeUtils;

public class FeedItemlistAdapter extends ArrayAdapter<FeedItem> {
	private OnClickListener onButActionClicked;
	private boolean showFeedtitle;
	private int selectedItemIndex;
	private List<FeedItem> objects;

	public static final int SELECTION_NONE = -1;

	public FeedItemlistAdapter(Context context, int textViewResourceId,
			List<FeedItem> objects, OnClickListener onButActionClicked,
			boolean showFeedtitle) {
		super(context, textViewResourceId, objects);
		this.objects = objects;
		this.onButActionClicked = onButActionClicked;
		this.showFeedtitle = showFeedtitle;
		this.selectedItemIndex = SELECTION_NONE;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		FeedItem item = getItem(position);

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
			holder.statusLabel = (View) convertView
					.findViewById(R.id.vStatusLabel);

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
				holder.title.setTypeface(Typeface.DEFAULT_BOLD);
				holder.statusLabel.setBackgroundColor(convertView
						.getResources().getColor(R.color.status_playing));
				holder.statusLabel.setVisibility(View.VISIBLE);
				break;
			case IN_PROGRESS:
				holder.title.setTypeface(Typeface.DEFAULT_BOLD);
				holder.statusLabel.setBackgroundColor(convertView
						.getResources().getColor(R.color.status_progress));
				holder.statusLabel.setVisibility(View.VISIBLE);
				break;
			case NEW:
				holder.title.setTypeface(Typeface.DEFAULT_BOLD);
				holder.statusLabel.setBackgroundColor(convertView
						.getResources().getColor(R.color.status_unread));
				holder.statusLabel.setVisibility(View.VISIBLE);
				break;
			default:
				holder.title.setTypeface(Typeface.DEFAULT);
				holder.statusLabel.setVisibility(View.INVISIBLE);
				break;
			}

			holder.published.setText(convertView.getResources().getString(
					R.string.published_prefix)
					+ DateUtils.formatSameDayTime(item.getPubDate().getTime(),
							System.currentTimeMillis(), DateFormat.MEDIUM,
							DateFormat.SHORT));

			if (item.getMedia() == null) {
				holder.downloaded.setVisibility(View.GONE);
				holder.downloading.setVisibility(View.GONE);
				holder.inPlaylist.setVisibility(View.GONE);
				holder.type.setVisibility(View.GONE);
				holder.lenSize.setVisibility(View.GONE);
			} else {
				holder.lenSize.setVisibility(View.VISIBLE);
				if (FeedManager.getInstance().isInQueue(item)) {
					holder.inPlaylist.setVisibility(View.VISIBLE);
				} else {
					holder.inPlaylist.setVisibility(View.GONE);
				}
				if (item.getMedia().isDownloaded()) {
					holder.lenSize.setText(convertView.getResources()
							.getString(R.string.length_prefix)
							+ Converter.getDurationStringLong(item.getMedia()
									.getDuration()));
					holder.downloaded.setVisibility(View.VISIBLE);
				} else {
					holder.lenSize
							.setText(convertView.getResources().getString(
									R.string.size_prefix)
									+ Converter.byteToString(item.getMedia()
											.getSize()));
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
			holder.butAction.setOnClickListener(onButActionClicked);
		} else {
			convertView.setVisibility(View.GONE);
		}
		return convertView;

	}

	static class Holder {
		TextView title;
		TextView feedtitle;
		TextView published;
		TextView lenSize;
		ImageView inPlaylist;
		ImageView downloaded;
		ImageView type;
		ImageView downloading;
		ImageButton butAction;
		View statusLabel;
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
		if (PodcastApp.getInstance().displayOnlyEpisodes()) {
			return EpisodeFilter.countItemsWithEpisodes(objects);
		} else {
			return super.getCount();
		}
	}

	@Override
	public FeedItem getItem(int position) {
		if (PodcastApp.getInstance().displayOnlyEpisodes()) {
			return EpisodeFilter.accessEpisodeByIndex(objects, position);
		} else {
			return super.getItem(position);
		}
	}

}
