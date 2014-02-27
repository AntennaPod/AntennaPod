package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.MediaType;
import de.danoeh.antennapod.util.Converter;

public class DefaultFeedItemlistAdapter extends BaseAdapter {

	ItemAccess itemAccess;
	private Context context;

	public DefaultFeedItemlistAdapter(Context context, ItemAccess itemAccess) {
		super();
		this.context = context;
		if (itemAccess == null) {
			throw new IllegalArgumentException("itemAccess must not be null");
		}
		this.itemAccess = itemAccess;
	}

	@Override
	public int getCount() {
		return itemAccess.getCount();

	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public FeedItem getItem(int position) {
		return itemAccess.getItem(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		final FeedItem item = getItem(position);

		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.default_feeditemlist_item, null);
			holder.title = (TextView) convertView
					.findViewById(R.id.txtvItemname);
			holder.lenSize = (TextView) convertView
					.findViewById(R.id.txtvLenSize);
			
			holder.published = (TextView) convertView
					.findViewById(R.id.txtvPublished);
			holder.type = (ImageView) convertView.findViewById(R.id.imgvType);
			convertView.setTag(holder);
			
		} else {
			holder = (Holder) convertView.getTag();
		}
		if (!(getItemViewType(position) == Adapter.IGNORE_ITEM_VIEW_TYPE)) {
			convertView.setVisibility(View.VISIBLE);
			holder.title.setText(item.getTitle());
			holder.published.setText(convertView.getResources().getString(
					R.string.published_prefix)
					+ DateUtils.getRelativeTimeSpanString(
							item.getPubDate().getTime(),
							System.currentTimeMillis(), 0, 0));
			if (item.getMedia() == null) {
				holder.type.setVisibility(View.GONE);
				holder.lenSize.setVisibility(View.GONE);
			} else {
				holder.lenSize.setVisibility(View.VISIBLE);
				holder.lenSize.setText(convertView.getResources().getString(
						R.string.size_prefix)
						+ Converter.byteToString(item.getMedia().getSize()));

				TypedArray typeDrawables = context
						.obtainStyledAttributes(new int[] { R.attr.type_audio,
								R.attr.type_video });
				MediaType mediaType = item.getMedia().getMediaType();
				if (mediaType == MediaType.AUDIO) {
					holder.type.setImageDrawable(typeDrawables.getDrawable(0));
                    holder.type.setContentDescription(context.getString(R.string.media_type_audio_label));
					holder.type.setVisibility(View.VISIBLE);
				} else if (mediaType == MediaType.VIDEO) {
					holder.type.setImageDrawable(typeDrawables.getDrawable(1));
                    holder.type.setContentDescription(context.getString(R.string.media_type_video_label));
                    holder.type.setVisibility(View.VISIBLE);
				} else {
					holder.type.setImageBitmap(null);
					holder.type.setVisibility(View.GONE);
				}
			}

		} else {
			convertView.setVisibility(View.GONE);
		}
		return convertView;
	}

	protected static class Holder {
		TextView title;
		TextView published;
		TextView lenSize;
		ImageView type;

	}

	public static interface ItemAccess {
		int getCount();

		FeedItem getItem(int position);
	}

	protected Context getContext() {
		return context;
	}
}
