package de.danoeh.antennapod.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedFile;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.service.download.DownloadRequest;
import de.danoeh.antennapod.service.download.DownloadStatus;
import de.danoeh.antennapod.service.download.Downloader;
import de.danoeh.antennapod.util.Converter;
import de.danoeh.antennapod.util.ThemeUtils;

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
		DownloadRequest request = getItem(position).getDownloadRequest();
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
					ThemeUtils.getSelectionBackgroundColor()));
		} else {
			convertView.setBackgroundResource(0);
		}
		
		holder.title.setText(request.getTitle());
		if (request.getStatusMsg() != 0) {
			holder.message.setText(request.getStatusMsg());
		}
		String strDownloaded = Converter.byteToString(request.getSoFar());
		if (request.getSize() != DownloadStatus.SIZE_UNKNOWN) {
			strDownloaded += " / " + Converter.byteToString(request.getSize());
			holder.percent.setText(request.getProgressPercent() + "%");
			holder.progbar.setProgress(request.getProgressPercent());
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
