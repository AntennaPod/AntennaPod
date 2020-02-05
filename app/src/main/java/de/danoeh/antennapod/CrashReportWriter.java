package de.danoeh.antennapod;

import android.os.Build;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.danoeh.antennapod.core.preferences.UserPreferences;

public class CrashReportWriter implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashReportWriter";

    private final Thread.UncaughtExceptionHandler defaultHandler;

    public CrashReportWriter() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static File getFile() {
        return new File(UserPreferences.getDataFolder(null), "crash-report.log");
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        File path = getFile();
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(path));
            out.println("[ Crash info ]");
            out.println("Time: " + new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
            out.println("AntennaPod version: " + BuildConfig.VERSION_NAME);
            out.println();
            out.println("[ StackTrace ]");
            ex.printStackTrace(out);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            IOUtils.closeQuietly(out);
        }
        defaultHandler.uncaughtException(thread, ex);
    }

    public static String getSystemInfo() {
        return "[ Environment ]" +
                "\nAndroid version: " + Build.VERSION.RELEASE +
                "\nOS version: " + System.getProperty("os.version") +
                "\nAntennaPod version: " + BuildConfig.VERSION_NAME +
                "\nModel: " + Build.MODEL +
                "\nDevice: " + Build.DEVICE +
                "\nProduct: " + Build.PRODUCT;
    }
}
