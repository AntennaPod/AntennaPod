package de.danoeh.antennapod.core.event;

public class ServiceEvent {
    public enum Action {
        SERVICE_STARTED
    }

    public final Action action;

    public ServiceEvent(Action action) {
        this.action = action;
    }
}
