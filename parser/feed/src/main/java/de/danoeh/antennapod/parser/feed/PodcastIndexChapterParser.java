package de.danoeh.antennapod.parser.feed;

import de.danoeh.antennapod.model.feed.Chapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PodcastIndexChapterParser {
    public static List<Chapter> parse(String jsonStr) {
        try {
            List<Chapter> chapters = new ArrayList<>();
            JSONObject obj = new JSONObject(jsonStr);
            JSONArray objChapters = obj.getJSONArray("chapters");
            for (int i = 0; i < objChapters.length(); i++) {
                JSONObject jsonObject = objChapters.getJSONObject(i);
                int startTime = jsonObject.optInt("startTime", 0);
                String title = jsonObject.optString("title");
                String link = jsonObject.optString("url");
                String img = jsonObject.optString("img");
                chapters.add(new Chapter(startTime * 1000L, title, link, img));
            }
            return chapters;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
