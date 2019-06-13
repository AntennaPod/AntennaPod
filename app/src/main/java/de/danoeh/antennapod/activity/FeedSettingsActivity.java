package de.danoeh.antennapod.activity;

import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.glide.FastBlurTransformation;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.fragment.FeedSettingsFragment;
import io.reactivex.Maybe;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Displays information about a feed.
 */
public class FeedSettingsActivity extends AppCompatActivity {
    public static final String EXTRA_FEED_ID = "de.danoeh.antennapod.extra.feedId";
    private static final String TAG = "FeedSettingsActivity";
    private Feed feed;
    private Disposable disposable;
    private ImageView imgvCover;
    private TextView txtvTitle;
    private ImageView imgvBackground;
    private TextView txtvAuthorHeader;

    /*
    private EditText etxtUsername;
    private EditText etxtPassword;
    private EditText etxtFilterText;
    private RadioButton rdoFilterInclude;
    private RadioButton rdoFilterExclude;
    private CheckBox cbxAutoDownload;
    private CheckBox cbxKeepUpdated;
    private Spinner spnAutoDelete;
    private boolean filterInclude = true;
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedsettings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        long feedId = getIntent().getLongExtra(EXTRA_FEED_ID, -1);

        imgvCover = findViewById(R.id.imgvCover);
        txtvTitle = findViewById(R.id.txtvTitle);
        txtvAuthorHeader = findViewById(R.id.txtvAuthor);
        imgvBackground = findViewById(R.id.imgvBackground);
        findViewById(R.id.butShowInfo).setVisibility(View.INVISIBLE);
        findViewById(R.id.butShowSettings).setVisibility(View.INVISIBLE);
        // https://github.com/bumptech/glide/issues/529
        imgvBackground.setColorFilter(new LightingColorFilter(0xff828282, 0x000000));

        /*
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
        */

        disposable = Maybe.create((MaybeOnSubscribe<Feed>) emitter -> {
                    Feed feed = DBReader.getFeed(feedId);
                    if (feed != null) {
                        emitter.onSuccess(feed);
                    } else {
                        emitter.onComplete();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    feed = result;
                    showFragment();
                    showHeader();
                }, error -> {
                    Log.d(TAG, Log.getStackTraceString(error));
                    finish();
                }, () -> {
                    Log.e(TAG, "Activity was started with invalid arguments");
                    finish();
                });
    }

    private void showFragment() {
        FeedSettingsFragment fragment = new FeedSettingsFragment();
        fragment.setArguments(getIntent().getExtras());

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =
                fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.settings_fragment_container, fragment);
        fragmentTransaction.commit();
    }

    public Feed getFeed() {
        return feed;
    }

    private void showHeader() {
        txtvTitle.setText(feed.getTitle());

        if (!TextUtils.isEmpty(feed.getAuthor())) {
            txtvAuthorHeader.setText(feed.getAuthor());
        }

        Glide.with(FeedSettingsActivity.this)
                .load(feed.getImageLocation())
                .apply(new RequestOptions()
                        .placeholder(R.color.light_gray)
                        .error(R.color.light_gray)
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .fitCenter()
                        .dontAnimate())
                .into(imgvCover);
        Glide.with(FeedSettingsActivity.this)
                .load(feed.getImageLocation())
                .apply(new RequestOptions()
                        .placeholder(R.color.image_readability_tint)
                        .error(R.color.image_readability_tint)
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .transform(new FastBlurTransformation())
                        .dontAnimate())
                .into(imgvBackground);
    }

    /*
    private void setupPrefs(Feed feed) {
        FeedPreferences prefs = feed.getPreferences();

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
    */

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
