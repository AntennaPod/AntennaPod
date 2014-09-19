package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;

import java.util.List;

/**
 * List adapter for showing a list of FeedItems with their title and description.
 */
public class FeedItemlistDescriptionAdapter extends ArrayAdapter<FeedItem> {

    public FeedItemlistDescriptionAdapter(Context context, int resource, List<FeedItem> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;

        FeedItem item = getItem(position);

        // Inflate layout
        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.itemdescription_listitem, parent, false);
            holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.description = (TextView) convertView.findViewById(R.id.txtvDescription);

            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.title.setText(item.getTitle());
        if (item.getDescription() != null) {
            holder.description.setText(item.getDescription());
        }

        return convertView;
    }

    static class Holder {
        TextView title;
        TextView description;
    }
}
