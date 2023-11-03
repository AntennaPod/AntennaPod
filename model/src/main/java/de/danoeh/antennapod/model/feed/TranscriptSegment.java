package de.danoeh.antennapod.model.feed;

public class TranscriptSegment {
    private long startTime;
    private long endTime;
    private String words;
    private String speaker;

    public TranscriptSegment(long start, long end, String w, String s) {
        startTime = start;
        endTime = end;
        words = w;
        speaker = s;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getWords() {
        return words;
    }

    public String getSpeaker() {
        return speaker;
    }
}
