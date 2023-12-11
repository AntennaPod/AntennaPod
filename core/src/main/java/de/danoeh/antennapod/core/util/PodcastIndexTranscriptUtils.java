package de.danoeh.antennapod.core.util;

import android.os.NetworkOnMainThreadException;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.parser.feed.PodcastIndexTranscriptParser;
import okhttp3.Request;
import okhttp3.Response;

public class PodcastIndexTranscriptUtils {

    private static final String TAG = "PodcastIndexTranscript";

    public static Transcript loadTranscriptFromUrl(String type, String url, boolean forceRefresh) {
        Transcript transcript;
        StringBuffer str = new StringBuffer();
        Thread downloadThread = new Thread() {
            @Override
            public void run() {
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
            }
        };
        downloadThread.start();
        try {
            downloadThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        transcript = PodcastIndexTranscriptParser.parse(str.toString(), type);
        return transcript;
    }

    public static Transcript loadTranscript(FeedMedia media) {
        String type = media.getItem().getPodcastIndexTranscriptType();
        String url = media.getItem().getPodcastIndexTranscriptUrl();
        if (url == null) {
            return null;
        }

        if (media.getItem().getTranscript() != null) {
            return media.getItem().getTranscript();
        }

        if (media.getItem().getPodcastIndexTranscriptText() != null) {
            return PodcastIndexTranscriptParser.parse(media.getItem().getPodcastIndexTranscriptText(), type);
        }

        if (media.getTranscriptFile_url() != null) {
            File dest = new File(media.getTranscriptFile_url());
            if (dest.exists()) {
                String transcriptStr = "";
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(media.getTranscriptFile_url()));
                    StringBuilder stringBuilder = new StringBuilder();
                    char[] buffer = new char[128];
                    while (reader.read(buffer) != -1) {
                        stringBuilder.append(new String(buffer));
                        buffer = new char[128];
                    }
                    reader.close();
                    transcriptStr = stringBuilder.toString();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                media.getItem().setPodcastIndexTranscriptText(transcriptStr);
                Transcript t = PodcastIndexTranscriptParser.parse(
                        media.getItem().getPodcastIndexTranscriptText(),
                        type);
                media.getItem().setTranscript(t);
                return t;
            }
        }


        if (url != null) {
            Transcript t = PodcastIndexTranscriptUtils.loadTranscriptFromUrl(type,
                    url,
                    false);
            if (t != null) {
                media.getItem().setPodcastIndexTranscriptText(t.toString());
                media.getItem().setTranscript(t);
                return t;
            }
        }
        return null;
    }
}
