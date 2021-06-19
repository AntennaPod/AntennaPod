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
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.WindowCompat;
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
import de.danoeh.antennapod.databinding.VideoplayerActivityBinding;
import de.danoeh.antennapod.dialog.PlaybackControlsDialog;
import de.danoeh.antennapod.dialog.ShareDialog;
import de.danoeh.antennapod.dialog.SkipPreferenceDialog;
import de.danoeh.antennapod.dialog.SleepTimerDialog;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
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
    private Handler videoControlsHider = new Handler(Looper.getMainLooper());
    private VideoplayerActivityBinding viewBinding;
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
        // has to be called before setting layout content
        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
        setTheme(R.style.Theme_AntennaPod_VideoPlayer);
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");
        StorageUtils.checkStorageAvailability(this);

        getWindow().setFormat(PixelFormat.TRANSPARENT);
        viewBinding = VideoplayerActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(viewBinding.getRoot());
        setupView();
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0x80000000));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
            videoControlsHider.removeCallbacks(hideVideoControls);
        }
        // Controller released; we will not receive buffering updates
        viewBinding.progressBar.setVisibility(View.GONE);
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
                viewBinding.progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onBufferEnd() {
                viewBinding.progressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onBufferUpdate(float progress) {
                viewBinding.sbPosition.setSecondaryProgress((int) (progress * viewBinding.sbPosition.getMax()));
            }

            @Override
            public void handleError(int code) {
                final AlertDialog.Builder errorDialog = new AlertDialog.Builder(VideoplayerActivity.this);
                errorDialog.setTitle(R.string.error_label);
                errorDialog.setMessage(MediaPlayerError.getErrorString(VideoplayerActivity.this, code));
                errorDialog.setNeutralButton(android.R.string.ok, (dialog, which) -> finish());
                errorDialog.show();
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
                viewBinding.playButton.setIsShowPlay(showPlay);
            }

            @Override
            public void loadMediaInfo() {
                VideoplayerActivity.this.loadMediaInfo();
            }

            @Override
            public void onAwaitingVideoSurface() {
                setupVideoAspectRatio();
                if (videoSurfaceCreated && controller != null) {
                    Log.d(TAG, "Videosurface already created, setting videosurface now");
                    controller.setVideoSurface(viewBinding.videoView.getHolder());
                }
            }

            @Override
            public void onPlaybackEnd() {
                finish();
            }

            @Override
            protected void setScreenOn(boolean enable) {
                if (enable) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
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

    protected void setupView() {
        showTimeLeft = UserPreferences.shouldShowRemainingTime();
        Log.d("timeleft", showTimeLeft ? "true" : "false");
        viewBinding.durationLabel.setOnClickListener(v -> {
            showTimeLeft = !showTimeLeft;
            Playable media = controller.getMedia();
            if (media == null) {
                return;
            }

            TimeSpeedConverter converter = new TimeSpeedConverter(controller.getCurrentPlaybackSpeedMultiplier());
            String length;
            if (showTimeLeft) {
                int remainingTime = converter.convert(media.getDuration() - media.getPosition());
                length = "-" + Converter.getDurationStringLong(remainingTime);
            } else {
                int duration = converter.convert(media.getDuration());
                length = Converter.getDurationStringLong(duration);
            }
            viewBinding.durationLabel.setText(length);

            UserPreferences.setShowRemainTimeSetting(showTimeLeft);
            Log.d("timeleft on click", showTimeLeft ? "true" : "false");
        });

        viewBinding.sbPosition.setOnSeekBarChangeListener(this);
        viewBinding.rewindButton.setOnClickListener(v -> onRewind());
        viewBinding.rewindButton.setOnLongClickListener(v -> {
            SkipPreferenceDialog.showSkipPreference(VideoplayerActivity.this,
                    SkipPreferenceDialog.SkipDirection.SKIP_REWIND, null);
            return true;
        });
        viewBinding.playButton.setIsVideoScreen(true);
        viewBinding.playButton.setOnClickListener(v -> onPlayPause());
        viewBinding.fastForwardButton.setOnClickListener(v -> onFastForward());
        viewBinding.fastForwardButton.setOnLongClickListener(v -> {
            SkipPreferenceDialog.showSkipPreference(VideoplayerActivity.this,
                    SkipPreferenceDialog.SkipDirection.SKIP_FORWARD, null);
            return false;
        });
        // To suppress touches directly below the slider
        viewBinding.bottomControlsContainer.setOnTouchListener((view, motionEvent) -> true);
        viewBinding.bottomControlsContainer.setFitsSystemWindows(true);
        viewBinding.videoView.getHolder().addCallback(surfaceHolderCallback);
        viewBinding.videoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        setupVideoControlsToggler();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        viewBinding.videoPlayerContainer.setOnTouchListener(onVideoviewTouched);
        viewBinding.videoPlayerContainer.getViewTreeObserver().addOnGlobalLayoutListener(() ->
                viewBinding.videoView.setAvailableSize(
                        viewBinding.videoPlayerContainer.getWidth(), viewBinding.videoPlayerContainer.getHeight()));
    }

    private final Runnable hideVideoControls = () -> {
        if (videoControlsShowing) {
            Log.d(TAG, "Hiding video controls");
            getSupportActionBar().hide();
            hideVideoControls(true);
            videoControlsShowing = false;
        }
    };

    private final View.OnTouchListener onVideoviewTouched = (v, event) -> {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        if (PictureInPictureUtil.isInPictureInPictureMode(this)) {
            return true;
        }
        videoControlsHider.removeCallbacks(hideVideoControls);

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
    };

    private void showSkipAnimation(boolean isForward) {
        AnimationSet skipAnimation = new AnimationSet(true);
        skipAnimation.addAnimation(new ScaleAnimation(1f, 2f, 1f, 2f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
        skipAnimation.addAnimation(new AlphaAnimation(1f, 0f));
        skipAnimation.setFillAfter(false);
        skipAnimation.setDuration(800);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) viewBinding.skipAnimationImage.getLayoutParams();
        if (isForward) {
            viewBinding.skipAnimationImage.setImageResource(R.drawable.ic_fast_forward_video_white);
            params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        } else {
            viewBinding.skipAnimationImage.setImageResource(R.drawable.ic_fast_rewind_video_white);
            params.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        }

        viewBinding.skipAnimationImage.setVisibility(View.VISIBLE);
        viewBinding.skipAnimationImage.setLayoutParams(params);
        viewBinding.skipAnimationImage.startAnimation(skipAnimation);
        skipAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                viewBinding.skipAnimationImage.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void setupVideoControlsToggler() {
        videoControlsHider.removeCallbacks(hideVideoControls);
        videoControlsHider.postDelayed(hideVideoControls, 2500);
    }

    private void setupVideoAspectRatio() {
        if (videoSurfaceCreated && controller != null) {
            Pair<Integer, Integer> videoSize = controller.getVideoSize();
            if (videoSize != null && videoSize.first > 0 && videoSize.second > 0) {
                Log.d(TAG, "Width,height of video: " + videoSize.first + ", " + videoSize.second);
                viewBinding.videoView.setVideoSize(videoSize.first, videoSize.second);
            } else {
                Log.e(TAG, "Could not determine video size");
            }
        }
    }

    private void toggleVideoControlsVisibility() {
        if (videoControlsShowing) {
            getSupportActionBar().hide();
            hideVideoControls(true);
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
        if (controller == null) {
            return;
        }
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

    private final SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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

    private void showVideoControls() {
        viewBinding.bottomControlsContainer.setVisibility(View.VISIBLE);
        viewBinding.controlsContainer.setVisibility(View.VISIBLE);
        final Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        if (animation != null) {
            viewBinding.bottomControlsContainer.startAnimation(animation);
            viewBinding.controlsContainer.startAnimation(animation);
        }
        viewBinding.videoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    private void hideVideoControls(boolean showAnimation) {
        if (showAnimation) {
            final Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            if (animation != null) {
                viewBinding.bottomControlsContainer.startAnimation(animation);
                viewBinding.controlsContainer.startAnimation(animation);
            }
        }
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        viewBinding.bottomControlsContainer.setFitsSystemWindows(true);

        viewBinding.bottomControlsContainer.setVisibility(View.GONE);
        viewBinding.controlsContainer.setVisibility(View.GONE);
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

        boolean hasWebsiteLink = getWebsiteLinkWithFallback(media) != null;
        menu.findItem(R.id.visit_website_item).setVisible(hasWebsiteLink);

        boolean isItemAndHasLink = isFeedMedia && ShareUtils.hasLinkToShare(((FeedMedia) media).getItem());
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
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(VideoplayerActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP  | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }

        if (controller == null) {
            return false;
        }

        Playable media = controller.getMedia();
        if (media == null) {
            return false;
        }
        final @Nullable FeedItem feedItem = getFeedItem(media); // some options option requires FeedItem
        if (item.getItemId() == R.id.add_to_favorites_item && feedItem != null) {
            DBWriter.addFavoriteItem(feedItem);
            isFavorite = true;
            invalidateOptionsMenu();
        } else if (item.getItemId() == R.id.remove_from_favorites_item && feedItem != null) {
            DBWriter.removeFavoriteItem(feedItem);
            isFavorite = false;
            invalidateOptionsMenu();
        } else if (item.getItemId() == R.id.disable_sleeptimer_item
                || item.getItemId() == R.id.set_sleeptimer_item) {
            new SleepTimerDialog().show(getSupportFragmentManager(), "SleepTimerDialog");
        } else if (item.getItemId() == R.id.audio_controls) {
            PlaybackControlsDialog dialog = PlaybackControlsDialog.newInstance();
            dialog.show(getSupportFragmentManager(), "playback_controls");
        } else if (item.getItemId() == R.id.open_feed_item && feedItem != null) {
            Intent intent = MainActivity.getIntentToOpenFeed(this, feedItem.getFeedId());
            startActivity(intent);
        } else if (item.getItemId() == R.id.visit_website_item) {
            IntentUtils.openInBrowser(VideoplayerActivity.this, getWebsiteLinkWithFallback(media));
        } else if (item.getItemId() == R.id.share_item && feedItem != null) {
            ShareDialog shareDialog = ShareDialog.newInstance(feedItem);
            shareDialog.show(getSupportFragmentManager(), "ShareEpisodeDialog");
        } else {
            return false;
        }
        return true;
    }

    private static String getWebsiteLinkWithFallback(Playable media) {
        if (media == null) {
            return null;
        } else if (StringUtils.isNotBlank(media.getWebsiteLink())) {
            return media.getWebsiteLink();
        } else if (media instanceof FeedMedia) {
            return FeedItemUtil.getLinkWithFallback(((FeedMedia) media).getItem());
        }
        return null;
    }

    void onPositionObserverUpdate() {
        if (controller == null) {
            return;
        }

        TimeSpeedConverter converter = new TimeSpeedConverter(controller.getCurrentPlaybackSpeedMultiplier());
        int currentPosition = converter.convert(controller.getPosition());
        int duration = converter.convert(controller.getDuration());
        int remainingTime = converter.convert(
                controller.getDuration() - controller.getPosition());
        Log.d(TAG, "currentPosition " + Converter.getDurationStringLong(currentPosition));
        if (currentPosition == PlaybackService.INVALID_TIME
                || duration == PlaybackService.INVALID_TIME) {
            Log.w(TAG, "Could not react to position observer update because of invalid time");
            return;
        }
        viewBinding.positionLabel.setText(Converter.getDurationStringLong(currentPosition));
        if (showTimeLeft) {
            viewBinding.durationLabel.setText("-" + Converter.getDurationStringLong(remainingTime));
        } else {
            viewBinding.durationLabel.setText(Converter.getDurationStringLong(duration));
        }
        updateProgressbarPosition(currentPosition, duration);
    }

    private void updateProgressbarPosition(int position, int duration) {
        Log.d(TAG, "updateProgressbarPosition(" + position + ", " + duration + ")");
        float progress = ((float) position) / duration;
        viewBinding.sbPosition.setProgress((int) (progress * viewBinding.sbPosition.getMax()));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (controller == null) {
            return;
        }
        if (fromUser) {
            prog = progress / ((float) seekBar.getMax());
            TimeSpeedConverter converter = new TimeSpeedConverter(controller.getCurrentPlaybackSpeedMultiplier());
            int position = converter.convert((int) (prog * controller.getDuration()));
            viewBinding.seekPositionLabel.setText(Converter.getDurationStringLong(position));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        viewBinding.seekCardView.setScaleX(.8f);
        viewBinding.seekCardView.setScaleY(.8f);
        viewBinding.seekCardView.animate()
                .setInterpolator(new FastOutSlowInInterpolator())
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(200)
                .start();
        videoControlsHider.removeCallbacks(hideVideoControls);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (controller != null) {
            controller.seekTo((int) (prog * controller.getDuration()));
        }
        viewBinding.seekCardView.setScaleX(1f);
        viewBinding.seekCardView.setScaleY(1f);
        viewBinding.seekCardView.animate()
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
