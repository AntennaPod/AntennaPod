package de.danoeh.antennapod.ui.screen.onlinefeedview;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.OnlinefeedviewActivityBinding;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.ui.common.ThemeSwitcher;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter.ARG_FEEDURL;
import static de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter.ARG_STARTED_FROM_SEARCH;
import static de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter.ARG_WAS_MANUAL_URL;

/**
 * Downloads a feed from a feed URL and parses it. Subclasses can display the
 * feed object that was parsed. This activity MUST be started with a given URL
 * or an Exception will be thrown.
 * <p/>
 * If the feed cannot be downloaded or parsed, an error dialog will be displayed
 * and the activity will finish as soon as the error dialog is closed.
 */
public class OnlineFeedViewActivity extends AppCompatActivity {
    private static final String TAG = "OnlineFeedViewActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeSwitcher.getTranslucentTheme(this));
        super.onCreate(savedInstanceState);

        OnlinefeedviewActivityBinding viewBinding = OnlinefeedviewActivityBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        viewBinding.transparentBackground.setOnClickListener(v -> finish());
        viewBinding.card.setOnClickListener(null);
        viewBinding.card.setCardBackgroundColor(ThemeUtils.getColorFromAttr(this, R.attr.colorSurface));

        String feedUrl = null;
        if (getIntent().hasExtra(ARG_FEEDURL)) {
            feedUrl = getIntent().getStringExtra(ARG_FEEDURL);
        } else if (TextUtils.equals(getIntent().getAction(), Intent.ACTION_SEND)) {
            feedUrl = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        } else if (TextUtils.equals(getIntent().getAction(), Intent.ACTION_VIEW)) {
            feedUrl = getIntent().getDataString();
        }

        if (feedUrl == null) {
            Log.e(TAG, "feedUrl is null.");
            Toast.makeText(this, R.string.null_value_podcast_error, Toast.LENGTH_LONG).show();
            finish();
        } else {
            Log.d(TAG, "Activity was started with url " + feedUrl);
            // Remove subscribeonandroid.com from feed URL in order to subscribe to the actual feed URL
            if (feedUrl.contains("subscribeonandroid.com")) {
                feedUrl = feedUrl.replaceFirst("((www.)?(subscribeonandroid.com/))", "");
            }

            OnlineFeedViewFragment fragment = OnlineFeedViewFragment.newInstance(feedUrl,
                    getIntent().getBooleanExtra(ARG_STARTED_FROM_SEARCH, false),
                    getIntent().getBooleanExtra(ARG_WAS_MANUAL_URL, false));
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(viewBinding.fragmentContainer.getId(), fragment, OnlineFeedViewFragment.TAG)
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MessageEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");
        Snackbar snackbar = Snackbar.make(findViewById(R.id.fragmentContainer), event.message, Snackbar.LENGTH_LONG);
        if (event.action != null) {
            snackbar.setAction(event.actionText, v -> event.action.accept(this));
        }
        snackbar.show();
    }
}
