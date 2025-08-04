package de.danoeh.antennapod.parser.transcript;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;

public class VttTranscriptParser {
    private static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile("^(?:([0-9]{1,2}):)?([0-9]{2}):([0-9]{2})\\.([0-9]{3})$");

    private static final Pattern VOICE_SPAN =
            Pattern.compile("<v(?:\\.[^\\t\\n\\r &<>.]+)*[ \\t]([^\\n\\r&>]+)>");

    private record Timings(long start, long end) {}

    public static Transcript parse(String str) {
        // This is basically a very light WebVTT parser.
        // It uses WebVTT properties to be both exact and very light.
        // We will only be parsing the WebVTT cue blocks.

        if (StringUtils.isBlank(str)) {
            return null;
        }

        // WebVTT line terminator can be \r\n, \n or \n, let's use only one
        str = str.replaceAll("\r\n?", "\n");
        List<String> lines = Arrays.asList(str.split("\n"));

        Transcript transcript = new Transcript();
        Iterator<String> iterator = lines.iterator();
        Set<String> speakers = new HashSet<>();
        String speaker = "";
        TranscriptSegment segment = null;

        // Iterate through cue blocks
        while (iterator.hasNext()) {
            String line = iterator.next();

            if (!line.contains("-->")) {
                continue;
            }

            Timings timings = parseCueTimings(line);
            if (timings == null) {
                return null; // Input is broken
            }

            String payload = parseCuePayload(iterator);

            Matcher matcher = VOICE_SPAN.matcher(payload);
            if (matcher.find()) {
                speaker = matcher.group(1);
                speakers.add(speaker);
            }

            payload = Jsoup.parse(payload).text(); // remove all HTML tags

            // should we merge this segment with the previous one?
            if (segment != null && segment.getSpeaker().equals(speaker)
                    && timings.end - segment.getStartTime() < TranscriptParser.MAX_SPAN) {
                segment.append(timings.end, payload);
            } else {
                if (segment != null) {
                    transcript.addSegment(segment);
                }
                segment = new TranscriptSegment(timings.start, timings.end, payload, speaker);
            }

            // do we have a candidate segment long enough to add it without trying to add more
            if (segment.getEndTime() - segment.getStartTime() >= TranscriptParser.MIN_SPAN) {
                transcript.addSegment(segment);
                segment = null;
            }
        }

        if (segment != null) {
            transcript.addSegment(segment);
        }

        if (transcript.getSegmentCount() == 0) {
            return null;
        }
        transcript.setSpeakers(speakers);
        return transcript;
    }

    private static long parseIntOrNull(@Nullable String s) {
        return StringUtils.isEmpty(s) ? 0 : Integer.parseInt(s);
    }

    private static long parseTimestamp(@NonNull String timestamp) {
        Matcher matcher = TIMESTAMP_PATTERN.matcher(timestamp);
        if (!matcher.matches()) {
            return -1;
        }
        long hours = parseIntOrNull(matcher.group(1));
        long minutes = parseIntOrNull(matcher.group(2));
        long seconds = parseIntOrNull(matcher.group(3));
        long milliseconds = parseIntOrNull(matcher.group(4));
        return (hours * 60 * 60 * 1000) + (minutes * 60 * 1000) + (seconds * 1000) + milliseconds;
    }

    @Nullable
    private static Timings parseCueTimings(@NonNull String line) {
        String[] timestamps = line.split("-->");
        if (timestamps.length < 2) {
            return null;
        }
        long start = parseTimestamp(timestamps[0].trim());
        long end = parseTimestamp(timestamps[1].trim().split("[ \\t]")[0]);
        if (start == -1 || end == -1) {
            return null;
        }
        return new Timings(start, end);
    }

    @NonNull
    private static String parseCuePayload(@NonNull Iterator<String> iterator) {
        StringBuilder body = new StringBuilder();
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line.isEmpty()) {
                break;
            }
            body.append(line.strip());
            body.append(" ");
        }
        return body.toString().strip();
    }

}
