package de.podfetcher.adapter;

import java.util.List;

import de.podfetcher.feed.FeedItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FeedItemlistAdapter extends ArrayAdapter<FeedItem> {
	
	int resource;

	public FeedItemlistAdapter(Context context, int resource,
			int textViewResourceId, List<FeedItem> objects) {
		super(context, resource, textViewResourceId, objects);
		this.resource = resource;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	
	}
}
