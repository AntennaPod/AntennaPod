package de.danoeh.antennapod.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.widget.IconTextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.fragment.AddFeedFragment;
import de.danoeh.antennapod.fragment.DownloadsFragment;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.NavDrawerFragment;
import de.danoeh.antennapod.fragment.PlaybackHistoryFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * BaseAdapter for the navigation drawer
 */
public class NavListAdapter extends BaseAdapter
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int VIEW_TYPE_COUNT = 3;
    public static final int VIEW_TYPE_NAV = 0;
    public static final int VIEW_TYPE_SECTION_DIVIDER = 1;
    private static final int VIEW_TYPE_SUBSCRIPTION = 2;

    /**
     * a tag used as a placeholder to indicate if the subscription list should be displayed or not
     * This tag doesn't correspond to any specific activity.
     */
    public static final String SUBSCRIPTION_LIST_TAG = "SubscriptionList";

    private static List<String> tags;
    private static String[] titles;

    private final ItemAccess itemAccess;
    private final WeakReference<Activity> activity;
    public boolean showSubscriptionList = true;

    public NavListAdapter(ItemAccess itemAccess, Activity context) {
        this.itemAccess = itemAccess;
        this.activity = new WeakReference<>(context);

        titles = context.getResources().getStringArray(R.array.nav_drawer_titles);
        loadItems();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (UserPreferences.PREF_HIDDEN_DRAWER_ITEMS.equals(key)) {
            loadItems();
        }
    }

    private void loadItems() {
        List<String> newTags = new ArrayList<>(Arrays.asList(NavDrawerFragment.NAV_DRAWER_TAGS));
        List<String> hiddenFragments = UserPreferences.getHiddenDrawerItems();
        newTags.removeAll(hiddenFragments);

        if (newTags.contains(SUBSCRIPTION_LIST_TAG)) {
            // we never want SUBSCRIPTION_LIST_TAG to be in 'tags'
            // since it doesn't actually correspond to a position in the list, but is
            // a placeholder that indicates if we should show the subscription list in the
            // nav drawer at all.
            showSubscriptionList = true;
            newTags.remove(SUBSCRIPTION_LIST_TAG);
        } else {
            showSubscriptionList = false;
        }

        tags = newTags;
        notifyDataSetChanged();
    }

    public String getLabel(String tag) {
        int index = ArrayUtils.indexOf(NavDrawerFragment.NAV_DRAWER_TAGS, tag);
        return titles[index];
    }

    private Drawable getDrawable(String tag) {
        Activity context = activity.get();
        if (context == null) {
            return null;
        }
        int icon;
        switch (tag) {
            case QueueFragment.TAG:
                icon = R.attr.stat_playlist;
                break;
            case EpisodesFragment.TAG:
                icon = R.attr.feed;
                break;
            case DownloadsFragment.TAG:
                icon = R.attr.av_download;
                break;
            case PlaybackHistoryFragment.TAG:
                icon = R.attr.ic_history;
                break;
            case SubscriptionFragment.TAG:
                icon = R.attr.ic_folder;
                break;
            case AddFeedFragment.TAG:
                icon = R.attr.content_new;
                break;
            default:
                return null;
        }
        TypedArray ta = context.obtainStyledAttributes(new int[] { icon } );
        Drawable result = ta.getDrawable(0);
        ta.recycle();
        return result;
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }


    @Override
    public int getCount() {
        int baseCount = getSubscriptionOffset();
        if (showSubscriptionList) {
            baseCount += itemAccess.getCount();
        }
        return baseCount;
    }

    @Override
    public Object getItem(int position) {
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_NAV) {
            return getLabel(tags.get(position));
        } else if (viewType == VIEW_TYPE_SECTION_DIVIDER) {
            return "";
        } else {
            return itemAccess.getItem(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (0 <= position && position < tags.size()) {
            return VIEW_TYPE_NAV;
        } else if (position < getSubscriptionOffset()) {
            return VIEW_TYPE_SECTION_DIVIDER;
        } else {
            return VIEW_TYPE_SUBSCRIPTION;
        }
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    public int getSubscriptionOffset() {
        return tags.size() > 0 ? tags.size() + 1 : 0;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);
        View v;
        if (viewType == VIEW_TYPE_NAV) {
            v = getNavView((String) getItem(position), position, convertView, parent);
        } else if (viewType == VIEW_TYPE_SECTION_DIVIDER) {
            v = getSectionDividerView(convertView, parent);
        } else {
            v = getFeedView(position, convertView, parent);
        }
        if (v != null && viewType != VIEW_TYPE_SECTION_DIVIDER) {
            TypedValue typedValue = new TypedValue();

            if (position == itemAccess.getSelectedItemIndex()) {
                v.getContext().getTheme().resolveAttribute(R.attr.drawer_activated_color, typedValue, true);
                v.setBackgroundResource(typedValue.resourceId);
            } else {
                v.getContext().getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
                v.setBackgroundResource(typedValue.resourceId);
            }
        }
        return v;
    }

    private View getNavView(String title, int position, View convertView, ViewGroup parent) {
        Activity context = activity.get();
        if(context == null) {
            return null;
        }
        NavHolder holder;
        if (convertView == null) {
            holder = new NavHolder();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.nav_listitem, parent, false);

            holder.image = convertView.findViewById(R.id.imgvCover);
            holder.title = convertView.findViewById(R.id.txtvTitle);
            holder.count = convertView.findViewById(R.id.txtvCount);
            convertView.setTag(holder);
        } else {
            holder = (NavHolder) convertView.getTag();
        }

        holder.title.setText(title);

        // reset for re-use
        holder.count.setVisibility(View.GONE);
        holder.count.setOnClickListener(null);

        String tag = tags.get(position);
        if (tag.equals(QueueFragment.TAG)) {
            int queueSize = itemAccess.getQueueSize();
            if (queueSize > 0) {
                holder.count.setText(NumberFormat.getInstance().format(queueSize));
                holder.count.setVisibility(View.VISIBLE);
            }
        } else if (tag.equals(EpisodesFragment.TAG)) {
            int unreadItems = itemAccess.getNumberOfNewItems();
            if (unreadItems > 0) {
                holder.count.setText(NumberFormat.getInstance().format(unreadItems));
                holder.count.setVisibility(View.VISIBLE);
            }
        } else if (tag.equals(SubscriptionFragment.TAG)) {
            int sum = itemAccess.getFeedCounterSum();
            if (sum > 0) {
                holder.count.setText(NumberFormat.getInstance().format(sum));
                holder.count.setVisibility(View.VISIBLE);
            }
        } else if(tag.equals(DownloadsFragment.TAG) && UserPreferences.isEnableAutodownload()) {
            int epCacheSize = UserPreferences.getEpisodeCacheSize();
            // don't count episodes that can be reclaimed
            int spaceUsed = itemAccess.getNumberOfDownloadedItems() -
                    itemAccess.getReclaimableItems();

            if (epCacheSize > 0 && spaceUsed >= epCacheSize) {
                holder.count.setText("{md-disc-full 150%}");
                Iconify.addIcons(holder.count);
                holder.count.setVisibility(View.VISIBLE);
                holder.count.setOnClickListener(v ->
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.episode_cache_full_title)
                            .setMessage(R.string.episode_cache_full_message)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                            .show()
                );
            }
        }

        holder.image.setImageDrawable(getDrawable(tags.get(position)));

        return convertView;
    }

    private View getSectionDividerView(View convertView, ViewGroup parent) {
        Activity context = activity.get();
        if(context == null) {
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        convertView = inflater.inflate(R.layout.nav_section_item, parent, false);
        TextView feedsFilteredMsg = convertView.findViewById(R.id.nav_feeds_filtered_message);

        if (UserPreferences.getSubscriptionsFilter().isEnabled() && showSubscriptionList) {
            convertView.setEnabled(true);
            feedsFilteredMsg.setText("{md-info-outline} " + context.getString(R.string.subscriptions_are_filtered));
            Iconify.addIcons(feedsFilteredMsg);
            feedsFilteredMsg.setVisibility(View.VISIBLE);
        } else {
            convertView.setEnabled(false);
            feedsFilteredMsg.setVisibility(View.GONE);
        }

        return convertView;
    }

    private View getFeedView(int position, View convertView, ViewGroup parent) {
        Activity context = activity.get();
        if(context == null) {
            return null;
        }
        int feedPos = position - getSubscriptionOffset();
        Feed feed = itemAccess.getItem(feedPos);

        FeedHolder holder;
        if (convertView == null) {
            holder = new FeedHolder();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.nav_listitem, parent, false);

            holder.image = convertView.findViewById(R.id.imgvCover);
            holder.title = convertView.findViewById(R.id.txtvTitle);
            holder.failure = convertView.findViewById(R.id.itxtvFailure);
            holder.count = convertView.findViewById(R.id.txtvCount);
            convertView.setTag(holder);
        } else {
            holder = (FeedHolder) convertView.getTag();
        }

        Glide.with(context)
                .load(feed.getImageLocation())
                .apply(new RequestOptions()
                    .placeholder(R.color.light_gray)
                    .error(R.color.light_gray)
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .fitCenter()
                    .dontAnimate())
                .into(holder.image);

        holder.title.setText(feed.getTitle());

        if(feed.hasLastUpdateFailed()) {
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) holder.title.getLayoutParams();
            p.addRule(RelativeLayout.LEFT_OF, R.id.itxtvFailure);
            holder.failure.setVisibility(View.VISIBLE);
        } else {
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) holder.title.getLayoutParams();
            p.addRule(RelativeLayout.LEFT_OF, R.id.txtvCount);
            holder.failure.setVisibility(View.GONE);
        }
        int counter = itemAccess.getFeedCounter(feed.getId());
        if(counter > 0) {
            holder.count.setVisibility(View.VISIBLE);
            holder.count.setText(NumberFormat.getInstance().format(counter));
        } else {
            holder.count.setVisibility(View.GONE);
        }
        return convertView;
    }

    static class NavHolder {
        ImageView image;
        TextView title;
        TextView count;
    }

    static class FeedHolder {
        ImageView image;
        TextView title;
        IconTextView failure;
        TextView count;
    }

    public interface ItemAccess {
        int getCount();
        Feed getItem(int position);
        int getSelectedItemIndex();
        int getQueueSize();
        int getNumberOfNewItems();
        int getNumberOfDownloadedItems();
        int getReclaimableItems();
        int getFeedCounter(long feedId);
        int getFeedCounterSum();
    }

}
