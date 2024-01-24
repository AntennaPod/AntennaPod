package de.danoeh.antennapod.ui.home.sections;

import androidx.fragment.app.Fragment;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.EpisodesFragementInHome;
import de.danoeh.antennapod.fragment.QueueFragmentInHome;
import de.danoeh.antennapod.ui.home.HomeSection;

public class QueueExpanableSection extends HomeSection {
    public static final String TAG = "QueueExpandable";
    public boolean expandable = false;

    @Override
    protected String getSectionTitle() {
        return getString(R.string.queue_label);
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
        return new QueueFragmentInHome();
    }

    @Override
    protected boolean isExpandable() {
        return true;
    }
}
