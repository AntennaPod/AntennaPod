package de.danoeh.antennapod.ui.chapters;

import android.util.Log;

import de.danoeh.antennapod.parser.transcript.TranscriptParser;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    public static String loadTranscriptFromUrl(String type, String url, boolean forceRefresh) {
        StringBuilder str = new StringBuilder();
        Response response = null;

        try {
            Log.d(TAG, "Downloading transcript URL " + url.toString());
            Request request = new Request.Builder().url(url).cacheControl(CacheControl.FORCE_NETWORK).build();
            response = AntennapodHttpClient.getHttpClient().newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                str.append(response.body().string());
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

        // TT do we have to set the text?
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
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Transcript> future = executor.submit(() -> {
            String t = PodcastIndexTranscriptUtils.loadTranscriptFromUrl(transcriptType, transcriptUrl, false);
            if (StringUtils.isNotEmpty(t)) {
                return TranscriptParser.parse(t, transcriptType);
            }
            return null;
        });

        try {
            Transcript result = future.get();  // This will block until the Callable completes
            executor.shutdown();  // Remember to shutdown the executor
            return result;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
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
