package de.danoeh.antennapod.model.feed;

public class Transcript {
    private long _start;
    private long _end;
    private String _words;

    public Transcript (long start, long end, String words) {
        _start = start;
        _end = end;
        _words = words;
    }
}
