package de.danoeh.antennapod.system.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Utilities for accessing the package information.
 */
public final class PackageUtils {

    public static String getApplicationVersion(@NonNull Context context) {
        return Objects.requireNonNull(getPackageInfo(context),
                "Call to getPackageInfo() returned Null.").versionName;
    }

    @Nullable
    public static PackageInfo getPackageInfo(@NonNull Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private PackageUtils() {
        /* Utility classes should not instantiated */
    }
}
