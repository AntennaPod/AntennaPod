package de.danoeh.antennapod.model.feed;

public class TranscriptSegment {
    private final long startTime;
    private final long endTime;
    private final String words;
    private final String speaker;
    private final Boolean trimmed;

    public TranscriptSegment(long start, long end, String w, String s) {
        startTime = start;
        endTime = end;
        words = w;
        speaker = s;
        trimmed = false;
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

    public Boolean isTrimmed() {
        return trimmed;
    }

    public String getSpeaker() {
        return speaker;
    }
}