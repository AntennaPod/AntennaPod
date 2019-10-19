package de.danoeh.antennapod.core.service.playback;

import android.os.Handler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class RepeatTapActionHandlerTest {

    private static final int KEY_CODE = 0;

    @Mock
    private Handler scheduler;

    @Mock
    private Runnable normalAction;

    @Mock
    private Runnable doubleTapAction;

    @Mock
    private Runnable tripleTapAction;

    private final List<Runnable> actions = new ArrayList<>();

    @Captor
    private ArgumentCaptor<Runnable> tasks;

    private RepeatTapActionHandler handler;

    @Before
    public void setUp() {
        handler = new RepeatTapActionHandler(scheduler);

        actions.add(normalAction);
        actions.add(doubleTapAction);
        actions.add(tripleTapAction);
    }

    @Test
    public void testEventSingleKeySinglePress() {
        handler.event(KEY_CODE, actions);

        verifySchedulerInteractions(scheduler, tasks);
        runAll(tasks);

        verify(normalAction).run();
        verify(doubleTapAction, never()).run();
        verify(tripleTapAction, never()).run();
    }

    @Test
    public void testEventSingleKeyDoublePressAfterDelay() {
        handler.event(KEY_CODE, actions);

        verifySchedulerInteractions(scheduler, tasks);
        runAll(tasks);
        reset(scheduler);

        handler.event(KEY_CODE, actions);

        ArgumentCaptor<Runnable> nextTasks = ArgumentCaptor.forClass(Runnable.class);
        verifySchedulerInteractions(scheduler, nextTasks);
        runAll(nextTasks);

        verify(normalAction, times(2)).run();
        verify(doubleTapAction, never()).run();
        verify(tripleTapAction, never()).run();
    }

    @Test
    public void testEventSingleKeyDoublePress() {
        handler.event(KEY_CODE, actions);
        handler.event(KEY_CODE, actions);

        verifySchedulerInteractions(scheduler, tasks, atMost(2));
        runAll(tasks);

        verify(normalAction, never()).run();
        verify(doubleTapAction).run();
        verify(tripleTapAction, never()).run();
    }

    @Test
    public void testEventSingleKeyTriplePress() {
        handler.event(KEY_CODE, actions);
        handler.event(KEY_CODE, actions);
        handler.event(KEY_CODE, actions);

        verifySchedulerInteractions(scheduler, tasks, atMost(3));
        runAll(tasks);

        verify(normalAction, never()).run();
        verify(doubleTapAction, never()).run();
        verify(tripleTapAction).run();
    }

    @Test
    public void testEventSingleKeyQuadruplePress() {
        handler.event(KEY_CODE, actions);
        handler.event(KEY_CODE, actions);
        handler.event(KEY_CODE, actions);
        handler.event(KEY_CODE, actions);

        verifySchedulerInteractions(scheduler, tasks, atMost(3));
        runAll(tasks);

        verify(normalAction, never()).run();
        verify(doubleTapAction, never()).run();
        verify(tripleTapAction).run();
    }

    @Test
    public void testEventDifferentKeysSinglePress() {
        handler.event(KEY_CODE, actions);

        Runnable otherAction = mock(Runnable.class);
        List<Runnable> otherActions = new ArrayList<>();
        otherActions.add(otherAction);
        handler.event(KEY_CODE + 1, otherActions);

        verifySchedulerInteractions(scheduler, tasks, atMost(2));
        runAll(tasks);

        verify(normalAction).run();
        verify(otherAction).run();
        verify(doubleTapAction, never()).run();
        verify(tripleTapAction, never()).run();
    }

    @Test
    public void testEventDifferentKeysDoublePressInterleaved() {
        Runnable otherAction = mock(Runnable.class);
        List<Runnable> otherActions = new ArrayList<>();
        otherActions.add(otherAction);

        handler.event(KEY_CODE, actions);
        handler.event(KEY_CODE + 1, otherActions);
        handler.event(KEY_CODE, actions);

        verifySchedulerInteractions(scheduler, tasks, atMost(3));
        runAll(tasks);

        verify(normalAction, times(2)).run();
        verify(otherAction).run();
        verify(doubleTapAction, never()).run();
        verify(tripleTapAction, never()).run();
    }

    private static void verifySchedulerInteractions(Handler scheduler,
                                                    ArgumentCaptor<Runnable> tasks) {
        verifySchedulerInteractions(scheduler, tasks, times(1));
    }

    private static void verifySchedulerInteractions(Handler scheduler,
                                                    ArgumentCaptor<Runnable> tasks,
                                                    VerificationMode mode) {
        verify(scheduler, mode).postDelayed(
                tasks.capture(),
                eq(RepeatTapActionHandler.DELAY_MS));
    }

    private static void runAll(ArgumentCaptor<Runnable> tasks) {
        for(Runnable r : tasks.getAllValues()) {
            r.run();
        }
    }
}