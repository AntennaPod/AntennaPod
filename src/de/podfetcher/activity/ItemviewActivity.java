package de.podfetcher.activity;

import java.io.File;
import android.net.Uri;
import com.actionbarsherlock.app.SherlockActivity;
import android.view.View;
import android.widget.ListView;
import android.os.Bundle;
import de.podfetcher.feed.*;
import android.util.Log;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import de.podfetcher.R;

/** Displays a single FeedItem and provides various actions */
public class ItemviewActivity extends SherlockActivity {
	private static final String TAG = "ItemviewActivity";

	private FeedManager manager;
	private FeedItem item;

	// Widgets
	private ImageView imgvImage;
	private TextView txtvTitle;
	private Button butPlay;
	private Button butDownload;
	private Button butRemove;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		manager = FeedManager.getInstance();
		extractFeeditem();
		populateUI();
	}

	/** Extracts FeedItem object the activity is supposed to display */
	private void extractFeeditem() {
		long itemId = getIntent().getLongExtra(FeedItemlistActivity.EXTRA_SELECTED_FEEDITEM, -1);
		long feedId = getIntent().getLongExtra(FeedlistActivity.EXTRA_SELECTED_FEED, -1);
		if(itemId == -1 || feedId == -1) {
			Log.e(TAG, "Received invalid selection of either feeditem or feed.");
		}
		Feed feed = manager.getFeed(feedId);
		item = manager.getFeedItem(itemId, feed);
	}

	private void populateUI() {
		setContentView(R.layout.feeditemview);	
		txtvTitle = (TextView) findViewById(R.id.txtvItemname);
		imgvImage = (ImageView) findViewById(R.id.imgvFeedimage);
		butPlay = (Button) findViewById(R.id.butPlay);
		butDownload = (Button) findViewById(R.id.butDownload);
		butRemove = (Button) findViewById(R.id.butRemove);

		setTitle(item.getFeed().getTitle());

		txtvTitle.setText(item.getTitle());
		if(item.getFeed().getImage() != null) {
			imgvImage.setImageURI(Uri.fromFile(new File(item.getFeed().getImage().getFile_url())));
		}
	}
}


