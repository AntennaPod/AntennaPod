package de.danoeh.antennapod.parser.feed;

/*
 JSON format
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

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.internal.StringUtil;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;

public class PodcastIndexTranscriptParser {

    public static class PodcastIndexTranscriptSrtParser {

        private static String TAG = "PodcastIndexTranscriptSrtParser";

        public static Transcript parse(String str) {
            Transcript transcript = new Transcript();

            TranscriptSegment transcriptSegment;
            List<String> lines = Arrays.asList(str.split("\n"));
            Iterator<String> iter = lines.iterator();

            // TT TODO Parse speaker
            String line;
            while (true) {
                try {
                    line = iter.next();
                    String body = null;
                    String speaker = "";
                    long startTimecode = 0;
                    long endTimecode = 0;
                    if (line.isEmpty()) {
                        continue;
                    }

                    if (line.contains("-->")) {
                        Log.d(TAG, line + "\n");
                        String[] timecodes = line.split("-->");
                        if (timecodes.length < 2) {
                            continue;
                        }
                        startTimecode = parseTimecode(timecodes[0].trim());
                        endTimecode = parseTimecode(timecodes[1].trim());
                        line = iter.next();
                        do {
                            body = line.strip();
                            line = iter.next();
                        } while (line.length() > 0);
                    }
                    if (!StringUtil.isBlank(body)) {
                        Log.d(TAG, Long.toString(startTimecode) + " " + body);
                        transcript.addSegment(new TranscriptSegment(startTimecode, endTimecode, body, speaker));
                    }
                } catch (NoSuchElementException e) {
                    return transcript;
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
        static String TAG = "PodcastIndexTranscriptJsonParser";
        static Transcript transcript = new Transcript();

        public static Transcript parse(String jsonStr) {
            try {
                JSONObject obj = new JSONObject(jsonStr);
                JSONArray objSegments = obj.getJSONArray("segments");
                String speaker = "";
                for (int i = 0; i < objSegments.length(); i++) {
                    JSONObject jsonObject = objSegments.getJSONObject(i);
                    long startTime = Double.valueOf(jsonObject.optDouble("startTime", 0) * 1000L).longValue();
                    long endTime = Double.valueOf(jsonObject.optDouble("endTime", startTime) * 1000L).longValue();
                    String body = jsonObject.optString("body");

                    Log.d(TAG, "JSON " + Long.toString(startTime) + " " + body);

                    transcript.addSegment(new TranscriptSegment(startTime, endTime, body, speaker));
                }
                return transcript;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static Transcript parse(String str, String type) {

        if ("application/json".equals(type)) {
            return PodcastIndexTranscriptJsonParser.parse(str);
        }

        if ("application/srt".equals(type)) {
            return PodcastIndexTranscriptSrtParser.parse(str);
        }
        return null;
    }
}