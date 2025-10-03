package de.danoeh.antennapod.core;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import de.danoeh.antennapod.core.utils.PackageUtils;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public class CrashReportWriter {
    private static final String TAG = "CrashReportWriter";

    public static File getFile() {
        return new File(UserPreferences.getDataFolder(null), "crash-report.log");
    }

    public static void write(Throwable exception) {
        File path = getFile();
        PrintWriter out = null;
        try {
            out = new PrintWriter(path, "UTF-8");
            exception.printStackTrace(out);

        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public static Date getTimestamp() {
        Date timestamp = null;

        try {
            File file = getFile();

            if (file.exists()) {
                timestamp = new Date(file.lastModified());
            }

        } catch (SecurityException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return timestamp;
    }

    public static String read() {
        String content = "";

        try {
            File file = getFile();

            if (file.exists()) {
                content = IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);
            }

        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return content;
    }

    public static String getSystemInfo(Context context) {
        return "## Environment"
                + "\nAndroid version: " + Build.VERSION.RELEASE
                + "\nOS version: " + System.getProperty("os.version")
                + "\nAntennaPod version: " + PackageUtils.getApplicationVersion(context)
                + "\nModel: " + Build.MODEL
                + "\nDevice: " + Build.DEVICE
                + "\nProduct: " + Build.PRODUCT;
    }
}