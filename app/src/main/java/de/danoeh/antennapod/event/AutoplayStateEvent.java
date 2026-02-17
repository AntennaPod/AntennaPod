package de.danoeh.antennapod.event;

public class AutoplayStateEvent {
    private final long mediaId;
    private final boolean autoplayEnabled;
    private final boolean toggleVisible;

    public AutoplayStateEvent(long mediaId, boolean autoplayEnabled, boolean toggleVisible) {
        this.mediaId = mediaId;
        this.autoplayEnabled = autoplayEnabled;
        this.toggleVisible = toggleVisible;
    }

    public long getMediaId() {
        return mediaId;
    }

    public boolean getAutoplayEnabled() {
        return autoplayEnabled;
    }

    public boolean isToggleVisible() {
        return toggleVisible;
    }

    @Override
    public String toString() {
        return "AutoplayStateEvent{mediaId=" + mediaId
                + ", autoplayEnabled=" + autoplayEnabled
                + ", toggleVisible=" + toggleVisible + '}';
    }
}
