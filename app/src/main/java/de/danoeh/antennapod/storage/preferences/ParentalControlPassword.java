package de.danoeh.antennapod.storage.preferences;

import android.content.Context;

public class ParentalControlPassword {
    private static final String PREFS_NAME = "ParentalControlPrefs";
    private static final String PREF_PASSWORD = "password";

    public static boolean isPasswordSet(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).contains(PREF_PASSWORD);
    }

    public static boolean verifyPassword(Context context, String password) {
        String stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREF_PASSWORD, null);
        return stored != null && stored.equals(password);
    }

    public static void setPassword(Context context, String password) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(PREF_PASSWORD, password).apply();
    }

    public static void clearPassword(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(PREF_PASSWORD).apply();
    }
}
