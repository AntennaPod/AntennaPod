package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.NavDrawerData;
import de.danoeh.antennapod.fragment.FeedItemlistFragment;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import de.danoeh.antennapod.model.feed.Feed;
import jp.shts.android.library.TriangleLabelView;

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

    public SubscriptionsRecyclerAdapter(MainActivity mainActivity) {
        super(mainActivity);
        this.mainActivityRef = new WeakReference<>(mainActivity);
        this.listItems = new ArrayList<>();
        setHasStableIds(true);
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
        TextView feedTitle = itemView.findViewById(R.id.txtvTitle);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) feedTitle.getLayoutParams();
        int topAndBottomItemId = R.id.imgvCover;
        int belowItemId = 0;

        if (viewType == COVER_WITH_TITLE) {
            topAndBottomItemId = 0;
            belowItemId = R.id.imgvCover;
            feedTitle.setBackgroundColor(feedTitle.getContext().getResources().getColor(R.color.feed_text_bg));
            int padding = (int) convertDpToPixel(feedTitle.getContext(), 6);
            feedTitle.setPadding(padding, padding, padding, padding);
        }
        params.addRule(RelativeLayout.BELOW, belowItemId);
        params.addRule(RelativeLayout.ALIGN_TOP, topAndBottomItemId);
        params.addRule(RelativeLayout.ALIGN_BOTTOM, topAndBottomItemId);
        feedTitle.setLayoutParams(params);
        feedTitle.setSingleLine(viewType == COVER_WITH_TITLE);
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
            holder.imageView.setAlpha(0.6f);
            holder.count.setVisibility(View.GONE);
        } else {
            holder.selectView.setVisibility(View.GONE);
            holder.imageView.setAlpha(1.0f);
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
        private final TextView feedTitle;
        private final ImageView imageView;
        private final TriangleLabelView count;
        private final FrameLayout selectView;
        private final CheckBox selectCheckbox;

        public SubscriptionViewHolder(@NonNull View itemView) {
            super(itemView);
            feedTitle = itemView.findViewById(R.id.txtvTitle);
            imageView = itemView.findViewById(R.id.imgvCover);
            count = itemView.findViewById(R.id.triangleCountView);
            selectView = itemView.findViewById(R.id.selectView);
            selectCheckbox = itemView.findViewById(R.id.selectCheckBox);
        }

        public void bind(NavDrawerData.DrawerItem drawerItem) {
            Drawable drawable = AppCompatResources.getDrawable(selectView.getContext(),
                    R.drawable.ic_checkbox_background);
            selectView.setBackground(drawable); // Setting this in XML crashes API <= 21
            feedTitle.setText(drawerItem.getTitle());
            imageView.setContentDescription(drawerItem.getTitle());
            feedTitle.setVisibility(View.VISIBLE);
            if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL) {
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
                        && feed.getImageUrl() != null && feed.getImageUrl().startsWith(Feed.PREFIX_GENERATIVE_COVER);
                new CoverLoader(mainActivityRef.get())
                        .withUri(feed.getImageUrl())
                        .withPlaceholderView(feedTitle, textAndImageCombind)
                        .withCoverView(imageView)
                        .load();
            } else {
                new CoverLoader(mainActivityRef.get())
                        .withResource(R.drawable.ic_tag)
                        .withPlaceholderView(feedTitle, true)
                        .withCoverView(imageView)
                        .load();
            }
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
