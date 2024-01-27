package de.danoeh.antennapod.model.feed;

public class TranscriptSegment {
    final private long startTime;
    final private long endTime;
    final private String words;
    final private String speaker;
    final private Boolean trimmed;

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