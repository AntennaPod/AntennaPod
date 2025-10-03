package de.danoeh.antennapod;

import androidx.annotation.NonNull;

import de.danoeh.antennapod.core.CrashReportWriter;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    public UncaughtExceptionHandler() {
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        CrashReportWriter.write(throwable);
        defaultUncaughtExceptionHandler.uncaughtException(thread, throwable);
    }
}
