package de.danoeh.antennapod.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.ImageLoader;
import de.danoeh.antennapod.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.util.LangUtils;
import de.danoeh.antennapod.util.menuhandler.FeedMenuHandler;

/**
 * Displays information about a feed.
 */
public class FeedInfoActivity extends ActionBarActivity {
    private static final String TAG = "FeedInfoActivity";

    public static final String EXTRA_FEED_ID = "de.danoeh.antennapod.extra.feedId";

    private Feed feed;

    private ImageView imgvCover;
    private TextView txtvTitle;
    private TextView txtvDescription;
    private TextView txtvLanguage;
    private TextView txtvAuthor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedinfo);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        long feedId = getIntent().getLongExtra(EXTRA_FEED_ID, -1);

        AsyncTask<Long, Void, Feed> loadTask = new AsyncTask<Long, Void, Feed>() {

            @Override
            protected Feed doInBackground(Long... params) {
                return DBReader.getFeed(FeedInfoActivity.this, params[0]);
            }

            @Override
            protected void onPostExecute(Feed result) {
                super.onPostExecute(result);
                if (result != null) {
                    feed = result;
                    if (AppConfig.DEBUG)
                        Log.d(TAG, "Language is " + feed.getLanguage());
                    if (AppConfig.DEBUG)
                        Log.d(TAG, "Author is " + feed.getAuthor());
                    imgvCover = (ImageView) findViewById(R.id.imgvCover);
                    txtvTitle = (TextView) findViewById(R.id.txtvTitle);
                    txtvDescription = (TextView) findViewById(R.id.txtvDescription);
                    txtvLanguage = (TextView) findViewById(R.id.txtvLanguage);
                    txtvAuthor = (TextView) findViewById(R.id.txtvAuthor);
                    imgvCover.post(new Runnable() {

                        @Override
                        public void run() {
                            ImageLoader.getInstance().loadThumbnailBitmap(
                                    feed.getImage(), imgvCover);
                        }
                    });

                    txtvTitle.setText(feed.getTitle());
                    txtvDescription.setText(feed.getDescription());
                    if (feed.getAuthor() != null) {
                        txtvAuthor.setText(feed.getAuthor());
                    }
                    if (feed.getLanguage() != null) {
                        txtvLanguage.setText(LangUtils
                                .getLanguageString(feed.getLanguage()));
                    }
                    supportInvalidateOptionsMenu();

                } else {
                    Log.e(TAG, "Activity was started with invalid arguments");
                }
            }
        };
        loadTask.execute(feedId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (feed != null) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.feedinfo, menu);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (feed != null) {
        menu.findItem(R.id.support_item).setVisible(
                feed.getPaymentLink() != null);
        menu.findItem(R.id.share_link_item).setVisible(feed.getLink() != null);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                try {
                    return FeedMenuHandler.onOptionsItemClicked(this, item, feed);
                } catch (DownloadRequestException e) {
                    e.printStackTrace();
                    DownloadRequestErrorDialogCreator.newRequestErrorDialog(this,
                            e.getMessage());
                }
                return super.onOptionsItemSelected(item);
        }
    }
}
