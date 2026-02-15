package de.danoeh.antennapod.storage.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Manages the parental control password with secure hashing.
 */
public class ParentalControlPassword {
    private static final String TAG = "ParentalControlPassword";
    private static final String PREFS_NAME = "ParentalControlPrefs";
    private static final String PREF_PASSWORD_HASH = "password_hash";
    private static final String PREF_SALT = "password_salt";
    private static final int HASH_ITERATIONS = 10000;

    /**
     * Check if a password has been set.
     */
    public static boolean isPasswordSet(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.contains(PREF_PASSWORD_HASH);
    }

    /**
     * Verify if the provided password matches the stored hash.
     */
    public static boolean verifyPassword(Context context, String password) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String storedHash = prefs.getString(PREF_PASSWORD_HASH, null);
        String storedSalt = prefs.getString(PREF_SALT, null);

        if (storedHash == null || storedSalt == null) {
            return false;
        }

        try {
            byte[] salt = Base64.getDecoder().decode(storedSalt);
            String computedHash = hashPassword(password, salt);
            return storedHash.equals(computedHash);
        } catch (Exception e) {
            Log.e(TAG, "Error verifying password", e);
            return false;
        }
    }

    /**
     * Set a new password (hashed with a new salt).
     */
    public static void setPassword(Context context, String password) {
        try {
            byte[] salt = generateSalt();
            String hash = hashPassword(password, salt);
            String saltBase64 = Base64.getEncoder().encodeToString(salt);

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(PREF_PASSWORD_HASH, hash)
                    .putString(PREF_SALT, saltBase64)
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting password", e);
        }
    }

    /**
     * Clear the stored password.
     */
    public static void clearPassword(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(PREF_PASSWORD_HASH)
                .remove(PREF_SALT)
                .apply();
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    private static String hashPassword(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            digest.update(salt);

            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // Apply multiple iterations for better security
            for (int i = 0; i < HASH_ITERATIONS; i++) {
                digest.reset();
                hash = digest.digest(hash);
            }

            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
