package de.danoeh.antennapod.ui.chapters;

import android.util.Log;

import de.danoeh.antennapod.parser.transcript.TranscriptParser;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import de.danoeh.antennapod.model.feed.Transcript;
import okhttp3.CacheControl;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class PodcastIndexTranscriptUtils {

    private static final String TAG = "PodcastIndexTranscript";

    public static String loadTranscriptFromUrl(String url, boolean forceRefresh) {
        StringBuilder str = new StringBuilder();
        Response response = null;

        CacheControl cache = CacheControl.FORCE_NETWORK;

        try {
            Log.d(TAG, "Downloading transcript URL " + url.toString());
            Request request;
            if (forceRefresh) {
                request = new Request.Builder().url(url).cacheControl(cache).build();
            } else {
                request = new Request.Builder().url(url)
                        .cacheControl(cache)
                        .build();
            }
            response = AntennapodHttpClient.getHttpClient().newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                Log.d(TAG, "Done Downloading transcript URL " + url.toString());
                str.append(response.body().string());
            } else {
                Log.d(TAG, "Error Downloading transcript URL " + url.toString() + response.message());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return str.toString();
    }

    public static Transcript loadTranscript(FeedMedia media) {
        String transcriptType = media.getItem().getPodcastIndexTranscriptType();

        if (media.getItem().getTranscript() != null) {
            return media.getTranscript();
        }

        if (media.getTranscriptFileUrl() != null) {
            File transcriptFile = new File(media.getTranscriptFileUrl());
            try {
                if (transcriptFile != null && transcriptFile.exists()) {
                    String t = FileUtils.readFileToString(transcriptFile, (String) null);
                    if (StringUtils.isNotEmpty(t)) {
                        media.setTranscript(TranscriptParser.parse(t, transcriptType));
                        return media.getTranscript();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String transcriptUrl = media.getItem().getPodcastIndexTranscriptUrl();
        String t = PodcastIndexTranscriptUtils.loadTranscriptFromUrl(transcriptUrl, true);
        if (StringUtils.isNotEmpty(t)) {
            return TranscriptParser.parse(t, transcriptType);
        }
        return null;
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
