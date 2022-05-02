package de.danoeh.antennapod.ui.home.sections;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.databinding.HomeWelcomeBannerBinding;
import de.danoeh.antennapod.fragment.AddFeedFragment;

public class WelcomeBannerSection extends Fragment {
    public static final String TAG = "WelcomeBannerSection";
    HomeWelcomeBannerBinding viewBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewBinding = HomeWelcomeBannerBinding.inflate(inflater);
        viewBinding.addFeedButton.setOnClickListener(v ->
                ((MainActivity) requireActivity()).loadChildFragment(new AddFeedFragment()));
        loadItems();
        return viewBinding.getRoot();
    }

    private void loadItems() {
        int numSubscriptions = DBReader.getNavDrawerData().items.size();
        viewBinding.getRoot().setVisibility(numSubscriptions == 0 ? View.VISIBLE : View.GONE);
    }
}
