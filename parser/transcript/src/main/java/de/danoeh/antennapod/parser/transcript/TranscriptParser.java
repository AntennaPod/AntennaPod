package de.danoeh.antennapod.parser.transcript;

import de.danoeh.antennapod.model.feed.TranscriptType;
import org.apache.commons.lang3.StringUtils;

import de.danoeh.antennapod.model.feed.Transcript;

public class TranscriptParser {
    static final long MIN_SPAN = 5000L; // Merge short segments together to form a span of 5 seconds
    static final long MAX_SPAN = 8000L; // Don't go beyond 10 seconds when merging

    public static Transcript parse(String str, String typeStr) {
        if (str == null || StringUtils.isBlank(str)) {
            return null;
        }
        TranscriptType type = TranscriptType.fromMime(typeStr);
        return switch (type) {
            case JSON -> JsonTranscriptParser.parse(str);
            case VTT -> VttTranscriptParser.parse(str);
            case SRT -> SrtTranscriptParser.parse(str);
            default -> null;
        };
    }
}
