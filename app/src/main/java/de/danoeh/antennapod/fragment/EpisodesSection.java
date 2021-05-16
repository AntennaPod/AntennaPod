package de.danoeh.antennapod.fragment;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.List;

import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.model.feed.FeedItem;

public class EpisodesSection extends HomeSection {

    public EpisodesSection(Fragment context) {
        super(context);
        sectionTitle = "Episodes";
        sectionFragment = new PowerEpisodesFragment();
    }

    @NonNull
    @Override
    protected View.OnClickListener navigate() {
        return view -> {
            ((MainActivity) context.requireActivity()).loadFragment(AddFeedFragment.TAG, null);
        };
    }

    @NonNull
    @Override
    protected List<FeedItem> loadItems() {
        return null;
    }
}
