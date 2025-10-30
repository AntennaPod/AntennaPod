package de.danoeh.antennapod.parser.transcript;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.internal.StringUtil;

import java.util.HashSet;
import java.util.Set;

import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;

public class JsonTranscriptParser {
    public static Transcript parse(String jsonStr) {
        try {
            Transcript transcript = new Transcript();
            long startTime = -1L;
            long endTime = -1L;
            long segmentStartTime = -1L;
            long segmentEndTime = -1L;
            long duration = 0L;
            String speaker = "";
            String prevSpeaker = "";
            String segmentBody = "";
            JSONArray objSegments;
            Set<String> speakers = new HashSet<>();

            try {
                JSONObject obj = new JSONObject(jsonStr);
                objSegments = obj.getJSONArray("segments");
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }

            for (int i = 0; i < objSegments.length(); i++) {
                JSONObject jsonObject = objSegments.getJSONObject(i);
                segmentEndTime = endTime;
                startTime = Double.valueOf(jsonObject.optDouble("startTime", -1) * 1000L).longValue();
                endTime = Double.valueOf(jsonObject.optDouble("endTime", -1) * 1000L).longValue();
                if (startTime < 0 || endTime < 0) {
                    continue;
                }
                if (segmentStartTime == -1L) {
                    segmentStartTime = startTime;
                }
                duration += endTime - startTime;

                prevSpeaker = speaker;
                speaker = jsonObject.optString("speaker");
                speakers.add(speaker);
                if (StringUtils.isEmpty(speaker) && StringUtils.isNotEmpty(prevSpeaker)) {
                    speaker = prevSpeaker;
                }
                String body = jsonObject.optString("body");
                if (!prevSpeaker.equals(speaker)) {
                    if (StringUtils.isNotEmpty(segmentBody)) {
                        segmentBody = StringUtils.trim(segmentBody);
                        transcript.addSegment(new TranscriptSegment(segmentStartTime,
                                segmentEndTime,
                                segmentBody,
                                prevSpeaker));
                        segmentStartTime = startTime;
                        segmentBody = body.toString();
                        duration = 0L;
                        continue;
                    }
                }

                segmentBody += " " + body;

                if (duration >= TranscriptParser.MIN_SPAN) {
                    // Look ahead and make sure the next segment does not start with an alphanumeric character
                    if ((i + 1) < objSegments.length()) {
                        String nextSegmentFirstChar = objSegments.getJSONObject(i + 1)
                                .optString("body")
                                .substring(0, 1);
                        if (!StringUtils.isAlphanumeric(nextSegmentFirstChar)
                                && (duration < TranscriptParser.MAX_SPAN)) {
                            continue;
                        }
                    }
                    segmentBody = StringUtils.trim(segmentBody);
                    transcript.addSegment(new TranscriptSegment(segmentStartTime, endTime, segmentBody, speaker));
                    duration = 0L;
                    segmentBody = "";
                    segmentStartTime = -1L;
                }
            }

            if (!StringUtil.isBlank(segmentBody)) {
                segmentBody = StringUtils.trim(segmentBody);
                transcript.addSegment(new TranscriptSegment(segmentStartTime, endTime, segmentBody, speaker));
            }

            if (transcript.getSegmentCount() > 0) {
                transcript.setSpeakers(speakers);
                return transcript;
            } else {
                return null;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
