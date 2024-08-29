package de.danoeh.antennapod.ui.screen.preferences;

public final class ReorderDialogItem {
    public enum ViewType {
        Section,
        Header
    }

    private final ViewType viewType;
    private final String tag;
    private final String title;

    public ReorderDialogItem(ViewType viewType, String tag, String title) {
        this.viewType = viewType;
        this.tag = tag;
        this.title = title;
    }

    public ViewType getViewType() {
        return viewType;
    }

    public String getTitle() {
        return title;
    }

    public String getTag() {
        return tag;
    }
}
