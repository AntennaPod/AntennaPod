package de.danoeh.antennapod.adapter;

import android.content.Context;

import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.Locale;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.LocalFeedUpdater;
import de.danoeh.antennapod.fragment.FeedItemlistFragment;
import jp.shts.android.library.TriangleLabelView;

/**
 * Adapter for subscriptions
 */
public class SubscriptionsAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

    /** placeholder object that indicates item should be added */
    public static final Object ADD_ITEM_OBJ = new Object();

    /** the position in the view that holds the add item; 0 is the first, -1 is the last position */
    private static final String TAG = "SubscriptionsAdapter";

    private final WeakReference<MainActivity> mainActivityRef;
    private final ItemAccess itemAccess;

    public SubscriptionsAdapter(MainActivity mainActivity, ItemAccess itemAccess) {
        this.mainActivityRef = new WeakReference<>(mainActivity);
        this.itemAccess = itemAccess;
    }

    @Override
    public int getCount() {
        return itemAccess.getCount();
    }

    @Override
    public Object getItem(int position) {
        return itemAccess.getItem(position);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        return itemAccess.getItem(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;

        if (convertView == null) {
            holder = new Holder();

            LayoutInflater layoutInflater =
                    (LayoutInflater) mainActivityRef.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.subscription_item, parent, false);
            holder.feedTitle = convertView.findViewById(R.id.txtvTitle);
            holder.imageView = convertView.findViewById(R.id.imgvCover);
            holder.count = convertView.findViewById(R.id.triangleCountView);


            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        final Feed feed = (Feed) getItem(position);
        if (feed == null) return null;

        holder.feedTitle.setText(feed.getTitle());
        holder.imageView.setContentDescription(feed.getTitle());
        holder.feedTitle.setVisibility(View.VISIBLE);

        // Fix TriangleLabelView corner for RTL
        if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                == ViewCompat.LAYOUT_DIRECTION_RTL) {
            holder.count.setCorner(TriangleLabelView.Corner.TOP_LEFT);
        }

        int count = itemAccess.getFeedCounter(feed.getId());
        if(count > 0) {
            holder.count.setPrimaryText(
                    NumberFormat.getInstance().format(itemAccess.getFeedCounter(feed.getId())));
            holder.count.setVisibility(View.VISIBLE);
        } else {
            holder.count.setVisibility(View.GONE);
        }

        boolean textAndImageCombined = feed.isLocalFeed()
                && LocalFeedUpdater.getDefaultIconUrl(convertView.getContext()).equals(feed.getImageUrl());
        new CoverLoader(mainActivityRef.get())
                .withUri(feed.getImageLocation())
                .withPlaceholderView(holder.feedTitle, textAndImageCombined)
                .withCoverView(holder.imageView)
                .load();

        return convertView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Fragment fragment = FeedItemlistFragment.newInstance(getItemId(position));
        mainActivityRef.get().loadChildFragment(fragment);
    }

    static class Holder {
        public TextView feedTitle;
        public ImageView imageView;
        public TriangleLabelView count;
    }

    public interface ItemAccess {
        int getCount();
        Feed getItem(int position);
        int getFeedCounter(long feedId);
    }
}
