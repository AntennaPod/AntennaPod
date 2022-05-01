package de.danoeh.antennapod.ui.home;

import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.HomeSectionBinding;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import kotlin.Unit;
import slush.AdapterAppliedResult;
import slush.Slush;
import slush.listeners.OnBindListener;

import java.util.List;

/**
 * Section on the HomeFragment
 * can have different I item types
 */
public abstract class HomeSection<I> implements View.OnCreateContextMenuListener {
    protected HomeFragment context;

    protected HomeSectionBinding viewBinding;
    protected FeedItem selectedItem;

    public HomeSection(HomeFragment context) {
        this.context = context;
        viewBinding = HomeSectionBinding.inflate(context.getLayoutInflater());
        viewBinding.titleLabel.setText(getSectionTitle());
        viewBinding.moreButton.setText(context.getString(R.string.navigate_arrows, getMoreLinkTitle()));
        viewBinding.moreButton.setOnClickListener(navigate());
        if (TextUtils.isEmpty(getMoreLinkTitle())) {
            viewBinding.moreButton.setVisibility(View.INVISIBLE);
        }
    }

    public void addSectionTo(LinearLayout parent) {
        parent.addView(viewBinding.getRoot());
        context.registerForContextMenu(viewBinding.recyclerView);
        hideIfEmpty();
    }

    private void hideIfEmpty() {
        boolean isVisible = viewBinding.recyclerView.getAdapter() != null
                && viewBinding.recyclerView.getAdapter().getItemCount() > 0;
        viewBinding.getRoot().setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        MenuInflater inflater = context.requireActivity().getMenuInflater();
        inflater.inflate(R.menu.feeditemlist_context, contextMenu);
        contextMenu.setHeaderTitle(selectedItem.getTitle());
        FeedItemMenuHandler.onPrepareMenu(contextMenu, selectedItem, R.id.skip_episode_item);
    }

    protected AdapterAppliedResult<I> easySlush(int layout, OnBindListener<I> onBindListener) {
        return new Slush.SingleType<I>()
                .setItemLayout(layout)
                .setLayoutManager(new LinearLayoutManager(context.getContext(), RecyclerView.HORIZONTAL, false))
                .setItems(loadItems())
                .onItemClickWithItem(this::onItemClick)
                .onBind(onBindListener)
                .into(viewBinding.recyclerView);
    }

    protected abstract String getSectionTitle();

    protected abstract String getMoreLinkTitle();
    protected abstract View.OnClickListener navigate();

    @NonNull
    protected abstract List<I> loadItems();

    protected abstract Unit onItemClick(View view, I item);
}