package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.ContextMenu;
import android.view.InputDevice;
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
import androidx.appcompat.content.res.AppCompatResources;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.elevation.SurfaceColors;
import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.NavDrawerData;
import de.danoeh.antennapod.fragment.FeedItemlistFragment;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import de.danoeh.antennapod.model.feed.Feed;

/**
 * Adapter for subscriptions
 */
public class SubscriptionsRecyclerAdapter extends SelectableAdapter<SubscriptionsRecyclerAdapter.SubscriptionViewHolder>
        implements View.OnCreateContextMenuListener {
    private static final int COVER_WITH_TITLE = 1;

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
        View itemView = LayoutInflater.from(mainActivityRef.get()).inflate(R.layout.subscription_item, parent, false);
        itemView.findViewById(R.id.titleLabel).setVisibility(viewType == COVER_WITH_TITLE ? View.VISIBLE : View.GONE);
        return new SubscriptionViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SubscriptionViewHolder holder, int position) {
        NavDrawerData.DrawerItem drawerItem = listItems.get(position);
        boolean isFeed = drawerItem.type == NavDrawerData.DrawerItem.Type.FEED;
        holder.bind(drawerItem);
        holder.itemView.setOnCreateContextMenuListener(this);
        if (inActionMode()) {
            if (isFeed) {
                holder.selectCheckbox.setVisibility(View.VISIBLE);
                holder.selectView.setVisibility(View.VISIBLE);
            }
            holder.selectCheckbox.setChecked((isSelected(position)));
            holder.selectCheckbox.setOnCheckedChangeListener((buttonView, isChecked)
                    -> setSelected(holder.getBindingAdapterPosition(), isChecked));
            holder.coverImage.setAlpha(0.6f);
            holder.count.setVisibility(View.GONE);
        } else {
            holder.selectView.setVisibility(View.GONE);
            holder.coverImage.setAlpha(1.0f);
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
                    holder.selectCheckbox.setChecked(!isSelected(holder.getBindingAdapterPosition()));
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
        return UserPreferences.shouldShowSubscriptionTitle() ? COVER_WITH_TITLE : 0;
    }

    public class SubscriptionViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final ImageView coverImage;
        private final TextView count;
        private final TextView fallbackTitle;
        private final FrameLayout selectView;
        private final CheckBox selectCheckbox;
        private final CardView card;

        public SubscriptionViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.titleLabel);
            coverImage = itemView.findViewById(R.id.coverImage);
            count = itemView.findViewById(R.id.countViewPill);
            fallbackTitle = itemView.findViewById(R.id.fallbackTitleLabel);
            selectView = itemView.findViewById(R.id.selectContainer);
            selectCheckbox = itemView.findViewById(R.id.selectCheckBox);
            card = itemView.findViewById(R.id.outerContainer);
        }

        public void bind(NavDrawerData.DrawerItem drawerItem) {
            Drawable drawable = AppCompatResources.getDrawable(selectView.getContext(),
                    R.drawable.ic_checkbox_background);
            selectView.setBackground(drawable); // Setting this in XML crashes API <= 21
            title.setText(drawerItem.getTitle());
            fallbackTitle.setText(drawerItem.getTitle());
            coverImage.setContentDescription(drawerItem.getTitle());
            if (drawerItem.getCounter() > 0) {
                count.setText(NumberFormat.getInstance().format(drawerItem.getCounter()));
                count.setVisibility(View.VISIBLE);
            } else {
                count.setVisibility(View.GONE);
            }

            CoverLoader coverLoader = new CoverLoader(mainActivityRef.get());
            boolean textAndImageCombined;
            if (drawerItem.type == NavDrawerData.DrawerItem.Type.FEED) {
                Feed feed = ((NavDrawerData.FeedDrawerItem) drawerItem).feed;
                textAndImageCombined = feed.isLocalFeed() && feed.getImageUrl() != null
                        && feed.getImageUrl().startsWith(Feed.PREFIX_GENERATIVE_COVER);
                coverLoader.withUri(feed.getImageUrl());
            } else {
                textAndImageCombined = true;
                coverLoader.withResource(R.drawable.ic_tag);
            }
            if (UserPreferences.shouldShowSubscriptionTitle()) {
                // No need for fallback title when already showing title
                fallbackTitle.setVisibility(View.GONE);
            } else {
                coverLoader.withPlaceholderView(fallbackTitle, textAndImageCombined);
            }
            coverLoader.withCoverView(coverImage);
            coverLoader.load();

            float density = mainActivityRef.get().getResources().getDisplayMetrics().density;
            card.setCardBackgroundColor(SurfaceColors.getColorForElevation(mainActivityRef.get(), 1 * density));

            int textPadding = columnCount <= 3 ? 16 : 8;
            title.setPadding(textPadding, textPadding, textPadding, textPadding);
            fallbackTitle.setPadding(textPadding, textPadding, textPadding, textPadding);

            int textSize = 14;
            if (columnCount == 3) {
                textSize = 15;
            } else if (columnCount == 2) {
                textSize = 16;
            }
            title.setTextSize(textSize);
            fallbackTitle.setTextSize(textSize);
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
