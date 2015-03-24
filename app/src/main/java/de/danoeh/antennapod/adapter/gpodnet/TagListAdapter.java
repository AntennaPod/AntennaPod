package de.danoeh.antennapod.adapter.gpodnet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetTag;

/**
 * Adapter for displaying a list of GPodnetPodcast-Objects.
 */
public class TagListAdapter extends ArrayAdapter<GpodnetTag> {

    public TagListAdapter(Context context, int resource, List<GpodnetTag> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;

        GpodnetTag tag = getItem(position);

        // Inflate Layout
        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.gpodnet_tag_listitem, parent, false);
            holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.usage = (TextView) convertView.findViewById(R.id.txtvUsage);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.title.setText(tag.getTitle());
        holder.usage.setText(String.valueOf(tag.getUsage()));

        return convertView;
    }

    static class Holder {
        TextView title;
        TextView usage;
    }
}
