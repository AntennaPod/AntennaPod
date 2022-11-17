package de.danoeh.antennapod.ui.home;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.adapter.HorizontalItemListAdapter;
import de.danoeh.antennapod.databinding.HomeSectionBinding;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import org.greenrobot.eventbus.EventBus;

/**
 * Section on the HomeFragment
 */
public abstract class HomeSection extends Fragment implements View.OnCreateContextMenuListener {
    public static final String TAG = "HomeSection";
    protected HomeSectionBinding viewBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewBinding = HomeSectionBinding.inflate(inflater);
        viewBinding.titleLabel.setText(getSectionTitle());
        viewBinding.moreButton.setText(getString(R.string.navigate_arrows, getMoreLinkTitle()));
        viewBinding.moreButton.setOnClickListener((view) -> handleMoreClick());
        if (TextUtils.isEmpty(getMoreLinkTitle())) {
            viewBinding.moreButton.setVisibility(View.INVISIBLE);
        }
        return viewBinding.getRoot();
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (!getUserVisibleHint() || !isVisible() || !isMenuVisible()) {
            // The method is called on all fragments in a ViewPager, so this needs to be ignored in invisible ones.
            // Apparently, none of the visibility check method works reliably on its own, so we just use all.
            return false;
        }
        FeedItem longPressedItem;
        if (viewBinding.recyclerView.getAdapter() instanceof EpisodeItemListAdapter) {
            EpisodeItemListAdapter adapter = (EpisodeItemListAdapter) viewBinding.recyclerView.getAdapter();
            longPressedItem = adapter.getLongPressedItem();
        } else if (viewBinding.recyclerView.getAdapter() instanceof HorizontalItemListAdapter) {
            HorizontalItemListAdapter adapter = (HorizontalItemListAdapter) viewBinding.recyclerView.getAdapter();
            longPressedItem = adapter.getLongPressedItem();
        } else {
            return false;
        }

        if (longPressedItem == null) {
            Log.i(TAG, "Selected item or listAdapter was null, ignoring selection");
            return super.onContextItemSelected(item);
        }
        return FeedItemMenuHandler.onMenuItemClicked(this, item.getItemId(), longPressedItem);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        registerForContextMenu(viewBinding.recyclerView);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        unregisterForContextMenu(viewBinding.recyclerView);
    }

    protected abstract String getSectionTitle();

    protected abstract String getMoreLinkTitle();

    protected abstract void handleMoreClick();
}
