package de.danoeh.antennapod.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.view.AspectRatioVideoView;

/**
 * Activity for playing video files.
 */
public class VideoplayerActivity extends MediaplayerActivity {
    public static final String TAG = "VideoplayerActivity";

    /**
     * True if video controls are currently visible.
     */
    private boolean videoControlsShowing = true;
    private boolean videoSurfaceCreated = false;
    private boolean destroyingDueToReload = false;

    private VideoControlsHider videoControlsHider = new VideoControlsHider(this);

    private AtomicBoolean isSetup = new AtomicBoolean(false);

    private LinearLayout controls;
    private LinearLayout videoOverlay;
    private AspectRatioVideoView videoview;
    private ProgressBar progressIndicator;

    @Override
    protected void chooseTheme() {
        getActivity().setTheme(R.style.Theme_AntennaPod_VideoPlayer);
    }
/*
    @SuppressLint("AppCompatMethod")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY); // has to be called before setting layout content
        super.onCreate(savedInstanceState);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0x80000000));
    }*/
/*
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
    }*/

    @Override
    public void onPause() {
        videoControlsHider.stop();
        if (controller != null && controller.getStatus() == PlayerStatus.PLAYING) {
            controller.pause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
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
            //getSupportActionBar().setSubtitle(media.getEpisodeTitle());
            //getSupportActionBar().setTitle(media.getFeedTitle());
            return true;
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        if(isSetup.getAndSet(true)) {
            return root;
        }
        controls = (LinearLayout) root.findViewById(R.id.controls);
        videoOverlay = (LinearLayout) root.findViewById(R.id.overlay);
        videoview = (AspectRatioVideoView) root.findViewById(R.id.videoview);
        progressIndicator = (ProgressBar) root.findViewById(R.id.progressIndicator);
        videoview.getHolder().addCallback(surfaceHolderCallback);
        videoview.setOnTouchListener(onVideoviewTouched);

        if (Build.VERSION.SDK_INT >= 16) {
            videoview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        if (Build.VERSION.SDK_INT >= 14) {
            videoOverlay.setFitsSystemWindows(true);
        }

        setupVideoControlsToggler();
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        return root;
    }

    @Override
    protected void onAwaitingVideoSurface() {
        if (videoSurfaceCreated && controller != null) {
            Log.d(TAG, "Videosurface already created, setting videosurface now");

            Pair<Integer, Integer> videoSize = controller.getVideoSize();
            if (videoSize != null && videoSize.first > 0 && videoSize.second > 0) {
                Log.d(TAG, "Width,height of video: " + videoSize.first + ", " + videoSize.second);
                videoview.setVideoSize(videoSize.first, videoSize.second);
            } else {
                Log.e(TAG, "Could not determine video size");
            }
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

    View.OnTouchListener onVideoviewTouched = (v, event) -> {
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
    void setupVideoControlsToggler() {
        videoControlsHider.stop();
        videoControlsHider.start();
    }

    private void toggleVideoControlsVisibility() {
        if (videoControlsShowing) {
            //getSupportActionBar().hide();
            hideVideoControls();
        } else {
            //getSupportActionBar().show();
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
            getActivity().finish();
            startActivity(new Intent(getContext(), AudioplayerActivity.class));
        } else if (notificationCode == PlaybackService.EXTRA_CODE_CAST) {
            Log.d(TAG, "ReloadNotification received, switching to Castplayer now");
            destroyingDueToReload = true;
            getActivity().finish();
            startActivity(new Intent(getContext(), CastplayerActivity.class));
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
        final Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
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
        final Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
        if (animation != null) {
            videoOverlay.startAnimation(animation);
            controls.startAnimation(animation);
        }
        if (Build.VERSION.SDK_INT >= 14) {
            int videoviewFlag = (Build.VERSION.SDK_INT >= 16) ? View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION : 0;
            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN
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
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private static class VideoControlsHider extends Handler {

        private static final int DELAY = 2500;

        private WeakReference<VideoplayerActivity> activity;

        public VideoControlsHider(VideoplayerActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        private final Runnable hideVideoControls = () -> {
            VideoplayerActivity vpa = activity.get();
            if(vpa == null) {
                return;
            }
            if (vpa.videoControlsShowing) {
                Log.d(TAG, "Hiding video controls");
                //vpa.getSupportActionBar().hide();
                vpa.hideVideoControls();
                vpa.videoControlsShowing = false;
            }
        };

        public void start() {
            this.postDelayed(hideVideoControls, DELAY);
        }

        public void stop() {
            this.removeCallbacks(hideVideoControls);
        }

    }

}
