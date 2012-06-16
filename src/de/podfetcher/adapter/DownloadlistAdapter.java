package de.podfetcher.adapter;

import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.content.Context;

import de.podfetcher.R;
import de.podfetcher.util.Converter;
import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedFile;
import de.podfetcher.feed.FeedImage;
import de.podfetcher.feed.FeedMedia;
import de.podfetcher.service.DownloadObserver;

public class DownloadlistAdapter extends ArrayAdapter<DownloadObserver.DownloadStatus> {
    public DownloadlistAdapter(Context context,
            int textViewResourceId, DownloadObserver.DownloadStatus[] objects) {
        super(context, textViewResourceId, objects);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        DownloadObserver.DownloadStatus status = getItem(position);
        FeedFile feedFile = status.getFeedFile();
        // Inflate layout
        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.downloadlist_item, null);
            holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.downloaded = (TextView) convertView.findViewById(R.id.txtvDownloaded);
            holder.percent = (TextView) convertView.findViewById(R.id.txtvPercent);

            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }
        
        String titleText = null;
        if (feedFile.getClass() == FeedMedia.class) {
        	titleText = ((FeedMedia) feedFile).getItem().getTitle();
        } else if (feedFile.getClass() == Feed.class) {
        	titleText = ((Feed) feedFile).getTitle();
        } else if (feedFile.getClass() == FeedImage.class) {
        	titleText = "[Image] " + ((FeedImage) feedFile).getTitle();
        }
        holder.title.setText(titleText);
        holder.downloaded.setText(Converter.byteToString(status.getSoFar()) + " / "
                + Converter.byteToString(status.getSize()));
        holder.percent.setText(status.getProgressPercent() + "%");

        return convertView;
    }

    static class Holder {
        TextView title;
        TextView downloaded;
        TextView percent;
    }
}
