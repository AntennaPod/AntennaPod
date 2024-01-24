package de.danoeh.antennapod.ui.home.sections;

import androidx.fragment.app.Fragment;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.EpisodesFragementInHome;
import de.danoeh.antennapod.fragment.InboxFragmentInHome;
import de.danoeh.antennapod.ui.home.HomeSection;

public class EpisodesExpanableSection extends HomeSection {
    public static final String TAG = "EpisodesExpandable";

    @Override
    protected String getSectionTitle() {
        return getString(R.string.episodes_label);
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
        return new EpisodesFragementInHome();
    }

    @Override
    protected boolean isExpandable() {
        return true;
    }
}
