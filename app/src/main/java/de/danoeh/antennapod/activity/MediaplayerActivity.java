package de.danoeh.antennapod.activity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.NumberFormat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.ShareUtils;
import de.danoeh.antennapod.core.util.StorageUtils;
import de.danoeh.antennapod.core.util.TimeSpeedConverter;
import de.danoeh.antennapod.core.util.gui.PictureInPictureUtil;
import de.danoeh.antennapod.core.util.playback.MediaPlayerError;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.dialog.PlaybackControlsDialog;
import de.danoeh.antennapod.dialog.ShareDialog;
import de.danoeh.antennapod.dialog.SkipPreferenceDialog;
import de.danoeh.antennapod.dialog.SleepTimerDialog;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Provides general features which are both needed for playing audio and video
 * files.
 */
public abstract class MediaplayerActivity extends CastEnabledActivity implements OnSeekBarChangeListener {
    private static final String TAG = "MediaplayerActivity";
    private static final String PREFS = "MediaPlayerActivityPreferences";
    private static final String PREF_SHOW_TIME_LEFT = "showTimeLeft";

    PlaybackController controller;

    private TextView txtvPosition;
    private TextView txtvLength;
    SeekBar sbPosition;
    private ImageButton butRev;
    private TextView txtvRev;
    private ImageButton butPlay;
    private ImageButton butFF;
    private TextView txtvFF;
    private ImageButton butSkip;
    private CardView cardViewSeek;
    private TextView txtvSeek;

    private boolean showTimeLeft = false;

    private boolean isFavorite = false;

    private Disposable disposable;

    private PlaybackController newPlaybackController() {
        return new PlaybackController(this) {

            @Override
            public void setupGUI() {
                MediaplayerActivity.this.setupGUI();
            }

            @Override
            public void onPositionObserverUpdate() {
                MediaplayerActivity.this.onPositionObserverUpdate();
            }

            @Override
            public void onBufferStart() {
                MediaplayerActivity.this.onBufferStart();
            }

            @Override
            public void onBufferEnd() {
                MediaplayerActivity.this.onBufferEnd();
            }

            @Override
            public void onBufferUpdate(float progress) {
                MediaplayerActivity.this.onBufferUpdate(progress);
            }

            @Override
            public void handleError(int code) {
                MediaplayerActivity.this.handleError(code);
            }

            @Override
            public void onReloadNotification(int code) {
                MediaplayerActivity.this.onReloadNotification(code);
            }

            @Override
            public void onSleepTimerUpdate() {
                supportInvalidateOptionsMenu();
            }

            @Override
            public ImageButton getPlayButton() {
                return butPlay;
            }

            @Override
            public boolean loadMediaInfo() {
                return MediaplayerActivity.this.loadMediaInfo();
            }

            @Override
            public void onAwaitingVideoSurface() {
                MediaplayerActivity.this.onAwaitingVideoSurface();
            }

            @Override
            public void onShutdownNotification() {
                finish();
            }

            @Override
            public void onPlaybackEnd() {
                finish();
            }

            @Override
            public void onPlaybackSpeedChange() {
                MediaplayerActivity.this.onPlaybackSpeedChange();
            }

            @Override
            protected void setScreenOn(boolean enable) {
                super.setScreenOn(enable);
                MediaplayerActivity.this.setScreenOn(enable);
            }

            @Override
            public void onSetSpeedAbilityChanged() {
                MediaplayerActivity.this.onSetSpeedAbilityChanged();
            }
        };
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        onPositionObserverUpdate();
    }

    private void onSetSpeedAbilityChanged() {
        Log.d(TAG, "onSetSpeedAbilityChanged()");
        updatePlaybackSpeedButton();
    }

    private void onPlaybackSpeedChange() {
        updatePlaybackSpeedButtonText();
    }

    void chooseTheme() {
        setTheme(UserPreferences.getTheme());
    }

    void setScreenOn(boolean enable) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        chooseTheme();
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");
        StorageUtils.checkStorageAvailability(this);

