package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.util.ThemeUtils;
import de.danoeh.antennapod.view.CircularProgressBar;

public class DownloadlistAdapter extends BaseAdapter {

    private final ItemAccess itemAccess;
    private final Context context;

    public DownloadlistAdapter(Context context, ItemAccess itemAccess) {
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
        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.downloadlist_item, parent, false);
            holder.title = convertView.findViewById(R.id.txtvTitle);
            holder.status = convertView.findViewById(R.id.txtvStatus);
            holder.secondaryActionButton = convertView.findViewById(R.id.secondaryActionButton);
            holder.secondaryActionIcon = convertView.findViewById(R.id.secondaryActionIcon);
            holder.secondaryActionProgress = convertView.findViewById(R.id.secondaryActionProgress);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.title.setText(request.getTitle());
        holder.secondaryActionIcon.setImageResource(ThemeUtils.getDrawableFromAttr(context, R.attr.navigation_cancel));
        holder.secondaryActionButton.setContentDescription(context.getString(R.string.cancel_download_label));
        holder.secondaryActionButton.setTag(downloader);
        holder.secondaryActionButton.setOnClickListener(butSecondaryListener);
        holder.secondaryActionProgress.setPercentage(0, request);

        String status = "";
        if (request.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
            status += context.getString(R.string.download_type_feed);
        } else if (request.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
            status += context.getString(R.string.download_type_media);
        }
        status += " · ";
        if (request.getSoFar() <= 0) {
            status += context.getString(R.string.download_pending);
        } else {
            status += Formatter.formatShortFileSize(context, request.getSoFar());
            if (request.getSize() != DownloadStatus.SIZE_UNKNOWN) {
                status += " / " + Formatter.formatShortFileSize(context, request.getSize());
                holder.secondaryActionProgress.setPercentage(
                        0.01f * Math.max(1, request.getProgressPercent()), request);
            }
        }
        holder.status.setText(status);

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
        TextView status;
        View secondaryActionButton;
        ImageView secondaryActionIcon;
        CircularProgressBar secondaryActionProgress;
    }

    public interface ItemAccess {
        int getCount();

        Downloader getItem(int position);

        void onSecondaryActionClick(Downloader downloader);
    }

}
