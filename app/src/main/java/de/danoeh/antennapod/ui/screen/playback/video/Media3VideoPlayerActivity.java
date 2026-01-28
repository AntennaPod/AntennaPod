package de.danoeh.antennapod.ui.screen.playback.video;

import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.ui.PlayerView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.playback.service.Media3PlaybackService;

import java.util.concurrent.ExecutionException;

public class Media3VideoPlayerActivity extends AppCompatActivity {
    private static final String TAG = "M3VideoPlayerActivity";
    private PlayerView playerView;
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setTheme(R.style.Theme_AntennaPod_Dark);
        setContentView(R.layout.media3_video_player_activity);
        playerView = findViewById(R.id.player_view);

        setupFullScreenMode();
        setupPictureInPicture();
    }

    private void setupFullScreenMode() {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void setupPictureInPicture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();

        if (mediaController != null) {
            int videoWidth = mediaController.getVideoSize().width;
            int videoHeight = mediaController.getVideoSize().height;
            if (videoWidth > 0 && videoHeight > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    android.util.Rational aspectRatio = new android.util.Rational(videoWidth, videoHeight);
                    builder.setAspectRatio(aspectRatio);
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true);
            if (playerView != null) {
                builder.setSourceRectHint(playerView.getClipBounds());
            }
        }

        setPictureInPictureParams(builder.build());
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && PictureInPictureUtil.supportsPictureInPicture(this)
                && !PictureInPictureUtil.isInPictureInPictureMode(this)) {
            enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
                                              @NonNull android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        playerView.setUseController(!isInPictureInPictureMode);
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
                playerView.setPlayer(mediaController);
                setupPictureInPicture();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error getting media controller", e);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void onStop() {
        playerView.setPlayer(null);
        if (mediaController != null) {
            mediaController.release();
            mediaController = null;
        }
        MediaController.releaseFuture(controllerFuture);
        super.onStop();
    }
}
