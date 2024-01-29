package de.danoeh.antennapod.core.util;

import android.os.NetworkOnMainThreadException;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import de.danoeh.antennapod.parser.transcript.PodcastIndexTranscriptParser;

import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.Transcript;
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
            new PodcastIndexTranscriptParser.parse(media.getItem().getPodcastIndexTranscriptText(), type);
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
            String t = PodcastIndexTranscriptUtils.loadTranscriptFromUrl(type,
                    url,
                    false);
            if (t != null) {
                // TODO
                media.getItem().setPodcastIndexTranscriptText(t);
                Transcript transcript = PodcastIndexTranscriptParser.parse(t, type);
                media.getItem().setTranscript(transcript);
                return transcript;
            }
        }
        return null;
    }
}
