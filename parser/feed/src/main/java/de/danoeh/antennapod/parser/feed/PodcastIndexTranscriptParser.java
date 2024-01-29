package de.danoeh.antennapod.parser.feed;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.internal.StringUtil;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
            String speaker = "";
            String body = "";
            String line;
            String segmentBody = "";
            long startTimecode = 0L;
            long spanStartTimecode = 0L;
            long endTimecode = 0L;
            long span = 1000L;
            long duration = 0L;

            while (true) {
                try {
                    line = iter.next();

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
                        if (spanStartTimecode == 0) {
                            spanStartTimecode = startTimecode;
                        }
                        duration += endTimecode - startTimecode;
                        do {
                            line = iter.next();
                            if (StringUtil.isBlank(line)) {
                                break;
                            }
                            body = body.concat(" " + line.strip());
                        } while (iter.hasNext());
                    }

                    if (body.contains(":")) {
                        String [] parts = body.split(":");
                        speaker = parts[0];
                        body = parts[1].strip();
                    }
                    if (!StringUtil.isBlank(body)) {
                        segmentBody += " " + body;
                        if (duration >= span) {
                            Log.d(TAG, "SRT : " + Long.toString(spanStartTimecode) + " " + segmentBody);
                            transcript.addSegment(new TranscriptSegment(spanStartTimecode,
                                    endTimecode,
                                    segmentBody,
                                    speaker));
                            startTimecode = endTimecode;
                            duration = 0L;
                            spanStartTimecode = 0L;
                            segmentBody = "";
                        }
                        body = "";
                    }
                } catch (NoSuchElementException e) {
                    if (! StringUtil.isBlank(segmentBody)) {
                        Log.d(TAG, "SRT : " + Long.toString(spanStartTimecode) + " " + segmentBody);

                        transcript.addSegment(new TranscriptSegment(spanStartTimecode,
                                endTimecode,
                                segmentBody,
                                speaker));
                    }

                    return transcript;
                }
            }
        }

        static long parseTimecode(String timecode) {
            String[] parts = timecode.split(":");
            try {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2].substring(0, 2));
                int milliseconds = Integer.parseInt(parts[2].substring(3));
                return (hours * 60 * 60 * 1000) + (minutes * 60 * 1000) + (seconds * 1000) + milliseconds;
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return -1;
            }
        }
    }


    public static class PodcastIndexTranscriptJsonParser {
        static String TAG = "PodcastIndexTranscriptJsonParser";

        public static Transcript parse(String jsonStr) {
            try {
                Transcript transcript = new Transcript();
                long startTime = 0L;
                long endTime = 0L;
                String speaker = "";
                long span = 1000L;
                long spanStartTime = 0L;
                long duration = 0L;
                String segmentBody = "";
                long segmentStartTime = -1L;
                JSONObject obj = new JSONObject(jsonStr);
                JSONArray objSegments = obj.getJSONArray("segments");

                for (int i = 0; i < objSegments.length(); i++) {
                    JSONObject jsonObject = objSegments.getJSONObject(i);
                    startTime = Double.valueOf(jsonObject.optDouble("startTime", 0) * 1000L).longValue();
                    if (segmentStartTime == -1L) {
                        segmentStartTime = startTime;
                    }
                    endTime = Double.valueOf(jsonObject.optDouble("endTime", endTime) * 1000L).longValue();
                    duration += endTime - startTime;
                    if (spanStartTime == 0L) {
                        spanStartTime = startTime;
                    }

                    speaker = jsonObject.optString("speaker");

                    String body = jsonObject.optString("body");
                    segmentBody += body + " ";

                    if (duration >= span) {
                        transcript.addSegment(new TranscriptSegment(segmentStartTime, endTime, segmentBody, speaker));
                        Log.d(TAG, "JSON " + segmentBody);
                        duration = 0L;
                        segmentBody = "";
                        segmentStartTime = -1L;
                    }
                }

                if (! StringUtil.isBlank(segmentBody)) {
                    transcript.addSegment(new TranscriptSegment(segmentStartTime, endTime, segmentBody, speaker));
                    Log.d(TAG, "JSON [last]" + segmentBody);
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
        // TT TODO - There should be a more Locale friendly way to format
        return String.format(Locale.US, "%d:%02d:%02d", duration / 3600, (duration % 3600) / 60, (duration % 60));
    }
}