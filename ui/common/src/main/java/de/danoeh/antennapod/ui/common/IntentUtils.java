package de.danoeh.antennapod.ui.common;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public abstract class IntentUtils {
    private static final String TAG = "IntentUtils";

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

    public static void sendLocalBroadcast(Context context, String action) {
        context.sendBroadcast(new Intent(action).setPackage(context.getPackageName()));
    }

    public static void openInBrowser(Context context, String url) {
        try {
            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(myIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.pref_no_browser_found, Toast.LENGTH_LONG).show();
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public static String getLocalizedWebsiteLink(Context context) {
        try (InputStream is = context.getAssets().open("website-languages.txt")) {
            String[] languages = IOUtils.toString(is, StandardCharsets.UTF_8.name()).split("\n");
            String deviceLanguage = Locale.getDefault().getLanguage();
            if (ArrayUtils.contains(languages, deviceLanguage) && !"en".equals(deviceLanguage)) {
                return "https://antennapod.org/" + deviceLanguage;
            } else {
                return "https://antennapod.org";
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
