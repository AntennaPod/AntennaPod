package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
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
public class SubscriptionsRecyclerAdapter extends RecyclerView.Adapter<SubscriptionsRecyclerAdapter.SubscriptionViewHolder> implements View.OnCreateContextMenuListener
{
    /** the position in the view that holds the add item; 0 is the first, -1 is the last position */
    private static final String TAG = "SubscriptionsAdapter";

    private final WeakReference<MainActivity> mainActivityRef;
    private final ItemAccess itemAccess;
    private Feed selectedFeed = null;
    public SubscriptionsRecyclerAdapter(MainActivity mainActivity, ItemAccess itemAccess) {
        this.mainActivityRef = new WeakReference<>(mainActivity);
        this.itemAccess = itemAccess;
    }


    public Object getItem(int position) {
        return itemAccess.getItem(position);
    }
//    @Override
//    public int getCount() {
//        return itemAccess.getCount();
//    }
//
//
//    @Override
//    public boolean hasStableIds() {
//        return true;
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return ((NavDrawerData.DrawerItem) getItem(position)).id;
//    }
//
//    @Override
//    public View getView(int position, View convertView, ViewGroup parent) {
//        Holder holder;
//
//        if (convertView == null) {
//            holder = new Holder();
//
//            LayoutInflater layoutInflater =
//                    (LayoutInflater) mainActivityRef.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            convertView = layoutInflater.inflate(R.layout.subscription_item, parent, false);
//            holder.feedTitle = convertView.findViewById(R.id.txtvTitle);
//            holder.imageView = convertView.findViewById(R.id.imgvCover);
//            holder.count = convertView.findViewById(R.id.triangleCountView);
//
//
//            convertView.setTag(holder);
//        } else {
//            holder = (Holder) convertView.getTag();
//        }
//
//        final NavDrawerData.DrawerItem drawerItem = (NavDrawerData.DrawerItem) getItem(position);
//        if (drawerItem == null) {
//            return null;
//        }
//
//        holder.feedTitle.setText(drawerItem.getTitle());
//        holder.imageView.setContentDescription(drawerItem.getTitle());
//        holder.feedTitle.setVisibility(View.VISIBLE);
//
//        // Fix TriangleLabelView corner for RTL
//        if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
//                == ViewCompat.LAYOUT_DIRECTION_RTL) {
//            holder.count.setCorner(TriangleLabelView.Corner.TOP_LEFT);
//        }
//
//        if (drawerItem.getCounter() > 0) {
//            holder.count.setPrimaryText(NumberFormat.getInstance().format(drawerItem.getCounter()));
//            holder.count.setVisibility(View.VISIBLE);
//        } else {
//            holder.count.setVisibility(View.GONE);
//        }
//
//        if (drawerItem.type == NavDrawerData.DrawerItem.Type.FEED) {
//            Feed feed = ((NavDrawerData.FeedDrawerItem) drawerItem).feed;
//            boolean textAndImageCombined = feed.isLocalFeed()
//                    && LocalFeedUpdater.getDefaultIconUrl(convertView.getContext()).equals(feed.getImageUrl());
//            new CoverLoader(mainActivityRef.get())
//                    .withUri(feed.getImageUrl())
//                    .withPlaceholderView(holder.feedTitle, textAndImageCombined)
//                    .withCoverView(holder.imageView)
//                    .load();
//        } else {
//            new CoverLoader(mainActivityRef.get())
//                    .withResource(R.drawable.ic_folder)
//                    .withPlaceholderView(holder.feedTitle, true)
//                    .withCoverView(holder.imageView)
//                    .load();
//        }
//        return convertView;
//    }

//    @Override
//    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        final NavDrawerData.DrawerItem drawerItem = (NavDrawerData.DrawerItem) getItem(position);
//        if (drawerItem == null) {
//            return;
//        }
//        if (drawerItem.type == NavDrawerData.DrawerItem.Type.FEED) {
//            Feed feed = ((NavDrawerData.FeedDrawerItem) drawerItem).feed;
//            Fragment fragment = FeedItemlistFragment.newInstance(feed.getId());
//            mainActivityRef.get().loadChildFragment(fragment);
//        } else if (drawerItem.type == NavDrawerData.DrawerItem.Type.FOLDER) {
//            Fragment fragment = SubscriptionFragment.newInstance(drawerItem.getTitle());
//            mainActivityRef.get().loadChildFragment(fragment);
//        }
//    }

    @NonNull
    @Override
    public SubscriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mainActivityRef.get()).inflate(R.layout.subscription_item, parent, false);
        SubscriptionViewHolder viewHolder = new SubscriptionViewHolder(itemView);

        return viewHolder;
    }


    @Override
    public void onBindViewHolder(@NonNull SubscriptionViewHolder holder, int position) {
        holder.onBind(itemAccess.getItem(position));
        holder.itemView.setOnCreateContextMenuListener(this);

    }

    @Override
    public int getItemCount() {
        return itemAccess.getCount();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        PopupMenu popup = new PopupMenu(v.getContext(), v);
        popup.getMenuInflater().inflate(R.menu.nav_feed_context, popup.getMenu());
//        popup.setOnMenuItemClickListener(this);
        popup.show();

//        if (menuInfo == null) {
//            return;
//        }
//        AdapterView.AdapterContextMenuInfo adapterInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
//        int position = adapterInfo.position;
//
//        NavDrawerData.DrawerItem selectedObject = (NavDrawerData.DrawerItem) getItem(position);
//
//        if (selectedObject.type == NavDrawerData.DrawerItem.Type.FEED) {
//            MenuInflater inflater = mainActivityRef.get().getMenuInflater();
//            inflater.inflate(R.menu.nav_feed_context, menu);
//           // selectedFeed = ((NavDrawerData.FeedDrawerItem) selectedObject).feed;
//        }
//        menu.setHeaderTitle(selectedObject.getTitle());
    }

    public interface ItemAccess {
        int getCount();

        NavDrawerData.DrawerItem getItem(int position);
    }

    class SubscriptionViewHolder extends RecyclerView.ViewHolder {
        private TextView feedTitle;
        private ImageView imageView;
        private TriangleLabelView count;
        public SubscriptionViewHolder(@NonNull View itemView) {
            super(itemView);
            feedTitle = itemView.findViewById(R.id.txtvTitle);
            imageView = itemView.findViewById(R.id.imgvCover);
            count = itemView.findViewById(R.id.triangleCountView);
        }

        public void onBind(NavDrawerData.DrawerItem drawerItem) {
            feedTitle.setText(drawerItem.getTitle());
            imageView.setContentDescription(drawerItem.getTitle());
            feedTitle.setVisibility(View.VISIBLE);

            if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                    ==ViewCompat.LAYOUT_DIRECTION_RTL) {
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
            itemView.setOnLongClickListener(v -> {
//                if (menuInfo == null) {
//                    return;
//                }
//                AdapterView.AdapterContextMenuInfo adapterInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
//                int position = adapterInfo.position;

                NavDrawerData.DrawerItem selectedObject = (NavDrawerData.DrawerItem) drawerItem;

                if (selectedObject.type == NavDrawerData.DrawerItem.Type.FEED) {
//                    MenuInflater inflater = mainActivityRef.get().getMenuInflater();
//                    inflater.inflate(R.menu.nav_feed_context, menu);
                    selectedFeed = ((NavDrawerData.FeedDrawerItem) selectedObject).feed;
                }
//                menu.setHeaderTitle(selectedObject.getTitle());
                return false;
            });
        }


    }



}
