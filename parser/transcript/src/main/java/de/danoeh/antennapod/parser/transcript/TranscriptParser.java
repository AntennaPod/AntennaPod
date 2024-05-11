package de.danoeh.antennapod.parser.transcript;

import org.apache.commons.lang3.StringUtils;
import java.util.Locale;

import de.danoeh.antennapod.model.feed.Transcript;

public class TranscriptParser {
    static final long MIN_SPAN = 50000L; // merge short segments together to form a span of 5 seconds
    static final long MAX_SPAN = 80000L; // Do go beyond 8 seconds when merging

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

    public static String secondsToTime(long msecs) {
        int duration = Math.toIntExact(msecs / 1000L);
        return String.format(Locale.getDefault(), "%d:%02d:%02d",
                duration / 3600,
                (duration % 3600) / 60,
                (duration % 60));
    }
}
