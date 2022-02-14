package de.danoeh.antennapod.model.feed;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PodcastIndexChapter extends Chapter {
    public static final int PODCASTINDEX_CHAPTER = 4;
    public static String TAG = "PodcastIndexChapter";

    public PodcastIndexChapter(int startTime,
                               String title,
                               String link,
                               String imageUrl) {
        super(startTime * 1000, title, link, imageUrl);
    }

    @Override
    public int getChapterType() {
        return PODCASTINDEX_CHAPTER;
    }

    @Override
    public String toString() {
        return "PodcastIndexChapter "
                + getTitle()
                + " " + getStart()
                + " " + getLink()
                + " " + getImageUrl();
    }

    public static List<Chapter> parseChapters(String jsonStr) {
        String body = jsonStr;
        List<Chapter> chapters = null;
        try {
            JSONObject obj = new JSONObject(body);
            JSONArray objChapters = obj.getJSONArray("chapters");
            for (int i = 0; i < objChapters.length(); i++) {
                String title;
                String link;
                String img;
                int startTime = 0;
                JSONObject jsonObject = objChapters.getJSONObject(i);
                startTime = jsonObject.optInt("startTime", 0);

                title = jsonObject.optString("title", null);

                link = jsonObject.optString("url", null);

                img = jsonObject.optString("url", null);

                PodcastIndexChapter chapter = new PodcastIndexChapter(
                        startTime,
                        title,
                        link,
                        img);
                if (chapters == null) {
                    chapters = new ArrayList<>();
                }
                chapters.add(chapter);
                return chapters;
            }
        } catch (JSONException e) {
            Log.d(TAG, "Error loading Chapter" + e.toString());
        } finally {
            return null; 
        }
    }
}
