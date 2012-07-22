package de.danoeh.antennapod.adapter;

import java.text.DateFormat;
import java.util.List;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.FeedlistAdapter.Holder;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.SimpleChapter;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.Converter;
import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class SCListAdapter extends ArrayAdapter<SimpleChapter> {

	private static final String TAG = "SCListAdapter";

	public SCListAdapter(Context context, int textViewResourceId,
			List<SimpleChapter> objects) {
		super(context, textViewResourceId, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;

		SimpleChapter sc = getItem(position);

		// Inflate Layout
		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			convertView = inflater.inflate(R.layout.simplechapter_item, null);
			holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
			holder.start = (TextView) convertView.findViewById(R.id.txtvStart);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();

		}
		
		holder.title.setText(sc.getTitle());
		holder.start.setText(Converter.getDurationStringLong((int) sc.getStart()));
		
		SimpleChapter current = sc.getItem().getCurrentChapter();
		if (current != null) {
			if (current == sc) {
				holder.title.setTextColor(convertView.getResources().getColor(R.color.bright_blue));
			} else {
				holder.title.setTextColor(Color.parseColor("black"));
			}
		} else {
			Log.w(TAG, "Could not find out what the current chapter is.");
		}
		
		return convertView;
	}

	static class Holder {
		TextView title;
		TextView start;
	}

}
