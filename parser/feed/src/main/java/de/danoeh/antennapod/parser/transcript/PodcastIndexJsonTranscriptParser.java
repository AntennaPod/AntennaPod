package de.danoeh.antennapod.parser.transcript;

import android.util.Log;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.internal.StringUtil;

import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;

public class PodcastIndexJsonTranscriptParser {
    static String TAG = "PodcastIndexJsonTranscriptParser";

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
            long segmentStartTime = 0L;
            JSONObject obj = new JSONObject(jsonStr);
            JSONArray objSegments = obj.getJSONArray("segments");

            for (int i = 0; i < objSegments.length(); i++) {
                JSONObject jsonObject = objSegments.getJSONObject(i);
                startTime = Double.valueOf(jsonObject.optDouble("startTime", -1) * 1000L).longValue();
                endTime = Double.valueOf(jsonObject.optDouble("endTime", -1) * 1000L).longValue();
                if (startTime < 0 || endTime < 0) {
                    continue;
                }
                if (segmentStartTime == 0L) {
                    segmentStartTime = startTime;
                }
                duration += endTime - startTime;
                if (spanStartTime == 0L) {
                    spanStartTime = startTime;
                }

                speaker = jsonObject.optString("speaker");

                String body = jsonObject.optString("body");
                segmentBody += body + " ";

                if (duration >= span) {
                    segmentBody = StringUtils.trim(segmentBody);
                    transcript.addSegment(new TranscriptSegment(segmentStartTime, endTime, segmentBody, speaker));
                    Log.d(TAG, "JSON " + segmentBody);
                    duration = 0L;
                    segmentBody = "";
                    segmentStartTime = 0L;
                }
            }

            if (! StringUtil.isBlank(segmentBody)) {
                segmentBody = StringUtils.trim(segmentBody);
                transcript.addSegment(new TranscriptSegment(segmentStartTime, endTime, segmentBody, speaker));
                Log.d(TAG, "JSON [last]" + segmentBody);
            }

            if (transcript.getSegmentCount() > 0) {
                return transcript;
            } else {
                return null;
            }

        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
