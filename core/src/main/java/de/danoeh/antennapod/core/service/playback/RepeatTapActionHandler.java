package de.danoeh.antennapod.core.service.playback;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;

/**
 * Performs actions based on whether or not a key was entered multiple times in
 * a row.
 */
public class RepeatTapActionHandler {

    /**
     * The maximum amount of time between two presses of the same key for it to
     * be considered a repeated tap.
     */
    public static final long DELAY_MS = 400;

    /**
     * Logging tag
     */
    private static final String TAG = "RepeatTapActionHandler";

    /**
     * Used to delay key actions until we know whether or not a key was a part
     * of a double-tap.
     */
    private final Handler scheduler;

    /**
     * The last key request that was sent to this handler.
     */
    private KeyRequest lastRequest = null;

    public RepeatTapActionHandler() {
        this(new Handler(Looper.getMainLooper()));
    }

    public RepeatTapActionHandler(Handler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Determine if the given key press was hit multiple times in rapid succession.
     * <p>
     * If the key has been pressed only once, the first element in
     * {@code actions} will be run, if twice then the second element, etc.
     * <p>
     * Note: This method must be called on the main thread.
     *
     * @param keyCode The type of key that triggered the event.
     * @param actions The actions to execute for repeated taps.
     */
    public void event(int keyCode, List<Runnable> actions) {
        Log.v(TAG, "event(" + keyCode + ", " + actions.size() + ")");

        if (actions.isEmpty()) {
            Log.d(TAG, "No actions specified, not running any actions");
            return;
        }

        if (actions.size() == 1) {
            Log.d(TAG, "No multiple-tap actions specified. Running normal action immediately");
            actions.get(0).run();
            lastRequest = null;
            return;
        }

        boolean repeated = lastRequest != null &&
                           lastRequest.isAlive() &&
                           lastRequest.getKeyCode() == keyCode;
        if (repeated) {
            int count = lastRequest.getRepeatCount() + 1;
            if (count < actions.size()) {
                Log.d(TAG, "Cancelling existing request, key was repeated. New request " +
                           "will be executed in " + DELAY_MS + "ms");
                lastRequest.cancel();

                KeyRequest newRequest = new KeyRequest(keyCode, actions.get(count), count);
                scheduler.postDelayed(newRequest, DELAY_MS);

                lastRequest = newRequest;
            } else {
                Log.d(TAG, "Key was tapped " + count + " times but actions are " +
                           "only configured for " + actions.size() + " tap(s).");
            }
        } else {
            Log.d(TAG, "Submitting new request, to be executed in " + DELAY_MS + "ms");
            lastRequest = new KeyRequest(keyCode, actions.get(0), 0);
            scheduler.postDelayed(lastRequest, DELAY_MS);
        }
    }

    /**
     * A cancelable request to execute an action as a result of a key event.
     */
    private static final class KeyRequest implements Runnable {

        private final int keyCode;
        private final Runnable action;
        private final int repeatCount;

        private volatile boolean finished = false;

        /**
         * Creates a new request for the given key that will execute the given
         * action when it is run.
         * @param keyCode The key that initiated the request.
         * @param action The action to execute if the request is not canceled.
         * @param repeatCount The number of times the key was hit.
         */
        public KeyRequest(int keyCode, Runnable action, int repeatCount) {
            this.keyCode = keyCode;
            this.action = action;
            this.repeatCount = repeatCount;
        }

        @Override
        public void run() {
            if (isAlive()) {
                Log.d(TAG, "Executing action for '" + keyCode + "'");
                action.run();
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
         * @return false if the request has been canceled or has already
         *         finished, true otherwise.
         */
        public boolean isAlive() {
            return !finished;
        }

        public int getKeyCode() {
            return keyCode;
        }

        public int getRepeatCount() {
            return repeatCount;
        }
    }
}
