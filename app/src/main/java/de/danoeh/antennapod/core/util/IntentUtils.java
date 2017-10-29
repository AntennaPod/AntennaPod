package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.List;

public class IntentUtils {

    /*
     *  Checks if there is at least one exported activity that can be performed for the intent
     */
    public static boolean isCallable(final Context context, final Intent intent) {
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        for(ResolveInfo info : list) {
            if(info.activityInfo.exported) {
                return true;
            }
        }
        return false;
    }

}
