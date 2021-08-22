package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.LocalFeedUpdater;
import de.danoeh.antennapod.core.storage.NavDrawerData;
import de.danoeh.antennapod.fragment.FeedItemlistFragment;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import de.danoeh.antennapod.model.feed.Feed;
import jp.shts.android.library.TriangleLabelView;

/**
 * Adapter for subscriptions
 */
public class SubscriptionsRecyclerAdapter
        extends SelectableAdapter<SubscriptionsRecyclerAdapter.SubscriptionViewHolder>
        implements View.OnCreateContextMenuListener,
        FeedsItemMoveCallback.ItemTouchHelperContract {
    private static final String TAG = "SubscriptionsRecyclerAdapter";
    private final WeakReference<MainActivity> mainActivityRef;
    private List<NavDrawerData.DrawerItem> listItems;
    private Feed selectedFeed = null;
    int longPressedPosition = 0; // used to init actionMode

    // Experimental
    private boolean dragNDropMode = false;
    private StartDragListener startDragListener;
    public void setDragNDropMode(boolean dragNDropMode) {
        this.dragNDropMode = dragNDropMode;
    }

    public boolean isDragNDropMode() {
        return dragNDropMode;
    }

    public SubscriptionsRecyclerAdapter(MainActivity mainActivity) {
        super(mainActivity);
        this.mainActivityRef = new WeakReference<>(mainActivity);
        this.listItems = new ArrayList<>();
        setHasStableIds(true);
    }

    public Object getItem(int position) {
        return listItems.get(position);
    }

    public Feed getSelectedFeed() {
        return selectedFeed;
    }

    @NonNull
    @Override
    public SubscriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mainActivityRef.get()).inflate(R.layout.subscription_item, parent, false);
        return new SubscriptionViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SubscriptionViewHolder holder, int position) {
        NavDrawerData.DrawerItem drawerItem = listItems.get(position);
        boolean isFeed = drawerItem.type == NavDrawerData.DrawerItem.Type.FEED;
        holder.bind(drawerItem);
        if(!dragNDropMode) {
            holder.itemView.setOnCreateContextMenuListener(this);
            holder.selectView.setVisibility(View.GONE);
            holder.dragHandle.setVisibility(View.GONE);
            holder.dragHandle.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startDragListener.requestDrag(holder);
                }
                return false;
            });
        }
        else {
            holder.itemView.setOnCreateContextMenuListener(null);
            holder.selectView.setVisibility(View.VISIBLE);
            holder.dragHandle.setVisibility(View.VISIBLE);
            holder.selectCheckbox.setVisibility(View.GONE);
        }

        if (inActionMode()) {
            if (isFeed) {
                holder.selectCheckbox.setVisibility(View.VISIBLE);
                holder.selectView.setVisibility(View.VISIBLE);
            }
            holder.selectCheckbox.setChecked((isSelected(position)));
            holder.selectCheckbox.setOnCheckedChangeListener((buttonView, isChecked)
                    -> setSelected(holder.getBindingAdapterPosition(), isChecked));
            holder.imageView.setAlpha(0.6f);
            holder.count.setVisibility(View.GONE);
        } else {
            if(!dragNDropMode)
            holder.selectView.setVisibility(View.GONE);
            holder.imageView.setAlpha(1.0f);
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (!inActionMode()) {
                if (isFeed) {
                    selectedFeed = ((NavDrawerData.FeedDrawerItem) getItem(holder.getBindingAdapterPosition())).feed;
                    longPressedPosition = holder.getBindingAdapterPosition();
                } else {
                    selectedFeed = null;
                }
            }
            return false;
        });

        holder.itemView.setOnClickListener(v -> {
            if (isFeed) {
                if (inActionMode()) {
                    holder.selectCheckbox.setChecked(!isSelected(holder.getBindingAdapterPosition()));
                } else if(!isDragNDropMode()) {
                    Fragment fragment = FeedItemlistFragment.newInstance(((NavDrawerData.FeedDrawerItem) drawerItem).feed.getId());
                            mainActivityRef.get().loadChildFragment(fragment);
                }else  {
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
        return listItems.get(position).id;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (selectedFeed != null && !inActionMode()) {
            MenuInflater inflater = mainActivityRef.get().getMenuInflater();
            inflater.inflate(R.menu.nav_feed_context, menu);
            menu.setHeaderTitle(selectedFeed.getTitle());
        }
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
    }

    @Override
    public void setSelected(int pos, boolean selected) {
        NavDrawerData.DrawerItem drawerItem = listItems.get(pos);
        if (drawerItem.type == NavDrawerData.DrawerItem.Type.FEED) {
            super.setSelected(pos, selected);
        }
    }

    public class SubscriptionViewHolder extends RecyclerView.ViewHolder {
        private TextView feedTitle;
        private ImageView imageView;
        private TriangleLabelView count;
        private FrameLayout selectView;
        private CheckBox selectCheckbox;
        private ImageView dragHandle;
        public SubscriptionViewHolder(@NonNull View itemView) {
            super(itemView);
            feedTitle = itemView.findViewById(R.id.txtvTitle);
            imageView = itemView.findViewById(R.id.imgvCover);
            count = itemView.findViewById(R.id.triangleCountView);
            selectView = itemView.findViewById(R.id.selectView);
            selectCheckbox = itemView.findViewById(R.id.selectCheckBox);
            dragHandle = itemView.findViewById(R.id.dragHandle);
        }

        public void bind(NavDrawerData.DrawerItem drawerItem) {
            feedTitle.setText(drawerItem.getTitle());
            imageView.setContentDescription(drawerItem.getTitle());
            feedTitle.setVisibility(View.VISIBLE);
            if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                    == ViewCompat.LAYOUT_DIRECTION_RTL) {
                count.setCorner(TriangleLabelView.Corner.TOP_LEFT);
            }

            if (drawerItem.getCounter() > 0) {
                count.setPrimaryText(NumberFormat.getInstance().format(drawerItem.getCounter()));
                count.setVisibility(View.VISIBLE);
            } else {
                count.setVisibility(View.GONE);
            }

            if (drawerItem.type == NavDrawerData.DrawerItem.Type.FEED) {
                Feed feed = ((NavDrawerData.FeedDrawerItem) drawerItem).feed;
                boolean textAndImageCombind = feed.isLocalFeed()
                        && LocalFeedUpdater.getDefaultIconUrl(itemView.getContext()).equals(feed.getImageUrl());
                new CoverLoader(mainActivityRef.get())
                        .withUri(feed.getImageUrl())
                        .withPlaceholderView(feedTitle, textAndImageCombind)
                        .withCoverView(imageView)
                        .load();
            } else {
                new CoverLoader(mainActivityRef.get())
                        .withResource(R.drawable.ic_folder)
                        .withPlaceholderView(feedTitle, true)
                        .withCoverView(imageView)
                        .load();
            }
        }
    }

    public static float convertDpToPixel(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        Log.d("RowMoved", "from position " + fromPosition + " to position " + toPosition);
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(listItems, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(listItems, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onRowSelected(RecyclerView.ViewHolder myViewHolder) {
    }

    @Override
    public void onRowClear(RecyclerView.ViewHolder myViewHolder) {

    }

    public void setStartDragListener(StartDragListener startDragListener) {
        this.startDragListener = startDragListener;
    }

    public interface StartDragListener {
        void requestDrag(RecyclerView.ViewHolder viewHolder);
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
