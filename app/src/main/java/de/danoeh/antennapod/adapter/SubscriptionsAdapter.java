package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.lang.ref.WeakReference;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.fragment.AddFeedFragment;
import de.danoeh.antennapod.fragment.ItemlistFragment;
import jp.shts.android.library.TriangleLabelView;

/**
 * Adapter for subscriptions
 */
public class SubscriptionsAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

    /** placeholder object that indicates item should be added */
    public static final Object ADD_ITEM_OBJ = new Object();

    /** the position in the view that holds the add item; 0 is the first, -1 is the last position */
    private static final int ADD_POSITION = -1;
    private static final String TAG = "SubscriptionsAdapter";

    private final WeakReference<MainActivity> mainActivityRef;
    private final ItemAccess itemAccess;

    public SubscriptionsAdapter(MainActivity mainActivity, ItemAccess itemAccess) {
        this.mainActivityRef = new WeakReference<>(mainActivity);
        this.itemAccess = itemAccess;
    }

    private int getAddTilePosition() {
        if(ADD_POSITION < 0) {
            return ADD_POSITION + getCount();
        }
        return ADD_POSITION;
    }

    private int getAdjustedPosition(int origPosition) {
        return origPosition < getAddTilePosition() ? origPosition : origPosition - 1;
    }

    @Override
    public int getCount() {
        return 1 + itemAccess.getCount();
    }

    @Override
    public Object getItem(int position) {
        if (position == getAddTilePosition()) {
            return ADD_ITEM_OBJ;
        }
        return itemAccess.getItem(getAdjustedPosition(position));
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        if (position == getAddTilePosition()) {
            return 0;
        }
        return itemAccess.getItem(getAdjustedPosition(position)).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;

        if (convertView == null) {
            holder = new Holder();

            LayoutInflater layoutInflater =
                    (LayoutInflater) mainActivityRef.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.subscription_item, parent, false);
            holder.feedTitle = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.imageView = (ImageView) convertView.findViewById(R.id.imgvCover);
            holder.count = (TriangleLabelView) convertView.findViewById(R.id.triangleCountView);


            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        if (position == getAddTilePosition()) {
            holder.feedTitle.setText("{md-add 500%}\n\n" + mainActivityRef.get().getString(R.string.add_feed_label));
                    holder.feedTitle.setVisibility(View.VISIBLE);
            // prevent any accidental re-use of old values (not sure how that would happen...)
            holder.count.setPrimaryText("");
            // make it go away, we don't need it for add feed
            holder.count.setVisibility(View.INVISIBLE);

            // when this holder is reused, we could else end up with a cover image
            Glide.clear(holder.imageView);

            return convertView;
        }

        final Feed feed = (Feed) getItem(position);
        if (feed == null) return null;

        holder.feedTitle.setText(feed.getTitle());
        holder.feedTitle.setVisibility(View.VISIBLE);
        int count = itemAccess.getFeedCounter(feed.getId());
        if(count > 0) {
            holder.count.setPrimaryText(String.valueOf(itemAccess.getFeedCounter(feed.getId())));
            holder.count.setVisibility(View.VISIBLE);
        } else {
            holder.count.setVisibility(View.GONE);
        }
        Glide.with(mainActivityRef.get())
                .load(feed.getImageLocation())
                .error(R.color.light_gray)
                .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                .fitCenter()
                .dontAnimate()
                .into(new CoverTarget(null, holder.feedTitle, holder.imageView, mainActivityRef.get()));

        return convertView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == getAddTilePosition()) {
            mainActivityRef.get().loadChildFragment(new AddFeedFragment());
        } else {
            Fragment fragment = ItemlistFragment.newInstance(getItemId(position));
            mainActivityRef.get().loadChildFragment(fragment);
        }
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
