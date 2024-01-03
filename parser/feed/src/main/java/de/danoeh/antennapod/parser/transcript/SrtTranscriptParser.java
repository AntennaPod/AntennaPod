package de.danoeh.antennapod.parser.transcript;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.internal.StringUtil;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;

public class SrtTranscriptParser {
    private static String TAG = "SrtTranscriptParser";

    public static Transcript parse(String str) {
        Transcript transcript = new Transcript();

        List<String> lines = Arrays.asList(str.split("\n"));
        Iterator<String> iter = lines.iterator();
        String speaker = "";
        StringBuffer body = new StringBuffer("");
        String line;
        String segmentBody = "";
        long startTimecode = 0L;
        long spanStartTimecode = 0L;
        long endTimecode = 0L;
        long span = 1000L;
        long duration = 0L;

        while (iter.hasNext()) {
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
                if (startTimecode == -1 || endTimecode == -1) {
                    continue;
                }

                if (spanStartTimecode == 0) {
                    spanStartTimecode = startTimecode;
                }
                duration += endTimecode - startTimecode;
                do {
                    line = iter.next();
                    if (StringUtil.isBlank(line)) {
                        break;
                    }
                    body.append(line.strip());
                    body.append(" ");
                } while (iter.hasNext());
            }

            if (body.indexOf(":") != -1) {
                String [] parts = body.toString().split(":");
                if (parts.length < 2) {
                    continue;
                }
                speaker = parts[0];
                body = new StringBuffer(parts[1].strip());
            }
            if (!StringUtil.isBlank(body.toString())) {
                segmentBody += " " + body;
                segmentBody = StringUtils.trim(segmentBody);
                if (duration >= span && endTimecode > spanStartTimecode) {
                    Log.d(TAG, "SRT : " + Long.toString(spanStartTimecode) + " " + segmentBody);
                    transcript.addSegment(new TranscriptSegment(spanStartTimecode,
                            endTimecode,
                            segmentBody,
                            speaker));
                    duration = 0L;
                    spanStartTimecode = 0L;
                    segmentBody = "";
                }
                body = new StringBuffer("");
            }
        }

        if (! StringUtil.isBlank(segmentBody) && endTimecode > spanStartTimecode) {
            Log.d(TAG, "SRT : " + Long.toString(spanStartTimecode) + " " + segmentBody);

            segmentBody = StringUtils.trim(segmentBody);
            transcript.addSegment(new TranscriptSegment(spanStartTimecode,
                    endTimecode,
                    segmentBody,
                    speaker));
        }
        if (transcript.getSegmentCount() > 0) {
            return transcript;
        } else {
            return null;
        }
    }

    static long parseTimecode(String timecode) {
        String[] parts = timecode.split(":");
        if (parts.length < 3) {
            return -1;
        }
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
