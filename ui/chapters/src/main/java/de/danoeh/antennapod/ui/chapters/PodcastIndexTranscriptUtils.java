package de.danoeh.antennapod.ui.chapters;

import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.Charset;

import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.parser.transcript.TranscriptParser;
import okhttp3.CacheControl;
import okhttp3.Request;
import okhttp3.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class PodcastIndexTranscriptUtils {

    private static final String TAG = "PodcastIndexTranscript";


    public static String loadTranscriptFromUrl(String url, boolean forceRefresh) {
        if (forceRefresh) {
            return loadTranscriptFromUrl(url, CacheControl.FORCE_NETWORK);
        }
        String str = loadTranscriptFromUrl(url, CacheControl.FORCE_CACHE);
        if (str == null || str.length() <= 1) {
            // Some publishers use one dummy transcript before actual transcript are available
            return loadTranscriptFromUrl(url, CacheControl.FORCE_NETWORK);
        }
        return str;
    }

    private static String loadTranscriptFromUrl(String url, CacheControl cacheControl) {
        StringBuilder str = new StringBuilder();
        Response response = null;

        try {
            Log.d(TAG, "Downloading transcript URL " + url.toString());
            Request request = new Request.Builder().url(url).cacheControl(cacheControl).build();
            response = AntennapodHttpClient.getHttpClient().newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                Log.d(TAG, "Done Downloading transcript URL " + url.toString());
                str.append(response.body().string());
            } else {
                Log.d(TAG, "Error Downloading transcript URL " + url.toString() + response.message());
            }
        } catch (InterruptedIOException e) {
            Log.d(TAG, "InterruptedIOException while downloading transcript URL " + url.toString());
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return str.toString();
    }

    public static Transcript loadTranscript(FeedMedia media, Boolean forceRefresh) {
        String transcriptType = media.getItem().getPodcastIndexTranscriptType();

        if (!forceRefresh && media.getItem().getTranscript() != null) {
            return media.getTranscript();
        }

        if (!forceRefresh && media.getTranscriptFileUrl() != null) {
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
        String t = PodcastIndexTranscriptUtils.loadTranscriptFromUrl(transcriptUrl, forceRefresh);
        if (StringUtils.isNotEmpty(t)) {
            return TranscriptParser.parse(t, transcriptType);
        }
        return null;
    }

    public static void storeTranscript(FeedMedia media, String transcript) {
        File transcriptFile = new File(media.getTranscriptFileUrl());
        FileOutputStream ostream = null;
        try {
            if (transcriptFile.exists()) {
                Boolean status = transcriptFile.delete();
                if (! status) {
                    Log.e(TAG, "Failed to delete existing transcript file " + transcriptFile.getAbsolutePath());
                }
            }
            if (transcriptFile.createNewFile()) {
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
