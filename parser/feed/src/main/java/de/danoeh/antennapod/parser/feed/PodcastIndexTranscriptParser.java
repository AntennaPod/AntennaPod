package de.danoeh.antennapod.parser.feed;
/*
{
    "version": "1.0.0",
    "segments": [
        {
            "startTime": 0.8,
            "endTime": 1.2,
            "body": "And"
        },
        {
            "startTime": 1.2,
            "endTime": 1.4,
            "body": "the"
        },
 */

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.Transcript;


class PodcastIndexTranscriptJsonParser {
    public static List<Transcript> parse(String jsonStr) {
        try {
            List<Transcript> transcripts = new ArrayList<>();
            JSONObject obj = new JSONObject(jsonStr);
            JSONArray objSegments= obj.getJSONArray("segments");
            for (int i = 0; i < objSegments.length(); i++) {
                JSONObject jsonObject = objSegments.getJSONObject(i);
                int startTime = jsonObject.optInt("startTime", 0);
                int endTime = jsonObject.optInt("endTime", startTime);
                String body = jsonObject.optString("body");
                transcripts.add(new Transcript(startTime * 1000L, endTime * 1000L, body));
            }
            return transcripts;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
public class PodcastIndexTranscriptParser {
    public static List<Transcript> parse(String str, String type) {

        if ("application/json".equals(type)) {
           return PodcastIndexTranscriptJsonParser.parse(str);
        }
        return null;
    }
}

