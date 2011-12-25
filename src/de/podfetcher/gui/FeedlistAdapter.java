package de.podfetcher.gui;

import java.io.File;
import java.util.List;

import de.podfetcher.R;
import de.podfetcher.feed.Feed;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class FeedlistAdapter extends ArrayAdapter<Feed> {

	int resource;
	
	public FeedlistAdapter(Context context, int resource,
			int textViewResourceId, List<Feed> objects) {
		super(context, resource, textViewResourceId, objects);
		this.resource = resource;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout feedlistView;
		
		
		Feed feed = getItem(position);
		// Inflate Layout
		if (convertView == null) {
			feedlistView = new LinearLayout(getContext());
		      String inflater = Context.LAYOUT_INFLATER_SERVICE;
		      LayoutInflater vi = (LayoutInflater)getContext().getSystemService(inflater);
		      vi.inflate(resource, feedlistView, true);
		    } else {
		    	feedlistView = (LinearLayout) convertView;
		    }
		
		ImageView imageView = (ImageView)feedlistView.findViewById(R.id.imgvFeedimage);	
		TextView txtvFeedname = (TextView)feedlistView.findViewById(R.id.txtvFeedname);
		TextView txtvNewEpisodes = (TextView)feedlistView.findViewById(R.id.txtvNewEpisodes);
		
		imageView.setImageURI(Uri.fromFile(new File(feed.getFile_url())));	// TODO select default picture when no image downloaded
		txtvFeedname.setText(feed.getTitle());
		// TODO find new Episodes txtvNewEpisodes.setText(feed)
		return feedlistView;
	}
	
	

}
