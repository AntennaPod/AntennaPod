package de.danoeh.antennapod.model.feed;

public enum TranscriptType {
    JSON(4, "application/json"),
    VTT(3, "text/vtt"),
    SRT(2, "application/srt"),
    NONE(0, "");

    public final int priority;
    public final String canonicalMime;

    TranscriptType(int priority, String canonicalMime) {
        this.priority = priority;
        this.canonicalMime = canonicalMime;
    }

    public static TranscriptType fromMime(String type) {
        if (type == null) {
            return NONE;
        }
        return switch (type) {
            case "application/json" -> JSON;
            case "text/vtt" -> VTT;
            case "application/srt", "application/srr", "application/x-subrip" -> SRT;
            default -> NONE;
        };
    }
}
