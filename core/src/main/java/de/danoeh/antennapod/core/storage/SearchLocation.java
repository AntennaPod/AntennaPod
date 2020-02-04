package de.danoeh.antennapod.core.storage;

import androidx.annotation.StringRes;
import de.danoeh.antennapod.core.R;

public enum SearchLocation {
    TITLE(R.string.found_in_title_label),
    CHAPTERS(R.string.found_in_chapters_label),
    SHOWNOTES(R.string.found_in_shownotes_label);

    private int description;
    SearchLocation(@StringRes int description) {
        this.description = description;
    }

    public @StringRes int getDescription() {
        return description;
    }
}
