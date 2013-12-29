package de.danoeh.antennapod.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.VideoView;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.MediaType;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.service.playback.PlaybackService;
import de.danoeh.antennapod.service.playback.PlayerStatus;
import de.danoeh.antennapod.util.playback.ExternalMedia;
import de.danoeh.antennapod.util.playback.Playable;

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
    private VideoControlsHider videoControlsToggler;

    private LinearLayout videoOverlay;
    private VideoView videoview;
    private ProgressBar progressIndicator;

    @Override
    protected void chooseTheme() {
        setTheme(R.style.Theme_AntennaPod_Dark);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= 11) {
            requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        }
        super.onCreate(savedInstanceState);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0x80000000));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoControlsToggler != null) {
            videoControlsToggler.cancel(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            Intent intent = getIntent();
            if (AppConfig.DEBUG)
                Log.d(TAG, "Received VIEW intent: "
                        + intent.getData().getPath());
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
        }
    }

    @Override
    protected boolean loadMediaInfo() {
        if (!super.loadMediaInfo()) {
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
        super.setupGUI();
        videoOverlay = (LinearLayout) findViewById(R.id.overlay);
        videoview = (VideoView) findViewById(R.id.videoview);
        progressIndicator = (ProgressBar) findViewById(R.id.progressIndicator);
        videoview.getHolder().addCallback(surfaceHolderCallback);
        videoview.setOnTouchListener(onVideoviewTouched);

        setupVideoControlsToggler();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onAwaitingVideoSurface() {
        if (videoSurfaceCreated) {
            if (AppConfig.DEBUG)
                Log.d(TAG,
                        "Videosurface already created, setting videosurface now");
            controller.setVideoSurface(videoview.getHolder());
        }
    }

    @Override
    protected void postStatusMsg(int resId) {
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

    View.OnTouchListener onVideoviewTouched = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (videoControlsToggler != null) {
                    videoControlsToggler.cancel(true);
                }
                toggleVideoControlsVisibility();
                if (videoControlsShowing) {
                    setupVideoControlsToggler();
                }

                return true;
            } else {
                return false;
            }
        }
    };

    @SuppressLint("NewApi")
    void setupVideoControlsToggler() {
        if (videoControlsToggler != null) {
            videoControlsToggler.cancel(true);
        }
        videoControlsToggler = new VideoControlsHider();
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
            videoControlsToggler
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            videoControlsToggler.execute();
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

    /**
     * Hides the videocontrols after a certain period of time.
     */
    public class VideoControlsHider extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onCancelled() {
            videoControlsToggler = null;
        }

        @Override
        protected void onPostExecute(Void result) {
            videoControlsToggler = null;
        }

        private static final int WAITING_INTERVALL = 5000;
        private static final String TAG = "VideoControlsToggler";

        @Override
        protected void onProgressUpdate(Void... values) {
            if (videoControlsShowing) {
                if (AppConfig.DEBUG)
                    Log.d(TAG, "Hiding video controls");
                getSupportActionBar().hide();
                hideVideoControls();
                videoControlsShowing = false;
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Thread.sleep(WAITING_INTERVALL);
            } catch (InterruptedException e) {
                return null;
            }
            publishProgress();
            return null;
        }

    }

    private final SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            holder.setFixedSize(width, height);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (AppConfig.DEBUG)
                Log.d(TAG, "Videoview holder created");
            videoSurfaceCreated = true;
            if (controller.getStatus() == PlayerStatus.PLAYING) {
                if (controller.serviceAvailable()) {
                    controller.setVideoSurface(holder);
                } else {
                    Log.e(TAG,
                            "Could'nt attach surface to mediaplayer - reference to service was null");
                }
            }

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (AppConfig.DEBUG)
                Log.d(TAG, "Videosurface was destroyed");
            videoSurfaceCreated = false;
            controller.notifyVideoSurfaceAbandoned();
        }
    };


    @Override
    protected void onReloadNotification(int notificationCode) {
        if (notificationCode == PlaybackService.EXTRA_CODE_AUDIO) {
            if (AppConfig.DEBUG)
                Log.d(TAG,
                        "ReloadNotification received, switching to Audioplayer now");
            startActivity(new Intent(this, AudioplayerActivity.class));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        super.onStartTrackingTouch(seekBar);
        if (videoControlsToggler != null) {
            videoControlsToggler.cancel(true);
        }
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

    private void showVideoControls() {
        videoOverlay.setVisibility(View.VISIBLE);
        butPlay.setVisibility(View.VISIBLE);
        final Animation animation = AnimationUtils.loadAnimation(this,
                R.anim.fade_in);
        if (animation != null) {
            videoOverlay.startAnimation(animation);
            butPlay.startAnimation(animation);
        }
        if (Build.VERSION.SDK_INT >= 14) {
            videoview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void hideVideoControls() {
        final Animation animation = AnimationUtils.loadAnimation(this,
                R.anim.fade_out);
        if (animation != null) {
            videoOverlay.startAnimation(animation);
            butPlay.startAnimation(animation);
        }
        if (Build.VERSION.SDK_INT >= 14) {
            videoview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
        videoOverlay.setVisibility(View.GONE);
        butPlay.setVisibility(View.GONE);
    }

    @Override
    protected int getContentViewResourceId() {
        return R.layout.videoplayer_activity;
    }

}
