package de.danoeh.antennapod.model.feed;

public class Transcript {
    private long startTime;
    private long endTime;
    private String words;

    public Transcript(long start, long end, String w) {
        startTime = start;
        endTime = end;
        words = w;
    }

    public String getWords() {
        return words;
    }

}
