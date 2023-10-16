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
import org.jsoup.internal.StringUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import de.danoeh.antennapod.model.feed.Transcript;

public class PodcastIndexTranscriptParser {

    public static class PodcastIndexTranscriptSrtParser {

        public static List<Transcript> parse(String str) {
            List<Transcript> transcripts = new ArrayList<>();

            Transcript transcript;
            List<String> lines = Arrays.asList(str.split("\n"));
            Iterator<String> iter = lines.iterator();

            String line;
            while (true) {
                try {
                    line = iter.next();
                    String body = null;
                    long startTimecode = 0;
                    long endTimecode = 0;
                    if (line.isEmpty()) {
                        continue;
                    }

                    if (line.contains("-->")) {
                        String[] timecodes = line.split("-->");
                        if (timecodes.length < 2) {
                            continue;
                        }
                        startTimecode = parseTimecode(timecodes[0].trim());
                        endTimecode = parseTimecode(timecodes[1].trim());
                        line = iter.next();
                        body = line.strip();
                    }
                    if (startTimecode != 0 && endTimecode != 0 && ! StringUtil.isBlank(body)) {
                        transcript = new Transcript(startTimecode, endTimecode, body);
                        transcripts.add(transcript);
                    }
                } catch (NoSuchElementException e) {
                    return transcripts;
                }
            }
        }

        static long parseTimecode(String timecode) {
            String[] parts = timecode.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2].substring(0, 2));
            int milliseconds = Integer.parseInt(parts[2].substring(3));

            return (hours * 60 * 60 * 1000) + (minutes * 60 * 1000) + (seconds * 1000) + milliseconds;
        }
    }


    public static class PodcastIndexTranscriptJsonParser {
        public static List<Transcript> parse(String jsonStr) {
            try {
                List<Transcript> transcripts = new ArrayList<>();
                JSONObject obj = new JSONObject(jsonStr);
                JSONArray objSegments = obj.getJSONArray("segments");
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

    public static List<Transcript> parse(String str, String type) {

        if ("application/json".equals(type)) {
            return PodcastIndexTranscriptJsonParser.parse(str);
        }

        if ("application/srt".equals(type)) {
            return PodcastIndexTranscriptSrtParser.parse(str);
        }
        return null;
    }
}