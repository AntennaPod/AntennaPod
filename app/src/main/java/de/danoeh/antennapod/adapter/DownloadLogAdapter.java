package de.danoeh.antennapod.adapter;

import android.app.Activity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.ThemeUtils;
import de.danoeh.antennapod.view.viewholder.DownloadItemViewHolder;

/**
 * Displays a list of DownloadStatus entries.
 */
public class DownloadLogAdapter extends BaseAdapter {
    private static final String TAG = "DownloadLogAdapter";

    private final Activity context;
    private final ItemAccess itemAccess;

    public DownloadLogAdapter(Activity context, ItemAccess itemAccess) {
        super();
        this.itemAccess = itemAccess;
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DownloadItemViewHolder holder;
        if (convertView == null) {
            holder = new DownloadItemViewHolder(context, parent);
        } else {
            holder = (DownloadItemViewHolder) convertView.getTag();
        }

        DownloadStatus status = getItem(position);
        if (status.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
            holder.type.setText(R.string.download_type_feed);
        } else if (status.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
            holder.type.setText(R.string.download_type_media);
        }

        if (status.getTitle() != null) {
            holder.title.setText(status.getTitle());
        } else {
            holder.title.setText(R.string.download_log_title_unknown);
        }
        holder.date.setText(DateUtils.getRelativeTimeSpanString(status.getCompletionDate().getTime(),
                System.currentTimeMillis(), 0, 0));

        if (status.isSuccessful()) {
            holder.icon.setTextColor(ContextCompat.getColor(context, R.color.download_success_green));
            holder.icon.setText("{fa-check-circle}");
            holder.icon.setContentDescription(context.getString(R.string.download_successful));
            holder.secondaryActionButton.setVisibility(View.INVISIBLE);
            holder.reason.setVisibility(View.GONE);
        } else {
            holder.icon.setTextColor(ContextCompat.getColor(context, R.color.download_failed_red));
            holder.icon.setText("{fa-times-circle}");
            holder.icon.setContentDescription(context.getString(R.string.error_label));
            String reasonText = status.getReason().getErrorString(context);
            if (status.getReasonDetailed() != null) {
                reasonText += ": " + status.getReasonDetailed();
            }
            holder.reason.setText(reasonText);
            holder.reason.setVisibility(View.VISIBLE);

            if (newerWasSuccessful(position, status.getFeedfileType(), status.getFeedfileId())) {
                holder.secondaryActionButton.setVisibility(View.INVISIBLE);
                holder.secondaryActionButton.setOnClickListener(null);
                holder.secondaryActionButton.setTag(null);
            } else {
                holder.secondaryActionIcon.setImageResource(
                        ThemeUtils.getDrawableFromAttr(context, R.attr.navigation_refresh));
                holder.secondaryActionButton.setVisibility(View.VISIBLE);

                if (status.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
                    holder.secondaryActionButton.setOnClickListener(v -> {
                        holder.secondaryActionButton.setVisibility(View.INVISIBLE);
                        Feed feed = DBReader.getFeed(status.getFeedfileId());
                        if (feed == null) {
                            Log.e(TAG, "Could not find feed for feed id: " + status.getFeedfileId());
                            return;
                        }
                        try {
                            DBTasks.forceRefreshFeed(context, feed, true);
                        } catch (DownloadRequestException e) {
                            e.printStackTrace();
                        }
                    });
                } else if (status.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                    holder.secondaryActionButton.setOnClickListener(v -> {
                        holder.secondaryActionButton.setVisibility(View.INVISIBLE);
                        FeedMedia media = DBReader.getFeedMedia(status.getFeedfileId());
                        if (media == null) {
                            Log.e(TAG, "Could not find feed media for feed id: " + status.getFeedfileId());
                            return;
                        }
                        try {
                            DownloadRequester.getInstance().downloadMedia(context, true, media.getItem());
                            ((MainActivity) context).showSnackbarAbovePlayer(
                                    R.string.status_downloading_label, Toast.LENGTH_SHORT);
                        } catch (DownloadRequestException e) {
                            e.printStackTrace();
                            DownloadRequestErrorDialogCreator.newRequestErrorDialog(context, e.getMessage());
                        }
                    });
                }
            }
        }

        return holder.itemView;
    }

    private boolean newerWasSuccessful(int position, int feedTypeId, long id) {
        for (int i = 0; i < position; i++) {
            DownloadStatus status = getItem(i);
            if (status.getFeedfileType() == feedTypeId && status.getFeedfileId() == id && status.isSuccessful()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getCount() {
        return itemAccess.getCount();
    }

    @Override
    public DownloadStatus getItem(int position) {
        return itemAccess.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public interface ItemAccess {
        int getCount();

        DownloadStatus getItem(int position);
    }

}
