package de.danoeh.antennapod.parser.transcript;

import org.apache.commons.lang3.StringUtils;

import de.danoeh.antennapod.model.feed.Transcript;

public class TranscriptParser {
    static final long MIN_SPAN = 50000L; // Merge short segments together to form a span of 50 seconds
    static final long MAX_SPAN = 80000L; // Don't go beyond 80 seconds when merging

    public static Transcript parse(String str, String type) {
        if (str == null || StringUtils.isBlank(str)) {
            return null;
        }

        if ("application/json".equals(type)) {
            return JsonTranscriptParser.parse(str);
        }

        if ("application/srt".equals(type) || "application/srr".equals(type) || "application/x-subrip".equals(type)) {
            return SrtTranscriptParser.parse(str);
        }
        return null;
    }
}
