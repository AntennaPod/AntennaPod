package de.danoeh.antennapod.adapter;

import java.text.DateFormat;
import java.util.List;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedFile;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.util.DownloadError;

/** Displays a list of DownloadStatus entries. */
public class DownloadLogAdapter extends ArrayAdapter<DownloadStatus> {

	public DownloadLogAdapter(Context context, int textViewResourceId,
			List<DownloadStatus> objects) {
		super(context, textViewResourceId, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		DownloadStatus status = getItem(position);
		FeedFile feedfile = status.getFeedFile();
		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.downloadlog_item, null);
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
		holder.date.setText(DateUtils.formatSameDayTime(status
				.getCompletionDate().getTime(), System.currentTimeMillis(),
				DateFormat.SHORT, DateFormat.SHORT));
		if (status.isSuccessful()) {
			holder.successful.setTextColor(convertView.getResources().getColor(
					R.color.download_success_green));
			holder.successful.setText(R.string.download_successful);
			holder.reason.setVisibility(View.GONE);
		} else {
			holder.successful.setTextColor(convertView.getResources().getColor(
					R.color.download_failed_red));
			holder.successful.setText(R.string.download_failed);
			String reasonText = DownloadError.getErrorString(getContext(),
					status.getReason());
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

}
