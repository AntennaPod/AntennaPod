package de.danoeh.antennapod.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedFile;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.service.download.Downloader;
import de.danoeh.antennapod.util.Converter;
import de.danoeh.antennapod.R;

public class DownloadlistAdapter extends ArrayAdapter<Downloader> {
	private int selectedItemIndex;

	public static final int SELECTION_NONE = -1;

	public DownloadlistAdapter(Context context, int textViewResourceId,
			List<Downloader> objects) {
		super(context, textViewResourceId, objects);
		this.selectedItemIndex = SELECTION_NONE;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		DownloadStatus status = getItem(position).getStatus();
		FeedFile feedFile = status.getFeedFile();
		// Inflate layout
		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.downloadlist_item, null);
			holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
			holder.message = (TextView) convertView
					.findViewById(R.id.txtvMessage);
			holder.downloaded = (TextView) convertView
					.findViewById(R.id.txtvDownloaded);
			holder.percent = (TextView) convertView
					.findViewById(R.id.txtvPercent);
			holder.progbar = (ProgressBar) convertView
					.findViewById(R.id.progProgress);

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

		String titleText = null;
		if (feedFile.getClass() == FeedMedia.class) {
			titleText = ((FeedMedia) feedFile).getItem().getTitle();
		} else if (feedFile.getClass() == Feed.class) {
			titleText = ((Feed) feedFile).getTitle();
		} else if (feedFile.getClass() == FeedImage.class) {
			FeedImage image = (FeedImage) feedFile;
			if (image.getFeed() != null) {
				titleText = convertView.getResources().getString(
						R.string.image_of_prefix)
						+ image.getFeed().getTitle();
			} else {
				titleText = ((FeedImage) feedFile).getTitle();
			}
		}
		holder.title.setText(titleText);
		if (status.getStatusMsg() != 0) {
			holder.message.setText(status.getStatusMsg());
		}
		String strDownloaded = Converter.byteToString(status.getSoFar());
		if (status.getSize() != DownloadStatus.SIZE_UNKNOWN) {
			strDownloaded += " / " + Converter.byteToString(status.getSize());
			holder.percent.setText(status.getProgressPercent() + "%");
			holder.progbar.setProgress(status.getProgressPercent());
			holder.percent.setVisibility(View.VISIBLE);
		} else {
			holder.progbar.setProgress(0);
			holder.percent.setVisibility(View.INVISIBLE);
		}

		holder.downloaded.setText(strDownloaded);

		return convertView;
	}

	static class Holder {
		TextView title;
		TextView message;
		TextView downloaded;
		TextView percent;
		ProgressBar progbar;
	}

	public int getSelectedItemIndex() {
		return selectedItemIndex;
	}

	public void setSelectedItemIndex(int selectedItemIndex) {
		this.selectedItemIndex = selectedItemIndex;
		notifyDataSetChanged();
	}

}
