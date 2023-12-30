package de.danoeh.antennapod.core.util;

import android.os.NetworkOnMainThreadException;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.model.feed.FeedMedia;
import okhttp3.Request;
import okhttp3.Response;

public class PodcastIndexTranscriptUtils {

    private static final String TAG = "PodcastIndexTranscript";

    public static String loadTranscriptFromUrl(String type, String url, boolean forceRefresh) {
        StringBuffer str = new StringBuffer();
        Response response = null;
        try {
            Log.d(TAG, "Downloading transcript URL " + url.toString());
            Request request = new Request.Builder().url(url).build();
            response = AntennapodHttpClient.getHttpClient().newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                str.append(response.body().string());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NetworkOnMainThreadException e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return str.toString();
    }

    public static void storeTranscript(FeedMedia media, String transcript) {
        File transcriptFile = new File(media.getTranscriptFile_url());
        try {
            if (!transcriptFile.exists()) {
                transcriptFile.createNewFile();
                FileOutputStream ostream = new FileOutputStream(transcriptFile);
                ostream.write(transcript.getBytes());
                ostream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
