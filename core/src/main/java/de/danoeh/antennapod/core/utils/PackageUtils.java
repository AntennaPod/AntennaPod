package de.danoeh.antennapod.core.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public class PackageUtils {

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
}
