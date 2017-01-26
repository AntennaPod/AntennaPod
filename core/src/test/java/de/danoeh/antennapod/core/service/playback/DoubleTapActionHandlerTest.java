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

import rx.functions.Action0;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DoubleTapActionHandlerTest {

    private static final String EVENT_ID = "0";

    @Mock
    private Handler scheduler;

    @Mock
    private Action0 normalAction;

    @Mock
    private Action0 doubleTapAction;

    @Captor
    private ArgumentCaptor<Runnable> tasks;

    private DoubleTapActionHandler handler;

    @Before
    public void setUp() {
        handler = new DoubleTapActionHandler(scheduler);
    }

    @Test
    public void testEventSingleKeySinglePress() {
        handler.event(EVENT_ID, normalAction, doubleTapAction);

        verifySchedulerInteractions(scheduler, tasks);
        runAll(tasks);

        verify(normalAction).call();
        verify(doubleTapAction, never()).call();
    }

    @Test
    public void testEventSingleKeyDoublePressAfterDelay() {
        handler.event(EVENT_ID, normalAction, doubleTapAction);

        verifySchedulerInteractions(scheduler, tasks);
        runAll(tasks);
        reset(scheduler);

        handler.event(EVENT_ID, normalAction, doubleTapAction);

        ArgumentCaptor<Runnable> nextTasks = ArgumentCaptor.forClass(Runnable.class);
        verifySchedulerInteractions(scheduler, nextTasks);
        runAll(nextTasks);

        verify(normalAction, times(2)).call();
        verify(doubleTapAction, never()).call();
    }

    @Test
    public void testEventSingleKeyDoublePress() {
        handler.event(EVENT_ID, normalAction, doubleTapAction);
        handler.event(EVENT_ID, normalAction, doubleTapAction);

        verifySchedulerInteractions(scheduler, tasks);
        runAll(tasks);

        verify(normalAction, never()).call();
        verify(doubleTapAction).call();
    }

    @Test
    public void testEventSingleKeyTriplePress() {
        Action0 otherAction = mock(Action0.class);
        Action0 otherDoubleTapAction = mock(Action0.class);
        handler.event(EVENT_ID, normalAction, doubleTapAction);
        handler.event(EVENT_ID, normalAction, doubleTapAction);
        handler.event(EVENT_ID, otherAction, otherDoubleTapAction);

        verifySchedulerInteractions(scheduler, tasks, times(2));
        runAll(tasks);

        verify(normalAction, never()).call();
        verify(doubleTapAction).call();
        verify(otherAction).call();
        verify(otherDoubleTapAction, never()).call();
    }

    @Test
    public void testEventDifferentKeysSinglePress() {
        Action0 otherAction = mock(Action0.class);
        handler.event(EVENT_ID, normalAction, doubleTapAction);
        handler.event(EVENT_ID + 1, otherAction, doubleTapAction);

        verifySchedulerInteractions(scheduler, tasks, times(2));
        runAll(tasks);

        verify(normalAction).call();
        verify(otherAction).call();
        verify(doubleTapAction, never()).call();
    }

    @Test
    public void testEventDifferentKeysDoublePressInterleaved() {
        Action0 otherAction = mock(Action0.class);
        handler.event(EVENT_ID, normalAction, doubleTapAction);
        handler.event(EVENT_ID + 1, otherAction, doubleTapAction);
        handler.event(EVENT_ID, normalAction, doubleTapAction);

        verifySchedulerInteractions(scheduler, tasks, times(3));
        runAll(tasks);

        verify(normalAction, times(2)).call();
        verify(otherAction).call();
        verify(doubleTapAction, never()).call();
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
                eq(DoubleTapActionHandler.DELAY_MS));
    }

    private static void runAll(ArgumentCaptor<Runnable> tasks) {
        for(Runnable r : tasks.getAllValues()) {
            r.run();
        }
    }
}