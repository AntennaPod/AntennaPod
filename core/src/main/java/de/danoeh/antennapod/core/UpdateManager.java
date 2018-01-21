package de.danoeh.antennapod.core;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.antennapod.audio.MediaPlayer;

import java.io.File;
import java.util.List;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;

/*
 * This class's job is do perform maintenance tasks whenever the app has been updated
 */
class UpdateManager {

    private static final String TAG = UpdateManager.class.getSimpleName();

    private static final String PREF_NAME = "app_version";
    private static final String KEY_VERSION_CODE = "version_code";

    private static int currentVersionCode;

    private static Context context;
    private static SharedPreferences prefs;

    public static void init(Context context) {
        UpdateManager.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            currentVersionCode = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to obtain package info for package name: " + context.getPackageName(), e);
            currentVersionCode = 0;
            return;
        }
        final int oldVersionCode = getStoredVersionCode();
        Log.d(TAG, "old: " + oldVersionCode + ", current: " + currentVersionCode);
        if(oldVersionCode < currentVersionCode) {
            onUpgrade(oldVersionCode, currentVersionCode);
            setCurrentVersionCode();
        }
    }

    private static int getStoredVersionCode() {
        return prefs.getInt(KEY_VERSION_CODE, -1);
    }

    private static void setCurrentVersionCode() {
        prefs.edit().putInt(KEY_VERSION_CODE, currentVersionCode).apply();
    }

    private static void onUpgrade(final int oldVersionCode, final int newVersionCode) {
        if(oldVersionCode < 1030099) {
            // delete the now obsolete image cache
            // from now on, Glide will handle caching images
            new Thread() {
                public void run() {
                    List<Feed> feeds = DBReader.getFeedList();
                    for (Feed podcast : feeds) {
                        List<FeedItem> episodes = DBReader.getFeedItemList(podcast);
                        for (FeedItem episode : episodes) {
                            FeedImage image = episode.getImage();
                            if (image != null && image.isDownloaded() && image.getFile_url() != null) {
                                File imageFile = new File(image.getFile_url());
                                if (imageFile.exists()) {
                                    imageFile.delete();
                                }
                                image.setFile_url(null); // calls setDownloaded(false)
                                DBWriter.setFeedImage(image);
                            }
                        }
                    }
                }
            }.start();
        }
        if(oldVersionCode < 1050004) {
            if(MediaPlayer.isPrestoLibraryInstalled(context) && Build.VERSION.SDK_INT >= 16) {
                UserPreferences.enableSonic(true);
            }
        }
    }

}
