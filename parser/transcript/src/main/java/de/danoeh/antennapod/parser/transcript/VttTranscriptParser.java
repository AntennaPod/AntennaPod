package de.danoeh.antennapod.parser.transcript;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.internal.StringUtil;

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
    // TODO hour is optional
    private static final Pattern TIMECODE_PATTERN = Pattern.compile("^([0-9]{2}):([0-9]{2}):([0-9]{2})\\.([0-9]{3})$");

    private static final Pattern VOICE_SPAN = Pattern.compile("<v(?:\\.[^\\t\\n\\r &<>.]+)?[ \\t]([^\\n\\r&>]+)>");

    public static Transcript parse(String str) {
        Log.i("transcript", "parsing VTT");
        if (StringUtils.isBlank(str)) {
            return null;
        }

        // WebVTT line terminator can be \r\n, \n or \n, let's use only one
        str = str.replaceAll("\r\n?", "\n");

        Transcript transcript = new Transcript();
        List<String> lines = Arrays.asList(str.split("\n"));
        Iterator<String> iter = lines.iterator();
        String speaker = "";
        String prevSpeaker = "";
        StringBuilder body;
        String line;
        String segmentBody = "";
        long startTimecode = -1L;
        long spanStartTimecode = -1L;
        long spanEndTimecode = -1L;
        long endTimecode = -1L;
        long duration = 0L;
        Set<String> speakers = new HashSet<>();

        while (iter.hasNext()) {
            body = new StringBuilder();
            line = iter.next();

            if (line.isEmpty()) {
                continue;
            }

            spanEndTimecode = endTimecode;
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

            Matcher matcher = VOICE_SPAN.matcher(body.toString());
            if (matcher.find()) {
                prevSpeaker = speaker;
                speaker = matcher.group(1);
                speakers.add(speaker);
                body = new StringBuilder(body.substring(matcher.end()));
                if (StringUtils.isNotEmpty(prevSpeaker) && !StringUtils.equals(speaker, prevSpeaker)) {
                    if (StringUtils.isNotEmpty(segmentBody)) {
                        transcript.addSegment(new TranscriptSegment(spanStartTimecode,
                                spanEndTimecode, segmentBody, prevSpeaker));
                        duration = 0L;
                        spanStartTimecode = startTimecode;
                        segmentBody = body.toString();
                        continue;
                    }
                }
            } else {
                if (StringUtils.isNotEmpty(prevSpeaker) && StringUtils.isEmpty(speaker)) {
                    speaker = prevSpeaker;
                }
            }

            segmentBody += " " + body;
            segmentBody = StringUtils.trim(segmentBody);
            if (duration >= TranscriptParser.MIN_SPAN && endTimecode > spanStartTimecode) {
                transcript.addSegment(new TranscriptSegment(spanStartTimecode, endTimecode, segmentBody, speaker));
                duration = 0L;
                spanStartTimecode = -1L;
                segmentBody = "";
            }
        }

        if (!StringUtil.isBlank(segmentBody) && endTimecode > spanStartTimecode) {
            segmentBody = StringUtils.trim(segmentBody);
            transcript.addSegment(new TranscriptSegment(spanStartTimecode, endTimecode, segmentBody, speaker));
        }
        if (transcript.getSegmentCount() > 0) {
            transcript.setSpeakers(speakers);
            return transcript;
        } else {
            return null;
        }
    }

    static long parseTimecode(String timecode) {
        Matcher matcher = TIMECODE_PATTERN.matcher(timecode);
        if (!matcher.matches()) {
            return -1;
        }
        long hours = Integer.parseInt(matcher.group(1));
        long minutes = Integer.parseInt(matcher.group(2));
        long seconds = Integer.parseInt(matcher.group(3));
        long milliseconds = Integer.parseInt(matcher.group(4));
        return (hours * 60 * 60 * 1000) + (minutes * 60 * 1000) + (seconds * 1000) + milliseconds;
    }
}
