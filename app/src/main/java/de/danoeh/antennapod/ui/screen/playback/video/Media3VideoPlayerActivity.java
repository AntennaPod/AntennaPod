package de.danoeh.antennapod.ui.screen.playback.video;

import android.content.ComponentName;
import android.os.Bundle;
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
    private PlayerView playerView;
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AntennaPod_Dark);
        setContentView(R.layout.media3_video_player_activity);
        playerView = findViewById(R.id.player_view);
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
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
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
