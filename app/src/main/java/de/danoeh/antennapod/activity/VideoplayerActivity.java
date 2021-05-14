package de.danoeh.antennapod.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.WindowCompat;
import androidx.appcompat.app.ActionBar;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.bumptech.glide.Glide;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
import de.danoeh.antennapod.core.event.ServiceEvent;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
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
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.dialog.PlaybackControlsDialog;
import de.danoeh.antennapod.dialog.ShareDialog;
import de.danoeh.antennapod.dialog.SkipPreferenceDialog;
import de.danoeh.antennapod.dialog.SleepTimerDialog;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.view.AspectRatioVideoView;
import de.danoeh.antennapod.view.PlayButton;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Activity for playing video files.
 */
public class VideoplayerActivity extends CastEnabledActivity implements SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "VideoplayerActivity";

    /**
     * True if video controls are currently visible.
     */
    private boolean videoControlsShowing = true;
    private boolean videoSurfaceCreated = false;
    private boolean destroyingDueToReload = false;
    private long lastScreenTap = 0;

    private VideoControlsHider videoControlsHider = new VideoControlsHider(this);

    private final AtomicBoolean isSetup = new AtomicBoolean(false);

    private LinearLayout controls;
    private LinearLayout videoOverlay;
    private AspectRatioVideoView videoview;
    private ProgressBar progressIndicator;
    private FrameLayout videoframe;
    private ImageView skipAnimationView;
    private TextView txtvPosition;
    private TextView txtvLength;
    private SeekBar sbPosition;
    private ImageButton butRev;
    private TextView txtvRev;
    private PlayButton butPlay;
    private ImageButton butFF;
    private TextView txtvFF;
    private ImageButton butSkip;
    private CardView cardViewSeek;
    private TextView txtvSeek;

    private PlaybackController controller;
    private boolean showTimeLeft = false;
    private boolean isFavorite = false;
    private Disposable disposable;
    private float prog;

    @SuppressLint("AppCompatMethod")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY); // has to be called before setting layout content
        setTheme(R.style.Theme_AntennaPod_VideoPlayer);
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");
        StorageUtils.checkStorageAvailability(this);

        getWindow().setFormat(PixelFormat.TRANSPARENT);
        setupGUI();
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0x80000000));
    }

    @Override
    protected void onResume() {
        super.onResume();
        StorageUtils.checkStorageAvailability(this);
        if (PlaybackService.isCasting()) {
            Intent intent = PlaybackService.getPlayerActivityIntent(this);
            if (!intent.getComponent().getClassName().equals(VideoplayerActivity.class.getName())) {
                destroyingDueToReload = true;
                finish();
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onStop() {
        if (controller != null) {
            controller.release();
            controller = null; // prevent leak
        }
        if (disposable != null) {
            disposable.dispose();
        }
        EventBus.getDefault().unregister(this);
        super.onStop();
        if (!PictureInPictureUtil.isInPictureInPictureMode(this)) {
            videoControlsHider.stop();
        }
        progressIndicator.setVisibility(View.GONE); // Controller released; we will not receive buffering updates
    }

    @Override
    public void onUserLeaveHint() {
        if (!PictureInPictureUtil.isInPictureInPictureMode(this) && UserPreferences.getVideoBackgroundBehavior()
                == UserPreferences.VideoBackgroundBehavior.PICTURE_IN_PICTURE) {
            compatEnterPictureInPicture();
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
    protected void onPause() {
        if (!PictureInPictureUtil.isInPictureInPictureMode(this)) {
            if (controller != null && controller.getStatus() == PlayerStatus.PLAYING) {
                controller.pause();
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        videoControlsHider.stop();
        videoControlsHider = null;
        super.onDestroy();
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

    private PlaybackController newPlaybackController() {
        return new PlaybackController(this) {
            @Override
            public void onPositionObserverUpdate() {
                VideoplayerActivity.this.onPositionObserverUpdate();
            }

            @Override
            public void onBufferStart() {
                VideoplayerActivity.this.onBufferStart();
            }

            @Override
            public void onBufferEnd() {
                VideoplayerActivity.this.onBufferEnd();
            }

            @Override
            public void onBufferUpdate(float progress) {
                if (sbPosition != null) {
                    sbPosition.setSecondaryProgress((int) (progress * sbPosition.getMax()));
                }
            }

            @Override
            public void handleError(int code) {
                VideoplayerActivity.this.handleError(code);
            }

            @Override
            public void onReloadNotification(int code) {
                VideoplayerActivity.this.onReloadNotification(code);
            }

            @Override
            public void onSleepTimerUpdate() {
                supportInvalidateOptionsMenu();
            }

            @Override
            protected void updatePlayButtonShowsPlay(boolean showPlay) {
                butPlay.setIsShowPlay(showPlay);
            }

            @Override
            public void loadMediaInfo() {
                VideoplayerActivity.this.loadMediaInfo();
            }

            @Override
            public void onAwaitingVideoSurface() {
                VideoplayerActivity.this.onAwaitingVideoSurface();
            }

            @Override
            public void onPlaybackEnd() {
                finish();
            }

            @Override
            protected void setScreenOn(boolean enable) {
                super.setScreenOn(enable);
                VideoplayerActivity.this.setScreenOn(enable);
            }
        };
    }

    protected void loadMediaInfo() {
        Log.d(TAG, "loadMediaInfo()");
        if (controller == null || controller.getMedia() == null) {
            return;
        }
        showTimeLeft = UserPreferences.shouldShowRemainingTime();
        onPositionObserverUpdate();
        checkFavorite();
        Playable media = controller.getMedia();
        if (media != null) {
            getSupportActionBar().setSubtitle(media.getEpisodeTitle());
            getSupportActionBar().setTitle(media.getFeedTitle());
        }
    }

    protected void setupGUI() {
        if (isSetup.getAndSet(true)) {
            return;
        }
        setContentView(R.layout.videoplayer_activity);
        sbPosition = findViewById(R.id.sbPosition);
        txtvPosition = findViewById(R.id.txtvPosition);
        cardViewSeek = findViewById(R.id.cardViewSeek);
        txtvSeek = findViewById(R.id.txtvSeek);

        showTimeLeft = UserPreferences.shouldShowRemainingTime();
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

                UserPreferences.setShowRemainTimeSetting(showTimeLeft);
                Log.d("timeleft on click", showTimeLeft ? "true" : "false");
            });
        }

        butRev = findViewById(R.id.butRev);
        txtvRev = findViewById(R.id.txtvRev);
        if (txtvRev != null) {
            txtvRev.setText(NumberFormat.getInstance().format(UserPreferences.getRewindSecs()));
        }
        butPlay = findViewById(R.id.butPlay);
        butPlay.setIsVideoScreen(true);
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
                SkipPreferenceDialog.showSkipPreference(VideoplayerActivity.this,
                        SkipPreferenceDialog.SkipDirection.SKIP_REWIND, txtvRev);
                return true;
            });
        }

        butPlay.setOnClickListener(v -> onPlayPause());

        if (butFF != null) {
            butFF.setOnClickListener(v -> onFastForward());
            butFF.setOnLongClickListener(v -> {
                SkipPreferenceDialog.showSkipPreference(VideoplayerActivity.this,
                        SkipPreferenceDialog.SkipDirection.SKIP_FORWARD, txtvFF);
                return false;
            });
        }

        if (butSkip != null) {
            butSkip.setOnClickListener(v ->
                    IntentUtils.sendLocalBroadcast(VideoplayerActivity.this, PlaybackService.ACTION_SKIP_CURRENT_EPISODE));
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        controls = findViewById(R.id.controls);
        videoOverlay = findViewById(R.id.overlay);
        videoview = findViewById(R.id.videoview);
        videoframe = findViewById(R.id.videoframe);
        progressIndicator = findViewById(R.id.progressIndicator);
        skipAnimationView = findViewById(R.id.skip_animation);
        videoview.getHolder().addCallback(surfaceHolderCallback);
        videoframe.setOnTouchListener(onVideoviewTouched);
        videoOverlay.setOnTouchListener((view, motionEvent) -> true); // To suppress touches directly below the slider
        videoview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        videoOverlay.setFitsSystemWindows(true);

        setupVideoControlsToggler();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        videoframe.getViewTreeObserver().addOnGlobalLayoutListener(() ->
                videoview.setAvailableSize(videoframe.getWidth(), videoframe.getHeight()));
    }

    protected void onAwaitingVideoSurface() {
        setupVideoAspectRatio();
        if (videoSurfaceCreated && controller != null) {
            Log.d(TAG, "Videosurface already created, setting videosurface now");
            controller.setVideoSurface(videoview.getHolder());
        }
    }

    private final View.OnTouchListener onVideoviewTouched = (v, event) -> {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (PictureInPictureUtil.isInPictureInPictureMode(this)) {
                return true;
            }
            videoControlsHider.stop();

            if (System.currentTimeMillis() - lastScreenTap < 300) {
                if (event.getX() > v.getMeasuredWidth() / 2.0f) {
                    onFastForward();
                    showSkipAnimation(true);
                } else {
                    onRewind();
                    showSkipAnimation(false);
                }
                if (videoControlsShowing) {
                    getSupportActionBar().hide();
                    hideVideoControls(false);
                    videoControlsShowing = false;
                }
                return true;
            }

            toggleVideoControlsVisibility();
            if (videoControlsShowing) {
                setupVideoControlsToggler();
            }

            lastScreenTap = System.currentTimeMillis();
            return true;
        } else {
            return false;
        }
    };

    private void showSkipAnimation(boolean isForward) {
        AnimationSet skipAnimation = new AnimationSet(true);
        skipAnimation.addAnimation(new ScaleAnimation(1f, 2f, 1f, 2f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
        skipAnimation.addAnimation(new AlphaAnimation(1f, 0f));
        skipAnimation.setFillAfter(false);
        skipAnimation.setDuration(800);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) skipAnimationView.getLayoutParams();
        if (isForward) {
            skipAnimationView.setImageResource(R.drawable.ic_fast_forward_video_white);
            params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        } else {
            skipAnimationView.setImageResource(R.drawable.ic_fast_rewind_video_white);
            params.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        }

        skipAnimationView.setVisibility(View.VISIBLE);
        skipAnimationView.setLayoutParams(params);
        skipAnimationView.startAnimation(skipAnimation);
        skipAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                skipAnimationView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    @SuppressLint("NewApi")
    private void setupVideoControlsToggler() {
        videoControlsHider.stop();
        videoControlsHider.start();
    }

    private void setupVideoAspectRatio() {
        if (videoSurfaceCreated && controller != null) {
            Pair<Integer, Integer> videoSize = controller.getVideoSize();
            if (videoSize != null && videoSize.first > 0 && videoSize.second > 0) {
                Log.d(TAG, "Width,height of video: " + videoSize.first + ", " + videoSize.second);
                videoview.setVideoSize(videoSize.first, videoSize.second);
            } else {
                Log.e(TAG, "Could not determine video size");
            }
        }
    }

    private void toggleVideoControlsVisibility() {
        if (videoControlsShowing) {
            getSupportActionBar().hide();
            hideVideoControls();
        } else {
            getSupportActionBar().show();
            showVideoControls();
        }
        videoControlsShowing = !videoControlsShowing;
    }

    void onRewind() {
        if (controller == null) {
            return;
        }
        int curr = controller.getPosition();
        controller.seekTo(curr - UserPreferences.getRewindSecs() * 1000);
        setupVideoControlsToggler();
    }

    void onPlayPause() {
        if(controller == null) {
            return;
        }
        controller.init();
        controller.playPause();
        setupVideoControlsToggler();
    }

    void onFastForward() {
        if (controller == null) {
            return;
        }
        int curr = controller.getPosition();
        controller.seekTo(curr + UserPreferences.getFastForwardSecs() * 1000);
        setupVideoControlsToggler();
    }

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

    private final SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            holder.setFixedSize(width, height);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "Videoview holder created");
            videoSurfaceCreated = true;
            if (controller != null && controller.getStatus() == PlayerStatus.PLAYING) {
                controller.setVideoSurface(holder);
            }
            setupVideoAspectRatio();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "Videosurface was destroyed");
            videoSurfaceCreated = false;
            if (controller != null && !destroyingDueToReload
                    && UserPreferences.getVideoBackgroundBehavior()
                    != UserPreferences.VideoBackgroundBehavior.CONTINUE_PLAYING) {
                controller.notifyVideoSurfaceAbandoned();
            }
        }
    };

    protected void onReloadNotification(int notificationCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && PictureInPictureUtil.isInPictureInPictureMode(this)) {
            if (notificationCode == PlaybackService.EXTRA_CODE_AUDIO
                    || notificationCode == PlaybackService.EXTRA_CODE_CAST) {
                finish();
            }
            return;
        }
        if (notificationCode == PlaybackService.EXTRA_CODE_CAST) {
            Log.d(TAG, "ReloadNotification received, switching to Castplayer now");
            destroyingDueToReload = true;
            finish();
            new MainActivityStarter(this).withOpenPlayer().start();
        }
    }

    protected void onBufferStart() {
        progressIndicator.setVisibility(View.VISIBLE);
    }

    protected void onBufferEnd() {
        progressIndicator.setVisibility(View.INVISIBLE);
    }

    @SuppressLint("NewApi")
    private void showVideoControls() {
        videoOverlay.setVisibility(View.VISIBLE);
        controls.setVisibility(View.VISIBLE);
        final Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        if (animation != null) {
            videoOverlay.startAnimation(animation);
            controls.startAnimation(animation);
        }
        videoview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    @SuppressLint("NewApi")
    private void hideVideoControls(boolean showAnimation) {
        if (showAnimation) {
            final Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            if (animation != null) {
                videoOverlay.startAnimation(animation);
                controls.startAnimation(animation);
            }
        }
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        videoOverlay.setFitsSystemWindows(true);

        videoOverlay.setVisibility(View.GONE);
        controls.setVisibility(View.GONE);
    }

    private void hideVideoControls() {
        hideVideoControls(true);
    }

    private void setScreenOn(boolean enable) {
        if (enable) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        onPositionObserverUpdate();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlaybackServiceChanged(ServiceEvent event) {
        if (event.action == ServiceEvent.Action.SERVICE_SHUT_DOWN) {
            finish();
        }
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

        if (PictureInPictureUtil.supportsPictureInPicture(this)) {
            menu.findItem(R.id.player_go_to_picture_in_picture).setVisible(true);
        }
        menu.findItem(R.id.audio_controls).setIcon(R.drawable.ic_sliders);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.player_go_to_picture_in_picture) {
            compatEnterPictureInPicture();
            return true;
        }
        if (controller == null) {
            return false;
        }
        Playable media = controller.getMedia();
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(VideoplayerActivity.this,
                    MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK);

            View cover = findViewById(R.id.imgvCover);
            if (cover != null) {
                ActivityOptionsCompat options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(VideoplayerActivity.this, cover, "coverTransition");
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
                        IntentUtils.openInBrowser(VideoplayerActivity.this, getWebsiteLinkWithFallback(media));
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
        videoControlsHider.stop();
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
        setupVideoControlsToggler();
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

    private void compatEnterPictureInPicture() {
        if (PictureInPictureUtil.supportsPictureInPicture(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getSupportActionBar().hide();
            hideVideoControls(false);
            enterPictureInPictureMode();
        }
    }

    private static class VideoControlsHider extends Handler {

        private static final int DELAY = 2500;

        private WeakReference<VideoplayerActivity> activity;

        VideoControlsHider(VideoplayerActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        private final Runnable hideVideoControls = () -> {
            VideoplayerActivity vpa = activity != null ? activity.get() : null;
            if (vpa == null) {
                return;
            }
            if (vpa.videoControlsShowing) {
                Log.d(TAG, "Hiding video controls");
                ActionBar actionBar = vpa.getSupportActionBar();
                if (actionBar != null) {
                    actionBar.hide();
                }
                vpa.hideVideoControls();
                vpa.videoControlsShowing = false;
            }
        };

        public void start() {
            this.postDelayed(hideVideoControls, DELAY);
        }

        void stop() {
            this.removeCallbacks(hideVideoControls);
        }

    }


    //Hardware keyboard support
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        View currentFocus = getCurrentFocus();
        if (currentFocus instanceof EditText) {
            return super.onKeyUp(keyCode, event);
        }

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        switch (keyCode) {
            case KeyEvent.KEYCODE_P: //Fallthrough
            case KeyEvent.KEYCODE_SPACE:
                onPlayPause();
                toggleVideoControlsVisibility();
                return true;
            case KeyEvent.KEYCODE_J: //Fallthrough
            case KeyEvent.KEYCODE_A:
            case KeyEvent.KEYCODE_COMMA:
                onRewind();
                showSkipAnimation(false);
                return true;
            case KeyEvent.KEYCODE_K: //Fallthrough
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_PERIOD:
                onFastForward();
                showSkipAnimation(true);
                return true;
            case KeyEvent.KEYCODE_F: //Fallthrough
            case KeyEvent.KEYCODE_ESCAPE:
                //Exit fullscreen mode
                onBackPressed();
                return true;
            case KeyEvent.KEYCODE_I:
                compatEnterPictureInPicture();
                return true;
            case KeyEvent.KEYCODE_PLUS: //Fallthrough
            case KeyEvent.KEYCODE_W:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_MINUS: //Fallthrough
            case KeyEvent.KEYCODE_S:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_M:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI);
                    return true;
                }
                break;
        }

        //Go to x% of video:
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            controller.seekTo((int) (0.1f * (keyCode - KeyEvent.KEYCODE_0) * controller.getDuration()));
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