        getWindow().setFormat(PixelFormat.TRANSPARENT);
        setupGUI();
    }

    @Override
    protected void onPause() {
        if (!PictureInPictureUtil.isInPictureInPictureMode(this)) {
            if (controller != null) {
                controller.reinitServiceIfPaused();
                controller.pause();
            }
        }
        super.onPause();
    }

    /**
     * Should be used to switch to another player activity if the mime type is
     * not the correct one for the current activity.
     */
    protected abstract void onReloadNotification(int notificationCode);

    /**
     * Should be used to inform the user that the PlaybackService is currently
     * buffering.
     */
    protected void onBufferStart() {

    }

    /**
     * Should be used to hide the view that was showing the 'buffering'-message.
     */
    protected void onBufferEnd() {

    }

    private void onBufferUpdate(float progress) {
        if (sbPosition != null) {
            sbPosition.setSecondaryProgress((int) (progress * sbPosition.getMax()));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        controller = newPlaybackController();
        controller.init();
        loadMediaInfo();
        onPositionObserverUpdate();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        if (controller != null) {
            controller.release();
            controller = null; // prevent leak
        }
        if (disposable != null) {
            disposable.dispose();
        }
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.get(this).trimMemory(level);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Glide.get(this).clearMemory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        requestCastButton(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mediaplayer, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (controller == null) {
            return false;
        }
        Playable media = controller.getMedia();
        boolean isFeedMedia = (media instanceof FeedMedia);

        menu.findItem(R.id.open_feed_item).setVisible(isFeedMedia); // FeedMedia implies it belongs to a Feed

        boolean hasWebsiteLink = ( getWebsiteLinkWithFallback(media) != null );
        menu.findItem(R.id.visit_website_item).setVisible(hasWebsiteLink);

        boolean isItemAndHasLink = isFeedMedia &&
                ShareUtils.hasLinkToShare(((FeedMedia) media).getItem());

        boolean isItemHasDownloadLink = isFeedMedia && ((FeedMedia) media).getDownload_url() != null;

        menu.findItem(R.id.share_item).setVisible(hasWebsiteLink || isItemAndHasLink || isItemHasDownloadLink);

        menu.findItem(R.id.add_to_favorites_item).setVisible(false);
        menu.findItem(R.id.remove_from_favorites_item).setVisible(false);
        if (isFeedMedia) {
            menu.findItem(R.id.add_to_favorites_item).setVisible(!isFavorite);
            menu.findItem(R.id.remove_from_favorites_item).setVisible(isFavorite);
        }

        menu.findItem(R.id.set_sleeptimer_item).setVisible(!controller.sleepTimerActive());
        menu.findItem(R.id.disable_sleeptimer_item).setVisible(controller.sleepTimerActive());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (controller == null) {
            return false;
        }
        Playable media = controller.getMedia();
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(MediaplayerActivity.this,
                    MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK);

            View cover = findViewById(R.id.imgvCover);
            if (cover != null) {
                ActivityOptionsCompat options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(MediaplayerActivity.this, cover, "coverTransition");
                startActivity(intent, options.toBundle());
            } else {
                startActivity(intent);
            }
            finish();
            return true;
        } else {
            if (media != null) {
                final @Nullable FeedItem feedItem = getFeedItem(media); // some options option requires FeedItem
                switch (item.getItemId()) {
                    case R.id.add_to_favorites_item:
                        if (feedItem != null) {
                            DBWriter.addFavoriteItem(feedItem);
                            isFavorite = true;
                            invalidateOptionsMenu();
                        }
                        break;
                    case R.id.remove_from_favorites_item:
                        if (feedItem != null) {
                            DBWriter.removeFavoriteItem(feedItem);
                            isFavorite = false;
                            invalidateOptionsMenu();
                        }
                        break;
                    case R.id.disable_sleeptimer_item: // Fall-through
                    case R.id.set_sleeptimer_item:
                        new SleepTimerDialog().show(getSupportFragmentManager(), "SleepTimerDialog");
                        break;
                    case R.id.audio_controls:
                        PlaybackControlsDialog dialog = PlaybackControlsDialog.newInstance();
                        dialog.show(getSupportFragmentManager(), "playback_controls");
                        break;
                    case R.id.open_feed_item:
                        if (feedItem != null) {
                            Intent intent = MainActivity.getIntentToOpenFeed(this, feedItem.getFeedId());
                            startActivity(intent);
                        }
                        break;
                    case R.id.visit_website_item:
                        IntentUtils.openInBrowser(MediaplayerActivity.this, getWebsiteLinkWithFallback(media));
                        break;
                    case R.id.share_item:
                        if (feedItem != null) {
                            ShareDialog shareDialog = ShareDialog.newInstance(feedItem);
                            shareDialog.show(getSupportFragmentManager(), "ShareEpisodeDialog");
                        }
                        break;
                    default:
                        return false;
                }
                return true;
            } else {
                return false;
            }
        }
    }

    private static String getWebsiteLinkWithFallback(Playable media) {
        if (media == null) {
            return null;
        } else if (StringUtils.isNotBlank(media.getWebsiteLink())) {
            return media.getWebsiteLink();
        } else if (media instanceof FeedMedia) {
            return FeedItemUtil.getLinkWithFallback(((FeedMedia)media).getItem());
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        StorageUtils.checkStorageAvailability(this);
    }

    /**
     * Called by 'handleStatus()' when the PlaybackService is waiting for
     * a video surface.
     */
    protected abstract void onAwaitingVideoSurface();

    void onPositionObserverUpdate() {
        if (controller == null || txtvPosition == null || txtvLength == null) {
            return;
        }

        TimeSpeedConverter converter = new TimeSpeedConverter(controller.getCurrentPlaybackSpeedMultiplier());
        int currentPosition = converter.convert(controller.getPosition());
        int duration = converter.convert(controller.getDuration());
        int remainingTime = converter.convert(
                controller.getDuration() - controller.getPosition());
        Log.d(TAG, "currentPosition " + Converter.getDurationStringLong(currentPosition));
        if (currentPosition == PlaybackService.INVALID_TIME ||
                duration == PlaybackService.INVALID_TIME) {
            Log.w(TAG, "Could not react to position observer update because of invalid time");
            return;
        }
        txtvPosition.setText(Converter.getDurationStringLong(currentPosition));
        if (showTimeLeft) {
            txtvLength.setText("-" + Converter.getDurationStringLong(remainingTime));
        } else {
            txtvLength.setText(Converter.getDurationStringLong(duration));
        }
        updateProgressbarPosition(currentPosition, duration);
    }

    private void updateProgressbarPosition(int position, int duration) {
        Log.d(TAG, "updateProgressbarPosition(" + position + ", " + duration + ")");
        if(sbPosition == null) {
            return;
        }
        float progress = ((float) position) / duration;
        sbPosition.setProgress((int) (progress * sbPosition.getMax()));
    }

    /**
     * Load information about the media that is going to be played or currently
     * being played. This method will be called when the activity is connected
     * to the PlaybackService to ensure that the activity has the right
     * FeedMedia object.
     */
    boolean loadMediaInfo() {
        Log.d(TAG, "loadMediaInfo()");
        if(controller == null || controller.getMedia() == null) {
            return false;
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        showTimeLeft = prefs.getBoolean(PREF_SHOW_TIME_LEFT, false);
        onPositionObserverUpdate();
        checkFavorite();
        updatePlaybackSpeedButton();
        return true;
    }

    void updatePlaybackSpeedButton() {
        // Only meaningful on AudioplayerActivity, where it is overridden.
    }

    void updatePlaybackSpeedButtonText() {
        // Only meaningful on AudioplayerActivity, where it is overridden.
    }

    void setupGUI() {
        setContentView(getContentViewResourceId());
        sbPosition = findViewById(R.id.sbPosition);
        txtvPosition = findViewById(R.id.txtvPosition);
        cardViewSeek = findViewById(R.id.cardViewSeek);
        txtvSeek = findViewById(R.id.txtvSeek);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        showTimeLeft = prefs.getBoolean(PREF_SHOW_TIME_LEFT, false);
        Log.d("timeleft", showTimeLeft ? "true" : "false");
        txtvLength = findViewById(R.id.txtvLength);
        if (txtvLength != null) {
            txtvLength.setOnClickListener(v -> {
                showTimeLeft = !showTimeLeft;
                Playable media = controller.getMedia();
                if (media == null) {
                    return;
                }

                TimeSpeedConverter converter = new TimeSpeedConverter(controller.getCurrentPlaybackSpeedMultiplier());
                String length;
                if (showTimeLeft) {
                    int remainingTime = converter.convert(
                            media.getDuration() - media.getPosition());

                    length = "-" + Converter.getDurationStringLong(remainingTime);
                } else {
                    int duration = converter.convert(media.getDuration());
                    length = Converter.getDurationStringLong(duration);
                }
                txtvLength.setText(length);

                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(PREF_SHOW_TIME_LEFT, showTimeLeft);
                editor.apply();
                Log.d("timeleft on click", showTimeLeft ? "true" : "false");
            });
        }

        butRev = findViewById(R.id.butRev);
        txtvRev = findViewById(R.id.txtvRev);
        if (txtvRev != null) {
            txtvRev.setText(NumberFormat.getInstance().format(UserPreferences.getRewindSecs()));
        }
        butPlay = findViewById(R.id.butPlay);
        butFF = findViewById(R.id.butFF);
        txtvFF = findViewById(R.id.txtvFF);
        if (txtvFF != null) {
            txtvFF.setText(NumberFormat.getInstance().format(UserPreferences.getFastForwardSecs()));
        }
        butSkip = findViewById(R.id.butSkip);

        // SEEKBAR SETUP

        sbPosition.setOnSeekBarChangeListener(this);

        // BUTTON SETUP

        if (butRev != null) {
            butRev.setOnClickListener(v -> onRewind());
            butRev.setOnLongClickListener(v -> {
                SkipPreferenceDialog.showSkipPreference(MediaplayerActivity.this,
                        SkipPreferenceDialog.SkipDirection.SKIP_REWIND, txtvRev);
                return true;
            });
        }

        butPlay.setOnClickListener(v -> onPlayPause());

        if (butFF != null) {
            butFF.setOnClickListener(v -> onFastForward());
            butFF.setOnLongClickListener(v -> {
                SkipPreferenceDialog.showSkipPreference(MediaplayerActivity.this,
                        SkipPreferenceDialog.SkipDirection.SKIP_FORWARD, txtvFF);
                return false;
            });
        }

        if (butSkip != null) {
            butSkip.setOnClickListener(v ->
                    IntentUtils.sendLocalBroadcast(MediaplayerActivity.this, PlaybackService.ACTION_SKIP_CURRENT_EPISODE));
        }
    }

    void onRewind() {
        if (controller == null) {
            return;
        }
        int curr = controller.getPosition();
        controller.seekTo(curr - UserPreferences.getRewindSecs() * 1000);
    }

    void onPlayPause() {
        if(controller == null) {
            return;
        }
        controller.init();
        controller.playPause();
    }

    void onFastForward() {
        if (controller == null) {
            return;
        }
        int curr = controller.getPosition();
        controller.seekTo(curr + UserPreferences.getFastForwardSecs() * 1000);
    }

    protected abstract int getContentViewResourceId();

    private void handleError(int errorCode) {
        final AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
        errorDialog.setTitle(R.string.error_label);
        errorDialog.setMessage(MediaPlayerError.getErrorString(this, errorCode));
        errorDialog.setNeutralButton("OK",
                (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                }
        );
        errorDialog.create().show();
    }

    private float prog;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (controller == null || txtvLength == null) {
            return;
        }
        if (fromUser) {
            prog = progress / ((float) seekBar.getMax());
            TimeSpeedConverter converter = new TimeSpeedConverter(controller.getCurrentPlaybackSpeedMultiplier());
            int position = converter.convert((int) (prog * controller.getDuration()));
            txtvSeek.setText(Converter.getDurationStringLong(position));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        cardViewSeek.setScaleX(.8f);
        cardViewSeek.setScaleY(.8f);
        cardViewSeek.animate()
                .setInterpolator(new FastOutSlowInInterpolator())
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(200)
                .start();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (controller != null) {
            controller.seekTo((int) (prog * controller.getDuration()));
        }
        cardViewSeek.setScaleX(1f);
        cardViewSeek.setScaleY(1f);
        cardViewSeek.animate()
                .setInterpolator(new FastOutSlowInInterpolator())
                .alpha(0f).scaleX(.8f).scaleY(.8f)
                .setDuration(200)
                .start();
    }

    private void checkFavorite() {
        FeedItem feedItem = getFeedItem(controller.getMedia());
        if (feedItem == null) {
            return;
        }
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() -> DBReader.getFeedItem(feedItem.getId()))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                item -> {
                    boolean isFav = item.isTagged(FeedItem.TAG_FAVORITE);
                    if (isFavorite != isFav) {
                        isFavorite = isFav;
                        invalidateOptionsMenu();
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @Nullable
    private static FeedItem getFeedItem(@Nullable Playable playable) {
        if (playable instanceof FeedMedia) {
            return ((FeedMedia) playable).getItem();
        } else {
            return null;
        }
    }
}
