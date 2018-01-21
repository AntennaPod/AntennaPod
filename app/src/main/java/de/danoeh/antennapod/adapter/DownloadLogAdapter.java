package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.joanzapata.iconify.widget.IconButton;
import com.joanzapata.iconify.widget.IconTextView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DownloadRequestException;

/** Displays a list of DownloadStatus entries. */
public class DownloadLogAdapter extends BaseAdapter {

	private static final String TAG = "DownloadLogAdapter";

	private final Context context;

    private final ItemAccess itemAccess;

	public DownloadLogAdapter(Context context, ItemAccess itemAccess) {
		super();
        this.itemAccess = itemAccess;
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		DownloadStatus status = getItem(position);
		if (convertView == null) {
			holder = new Holder();
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.downloadlog_item, parent, false);
			holder.icon = (IconTextView) convertView.findViewById(R.id.txtvIcon);
			holder.retry = (IconButton) convertView.findViewById(R.id.btnRetry);
			holder.date = (TextView) convertView.findViewById(R.id.txtvDate);
			holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
			if(Build.VERSION.SDK_INT >= 23) {
				holder.title.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL);
			}
			holder.type = (TextView) convertView.findViewById(R.id.txtvType);
			holder.reason = (TextView) convertView.findViewById(R.id.txtvReason);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}
		if (status.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
			holder.type.setText(R.string.download_type_feed);
		} else if (status.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
			holder.type.setText(R.string.download_type_media);
		} else if (status.getFeedfileType() == FeedImage.FEEDFILETYPE_FEEDIMAGE) {
			holder.type.setText(R.string.download_type_image);
		}
		if (status.getTitle() != null) {
			holder.title.setText(status.getTitle());
		} else {
			holder.title.setText(R.string.download_log_title_unknown);
		}
		holder.date.setText(DateUtils.getRelativeTimeSpanString(
				status.getCompletionDate().getTime(),
				System.currentTimeMillis(), 0, 0));
		if (status.isSuccessful()) {
			holder.icon.setTextColor(ContextCompat.getColor(convertView.getContext(),
					R.color.download_success_green));
			holder.icon.setText("{fa-check-circle}");
			holder.retry.setVisibility(View.GONE);
			holder.reason.setVisibility(View.GONE);
		} else {
			holder.icon.setTextColor(ContextCompat.getColor(convertView.getContext(),
					R.color.download_failed_red));
			holder.icon.setText("{fa-times-circle}");
			String reasonText = status.getReason().getErrorString(context);
			if (status.getReasonDetailed() != null) {
				reasonText += ": " + status.getReasonDetailed();
			}
			holder.reason.setText(reasonText);
			holder.reason.setVisibility(View.VISIBLE);
			if(status.getFeedfileType() != FeedImage.FEEDFILETYPE_FEEDIMAGE &&
					!newerWasSuccessful(position, status.getFeedfileType(), status.getFeedfileId())) {
				holder.retry.setVisibility(View.VISIBLE);
				holder.retry.setOnClickListener(clickListener);
				ButtonHolder btnHolder;
				if(holder.retry.getTag() != null) {
					btnHolder = (ButtonHolder) holder.retry.getTag();
				}  else {
					btnHolder = new ButtonHolder();
				}
				btnHolder.typeId = status.getFeedfileType();
				btnHolder.id = status.getFeedfileId();
				holder.retry.setTag(btnHolder);
			} else {
				holder.retry.setVisibility(View.GONE);
				holder.retry.setOnClickListener(null);
				holder.retry.setTag(null);
			}
		}

		return convertView;
	}

	private final View.OnClickListener clickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			ButtonHolder holder = (ButtonHolder) v.getTag();
			if(holder.typeId == Feed.FEEDFILETYPE_FEED) {
				Feed feed = DBReader.getFeed(holder.id);
				if (feed != null) {
					try {
						DBTasks.forceRefreshFeed(context, feed);
					} catch (DownloadRequestException e) {
						e.printStackTrace();
					}
				} else {
					Log.wtf(TAG, "Could not find feed for feed id: " + holder.id);
				}
			} else if(holder.typeId == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
				FeedMedia media = DBReader.getFeedMedia(holder.id);
				if (media != null) {
					try {
						DBTasks.downloadFeedItems(context, media.getItem());
						Toast.makeText(context, R.string.status_downloading_label, Toast.LENGTH_SHORT).show();
					} catch (DownloadRequestException e) {
						e.printStackTrace();
						DownloadRequestErrorDialogCreator.newRequestErrorDialog(context, e.getMessage());
					}
				} else {
					Log.wtf(TAG, "Could not find media for id: " + holder.id);
				}
			} else {
				Log.wtf(TAG, "Unexpected type id: " + holder.typeId);
			}
			v.setVisibility(View.GONE);
		}
	};

	private boolean newerWasSuccessful(int position, int feedTypeId, long id) {
		for (int i = 0; i < position; i++) {
			DownloadStatus status = getItem(i);
			if (status.getFeedfileType() == feedTypeId && status.getFeedfileId() == id &&
					status.isSuccessful()) return true;
		}
		return false;
	}

	static class Holder {
		IconTextView icon;
		IconButton retry;
		TextView title;
		TextView type;
		TextView date;
		TextView reason;
	}

	static class ButtonHolder {
		int typeId;
		long id;
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
