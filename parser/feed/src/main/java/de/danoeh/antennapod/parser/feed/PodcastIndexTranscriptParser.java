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

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Date;
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

            transcript.setRawString(str);
            TranscriptSegment transcriptSegment;
            List<String> lines = Arrays.asList(str.split("\n"));
            Iterator<String> iter = lines.iterator();
            String speaker = "";
            String body = "";

            // TT TODO Parse speaker
            String line;
            String segmentBody = "";
            while (true) {
                try {
                    line = iter.next();
                    long startTimecode = 0;
                    long endTimecode = 0;
                    long span = 1000L;
                    long duration = 0L;

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
                        duration += endTimecode - startTimecode;
                        line = iter.next();
                        do {
                            body = body.concat(" " + line.strip());
                            line = iter.next();
                        } while (!StringUtil.isBlank(line));
                    }

                    if (body.contains(":")) {
                        String [] parts = body.split(":");
                        speaker = parts[0];
                        body = parts[1].strip();
                    }
                    if (!StringUtil.isBlank(body)) {
                        Log.d(TAG, Long.toString(startTimecode) + " " + body);
                        segmentBody += " " + body;
                        if (duration >= span) {
                            transcript.addSegment(new TranscriptSegment(startTimecode,
                                    endTimecode,
                                    segmentBody,
                                    speaker));
                            duration = 0L;
                            segmentBody = "";
                        }
                        body = "";
                        // transcript.addSegment(new TranscriptSegment(startTimecode, endTimecode, body, speaker));
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

        public static Transcript parse(String jsonStr) {
            try {
                Transcript transcript = new Transcript();
                transcript.setRawString(jsonStr);

                JSONObject obj = new JSONObject(jsonStr);
                JSONArray objSegments = obj.getJSONArray("segments");
                long span = 1000L;
                long duration = 0L;
                String segmentBody = "";
                long segmentStartTime = -1;

                for (int i = 0; i < objSegments.length(); i++) {
                    JSONObject jsonObject = objSegments.getJSONObject(i);
                    long startTime = Double.valueOf(jsonObject.optDouble("startTime", 0) * 1000L).longValue();
                    if (segmentStartTime == -1) {
                       segmentStartTime = startTime;
                    }
                    long endTime = Double.valueOf(jsonObject.optDouble("endTime", startTime) * 1000L).longValue();
                    duration += endTime - startTime;

                    String speaker = jsonObject.optString("speaker");

                    String body = jsonObject.optString("body");
                    segmentBody += body + " ";

                    if (duration >= span) {
                        transcript.addSegment(new TranscriptSegment(segmentStartTime, endTime, segmentBody, speaker));
                        Log.d(TAG, "JSON " + segmentBody);
                        duration = 0L;
                        segmentBody = "";
                        segmentStartTime = -1;
                    }
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

        if ("application/srt".equals(type) || "application/srr".equals(type) || "application/x-subrip".equals(type)) {
            return PodcastIndexTranscriptSrtParser.parse(str);
        }
        return null;
    }

    public static String secondsToTime(long msecs) {
        int duration = Math.toIntExact(msecs / 1000L);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return String.format("%d:%02d:%02d", duration / 3600, (duration % 3600) / 60, (duration % 60));
    }
}