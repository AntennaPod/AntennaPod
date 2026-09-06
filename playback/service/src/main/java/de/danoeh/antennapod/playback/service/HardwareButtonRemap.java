package de.danoeh.antennapod.playback.service;

import android.view.KeyEvent;
import androidx.media3.common.Player;

public class HardwareButtonRemap {
    private HardwareButtonRemap() {
    }

    public static void apply(Player player, int action, Runnable onNext) {
        switch (action) {
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                onNext.run();
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                player.seekTo(0);
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                player.seekBack();
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            default:
                player.seekForward();
                break;
        }
    }
}
