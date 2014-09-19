package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.DownloadStatus;

/** Displays a list of DownloadStatus entries. */
public class DownloadLogAdapter extends BaseAdapter {

	private Context context;

    private ItemAccess itemAccess;

	public DownloadLogAdapter(Context context, ItemAccess itemAccess) {
		super();
        this.itemAccess = itemAccess;
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		DownloadStatus status = getItem(position);
		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.downloadlog_item, parent, false);
			holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
			holder.type = (TextView) convertView.findViewById(R.id.txtvType);
			holder.date = (TextView) convertView.findViewById(R.id.txtvDate);
			holder.successful = (TextView) convertView
					.findViewById(R.id.txtvStatus);
			holder.reason = (TextView) convertView
					.findViewById(R.id.txtvReason);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}
		if (status.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
			holder.type.setText(R.string.download_type_feed);
		} else if (status.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
			holder.type.setText(R.string.download_type_media);
		} else if (status.getFeedfileType() == FeedImage.FEEDFILETYPE_FEEDIMAGE) {
			holder.type.setText(R.string.download_type_image);
		}
		if (status.getTitle() != null) {
			holder.title.setText(status.getTitle());
		} else {
			holder.title.setText(R.string.download_log_title_unknown);
		}
		holder.date.setText(DateUtils.getRelativeTimeSpanString(
				status.getCompletionDate().getTime(),
				System.currentTimeMillis(), 0, 0));
		if (status.isSuccessful()) {
			holder.successful.setTextColor(convertView.getResources().getColor(
					R.color.download_success_green));
			holder.successful.setText(R.string.download_successful);
			holder.reason.setVisibility(View.GONE);
		} else {
			holder.successful.setTextColor(convertView.getResources().getColor(
					R.color.download_failed_red));
			holder.successful.setText(R.string.download_failed);
			String reasonText = status.getReason().getErrorString(context);
			if (status.getReasonDetailed() != null) {
				reasonText += ": " + status.getReasonDetailed();
			}
			holder.reason.setText(reasonText);
			holder.reason.setVisibility(View.VISIBLE);
		}

		return convertView;
	}

	static class Holder {
		TextView title;
		TextView type;
		TextView date;
		TextView successful;
		TextView reason;
	}

	@Override
	public int getCount() {
        return itemAccess.getCount();
	}

	@Override
	public DownloadStatus getItem(int position) {
        return itemAccess.getItem(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

    public static interface ItemAccess {
        public int getCount();
        public DownloadStatus getItem(int position);
    }

}
