package de.danoeh.antennapod.core.event;

public class StatisticsEvent {

    public enum Action {
        STATISTICS_UPDATED
    }

    public final Action action;

    public StatisticsEvent(Action action) {
        this.action = action;
    }
}
