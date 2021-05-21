package de.danoeh.antennapod.fragment.homesections;

import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.HomeFragment;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import kotlin.Unit;
import slush.AdapterAppliedResult;
import slush.Slush;
import slush.listeners.OnBindListener;
import slush.utils.BasicDiffCallback;

/**
 * Section on the HomeFragment
 */
public abstract class HomeSection implements View.OnCreateContextMenuListener {

    HomeFragment context;

    protected View section;
    protected TextView tvTitle;
    protected TextView tvNavigate;
    protected RecyclerView recyclerView;

    //must be set by descendant
    protected String sectionTitle;
    protected String sectionNavigateTitle;

    protected FeedItem selectedItem;

    public HomeSection(HomeFragment context) {
        this.context = context;
        section = View.inflate(context.requireActivity(), R.layout.home_section, null);
        tvTitle = section.findViewById(R.id.sectionTitle);
        tvNavigate = section.findViewById(R.id.sectionNavigate);
        recyclerView = section.findViewById(R.id.sectionRecyclerView);
    }

    public void addSectionTo(LinearLayout parent) {
        tvTitle.setText(sectionTitle);
        if (!TextUtils.isEmpty(sectionNavigateTitle)) {
            tvNavigate.setText(sectionNavigateTitle.toLowerCase()+" >>");
            tvNavigate.setOnClickListener(navigate());
        }

        if (recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() > 0) {
            //don't add if empty
            parent.addView(section);
            context.registerForContextMenu(recyclerView);
        }
    }

    protected abstract View.OnClickListener navigate();

    @NonNull
    protected abstract List<FeedItem> loadItems();

    public void updateItems() {}

    protected abstract Unit onItemClick(View view, FeedItem feedItem);

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        MenuInflater inflater = context.requireActivity().getMenuInflater();
        inflater.inflate(R.menu.feeditemlist_context, contextMenu);
        contextMenu.setHeaderTitle(selectedItem.getTitle());
        FeedItemMenuHandler.onPrepareMenu(contextMenu, selectedItem, R.id.skip_episode_item);
    }

    protected AdapterAppliedResult<FeedItem> easySlush(int layout, OnBindListener<FeedItem> onBindListener) {
        return new Slush.SingleType<FeedItem>()
                .setItemLayout(layout)
                .setLayoutManager(new LinearLayoutManager(context.getContext(), RecyclerView.HORIZONTAL, false))
                .setItems(loadItems())
                .onItemClickWithItem(this::onItemClick)
                .onBind(onBindListener)
                .into(recyclerView);
    }

}
