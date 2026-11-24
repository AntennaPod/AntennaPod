package de.danoeh.antennapod.playback.service;

import android.content.ComponentName;
import android.content.Context;
import androidx.core.util.Consumer;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.ExecutionException;

/**
 * Communicates with the playback service. GUI classes should use this class to
 * control playback instead of communicating with the PlaybackService directly.
 */
public abstract class PlaybackController {

    public static void bindToMedia3Service(Context context, Consumer<MediaController> consumer) {
        SessionToken sessionToken = new SessionToken(context,
                new ComponentName(context, Media3PlaybackService.class));
        ListenableFuture<MediaController> controllerFuture =
                new MediaController.Builder(context, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            MediaController controller = null;
            try {
                controller = controllerFuture.get();
                consumer.accept(controller);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (controller != null) {
                    controller.release();
                }
            }
        }, MoreExecutors.directExecutor());

    }
}
