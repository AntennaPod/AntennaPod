package de.danoeh.antennapod.error;

import android.util.Log;
import de.danoeh.antennapod.BuildConfig;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;

public class RxJavaErrorHandlerSetup {
    private static final String TAG = "RxJavaErrorHandler";

    private RxJavaErrorHandlerSetup() {

    }

    public static void setupRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler(exception -> {
            if (exception instanceof UndeliverableException) {
                // Probably just disposed because the fragment was left
                Log.d(TAG, "Ignored exception: " + Log.getStackTraceString(exception));
                return;
            }

            // Usually, undeliverable exceptions are wrapped in an UndeliverableException.
            // If an undeliverable exception is a NPE (or some others), wrapping does not happen.
            // AntennaPod threads might throw NPEs after disposing because we set controllers to null.
            // Just swallow all exceptions here.
            Log.e(TAG, Log.getStackTraceString(exception));
            CrashReportWriter.write(exception);

            if (BuildConfig.DEBUG) {
                Thread.currentThread().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), exception);
            }
        });
    }
}
