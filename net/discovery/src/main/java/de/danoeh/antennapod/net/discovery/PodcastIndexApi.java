package de.danoeh.antennapod.net.discovery;

import de.danoeh.antennapod.net.common.UserAgentInterceptor;
import okhttp3.Request;

import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public abstract class PodcastIndexApi {
    public static Request.Builder buildAuthenticatedRequest(String url) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        Date now = new Date();
        calendar.setTime(now);
        long secondsSinceEpoch = calendar.getTimeInMillis() / 1000L;
        String apiHeaderTime = String.valueOf(secondsSinceEpoch);
        String data4Hash = BuildConfig.PODCASTINDEX_API_KEY + BuildConfig.PODCASTINDEX_API_SECRET + apiHeaderTime;
        String hashString = sha1(data4Hash);

        return new Request.Builder()
                .addHeader("X-Auth-Date", apiHeaderTime)
                .addHeader("X-Auth-Key", BuildConfig.PODCASTINDEX_API_KEY)
                .addHeader("Authorization", hashString)
                .addHeader("User-Agent", UserAgentInterceptor.USER_AGENT)
                .url(url);
    }

    private static String sha1(String clearString) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(clearString.getBytes("UTF-8"));
            return toHex(messageDigest.digest());
        } catch (Exception ignored) {
            ignored.printStackTrace();
            return null;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for (byte b : bytes) {
            buffer.append(String.format(Locale.getDefault(), "%02x", b));
        }
        return buffer.toString();
    }
}
