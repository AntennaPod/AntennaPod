package de.danoeh.antennapod.activity;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.view.*;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mobeta.android.dslv.DragSortListView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.ImageLoader;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.DBTasks;
import de.danoeh.antennapod.storage.DBWriter;
import de.danoeh.antennapod.util.UndoBarController;

import java.util.List;

public class OrganizeQueueActivity extends ActionBarActivity implements
		UndoBarController.UndoListener {
	private static final String TAG = "OrganizeQueueActivity";

	private static final int MENU_ID_ACCEPT = 2;

    private List<FeedItem> queue;

	private OrganizeAdapter adapter;
	private UndoBarController undoBarController;

    private DragSortListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(UserPreferences.getTheme());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.organize_queue);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		listView = (DragSortListView) findViewById(android.R.id.list);
		listView.setDropListener(dropListener);
		listView.setRemoveListener(removeListener);

		loadData();
		undoBarController = new UndoBarController(findViewById(R.id.undobar),
				this);
	}

    private void loadData() {
        AsyncTask<Void, Void, List<FeedItem>> loadTask = new AsyncTask<Void, Void, List<FeedItem>>() {

            @Override
            protected List<FeedItem> doInBackground(Void... voids) {
                return DBReader.getQueue(OrganizeQueueActivity.this);
            }

            @Override
            protected void onPostExecute(List<FeedItem> feedItems) {
                super.onPostExecute(feedItems);
                if (feedItems != null) {
                    queue = feedItems;
                    if (adapter == null) {
                        adapter = new OrganizeAdapter(OrganizeQueueActivity.this);
                        listView.setAdapter(adapter);
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Log.e(TAG, "Queue was null");
                }
            }
        };
        loadTask.execute();
    }

	@Override
	protected void onPause() {
		super.onPause();
		EventDistributor.getInstance().unregister(contentUpdate);
	}

	@Override
	protected void onStop() {
		super.onStop();
        DBTasks.autodownloadUndownloadedItems(getApplicationContext());
	}

	@Override
	protected void onResume() {
		super.onResume();
		EventDistributor.getInstance().register(contentUpdate);
	}

	private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {

		@Override
		public void update(EventDistributor eventDistributor, Integer arg) {
			if (((EventDistributor.QUEUE_UPDATE | EventDistributor.FEED_LIST_UPDATE) & arg) != 0) {
				loadData();
			}
		}
	};

	private DragSortListView.DropListener dropListener = new DragSortListView.DropListener() {

		@Override
		public void drop(int from, int to) {
            final FeedItem item = queue.remove(from);
            queue.add(to, item);
            adapter.notifyDataSetChanged();
            DBWriter.moveQueueItem(OrganizeQueueActivity.this, from, to, true);
		}
	};

	private DragSortListView.RemoveListener removeListener = new DragSortListView.RemoveListener() {

		@Override
		public void remove(int which) {
			FeedItem item = (FeedItem) listView.getAdapter().getItem(which);
            DBWriter.removeQueueItem(OrganizeQueueActivity.this, item.getId(), true);
			undoBarController.showUndoBar(false,
					getString(R.string.removed_from_queue), new UndoToken(item,
							which));
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return false;
		}
	}

	@Override
	public void onUndo(Parcelable token) {
		// Perform the undo
		UndoToken undoToken = (UndoToken) token;
		long itemId = undoToken.getFeedItemId();
		int position = undoToken.getPosition();
        DBWriter.addQueueItemAt(OrganizeQueueActivity.this, itemId, position, false);
	}

	private static class OrganizeAdapter extends BaseAdapter {

		private OrganizeQueueActivity organizeQueueActivity;

		public OrganizeAdapter(OrganizeQueueActivity organizeQueueActivity) {
			super();
			this.organizeQueueActivity = organizeQueueActivity;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Holder holder;
			final FeedItem item = getItem(position);

			if (convertView == null) {
				holder = new Holder();
				LayoutInflater inflater = (LayoutInflater) organizeQueueActivity
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(
						R.layout.organize_queue_listitem, null);
				holder.title = (TextView) convertView
						.findViewById(R.id.txtvTitle);
				holder.feedTitle = (TextView) convertView
						.findViewById(R.id.txtvFeedname);

				holder.feedImage = (ImageView) convertView
						.findViewById(R.id.imgvFeedimage);
				convertView.setTag(holder);
			} else {
				holder = (Holder) convertView.getTag();
			}

			holder.title.setText(item.getTitle());
			holder.feedTitle.setText(item.getFeed().getTitle());

			holder.feedImage.setTag(item.getImageLoaderCacheKey());
			ImageLoader.getInstance().loadThumbnailBitmap(
					item,
					holder.feedImage,
					(int) convertView.getResources().getDimension(
							R.dimen.thumbnail_length));

			return convertView;
		}

		static class Holder {
			TextView title;
			TextView feedTitle;
			ImageView feedImage;
		}

		@Override
		public int getCount() {
			if (organizeQueueActivity.queue != null) {
                return organizeQueueActivity.queue.size();
            } else {
                return 0;
            }
		}

		@Override
		public FeedItem getItem(int position) {
            if (organizeQueueActivity.queue != null) {
                return organizeQueueActivity.queue.get(position);
            } else {
                return null;
            }
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

	}

	private static class UndoToken implements Parcelable {
		private long itemId;
		private long feedId;
		private int position;

		public UndoToken(FeedItem item, int position) {
			this.itemId = item.getId();
			this.feedId = item.getFeed().getId();
			this.position = position;
		}

		private UndoToken(Parcel in) {
			itemId = in.readLong();
			feedId = in.readLong();
			position = in.readInt();
		}

		public static final Parcelable.Creator<UndoToken> CREATOR = new Parcelable.Creator<UndoToken>() {
			public UndoToken createFromParcel(Parcel in) {
				return new UndoToken(in);
			}

			public UndoToken[] newArray(int size) {
				return new UndoToken[size];
			}
		};

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel out, int flags) {
			out.writeLong(itemId);
			out.writeLong(feedId);
			out.writeInt(position);
		}

		public long getFeedItemId() {
			return itemId;
		}

		public int getPosition() {
			return position;
		}
	}

}
