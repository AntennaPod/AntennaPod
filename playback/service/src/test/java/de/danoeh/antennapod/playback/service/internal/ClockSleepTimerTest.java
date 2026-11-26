package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.content.ContextWrapper;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;
import org.greenrobot.eventbus.EventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class ClockSleepTimerTest {
    private ClockSleepTimer timer;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        SleepTimerPreferences.init(context);
        SleepTimerPreferences.setShakeToReset(true);
        Context contextWithoutSensors = new ContextWrapper(context) {
            @Override
            public Object getSystemService(String name) {
                return Context.SENSOR_SERVICE.equals(name) ? null : super.getSystemService(name);
            }
        };
        timer = new ClockSleepTimer(contextWithoutSensors);
    }

    @After
    public void tearDown() {
        if (EventBus.getDefault().isRegistered(timer)) {
            EventBus.getDefault().unregister(timer);
        }
    }

    @Test
    public void expiresWhenSensorsAreUnavailable() {
        timer.start(0);
        timer.playbackPositionUpdate(new PlaybackPositionEvent(0, 60000));

        assertFalse(timer.isActive());
        assertFalse(EventBus.getDefault().isRegistered(timer));
    }

    @Test
    public void vibratesAgainAfterReset() {
        SleepTimerPreferences.setVibrate(true);
        timer = new CountingClockSleepTimer(timer.getContext());
        timer.start(5000);
        timer.playbackPositionUpdate(new PlaybackPositionEvent(0, 60000));
        assertEquals(1, ((CountingClockSleepTimer) timer).vibrations);

        timer.reset();
        timer.playbackPositionUpdate(new PlaybackPositionEvent(0, 60000));
        assertEquals(2, ((CountingClockSleepTimer) timer).vibrations);
    }

    @Test
    public void vibratesAgainAfterExtending() {
        SleepTimerPreferences.setVibrate(true);
        timer = new CountingClockSleepTimer(timer.getContext());
        timer.start(5000);
        timer.playbackPositionUpdate(new PlaybackPositionEvent(0, 60000));
        assertEquals(1, ((CountingClockSleepTimer) timer).vibrations);

        timer.updateRemainingTime(5000);
        timer.playbackPositionUpdate(new PlaybackPositionEvent(0, 60000));
        assertEquals(2, ((CountingClockSleepTimer) timer).vibrations);
    }

    private static class CountingClockSleepTimer extends ClockSleepTimer {
        int vibrations = 0;

        CountingClockSleepTimer(Context context) {
            super(context);
        }

        @Override
        protected void vibrate() {
            vibrations++;
        }
    }
}
