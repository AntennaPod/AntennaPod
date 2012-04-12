package de.podfetcher.adapter;

import java.util.List;

import de.podfetcher.feed.FeedItem;
import de.podfetcher.util.Converter;
import de.podfetcher.R;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;

public class FeedItemlistAdapter extends ArrayAdapter<FeedItem> {
	

	public FeedItemlistAdapter(Context context,
			int textViewResourceId, List<FeedItem> objects) {
		super(context, textViewResourceId, objects);
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

			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();	
		}

		holder.title.setText(item.getTitle());
		holder.size.setText(Converter.byteToString(item.getMedia().getSize()));
		return convertView;
	
	}

	static class Holder {
		TextView title;
		TextView size;
	}
}
