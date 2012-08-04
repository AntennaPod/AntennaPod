package de.danoeh.antennapod.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.FeedImageLoader;
import de.danoeh.antennapod.miroguide.model.MiroGuideChannel;

public class MiroGuideChannelListAdapter extends ArrayAdapter<MiroGuideChannel> {

	public MiroGuideChannelListAdapter(Context context, int textViewResourceId,
			List<MiroGuideChannel> objects) {
		super(context, textViewResourceId, objects);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		MiroGuideChannel channel = getItem(position);

		// Inflate Layout
		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			convertView = inflater.inflate(R.layout.miroguide_channellist_item, null);
			holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);

			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}
		
		holder.title.setText(channel.getName());
		return convertView;
	}

	static class Holder {
		TextView title;
	}

	

}
