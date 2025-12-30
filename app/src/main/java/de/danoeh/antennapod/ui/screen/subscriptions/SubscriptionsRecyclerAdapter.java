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
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.SelectableAdapter;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.screen.feed.FeedItemlistFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapter for subscriptions
 */
public class SubscriptionsRecyclerAdapter extends SelectableAdapter<SubscriptionViewHolder>
        implements View.OnCreateContextMenuListener {
    private final WeakReference<MainActivity> mainActivityRef;
    private List<Feed> listItems;
    private Map<Long, Integer> feedCounters;
    private Feed selectedItem = null;
    int longPressedPosition = 0; // used to init actionMode
    private int columnCount = 3;

    public SubscriptionsRecyclerAdapter(MainActivity mainActivity) {
        super(mainActivity);
        this.mainActivityRef = new WeakReference<>(mainActivity);
        this.listItems = new ArrayList<>();
        this.feedCounters = Map.of();
        setHasStableIds(true);
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    public Object getItem(int position) {
        return listItems.get(position);
    }

    public Feed getSelectedItem() {
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
        Feed feed = listItems.get(position);
        holder.bind(feed, columnCount, feedCounters.containsKey(feed.getId()) ? feedCounters.get(feed.getId()) : 0);
        holder.itemView.setOnCreateContextMenuListener(this);
        int cardMargin = 0;
        if (inActionMode()) {
            if (holder.selectIcon != null) {
                holder.selectIcon.setVisibility(View.VISIBLE);
                holder.gradient.setVisibility(View.VISIBLE);
                holder.selectIcon.setImageResource(isSelected(position)
                        ? R.drawable.circle_checked : R.drawable.circle_unchecked);
                cardMargin = isSelected(position) ? (int) convertDpToPixel(
                        holder.itemView.getContext(), 12f) : 0;
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
            if (holder.selectIcon != null) {
                holder.selectIcon.setVisibility(View.GONE);
                holder.gradient.setVisibility(View.GONE);
            }
        }
        if (holder.selectIcon != null) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.card.getLayoutParams();
            params.leftMargin = cardMargin;
            params.topMargin = cardMargin;
            params.rightMargin = cardMargin;
            params.bottomMargin = cardMargin;
            holder.card.setLayoutParams(params);
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (!inActionMode()) {
                longPressedPosition = holder.getBindingAdapterPosition();
                selectedItem = feed;
            }
            return false;
        });

        holder.itemView.setOnTouchListener((v, e) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (e.isFromSource(InputDevice.SOURCE_MOUSE)
                        &&  e.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    if (!inActionMode()) {
                        longPressedPosition = holder.getBindingAdapterPosition();
                        selectedItem = feed;
                    }
                }
            }
            return false;
        });
        holder.itemView.setOnClickListener(v -> {
            if (inActionMode()) {
                setSelected(holder.getBindingAdapterPosition(), !isSelected(holder.getBindingAdapterPosition()));
                notifyItemChanged(holder.getBindingAdapterPosition());
            } else {
                Fragment fragment = FeedItemlistFragment.newInstance(feed.getId());
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
        return listItems.get(position).getId();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (inActionMode() || selectedItem == null) {
            return;
        }
        MenuInflater inflater = mainActivityRef.get().getMenuInflater();
        inflater.inflate(R.menu.nav_feed_context, menu);
        menu.findItem(R.id.multi_select).setVisible(true);
        menu.setHeaderTitle(selectedItem.getTitle());
    }

    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.multi_select) {
            startSelectMode(longPressedPosition);
            return true;
        }
        return false;
    }

    public Feed getLongPressedItem() {
        return listItems.get(longPressedPosition);
    }

    public List<Feed> getSelectedItems() {
        List<Feed> items = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            if (isSelected(i)) {
                items.add(listItems.get(i));
            }
        }
        return items;
    }

    public void setItems(List<Feed> listItems, Map<Long, Integer> feedCounters) {
        this.listItems = listItems;
        this.feedCounters = feedCounters;
        notifyDataSetChanged();
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
