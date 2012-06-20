package de.podfetcher.adapter;

import java.util.List;

import de.podfetcher.feed.FeedItem;
import de.podfetcher.util.Converter;
import de.podfetcher.R;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.content.Context;
import android.graphics.Typeface;

public class FeedItemlistAdapter extends ArrayAdapter<FeedItem> {
	private OnClickListener onButActionClicked;

	public FeedItemlistAdapter(Context context,
			int textViewResourceId, List<FeedItem> objects, OnClickListener onButActionClicked) {
		super(context, textViewResourceId, objects);
		this.onButActionClicked = onButActionClicked;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		FeedItem item = getItem(position);

		if(convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.feeditemlist_item, null);
			holder.title = (TextView) convertView.findViewById(R.id.txtvItemname);
			holder.size = (TextView) convertView.findViewById(R.id.txtvItemsize);
			holder.butAction = (ImageButton) convertView.findViewById(R.id.butAction);
			
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();	
		}

		holder.title.setText(item.getTitle());
		if (!item.isRead()) {
			holder.title.setTypeface(Typeface.DEFAULT_BOLD);
		} else {
			holder.title.setTypeface(Typeface.DEFAULT);
		}
		holder.size.setText(Converter.byteToString(item.getMedia().getSize()));
		holder.butAction.setFocusable(false);
		holder.butAction.setOnClickListener(onButActionClicked);
		return convertView;
	
	}

	static class Holder {
		TextView title;
		TextView size;
		ImageButton butAction;
	}
}
