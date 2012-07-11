package de.podfetcher.adapter;

import java.text.DateFormat;
import java.util.List;

import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedManager;
import de.podfetcher.util.Converter;
import de.podfetcher.R;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;

public class FeedItemlistAdapter extends ArrayAdapter<FeedItem> {
	private OnClickListener onButActionClicked;
	private boolean showFeedtitle;
	private int selectedItemIndex;

	public static final int SELECTION_NONE = -1;

	public FeedItemlistAdapter(Context context, int textViewResourceId,
			List<FeedItem> objects, OnClickListener onButActionClicked,
			boolean showFeedtitle) {
		super(context, textViewResourceId, objects);
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
			holder.encInfo = (RelativeLayout) convertView
					.findViewById(R.id.enc_info);
			if (showFeedtitle) {
				holder.feedtitle = (TextView) convertView
						.findViewById(R.id.txtvFeedname);
			}

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

		holder.title.setText(item.getTitle());
		if (showFeedtitle) {
			holder.feedtitle.setVisibility(View.VISIBLE);
			holder.feedtitle.setText(item.getFeed().getTitle());
		}
		if (!item.isRead()) {
			holder.title.setTypeface(Typeface.DEFAULT_BOLD);
		} else {
			holder.title.setTypeface(Typeface.DEFAULT);
		}

		holder.published.setText("Published: "
				+ DateUtils.formatSameDayTime(item.getPubDate().getTime(),
						System.currentTimeMillis(), DateFormat.SHORT,
						DateFormat.SHORT));

		if (item.getMedia() == null) {
			holder.encInfo.setVisibility(View.GONE);
		} else {
			holder.encInfo.setVisibility(View.VISIBLE);
			if (FeedManager.getInstance().isInQueue(item)) {
				holder.inPlaylist.setVisibility(View.VISIBLE);
			} else {
				holder.inPlaylist.setVisibility(View.GONE);
			}
			if (item.getMedia().isDownloaded()) {
				holder.lenSize.setText(Converter.getDurationStringShort(item
						.getMedia().getDuration()));
				holder.downloaded.setVisibility(View.VISIBLE);
			} else {
				holder.lenSize.setText(Converter.byteToString(item.getMedia()
						.getSize()));
				holder.downloaded.setVisibility(View.GONE);
			}

			if (item.getMedia().isDownloading()) {
				holder.downloading.setVisibility(View.VISIBLE);
			} else {
				holder.downloading.setVisibility(View.GONE);
			}

			String type = item.getMedia().getMime_type();

			if (type.startsWith("audio")) {
				holder.type.setImageResource(R.drawable.type_audio);
			} else if (type.startsWith("video")) {
				holder.type.setImageResource(R.drawable.type_video);
			} else {
				holder.type.setImageBitmap(null);
			}
		}

		holder.butAction.setFocusable(false);
		holder.butAction.setOnClickListener(onButActionClicked);

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
		RelativeLayout encInfo;
	}

	public int getSelectedItemIndex() {
		return selectedItemIndex;
	}

	public void setSelectedItemIndex(int selectedItemIndex) {
		this.selectedItemIndex = selectedItemIndex;
		notifyDataSetChanged();
	}

}
