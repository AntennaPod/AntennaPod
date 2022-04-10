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
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadRequestCreator;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.model.download.DownloadStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.core.util.DownloadErrorLabel;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.view.viewholder.DownloadLogItemViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of DownloadStatus entries.
 */
public class DownloadLogAdapter extends BaseAdapter {
    private static final String TAG = "DownloadLogAdapter";

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
        DownloadLogItemViewHolder holder;
        if (convertView == null) {
            holder = new DownloadLogItemViewHolder(context, parent);
            holder.itemView.setTag(holder);
        } else {
            holder = (DownloadLogItemViewHolder) convertView.getTag();
        }

        Object item = getItem(position);
        if (item instanceof DownloadStatus) {
            bind(holder, (DownloadStatus) item, position);
        } else if (item instanceof Downloader) {
            bind(holder, (Downloader) item, position);
        }
        return holder.itemView;
    }

    private void bind(DownloadLogItemViewHolder holder, DownloadStatus status, int position) {
        String statusText = "";
        if (status.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
            statusText += context.getString(R.string.download_type_feed);
        } else if (status.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
            statusText += context.getString(R.string.download_type_media);
        }
        statusText += " · ";
        statusText += DateUtils.getRelativeTimeSpanString(status.getCompletionDate().getTime(),
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, 0);
        holder.status.setText(statusText);

        if (status.getTitle() != null) {
            holder.title.setText(status.getTitle());
        } else {
            holder.title.setText(R.string.download_log_title_unknown);
        }

        if (status.isSuccessful()) {
            holder.icon.setTextColor(ContextCompat.getColor(context, R.color.download_success_green));
            holder.icon.setText("{fa-check-circle}");
            holder.icon.setContentDescription(context.getString(R.string.download_successful));
            holder.secondaryActionButton.setVisibility(View.INVISIBLE);
            holder.reason.setVisibility(View.GONE);
            holder.tapForDetails.setVisibility(View.GONE);
        } else {
            if (status.getReason() == DownloadError.ERROR_PARSER_EXCEPTION_DUPLICATE) {
                holder.icon.setTextColor(ContextCompat.getColor(context, R.color.download_warning_yellow));
                holder.icon.setText("{fa-exclamation-circle}");
            } else {
                holder.icon.setTextColor(ContextCompat.getColor(context, R.color.download_failed_red));
                holder.icon.setText("{fa-times-circle}");
            }
            holder.icon.setContentDescription(context.getString(R.string.error_label));
            holder.reason.setText(DownloadErrorLabel.from(status.getReason()));
            holder.reason.setVisibility(View.VISIBLE);
            holder.tapForDetails.setVisibility(View.VISIBLE);

            if (newerWasSuccessful(position - runningDownloads.size(),
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
                        DBTasks.forceRefreshFeed(context, feed, true);
                    });
                } else if (status.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                    holder.secondaryActionButton.setOnClickListener(v -> {
                        holder.secondaryActionButton.setVisibility(View.INVISIBLE);
                        FeedMedia media = DBReader.getFeedMedia(status.getFeedfileId());
                        if (media == null) {
                            Log.e(TAG, "Could not find feed media for feed id: " + status.getFeedfileId());
                            return;
                        }
                        DownloadService.download(context, true, DownloadRequestCreator.create(media).build());
                        ((MainActivity) context).showSnackbarAbovePlayer(
                                R.string.status_downloading_label, Toast.LENGTH_SHORT);
                    });
                }
            }
        }
    }

    private void bind(DownloadLogItemViewHolder holder, Downloader downloader, int position) {
        DownloadRequest request = downloader.getDownloadRequest();
        holder.title.setText(request.getTitle());
        holder.secondaryActionIcon.setImageResource(R.drawable.ic_cancel);
        holder.secondaryActionButton.setContentDescription(context.getString(R.string.cancel_download_label));
        holder.secondaryActionButton.setVisibility(View.VISIBLE);
        holder.secondaryActionButton.setTag(downloader);
        holder.secondaryActionButton.setOnClickListener(v -> {
            DownloadService.cancel(context, request.getSource());
            if (request.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                FeedMedia media = DBReader.getFeedMedia(request.getFeedfileId());
                FeedItem feedItem = media.getItem();
                feedItem.disableAutoDownload();
                DBWriter.setFeedItem(feedItem);
            }
        });
        holder.reason.setVisibility(View.GONE);
        holder.tapForDetails.setVisibility(View.GONE);
        holder.icon.setTextColor(ThemeUtils.getColorFromAttr(context, R.attr.colorPrimary));
        holder.icon.setText("{fa-arrow-circle-down}");
        holder.icon.setContentDescription(context.getString(R.string.status_downloading_label));

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
        return downloadLog.size() + runningDownloads.size();
    }

    @Override
    public Object getItem(int position) {
        if (position < runningDownloads.size()) {
            return runningDownloads.get(position);
        } else if (position - runningDownloads.size() < downloadLog.size()) {
            return downloadLog.get(position - runningDownloads.size());
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

}
