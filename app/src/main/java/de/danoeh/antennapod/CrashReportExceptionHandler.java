package de.danoeh.antennapod;

import androidx.annotation.NonNull;

import de.danoeh.antennapod.system.CrashReportWriter;

public class CrashReportExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    public CrashReportExceptionHandler() {
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        CrashReportWriter.write(throwable);
        defaultUncaughtExceptionHandler.uncaughtException(thread, throwable);
    }
}
