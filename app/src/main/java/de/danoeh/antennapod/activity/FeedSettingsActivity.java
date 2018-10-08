package de.danoeh.antennapod.activity;

import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedFilter;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.glide.FastBlurTransformation;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.menuhandler.FeedMenuHandler;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Displays information about a feed.
 */
public class FeedSettingsActivity extends AppCompatActivity {

    public static final String EXTRA_FEED_ID = "de.danoeh.antennapod.extra.feedId";
    private static final String TAG = "FeedSettingsActivity";
    private boolean autoDeleteChanged = false;
    private Feed feed;

    private ImageView imgvCover;
    private TextView txtvTitle;
    private EditText etxtUsername;
    private EditText etxtPassword;
    private EditText etxtFilterText;
    private RadioButton rdoFilterInclude;
    private RadioButton rdoFilterExclude;
    private CheckBox cbxAutoDownload;
    private CheckBox cbxKeepUpdated;
    private Spinner spnAutoDelete;
    private boolean filterInclude = true;

    private Subscription subscription;

    private boolean authInfoChanged = false;

    private final TextWatcher authTextWatcher = new TextWatcher() {
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

    private boolean filterTextChanged = false;

    private final TextWatcher filterTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            filterTextChanged = true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedsettings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        long feedId = getIntent().getLongExtra(EXTRA_FEED_ID, -1);

        imgvCover = findViewById(R.id.imgvCover);
        txtvTitle = findViewById(R.id.txtvTitle);
        TextView txtvAuthorHeader = findViewById(R.id.txtvAuthor);
        ImageView imgvBackground = findViewById(R.id.imgvBackground);
        findViewById(R.id.butShowInfo).setVisibility(View.INVISIBLE);
        findViewById(R.id.butShowSettings).setVisibility(View.INVISIBLE);
        // https://github.com/bumptech/glide/issues/529
        imgvBackground.setColorFilter(new LightingColorFilter(0xff828282, 0x000000));

        cbxAutoDownload = findViewById(R.id.cbxAutoDownload);
        cbxKeepUpdated = findViewById(R.id.cbxKeepUpdated);
        spnAutoDelete = findViewById(R.id.spnAutoDelete);
        etxtUsername = findViewById(R.id.etxtUsername);
        etxtPassword = findViewById(R.id.etxtPassword);
        etxtFilterText = findViewById(R.id.etxtEpisodeFilterText);
        rdoFilterInclude = findViewById(R.id.radio_filter_include);
        rdoFilterInclude.setOnClickListener(v -> {
            filterInclude = true;
            filterTextChanged = true;
        });
        rdoFilterExclude = findViewById(R.id.radio_filter_exclude);
        rdoFilterExclude.setOnClickListener(v -> {
            filterInclude = false;
            filterTextChanged = true;
        });

        subscription = Observable.fromCallable(()-> DBReader.getFeed(feedId))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result == null) {
                        Log.e(TAG, "Activity was started with invalid arguments");
                        finish();
                    }
                    feed = result;
                    FeedPreferences prefs = feed.getPreferences();
                    Glide.with(FeedSettingsActivity.this)
                            .load(feed.getImageLocation())
                            .placeholder(R.color.light_gray)
                            .error(R.color.light_gray)
                            .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                            .fitCenter()
                            .dontAnimate()
                            .into(imgvCover);
                    Glide.with(FeedSettingsActivity.this)
                            .load(feed.getImageLocation())
                            .placeholder(R.color.image_readability_tint)
                            .error(R.color.image_readability_tint)
                            .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                            .transform(new FastBlurTransformation(FeedSettingsActivity.this))
                            .dontAnimate()
                            .into(imgvBackground);

                    txtvTitle.setText(feed.getTitle());

                    if (!TextUtils.isEmpty(feed.getAuthor())) {
                        txtvAuthorHeader.setText(feed.getAuthor());
                    }

