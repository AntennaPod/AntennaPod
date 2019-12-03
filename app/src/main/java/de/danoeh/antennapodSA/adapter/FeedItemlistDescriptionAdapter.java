package de.danoeh.antennapodSA.adapter;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.core.feed.FeedItem;
import de.danoeh.antennapodSA.core.util.DateUtils;

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
            holder.title = convertView.findViewById(R.id.txtvTitle);
            holder.pubDate = convertView.findViewById(R.id.txtvPubDate);
            holder.description = convertView.findViewById(R.id.txtvDescription);

            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.title.setText(item.getTitle());
        holder.pubDate.setText(DateUtils.formatAbbrev(getContext(), item.getPubDate()));
        if (item.getDescription() != null) {
            String description = item.getDescription()
                    .replaceAll("\n", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            holder.description.setText(description);

            final int MAX_LINES_COLLAPSED = 3;
            holder.description.setMaxLines(MAX_LINES_COLLAPSED);
            holder.description.setOnClickListener(v -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                        && holder.description.getMaxLines() > MAX_LINES_COLLAPSED) {
                    holder.description.setMaxLines(MAX_LINES_COLLAPSED);
                } else {
                    holder.description.setMaxLines(2000);
                }
            });
        }
        return convertView;
    }

    static class Holder {
        TextView title;
        TextView pubDate;
        TextView description;
    }
}
