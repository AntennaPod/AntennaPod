package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.miroguide.model.MiroGuideItem;

import java.util.List;

public class MiroGuideItemlistAdapter extends ArrayAdapter<MiroGuideItem> {

	public MiroGuideItemlistAdapter(Context context, int textViewResourceId,
			List<MiroGuideItem> objects) {
		super(context, textViewResourceId, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		MiroGuideItem item = getItem(position);

		// Inflate Layout
		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			convertView = inflater.inflate(R.layout.miroguide_itemlist_item,
					null);
			holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
			holder.date = (TextView) convertView.findViewById(R.id.txtvDate);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}

		holder.title.setText(item.getName());
		if (item.getDate() != null) {
			holder.date.setText(DateUtils.getRelativeTimeSpanString(
					item.getDate().getTime(), System.currentTimeMillis(), 0, 0));
			holder.date.setVisibility(View.VISIBLE);
		} else {
			holder.date.setVisibility(View.GONE);
		}
		return convertView;
	}

	static class Holder {
		TextView title;
		TextView date;
	}

}
