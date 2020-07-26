package de.danoeh.antennapod.core.event;

import androidx.annotation.NonNull;

public class DownloadLogEvent {

    private DownloadLogEvent() {
    }

    public static DownloadLogEvent listUpdated() {
        return new DownloadLogEvent();
    }

    @NonNull
    @Override
    public String toString() {
        return "DownloadLogEvent";
    }
}
