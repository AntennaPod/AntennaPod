package de.danoeh.antennapod.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.util.playback.ExternalMedia;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.view.AspectRatioVideoView;

/**
 * Activity for playing video files.
 */
public class VideoplayerActivity extends MediaplayerActivity {
    private static final String TAG = "VideoplayerActivity";

    /**
     * True if video controls are currently visible.
     */
    private boolean videoControlsShowing = true;
    private boolean videoSurfaceCreated = false;
    private boolean destroyingDueToReload = false;

    private VideoControlsHider videoControlsHider = new VideoControlsHider(this);

    private final AtomicBoolean isSetup = new AtomicBoolean(false);

    private LinearLayout controls;
    private LinearLayout videoOverlay;
    private AspectRatioVideoView videoview;
    private ProgressBar progressIndicator;

    @Override
    protected void chooseTheme() {
        setTheme(R.style.Theme_AntennaPod_VideoPlayer);
    }

    @SuppressLint("AppCompatMethod")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY); // has to be called before setting layout content
        super.onCreate(savedInstanceState);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0x80000000));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            Intent intent = getIntent();
            Log.d(TAG, "Received VIEW intent: " + intent.getData().getPath());
            ExternalMedia media = new ExternalMedia(intent.getData().getPath(),
                    MediaType.VIDEO);
            Intent launchIntent = new Intent(this, PlaybackService.class);
            launchIntent.putExtra(PlaybackService.EXTRA_PLAYABLE, media);
            launchIntent.putExtra(PlaybackService.EXTRA_START_WHEN_PREPARED,
                    true);
            launchIntent.putExtra(PlaybackService.EXTRA_SHOULD_STREAM, false);
            launchIntent.putExtra(PlaybackService.EXTRA_PREPARE_IMMEDIATELY,
                    true);
            startService(launchIntent);
        } else if (PlaybackService.isCasting()) {
            Intent intent = PlaybackService.getPlayerActivityIntent(this);
            if (!intent.getComponent().getClassName().equals(VideoplayerActivity.class.getName())) {
                destroyingDueToReload = true;
                finish();
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onPause() {
        videoControlsHider.stop();
        if (controller != null && controller.getStatus() == PlayerStatus.PLAYING) {
            controller.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        videoControlsHider.stop();
        videoControlsHider = null;
        super.onDestroy();
    }

    @Override
    protected boolean loadMediaInfo() {
        if (!super.loadMediaInfo() || controller == null) {
            return false;
        }
        Playable media = controller.getMedia();
        if (media != null) {
            getSupportActionBar().setSubtitle(media.getEpisodeTitle());
            getSupportActionBar().setTitle(media.getFeedTitle());
            return true;
        }
        return false;
    }

    @Override
    protected void setupGUI() {
        if(isSetup.getAndSet(true)) {
            return;
        }
        super.setupGUI();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        controls = (LinearLayout) findViewById(R.id.controls);
        videoOverlay = (LinearLayout) findViewById(R.id.overlay);
        videoview = (AspectRatioVideoView) findViewById(R.id.videoview);
        progressIndicator = (ProgressBar) findViewById(R.id.progressIndicator);
        videoview.getHolder().addCallback(surfaceHolderCallback);
        videoview.setOnTouchListener(onVideoviewTouched);

        if (Build.VERSION.SDK_INT >= 16) {
            videoview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        if (Build.VERSION.SDK_INT >= 14) {
            videoOverlay.setFitsSystemWindows(true);
        }

        setupVideoControlsToggler();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onAwaitingVideoSurface() {
        setupVideoAspectRatio();
        if (videoSurfaceCreated && controller != null) {
            Log.d(TAG, "Videosurface already created, setting videosurface now");
            controller.setVideoSurface(videoview.getHolder());
        }
    }

    @Override
    protected void postStatusMsg(int resId, boolean showToast) {
        if (resId == R.string.player_preparing_msg) {
            progressIndicator.setVisibility(View.VISIBLE);
        } else {
            progressIndicator.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void clearStatusMsg() {
        progressIndicator.setVisibility(View.INVISIBLE);
    }

    private final View.OnTouchListener onVideoviewTouched = (v, event) -> {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            videoControlsHider.stop();
            toggleVideoControlsVisibility();
            if (videoControlsShowing) {
                setupVideoControlsToggler();
            }
            return true;
        } else {
            return false;
        }
    };

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

    @Override
    protected void onRewind() {
        super.onRewind();
        setupVideoControlsToggler();
    }

    @Override
    protected void onPlayPause() {
        super.onPlayPause();
        setupVideoControlsToggler();
    }

    @Override
    protected void onFastForward() {
        super.onFastForward();
        setupVideoControlsToggler();
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
                if (controller.serviceAvailable()) {
                    controller.setVideoSurface(holder);
                } else {
                    Log.e(TAG, "Couldn't attach surface to mediaplayer - reference to service was null");
                }
            }
            setupVideoAspectRatio();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "Videosurface was destroyed");
            videoSurfaceCreated = false;
            if (controller != null && !destroyingDueToReload) {
                controller.notifyVideoSurfaceAbandoned();
            }
        }
    };


    @Override
    protected void onReloadNotification(int notificationCode) {
        if (notificationCode == PlaybackService.EXTRA_CODE_AUDIO) {
            Log.d(TAG, "ReloadNotification received, switching to Audioplayer now");
            destroyingDueToReload = true;
            finish();
            startActivity(new Intent(this, AudioplayerActivity.class));
        } else if (notificationCode == PlaybackService.EXTRA_CODE_CAST) {
            Log.d(TAG, "ReloadNotification received, switching to Castplayer now");
            destroyingDueToReload = true;
            finish();
            startActivity(new Intent(this, CastplayerActivity.class));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        super.onStartTrackingTouch(seekBar);
        videoControlsHider.stop();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        super.onStopTrackingTouch(seekBar);
        setupVideoControlsToggler();
    }

    @Override
    protected void onBufferStart() {
        progressIndicator.setVisibility(View.VISIBLE);
    }

    @Override
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
        if (Build.VERSION.SDK_INT >= 14) {
            videoview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    @SuppressLint("NewApi")
    private void hideVideoControls() {
        final Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        if (animation != null) {
            videoOverlay.startAnimation(animation);
            controls.startAnimation(animation);
        }
        if (Build.VERSION.SDK_INT >= 14) {
            int videoviewFlag = (Build.VERSION.SDK_INT >= 16) ? View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION : 0;
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | videoviewFlag);
            videoOverlay.setFitsSystemWindows(true);
        }
        videoOverlay.setVisibility(View.GONE);
        controls.setVisibility(View.GONE);
    }

    @Override
    protected int getContentViewResourceId() {
        return R.layout.videoplayer_activity;
    }

    @Override
    protected void setScreenOn(boolean enable) {
        super.setScreenOn(enable);
        if (enable) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
            if(vpa == null) {
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

}
