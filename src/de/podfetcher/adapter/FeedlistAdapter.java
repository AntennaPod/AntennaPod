package de.podfetcher.adapter;

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
		Holder holder;	
		Feed feed = getItem(position);

		// Inflate Layout
		if (convertView == null) {
			holder = new Holder();
		      LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			  convertView = inflater.inflate(R.layout.feedlist_item, null);
			  holder.title = (TextView) convertView.findViewById(R.id.txtvFeedname);
			  holder.image = (ImageView) convertView.findViewById(R.id.imgvFeedimage);

			  convertView.setTag(holder);
		    } else {
				holder = (Holder) convertView.getTag();
		    }
		
		holder.title.setText(feed.getTitle());
		if(feed.getImage() != null) {	
			holder.image.setImageURI(Uri.fromFile(new File(feed.getImage().getFile_url())));	// TODO select default picture when no image downloaded
		}
		// TODO find new Episodes txtvNewEpisodes.setText(feed)
		return convertView;
	}

	static class Holder {
		TextView title;
		ImageView image;
	}

}
