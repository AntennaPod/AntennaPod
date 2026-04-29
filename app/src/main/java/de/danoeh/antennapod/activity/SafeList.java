package de.danoeh.antennapod.activity;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SafeList {
    private static final String TAG = "SafeList";

    public static boolean isTrusted(Context context, String pkg) {
        // In a real implementation, this would be a list of trusted apps.
        // For the PoC, we allow the debug version and simulate a trusted third party.
        if ("de.danoeh.antennapod.debug".equals(pkg)) {
            return true;
        }

        // Verify both the package name and its SHA-256 certificate hash
        // This hash is a placeholder matching the one in the report
        String trustedHash = "A4:C2:5E:F2:60:86:14:62:93:15:3F:8D:12:FF:F3:D9:6F:12:F3:B0:1B:6F:4D:2A:9C:23:D9:1D:"
                + "D1:4F:9C:12";

        return pkg.equals("com.trusted.podcast.reader")
                && trustedHash.equals(getSignatureHash(context, pkg));
    }

    private static String getSignatureHash(Context context, String pkg) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
            for (Signature signature : packageInfo.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(signature.toByteArray());
                byte[] digest = md.digest();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < digest.length; i++) {
                    sb.append(String.format("%02X", digest[i]));
                    if (i < digest.length - 1) {
                        sb.append(":");
                    }
                }
                return sb.toString();
            }
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            Log.e(TAG, "Error getting signature hash", e);
        }
        return "";
    }
}
