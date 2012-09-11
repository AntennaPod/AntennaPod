package de.danoeh.antennapod.adapter;

import java.text.DateFormat;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedFile;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.util.DownloadError;
import de.danoeh.antennapod.R;

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
			if (feedfile.getClass() == Feed.class) {
				holder.title.setText(((Feed) feedfile).getTitle());
				holder.type.setText("Feed");
			} else if (feedfile.getClass() == FeedMedia.class) {
				holder.title.setText(((FeedMedia) feedfile).getItem()
						.getTitle());
				holder.type.setText(((FeedMedia) feedfile).getMime_type());
			} else if (feedfile.getClass() == FeedImage.class) {
				holder.title.setText(((FeedImage) feedfile).getTitle());
				holder.type.setText("Image");
			}
			holder.date.setText(DateUtils.formatSameDayTime(status
					.getCompletionDate().getTime(), System.currentTimeMillis(),
					DateFormat.SHORT, DateFormat.SHORT));
			if (status.isSuccessful()) {
				holder.successful.setTextColor(Color.parseColor("green"));
				holder.successful.setText(R.string.download_successful);
				holder.reason.setVisibility(View.GONE);
			} else {
				holder.successful.setTextColor(Color.parseColor("red"));
				holder.successful.setText(R.string.download_failed);
				holder.reason.setText(DownloadError.getErrorString(
						getContext(), status.getReason()));
			}
		} else {
			holder = (Holder) convertView.getTag();
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
