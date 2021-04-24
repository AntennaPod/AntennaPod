package de.danoeh.antennapod.adapter;

import android.app.Activity;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.BaseAdapter;

import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.ListFragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.view.viewholder.DownloadHeadingViewHolder;
import de.danoeh.antennapod.view.viewholder.DownloadLogItemViewHolder;
import de.danoeh.antennapod.view.viewholder.RunningDownloadViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of DownloadStatus entries.
 */
public class DownloadLogAdapter extends BaseAdapter {
    private static final String TAG = "DownloadLogAdapter";

    private static final String ITEM_RUNNING_HEADER = "RunningHeader";
    private static final String ITEM_LOGS_HEADER = "LogsHeader";
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_LOG = 1;
    public static final int TYPE_RUNNING = 2;

    private final Activity context;
    private final ListFragment listFragment;
    private List<DownloadStatus> downloadLog = new ArrayList<>();
    private List<Downloader> runningDownloads = new ArrayList<>();

    public DownloadLogAdapter(Activity context, ListFragment listFragment) {
        super();
        this.context = context;
        this.listFragment = listFragment;
    }

    public void setDownloadLog(List<DownloadStatus> downloadLog) {
        this.downloadLog = downloadLog;
        notifyDataSetChanged();
    }

    public void setRunningDownloads(List<Downloader> runningDownloads) {
        this.runningDownloads = runningDownloads;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Object item = getItem(position);
        int viewType = getItemViewType(position);

        if (viewType == TYPE_LOG) {
            DownloadLogItemViewHolder holder;
            if (convertView == null || !(convertView.getTag() instanceof DownloadLogItemViewHolder)) {
                holder = new DownloadLogItemViewHolder(context, parent);
                holder.itemView.setTag(holder);
            } else {
                holder = (DownloadLogItemViewHolder) convertView.getTag();
            }
            bind(holder, (DownloadStatus) item, position);
            return holder.itemView;
        } else if (viewType == TYPE_RUNNING) {
            RunningDownloadViewHolder holder;
            if (convertView == null || !(convertView.getTag() instanceof RunningDownloadViewHolder)) {
                holder = new RunningDownloadViewHolder(context, parent);
                holder.itemView.setTag(holder);
            } else {
                holder = (RunningDownloadViewHolder) convertView.getTag();
            }
            bind(holder, (Downloader) item, position);
            return holder.itemView;
        } else {
            DownloadHeadingViewHolder holder;
            if (convertView == null || !(convertView.getTag() instanceof DownloadHeadingViewHolder)) {
                holder = new DownloadHeadingViewHolder(context, parent);
                holder.itemView.setTag(holder);
            } else {
                holder = (DownloadHeadingViewHolder) convertView.getTag();
            }
            bind(holder, item);
            return holder.itemView;
        }
    }

    private void bind(DownloadLogItemViewHolder holder, DownloadStatus status, int position) {
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
            holder.tapForDetails.setVisibility(View.GONE);
        } else {
            holder.icon.setTextColor(ContextCompat.getColor(context, R.color.download_failed_red));
            holder.icon.setText("{fa-times-circle}");
            holder.icon.setContentDescription(context.getString(R.string.error_label));
            holder.reason.setText(status.getReason().getErrorString(context));
            holder.reason.setVisibility(View.VISIBLE);
            holder.tapForDetails.setVisibility(View.VISIBLE);

            if (newerWasSuccessful(position - 2 - runningDownloads.size(),
                    status.getFeedfileType(), status.getFeedfileId())) {
                holder.secondaryActionButton.setVisibility(View.INVISIBLE);
                holder.secondaryActionButton.setOnClickListener(null);
                holder.secondaryActionButton.setTag(null);
            } else {
                holder.secondaryActionIcon.setImageResource(R.drawable.ic_refresh);
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
    }

    private void bind(RunningDownloadViewHolder holder, Downloader downloader, int position) {
        DownloadRequest request = downloader.getDownloadRequest();
        holder.title.setText(request.getTitle());
        holder.secondaryActionIcon.setImageResource(R.drawable.ic_cancel);
        holder.secondaryActionButton.setContentDescription(context.getString(R.string.cancel_download_label));
        holder.secondaryActionButton.setTag(downloader);
        holder.secondaryActionButton.setOnClickListener(v ->
                listFragment.onListItemClick(null, holder.itemView, position, 0));

        boolean percentageWasSet = false;
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
                percentageWasSet = true;
            }
        }
        if (!percentageWasSet) {
            holder.secondaryActionProgress.setPercentage(0, request);
        }
        holder.status.setText(status);
    }

    private void bind(DownloadHeadingViewHolder holder, Object item) {
        if (item == ITEM_RUNNING_HEADER) {
            holder.heading.setText(R.string.downloads_running_label_detailed);
            holder.noItems.setText(R.string.no_run_downloads_label);
            holder.noItems.setVisibility(runningDownloads.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            holder.heading.setText(R.string.downloads_log_label_detailed);
            holder.noItems.setText(R.string.no_log_downloads_label);
            holder.noItems.setVisibility(downloadLog.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private boolean newerWasSuccessful(int downloadStatusIndex, int feedTypeId, long id) {
        for (int i = 0; i < downloadStatusIndex; i++) {
            DownloadStatus status = downloadLog.get(i);
            if (status.getFeedfileType() == feedTypeId && status.getFeedfileId() == id && status.isSuccessful()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getCount() {
        return downloadLog.size() + runningDownloads.size() + 2;
    }

    @Override
    public Object getItem(int position) {
        if (position == 0) {
            return ITEM_RUNNING_HEADER;
        } else if (position - 1 < runningDownloads.size()) {
            return runningDownloads.get(position - 1);
        } else if (position - 1 - runningDownloads.size() == 0) {
            return ITEM_LOGS_HEADER;
        } else if (position - 2 - runningDownloads.size() < downloadLog.size()) {
            return downloadLog.get(position - 2 - runningDownloads.size());
        }
        return ITEM_LOGS_HEADER; // Error
    }

    public int getItemViewType(int position) {
        Object item = getItem(position);
        if (item == ITEM_RUNNING_HEADER || item == ITEM_LOGS_HEADER) {
            return TYPE_HEADER;
        } else if (item instanceof Downloader) {
            return TYPE_RUNNING;
        } else {
            return TYPE_LOG;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

}
