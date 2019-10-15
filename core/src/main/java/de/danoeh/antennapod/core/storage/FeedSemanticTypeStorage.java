package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import de.danoeh.antennapod.core.feed.FeedPreferences.SemanticType;

/**
 * Persistent storage for FeedPreferences.semanticType
 *
 * TODO-1077: to be replaced by db-based persistence.
 *
 */
public class FeedSemanticTypeStorage {
    private static final String STORAGE_PREFS_NAME = "FeedSemanticTypeStorage";

    private static Context context;

    private FeedSemanticTypeStorage() { }

    static void init(Context context) {
        FeedSemanticTypeStorage.context = context.getApplicationContext();
    }

    // Public scope as it's needed from FeedPreferences#fromCursor()
    @NonNull
    public static SemanticType getSemanticType(long feedId) {
        int typeCode = getStorage().getInt(toKey(feedId), SemanticType.EPISODIC.code);
        return SemanticType.valueOf(typeCode);
    }

    static void setSemanticType(long feedId, @NonNull SemanticType semanticType) {
        getStorage().edit()
                .putInt(toKey(feedId), semanticType.code)
                .apply();
    }

    static void clearAll() {
        getStorage().edit()
                .clear()
                .commit();
    }

    @NonNull
    private static SharedPreferences getStorage() {
        return context.getSharedPreferences(STORAGE_PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    private static String toKey(long feedId) {
        return Long.toString(feedId, 10);
    }
}
