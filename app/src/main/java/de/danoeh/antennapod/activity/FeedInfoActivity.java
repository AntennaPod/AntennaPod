package de.danoeh.antennapod.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.util.LangUtils;
import de.danoeh.antennapod.menuhandler.FeedMenuHandler;

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
    private EditText etxtUsername;
    private EditText etxtPassword;
    private CheckBox cbxAutoDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedinfo);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        long feedId = getIntent().getLongExtra(EXTRA_FEED_ID, -1);

        imgvCover = (ImageView) findViewById(R.id.imgvCover);
        txtvTitle = (TextView) findViewById(R.id.txtvTitle);
        txtvDescription = (TextView) findViewById(R.id.txtvDescription);
        txtvLanguage = (TextView) findViewById(R.id.txtvLanguage);
        txtvAuthor = (TextView) findViewById(R.id.txtvAuthor);
        cbxAutoDownload = (CheckBox) findViewById(R.id.cbxAutoDownload);
        etxtUsername = (EditText) findViewById(R.id.etxtUsername);
        etxtPassword = (EditText) findViewById(R.id.etxtPassword);

        AsyncTask<Long, Void, Feed> loadTask = new AsyncTask<Long, Void, Feed>() {

            @Override
            protected Feed doInBackground(Long... params) {
                return DBReader.getFeed(FeedInfoActivity.this, params[0]);
            }

            @Override
            protected void onPostExecute(Feed result) {
                if (result != null) {
                    feed = result;
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Language is " + feed.getLanguage());
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Author is " + feed.getAuthor());
                    imgvCover.post(new Runnable() {

                        @Override
                        public void run() {
                            Picasso.with(FeedInfoActivity.this)
                                    .load(feed.getImageUri())
                                    .fit()
                                    .into(imgvCover);
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

                    cbxAutoDownload.setEnabled(UserPreferences.isEnableAutodownload());
                    cbxAutoDownload.setChecked(feed.getPreferences().getAutoDownload());
                    cbxAutoDownload.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                            feed.getPreferences().setAutoDownload(checked);
                            feed.savePreferences(FeedInfoActivity.this);
                        }
                    });

                    etxtUsername.setText(feed.getPreferences().getUsername());
                    etxtPassword.setText(feed.getPreferences().getPassword());

                    etxtUsername.addTextChangedListener(authTextWatcher);
                    etxtPassword.addTextChangedListener(authTextWatcher);

                    supportInvalidateOptionsMenu();

                } else {
                    Log.e(TAG, "Activity was started with invalid arguments");
                }
            }
        };
        loadTask.execute(feedId);
    }


    private boolean authInfoChanged = false;

    private TextWatcher authTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            authInfoChanged = true;
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if (feed != null && authInfoChanged) {
            Log.d(TAG, "Auth info changed, saving credentials");
            FeedPreferences prefs = feed.getPreferences();
            prefs.setUsername(etxtUsername.getText().toString());
            prefs.setPassword(etxtPassword.getText().toString());
            DBWriter.setFeedPreferences(this, prefs);
            authInfoChanged = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.feedinfo, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.support_item).setVisible(
                feed != null && feed.getPaymentLink() != null);
        menu.findItem(R.id.share_link_item).setVisible(feed != null && feed.getLink() != null);
        menu.findItem(R.id.visit_website_item).setVisible(feed != null && feed.getLink() != null);
        return true;
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
