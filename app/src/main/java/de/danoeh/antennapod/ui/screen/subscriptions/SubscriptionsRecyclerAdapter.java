package de.danoeh.antennapod.ui.screen.subscriptions;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.view.ContextMenu;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.database.NavDrawerData;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.SelectableAdapter;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.screen.feed.FeedItemlistFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for subscriptions
 */
public class SubscriptionsRecyclerAdapter extends SelectableAdapter<SubscriptionViewHolder>
        implements View.OnCreateContextMenuListener {
    private final WeakReference<MainActivity> mainActivityRef;
    private List<NavDrawerData.DrawerItem> listItems;
    private NavDrawerData.DrawerItem selectedItem = null;
    int longPressedPosition = 0; // used to init actionMode
    private int columnCount = 3;

    public SubscriptionsRecyclerAdapter(MainActivity mainActivity) {
        super(mainActivity);
        this.mainActivityRef = new WeakReference<>(mainActivity);
        this.listItems = new ArrayList<>();
        setHasStableIds(true);
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    public Object getItem(int position) {
        return listItems.get(position);
    }

    public NavDrawerData.DrawerItem getSelectedItem() {
        return selectedItem;
    }

    @NonNull
    @Override
    public SubscriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == R.id.view_type_subscription_list) {
            View itemView = LayoutInflater.from(mainActivityRef.get())
                    .inflate(R.layout.subscription_list_item, parent, false);
            return new SubscriptionViewHolder(itemView, mainActivityRef.get());
        }
        View itemView = LayoutInflater.from(mainActivityRef.get())
                .inflate(R.layout.subscription_grid_item, parent, false);
        itemView.findViewById(R.id.titleLabel).setVisibility(
                viewType == R.id.view_type_subscription_grid_with_title ? View.VISIBLE : View.GONE);
        return new SubscriptionViewHolder(itemView, mainActivityRef.get());
    }

    @Override
    public void onBindViewHolder(@NonNull SubscriptionViewHolder holder, int position) {
        NavDrawerData.DrawerItem drawerItem = listItems.get(position);
        boolean isFeed = drawerItem.type == NavDrawerData.DrawerItem.Type.FEED;
        holder.bind(drawerItem, columnCount);
        holder.itemView.setOnCreateContextMenuListener(this);
        if (inActionMode()) {
            if (holder.selectView != null) {
                if (isFeed) {
                    holder.selectCheckbox.setVisibility(View.VISIBLE);
                }
                holder.selectView.setVisibility(isFeed ? View.VISIBLE : View.GONE);
                holder.selectCheckbox.setChecked((isSelected(position)));
                holder.selectCheckbox.setOnCheckedChangeListener((buttonView, isChecked)
                        -> setSelected(holder.getBindingAdapterPosition(), isChecked));
                holder.count.setVisibility(View.GONE);
            } else {
                holder.itemView.setBackgroundResource(android.R.color.transparent);
                if (isSelected(position)) {
                    holder.itemView.setBackgroundColor(0x88000000
                            + (0xffffff & ThemeUtils.getColorFromAttr(mainActivityRef.get(), R.attr.colorAccent)));
                }
            }
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
            if (holder.selectView != null) {
                holder.selectCheckbox.setVisibility(View.GONE);
                holder.selectView.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (!inActionMode()) {
                if (isFeed) {
                    longPressedPosition = holder.getBindingAdapterPosition();
                }
                selectedItem = drawerItem;
            }
            return false;
        });

        holder.itemView.setOnTouchListener((v, e) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (e.isFromSource(InputDevice.SOURCE_MOUSE)
                        &&  e.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    if (!inActionMode()) {
                        if (isFeed) {
                            longPressedPosition = holder.getBindingAdapterPosition();
                        }
                        selectedItem = drawerItem;
                    }
                }
            }
            return false;
        });
        holder.itemView.setOnClickListener(v -> {
            if (isFeed) {
                if (inActionMode()) {
                    setSelected(holder.getBindingAdapterPosition(), !isSelected(holder.getBindingAdapterPosition()));
                    notifyItemChanged(holder.getBindingAdapterPosition());
                } else {
                    Fragment fragment = FeedItemlistFragment
                            .newInstance(((NavDrawerData.FeedDrawerItem) drawerItem).feed.getId());
                    mainActivityRef.get().loadChildFragment(fragment);
                }
            } else if (!inActionMode()) {
                Fragment fragment = SubscriptionFragment.newInstance(drawerItem.getTitle());
                mainActivityRef.get().loadChildFragment(fragment);
            }
        });

    }

    @Override
    public int getItemCount() {
        return listItems.size();
    }

    @Override
    public long getItemId(int position) {
        if (position >= listItems.size()) {
            return RecyclerView.NO_ID; // Dummy views
        }
        return listItems.get(position).id;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (inActionMode() || selectedItem == null) {
            return;
        }
        MenuInflater inflater = mainActivityRef.get().getMenuInflater();
        if (selectedItem.type == NavDrawerData.DrawerItem.Type.FEED) {
            inflater.inflate(R.menu.nav_feed_context, menu);
            menu.findItem(R.id.multi_select).setVisible(true);
        } else {
            inflater.inflate(R.menu.nav_folder_context, menu);
        }
        menu.setHeaderTitle(selectedItem.getTitle());
    }

    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.multi_select) {
            startSelectMode(longPressedPosition);
            return true;
        }
        return false;
    }

    public List<Feed> getSelectedItems() {
        List<Feed> items = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            if (isSelected(i)) {
                NavDrawerData.DrawerItem drawerItem = listItems.get(i);
                if (drawerItem.type == NavDrawerData.DrawerItem.Type.FEED) {
                    Feed feed = ((NavDrawerData.FeedDrawerItem) drawerItem).feed;
                    items.add(feed);
                }
            }
        }
        return items;
    }

    public void setItems(List<NavDrawerData.DrawerItem> listItems) {
        this.listItems = listItems;
        notifyDataSetChanged();
    }

    @Override
    public void setSelected(int pos, boolean selected) {
        NavDrawerData.DrawerItem drawerItem = listItems.get(pos);
        if (drawerItem.type == NavDrawerData.DrawerItem.Type.FEED) {
            super.setSelected(pos, selected);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (columnCount == 1) {
            return R.id.view_type_subscription_list;
        } else if (UserPreferences.shouldShowSubscriptionTitle()) {
            return R.id.view_type_subscription_grid_with_title;
        } else {
            return R.id.view_type_subscription_grid_without_title;
        }
    }

    public static float convertDpToPixel(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    public static class GridDividerItemDecorator extends RecyclerView.ItemDecoration {
        @Override
        public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.onDraw(c, parent, state);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect,
                                   @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            Context context = parent.getContext();
            int insetOffset = (int) convertDpToPixel(context, 1f);
            outRect.set(insetOffset, insetOffset, insetOffset, insetOffset);
        }
    }
}
