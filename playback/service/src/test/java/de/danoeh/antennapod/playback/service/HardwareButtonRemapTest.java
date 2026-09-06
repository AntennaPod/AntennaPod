package de.danoeh.antennapod.playback.service;

import android.view.KeyEvent;
import androidx.media3.common.Player;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class HardwareButtonRemapTest {
    private final Player player = mock(Player.class);
    private final Runnable onNext = mock(Runnable.class);

    @Test
    public void nextActionRunsOnNextCallback() {
        HardwareButtonRemap.apply(player, KeyEvent.KEYCODE_MEDIA_NEXT, onNext);
        verify(onNext).run();
        verify(player, never()).seekTo(anyLong());
        verify(player, never()).seekBack();
        verify(player, never()).seekForward();
    }

    @Test
    public void previousActionSeeksToStart() {
        HardwareButtonRemap.apply(player, KeyEvent.KEYCODE_MEDIA_PREVIOUS, onNext);
        verify(player).seekTo(0);
        verify(onNext, never()).run();
    }

    @Test
    public void rewindActionSeeksBack() {
        HardwareButtonRemap.apply(player, KeyEvent.KEYCODE_MEDIA_REWIND, onNext);
        verify(player).seekBack();
        verify(onNext, never()).run();
    }

    @Test
    public void fastForwardActionSeeksForward() {
        HardwareButtonRemap.apply(player, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, onNext);
        verify(player).seekForward();
        verify(onNext, never()).run();
    }

    @Test
    public void unknownActionDefaultsToFastForward() {
        HardwareButtonRemap.apply(player, KeyEvent.KEYCODE_UNKNOWN, onNext);
        verify(player).seekForward();
        verify(onNext, never()).run();
    }
}
