package de.danoeh.antennapod.core.service.playback;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import rx.functions.Action0;

/**
 * Performs actions based on whether or not a key was single or double-tapped.
 */
public class DoubleTapActionHandler {

    /**
     * The maximum amount of time between two presses of the same key for it to be considered a
     * double-tap.
     */
    public static final long DELAY_MS = 400;

    /**
     * Logging tag
     */
    private static final String TAG = "DoubleTapActionHandler";

    /**
     * Used to delay key actions until we know whether or not a key event was a part of a
     * double-tap.
     */
    private final Handler scheduler;

    /**
     * The last key event that was sent to this handler.
     */
    private KeyRequest lastRequest = null;

    public DoubleTapActionHandler() {
        this(new Handler(Looper.getMainLooper()));
    }

    public DoubleTapActionHandler(Handler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Determine if the given key press was hit twice in rapid succession. If it was, then
     * {@code doubleTapAction} will be called and the normal action will be canceled. If not,
     * then {@code normalAction} will be executed as long as a second request does not come in
     * afterwards.
     * <p>
     * Note: This method must be called on the main thread.
     *
     * @param event The type of event being executed.
     * @param normalAction The action to execute if the key is <em>not</em> double tapped.
     * @param doubleTapAction The action to execute if the key was double tapped.
     */
    public void event(String event, Action0 normalAction, Action0 doubleTapAction) {
        Log.d(TAG, "event(" + event + ")");

        boolean doubleTapped = lastRequest != null &&
                               lastRequest.isAlive() &&
                               lastRequest.getEvent().equals(event);
        if (doubleTapped) {
            Log.d(TAG, "Cancelling existing request, key was double-tapped");
            lastRequest.cancel();
            lastRequest = null;
            doubleTapAction.call();
        } else {
            Log.d(TAG, "Submitting new request, to be executed in " + DELAY_MS + "ms");
            lastRequest = new KeyRequest(event, normalAction);
            scheduler.postDelayed(lastRequest, DELAY_MS);
        }
    }

    /**
     * A cancelable request to execute an action as a result of a key event.
     */
    private static final class KeyRequest implements Runnable {

        private final String event;

        private final Action0 action;

        private volatile boolean finished = false;

        /**
         * Creates a new request for the given key that will execute the given action when
         * it is run.
         *
         * @param event The key that initiated the request.
         * @param action The action to execute if the request is not canceled.
         */
        public KeyRequest(String event, Action0 action) {
            this.event = event;
            this.action = action;
        }

        @Override
        public void run() {
            if(!finished) {
                action.call();
                finished = true;
            }
        }

        /**
         * Cancels the request if it has not already been finished.
         */
        public void cancel() {
            finished = true;
        }

        /**
         * Whether or not the request is still waiting to be fulfilled.
         * @return false if the request has been canceled or has already finished, true otherwise.
         */
        public boolean isAlive() {
            return !finished;
        }

        /**
         * Gets the key code that initiated this request.
         * @return the key code.
         */
        public String getEvent() {
            return event;
        }
    }
}
