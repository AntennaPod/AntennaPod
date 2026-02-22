package de.danoeh.antennapod.playback.cast;

import android.content.Context;
import androidx.media3.common.Player;

public class CastPlayerWrapper {
    public static Player wrap(Player player, Context context) {
        return player;
    }
}
