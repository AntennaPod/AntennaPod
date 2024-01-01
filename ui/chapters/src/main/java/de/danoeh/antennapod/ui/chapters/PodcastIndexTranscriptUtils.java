package de.danoeh.antennapod.ui.chapters;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;

public class PodcastIndexTranscriptUtils {

    private static final String TAG = "PodcastIndexTranscript";

    public static String loadTranscriptFromUrl(String type, String url, boolean forceRefresh) {
        StringBuilder str = new StringBuilder();
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
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return str.toString();
    }

    public static void storeTranscript(FeedMedia media, String transcript) {
        File transcriptFile = new File(media.getTranscriptFileUrl());
        FileOutputStream ostream = null;
        try {
            if (!transcriptFile.exists() && transcriptFile.createNewFile()) {
                ostream = new FileOutputStream(transcriptFile);
                ostream.write(transcript.getBytes(Charset.forName("UTF-8")));
                ostream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(ostream);
        }
    }
}
