package de.danoeh.antennapod.parser.transcript;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.internal.StringUtil;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;

public class SrtTranscriptParser {
    private static String TAG = "SrtTranscriptParser";
    private static Pattern timecodePattern = Pattern.compile("^([0-9]{2}):([0-9]{2}):([0-9]{2}),([0-9]{3})$");
    // merge short segments together to form a span of 1 second
    public static long minSpan = 1000L;

    public static Transcript parse(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        str = str.replaceAll("\r\n", "\n");

        Transcript transcript = new Transcript();
        List<String> lines = Arrays.asList(str.split("\n"));
        Iterator<String> iter = lines.iterator();
        String speaker = "";
        StringBuffer body = new StringBuffer("");
        String line;
        String segmentBody = "";
        long startTimecode = -1L;
        long spanStartTimecode = -1L;
        long endTimecode = -1L;
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

                if (spanStartTimecode == -1) {
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
                String [] parts = body.toString().trim().split(":");
                if (parts.length < 2) {
                    continue;
                }
                speaker = parts[0];
                body = new StringBuffer(parts[1].strip());
            }
            if (!StringUtil.isBlank(body.toString())) {
                segmentBody += " " + body;
                segmentBody = StringUtils.trim(segmentBody);
                if (duration >= minSpan && endTimecode > spanStartTimecode) {
                    Log.d(TAG, "SRT : " + Long.toString(spanStartTimecode) + " " + segmentBody);
                    transcript.addSegment(new TranscriptSegment(spanStartTimecode,
                            endTimecode,
                            segmentBody,
                            speaker));
                    duration = 0L;
                    spanStartTimecode = -1L;
                    segmentBody = "";
                }
                body = new StringBuffer("");
            }
        }

        if (!StringUtil.isBlank(segmentBody) && endTimecode > spanStartTimecode) {
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

    // Time format 00:00:00,000
    static long parseTimecode(String timecode) {
        Matcher matcher = timecodePattern.matcher(timecode);
        if (! matcher.matches()) {
            return -1;
        }
        int hours;
        hours = Integer.parseInt(matcher.group(1));
        int minutes;
        minutes = Integer.parseInt(matcher.group(2));
        int seconds;
        seconds = Integer.parseInt(matcher.group(3));
        int milliseconds;
        milliseconds = Integer.parseInt(matcher.group(4));
        return (hours * 60 * 60 * 1000) + (minutes * 60 * 1000) + (seconds * 1000) + milliseconds;
    }
}