package de.danoeh.antennapod.model.feed;

public class TranscriptSegment {
    private long startTime;
    private long endTime;
    private String words;
    private String speaker;
    private Boolean trimmed;

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

    public long setStartTime(long t) {
        return startTime = t;
    }

    public long setEndTime(long t) {
        return endTime = t;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getWords() {
        return words;
    }

    public String setWords(String str) {
        return words = str;
    }

    public Boolean setTrimmed(Boolean t) {
       trimmed = t;
       return trimmed;
    }

    public Boolean isTrimmed() {
        return trimmed;
    }

    public String getSpeaker() {
        return speaker;
    }
}
