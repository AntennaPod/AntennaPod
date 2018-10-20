package de.danoeh.antennapod.core.util.gui;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

public class PictureInPictureUtil {
    private PictureInPictureUtil() {
    }

    public static boolean supportsPictureInPicture(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PackageManager packageManager = activity.getPackageManager();
            return packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
        } else {
            return false;
        }
    }

    public static boolean isInPictureInPictureMode(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && supportsPictureInPicture(activity)) {
            return activity.isInPictureInPictureMode();
        } else {
            return false;
        }
    }
}
