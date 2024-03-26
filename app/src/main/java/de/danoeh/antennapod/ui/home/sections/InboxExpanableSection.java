package de.danoeh.antennapod.ui.home.sections;

import androidx.fragment.app.Fragment;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.InboxFragmentInHome;
import de.danoeh.antennapod.ui.home.HomeSection;

public class InboxExpanableSection extends HomeSection {
    public static final String TAG = "InboxExpandable";
    public boolean expandable = false;

    @Override
    protected String getSectionTitle() {
        return getString(R.string.inbox_label);
    }

    @Override
    protected String getMoreLinkTitle() {
        return null;
    }

    @Override
    protected void handleMoreClick() {
        //do nothing
    }

    @Override
    protected Fragment getExpandable() {
        return new InboxFragmentInHome();
    }

    @Override
    protected boolean isExpandable() {
        return true;
    }
}
