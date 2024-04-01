package de.danoeh.antennapod.ui.home.settingsdialog;

final class ReorderHomeScreenDialogItem {
    enum ViewType {
        Section,
        Header
    }

    private final ViewType viewType;
    private final String title;

    ReorderHomeScreenDialogItem(ViewType viewType, String title) {
        this.viewType = viewType;
        this.title = title;
    }

    public ViewType getViewType() {
        return viewType;
    }

    public String getTitle() {
        return title;
    }
}
