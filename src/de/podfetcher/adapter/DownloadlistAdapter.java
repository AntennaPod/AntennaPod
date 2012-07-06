package de.podfetcher.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.podfetcher.R;
import de.podfetcher.asynctask.DownloadStatus;
import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedFile;
import de.podfetcher.feed.FeedImage;
import de.podfetcher.feed.FeedMedia;
import de.podfetcher.util.Converter;

public class DownloadlistAdapter extends ArrayAdapter<DownloadStatus> {
	private int selectedItemIndex;

	public static final int SELECTION_NONE = -1;

	public DownloadlistAdapter(Context context, int textViewResourceId,
			List<DownloadStatus> objects) {
		super(context, textViewResourceId, objects);
		this.selectedItemIndex = SELECTION_NONE;
	}



	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		DownloadStatus status = getItem(position);
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
			titleText = "[Image] " + ((FeedImage) feedFile).getTitle();
		}
		holder.title.setText(titleText);
		holder.message.setText(status.getStatusMsg());
		holder.downloaded.setText(Converter.byteToString(status.getSoFar())
				+ " / " + Converter.byteToString(status.getSize()));
		holder.percent.setText(status.getProgressPercent() + "%");
		holder.progbar.setProgress(status.getProgressPercent());

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
