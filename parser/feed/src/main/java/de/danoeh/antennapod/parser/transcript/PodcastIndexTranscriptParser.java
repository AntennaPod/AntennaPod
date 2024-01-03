package de.danoeh.antennapod.parser.transcript;

import org.apache.commons.lang3.StringUtils;

import de.danoeh.antennapod.model.feed.Transcript;

public class PodcastIndexTranscriptParser {

    public static Transcript parse(String str, String type) {
        if (str == null || StringUtils.isBlank(str)) {
            return null;
        }

        str = str.replaceAll("\r\n", "\n");

        if ("application/json".equals(type)) {
            return PodcastIndexJsonTranscriptParser.parse(str);
        }

        if ("application/srt".equals(type) || "application/srr".equals(type) || "application/x-subrip".equals(type)) {
            return SrtTranscriptParser.parse(str);
        }
        return null;
    }
}