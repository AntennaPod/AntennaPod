package de.danoeh.antennapod.ui.home;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.HomeSectionBinding;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;

/**
 * Section on the HomeFragment
 */
public abstract class HomeSection extends Fragment implements View.OnCreateContextMenuListener {
    protected HomeSectionBinding viewBinding;
    protected FeedItem selectedItem;

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
        registerForContextMenu(viewBinding.recyclerView);
        return viewBinding.getRoot();
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.feeditemlist_context, contextMenu);
        contextMenu.setHeaderTitle(selectedItem.getTitle());
        FeedItemMenuHandler.onPrepareMenu(contextMenu, selectedItem, R.id.skip_episode_item);
    }

    protected abstract String getSectionTitle();

    protected abstract String getMoreLinkTitle();

    protected abstract void handleMoreClick();
}
