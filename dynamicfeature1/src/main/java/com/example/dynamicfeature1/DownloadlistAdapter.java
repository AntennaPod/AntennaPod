package com.example.dynamicfeature1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.util.Converter;

public class DownloadlistAdapter extends BaseAdapter {

    private final ItemAccess itemAccess;
    private final Context context;

    public DownloadlistAdapter(Context context,
                               ItemAccess itemAccess) {
        super();
        this.context = context;
        this.itemAccess = itemAccess;
    }

    @Override
    public int getCount() {
        return itemAccess.getCount();
    }

    @Override
    public Downloader getItem(int position) {
        return itemAccess.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        Downloader downloader = getItem(position);
        DownloadRequest request = downloader.getDownloadRequest();
        // Inflate layout
        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.downloadlist_item, parent, false);
            holder.title = convertView.findViewById(R.id.txtvTitle);
            holder.downloaded = convertView
                    .findViewById(R.id.txtvDownloaded);
            holder.percent = convertView
                    .findViewById(R.id.txtvPercent);
            holder.progbar = convertView
                    .findViewById(R.id.progProgress);
            holder.butSecondary = convertView
                    .findViewById(R.id.butSecondaryAction);

            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.title.setText(request.getTitle());

        holder.progbar.setIndeterminate(request.getSoFar() <= 0);

        String strDownloaded = Converter.byteToString(request.getSoFar());
        if (request.getSize() != DownloadStatus.SIZE_UNKNOWN) {
            strDownloaded += " / " + Converter.byteToString(request.getSize());
            holder.percent.setText(request.getProgressPercent() + "%");
            holder.progbar.setProgress(request.getProgressPercent());
            holder.percent.setVisibility(View.VISIBLE);
        } else {
            holder.progbar.setProgress(0);
            holder.percent.setVisibility(View.INVISIBLE);
        }

        holder.downloaded.setText(strDownloaded);

        holder.butSecondary.setFocusable(false);
        holder.butSecondary.setTag(downloader);
        holder.butSecondary.setOnClickListener(butSecondaryListener);

        return convertView;
    }

    private final View.OnClickListener butSecondaryListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Downloader downloader = (Downloader) v.getTag();
            itemAccess.onSecondaryActionClick(downloader);
        }
    };

    static class Holder {
        TextView title;
        TextView downloaded;
        TextView percent;
        ProgressBar progbar;
        ImageButton butSecondary;
    }

    public interface ItemAccess {
        int getCount();

        Downloader getItem(int position);

        void onSecondaryActionClick(Downloader downloader);
    }

}
