package de.danoeh.antennapod.core.util.exception;

import android.util.Log;
import de.danoeh.antennapod.core.gpoddernet.GpodnetServiceException;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;

import java.io.InterruptedIOException;

public class RxJavaErrorHandlerSetup {

    private RxJavaErrorHandlerSetup() {

    }

    public static void setupRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler(originalCause -> {
            Throwable e = originalCause;
            if (e instanceof UndeliverableException) {
                e = e.getCause();
            }
            if (e instanceof GpodnetServiceException) {
                e = e.getCause();
            }
            if (e instanceof InterruptedException || e instanceof InterruptedIOException) {
                // fine, some blocking code was interrupted by a dispose call
                Log.d("RxJavaErrorHandler", "Ignored exception: " + Log.getStackTraceString(originalCause));
                return;
            }
            Thread.currentThread().getUncaughtExceptionHandler()
                    .uncaughtException(Thread.currentThread(), originalCause);
        });
    }
}
