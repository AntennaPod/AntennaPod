package de.danoeh.antennapod.activity;

import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.FeedImageLoader;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;

public class OrganizeQueueActivity extends SherlockListActivity {
	private static final String TAG = "OrganizeQueueActivity";

	private static final int MENU_ID_ACCEPT = 2;
	
	private OrganizeAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(PodcastApp.getThemeResourceId());
		super.onCreate(savedInstanceState);

		adapter = new OrganizeAdapter(this, 0, FeedManager.getInstance()
				.getQueue());
		setListAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		TypedArray drawables = obtainStyledAttributes(new int[] { R.attr.navigation_accept });
		menu.add(Menu.NONE, MENU_ID_ACCEPT, Menu.NONE, R.string.confirm_label)
				.setIcon(drawables.getDrawable(0))
				.setShowAsAction(
						MenuItem.SHOW_AS_ACTION_IF_ROOM
								| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ID_ACCEPT:
			finish();
			return true;
		default:
			return false;
		}
	}

	private static class OrganizeAdapter extends ArrayAdapter<FeedItem> {

		private Context context;

		public OrganizeAdapter(Context context, int textViewResourceId,
				List<FeedItem> objects) {
			super(context, textViewResourceId, objects);
			this.context = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Holder holder;
			final FeedItem item = getItem(position);

			if (convertView == null) {
				holder = new Holder();
				LayoutInflater inflater = (LayoutInflater) context
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

			holder.feedImage.setTag(item.getFeed().getImage());
			FeedImageLoader.getInstance().loadThumbnailBitmap(
					item.getFeed().getImage(),
					holder.feedImage,
					(int) convertView.getResources().getDimension(
							R.dimen.thumbnail_length));

			return convertView;
		}

		static class Holder {
			TextView title;
			TextView feedTitle;
			ImageView feedImage;
			View dragHandle;
		}

	}

}
