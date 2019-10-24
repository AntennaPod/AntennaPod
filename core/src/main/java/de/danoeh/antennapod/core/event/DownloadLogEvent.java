package de.danoeh.antennapod.core.event;

public class DownloadLogEvent {

    private DownloadLogEvent() {
    }

    public static DownloadLogEvent listUpdated() {
        return new DownloadLogEvent();
    }

    @Override
    public String toString() {
        return "DownloadLogEvent";
    }
}
