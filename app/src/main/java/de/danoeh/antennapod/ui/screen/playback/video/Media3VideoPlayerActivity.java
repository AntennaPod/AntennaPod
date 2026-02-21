package de.danoeh.antennapod.ui.screen.playback.video;

import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.media3.common.Player;
import androidx.media3.common.util.Util;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.Media3VideoPlayerActivityBinding;
import de.danoeh.antennapod.playback.service.Media3PlaybackService;
import de.danoeh.antennapod.playback.service.PlaybackController;
import java.util.concurrent.ExecutionException;

public class Media3VideoPlayerActivity extends AppCompatActivity {
    private static final String TAG = "M3VideoPlayerActivity";
    private Media3VideoPlayerActivityBinding viewBinding;
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setTheme(R.style.Theme_AntennaPod_Dark);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        viewBinding = Media3VideoPlayerActivityBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        viewBinding.playerView.setUseController(false);
        setupControlsView();
        setupFullScreenMode();
        setupPictureInPicture();
    }

    private void setupControlsView() {
        Toolbar toolbar = viewBinding.controlsView.getToolbar();
        toolbar.inflateMenu(R.menu.mediaplayer);

        viewBinding.controlsView.setListener(new VideoPlayerControlsView.ControlsListener() {
            @Override
            public void onPlayPause() {
                PlaybackController.bindToMedia3Service(Media3VideoPlayerActivity.this, controller -> {
                    if (controller.isPlaying()) {
                        controller.pause();
                    } else {
                        controller.play();
                    }
                });
            }

            @Override
            public void onRewind() {
                PlaybackController.bindToMedia3Service(Media3VideoPlayerActivity.this, MediaController::seekBack);
            }

            @Override
            public void onFastForward() {
                PlaybackController.bindToMedia3Service(Media3VideoPlayerActivity.this, MediaController::seekForward);
            }

            @Override
            public void onSeek(int positionMs) {
                PlaybackController.bindToMedia3Service(Media3VideoPlayerActivity.this,
                        controller -> controller.seekTo(positionMs));
            }
        });

        viewBinding.getRoot().setOnTouchListener((v, event) -> {
            viewBinding.controlsView.handleTouchEvent(event, PictureInPictureUtil.isInPictureInPictureMode(this));
            return true;
        });
    }

    private void setupFullScreenMode() {
        viewBinding.playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void setupPictureInPicture() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();

        if (mediaController != null) {
            int videoWidth = mediaController.getVideoSize().width;
            int videoHeight = mediaController.getVideoSize().height;
            if (videoWidth > 0 && videoHeight > 0) {
                if (Build.VERSION.SDK_INT >= 33) {
                    Rational aspectRatio = new Rational(videoWidth, videoHeight);
                    builder.setAspectRatio(aspectRatio);
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 31) {
            builder.setAutoEnterEnabled(true);
            if (viewBinding.playerView.getClipBounds() != null) {
                builder.setSourceRectHint(viewBinding.playerView.getClipBounds());
            }
        }

        setPictureInPictureParams(builder.build());
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= 26
                && PictureInPictureUtil.supportsPictureInPicture(this)
                && !PictureInPictureUtil.isInPictureInPictureMode(this)) {
            viewBinding.controlsView.hideControls(false);
            enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
                                              @NonNull android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        viewBinding.playerView.setUseController(!isInPictureInPictureMode);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SessionToken sessionToken = new SessionToken(this,
                new ComponentName(this, Media3PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                viewBinding.playerView.setPlayer(mediaController);
                setupPictureInPicture();
                setupMedia3Listeners();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error getting media controller", e);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void onStop() {
        viewBinding.playerView.setPlayer(null);
        if (mediaController != null) {
            mediaController.release();
            mediaController = null;
        }
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            controllerFuture = null;
        }
        viewBinding.controlsView.cancelAutoHide();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewBinding.playerView.setUseController(false);
    }

    private void setupMedia3Listeners() {
        if (mediaController == null) {
            return;
        }
        mediaController.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                viewBinding.controlsView.setPlayButtonShowsPlay(Util.shouldShowPlayButton(mediaController));
                if (isPlaying) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                viewBinding.controlsView.setPlayButtonShowsPlay(Util.shouldShowPlayButton(mediaController));
                if (playbackState == Player.STATE_BUFFERING) {
                    viewBinding.controlsView.setProgressBarVisibility(View.VISIBLE);
                } else {
                    viewBinding.controlsView.setProgressBarVisibility(View.INVISIBLE);
                }
            }
        });
    }
}