                    cbxAutoDownload.setEnabled(UserPreferences.isEnableAutodownload());
                    cbxAutoDownload.setChecked(prefs.getAutoDownload());
                    cbxAutoDownload.setOnCheckedChangeListener((compoundButton, checked) -> {
                        feed.getPreferences().setAutoDownload(checked);
                        feed.savePreferences();
                        updateAutoDownloadSettings();
                        ApplyToEpisodesDialog dialog = new ApplyToEpisodesDialog(FeedSettingsActivity.this,
                                feed, checked);
                        dialog.createNewDialog().show();
                    });
                    cbxKeepUpdated.setChecked(prefs.getKeepUpdated());
                    cbxKeepUpdated.setOnCheckedChangeListener((compoundButton, checked) -> {
                        feed.getPreferences().setKeepUpdated(checked);
                        feed.savePreferences();
                    });
                    spnAutoDelete.setOnItemSelectedListener(new OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                            FeedPreferences.AutoDeleteAction auto_delete_action;
                            switch (parent.getSelectedItemPosition()) {
                                case 0:
                                    auto_delete_action = FeedPreferences.AutoDeleteAction.GLOBAL;
                                    break;
                                case 1:
                                    auto_delete_action = FeedPreferences.AutoDeleteAction.YES;
                                    break;
                                case 2:
                                    auto_delete_action = FeedPreferences.AutoDeleteAction.NO;
                                    break;
                                default: // TODO - add exceptions here
                                    return;
                            }
                            feed.getPreferences().setAutoDeleteAction(auto_delete_action);// p
                            autoDeleteChanged = true;
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            // Another interface callback
                        }
                    });
                    spnAutoDelete.setSelection(prefs.getAutoDeleteAction().ordinal());

                    etxtUsername.setText(prefs.getUsername());
                    etxtPassword.setText(prefs.getPassword());

                    etxtUsername.addTextChangedListener(authTextWatcher);
                    etxtPassword.addTextChangedListener(authTextWatcher);

                    FeedFilter filter = prefs.getFilter();
                    if (filter.includeOnly()) {
                        etxtFilterText.setText(filter.getIncludeFilter());
                        rdoFilterInclude.setChecked(true);
                        rdoFilterExclude.setChecked(false);
                        filterInclude = true;
                    } else if (filter.excludeOnly()) {
                        etxtFilterText.setText(filter.getExcludeFilter());
                        rdoFilterInclude.setChecked(false);
                        rdoFilterExclude.setChecked(true);
                        filterInclude = false;
                    } else {
                        Log.d(TAG, "No filter set");
                        rdoFilterInclude.setChecked(false);
                        rdoFilterExclude.setChecked(false);
                        etxtFilterText.setText("");
                    }
                    etxtFilterText.addTextChangedListener(filterTextWatcher);

                    supportInvalidateOptionsMenu();
                    updateAutoDownloadSettings();
                }, error -> {
                    Log.d(TAG, Log.getStackTraceString(error));
                    finish();
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (feed != null) {
            FeedPreferences prefs = feed.getPreferences();
            if (authInfoChanged) {
                Log.d(TAG, "Auth info changed, saving credentials");
                prefs.setUsername(etxtUsername.getText().toString());
                prefs.setPassword(etxtPassword.getText().toString());
            }
            if (filterTextChanged) {
                Log.d(TAG, "Filter info changed, saving...");
                String filterText = etxtFilterText.getText().toString();
                String includeString = "";
                String excludeString = "";
                if (filterInclude) {
                    includeString = filterText;
                } else {
                    excludeString = filterText;
                }
                prefs.setFilter(new FeedFilter(includeString, excludeString));
            }
            if (authInfoChanged || autoDeleteChanged || filterTextChanged) {
                DBWriter.setFeedPreferences(prefs);
            }
            authInfoChanged = false;
            autoDeleteChanged = false;
            filterTextChanged = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(subscription != null) {
            subscription.unsubscribe();
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
        menu.findItem(R.id.visit_website_item).setVisible(feed != null && feed.getLink() != null &&
                IntentUtils.isCallable(this, new Intent(Intent.ACTION_VIEW, Uri.parse(feed.getLink()))));
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

    private void updateAutoDownloadSettings() {
        if (feed != null && feed.getPreferences() != null) {
            boolean enabled = feed.getPreferences().getAutoDownload() && UserPreferences.isEnableAutodownload();
            rdoFilterInclude.setEnabled(enabled);
            rdoFilterExclude.setEnabled(enabled);
            etxtFilterText.setEnabled(enabled);
        }
    }

    private static class ApplyToEpisodesDialog extends ConfirmationDialog {

        private final Feed feed;
        private final boolean autoDownload;

        ApplyToEpisodesDialog(Context context, Feed feed, boolean autoDownload) {
            super(context, R.string.auto_download_apply_to_items_title,
                    R.string.auto_download_apply_to_items_message);
            this.feed = feed;
            this.autoDownload = autoDownload;
            setPositiveText(R.string.yes);
            setNegativeText(R.string.no);
        }

        @Override
        public  void onConfirmButtonPressed(DialogInterface dialog) {
            DBWriter.setFeedsItemsAutoDownload(feed, autoDownload);
        }
    }

}
