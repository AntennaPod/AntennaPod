package de.danoeh.antennapod.fragment.homesections;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.fragment.PowerEpisodesFragment;
import de.danoeh.antennapod.model.feed.FeedItem;

public class EpisodesSection extends HomeSection {

    public EpisodesSection(Fragment context) {
        super(context);
        sectionTitle = context.getString(R.string.episodes_label);
        sectionNavigateTitle = context.getString(R.string.episodes_label);
        sectionFragment = new PowerEpisodesFragment();
        ((PowerEpisodesFragment) sectionFragment).hideToolbar = true;
        expandsToFillHeight = true;
    }

    @NonNull
    @Override
    protected View.OnClickListener navigate() {
        return view -> {
            ((MainActivity) context.requireActivity()).loadFragment(PowerEpisodesFragment.TAG, null);
        };
    }

    @NonNull
    @Override
    protected List<FeedItem> loadItems() {
        return null;
    }
}
