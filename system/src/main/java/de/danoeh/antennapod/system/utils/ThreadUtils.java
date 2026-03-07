package de.danoeh.antennapod.system.utils;

import android.os.Build;
import android.os.Looper;
import de.danoeh.antennapod.system.BuildConfig;

public final class ThreadUtils {
    private ThreadUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Assert to notify developers that they are not supposed to call a function on the main thread.
     * If you get an exception in this method, you should move your calls to background threads.
     */
    public static void assertNotMainThread() {
        if (BuildConfig.DEBUG) {
            if (Looper.myLooper() == Looper.getMainLooper() && !isTest()) {
                throw new RuntimeException("I/O on main thread");
            }
        }
    }

    private static boolean isTest() {
        if ("robolectric".equals(Build.FINGERPRINT)) {
            return true;
        }
        try {
            Class.forName("org.junit.Test");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
