package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;

/**
 * BaseAdapter for the navigation drawer
 */
public class NavListAdapter extends BaseAdapter {
    public static final int VIEW_TYPE_COUNT = 3;
    public static final int VIEW_TYPE_NAV = 0;
    public static final int VIEW_TYPE_SECTION_DIVIDER = 1;
    public static final int VIEW_TYPE_SUBSCRIPTION = 2;

    public static final int[] NAV_TITLES = {R.string.all_episodes_label, R.string.queue_label, R.string.downloads_label, R.string.playback_history_label, R.string.add_feed_label};

    private final Drawable[] drawables;

    public static final int SUBSCRIPTION_OFFSET = 1 + NAV_TITLES.length;

    private ItemAccess itemAccess;
    private Context context;

    public NavListAdapter(ItemAccess itemAccess, Context context) {
        this.itemAccess = itemAccess;
        this.context = context;

        TypedArray ta = context.obtainStyledAttributes(new int[]{R.attr.ic_new, R.attr.stat_playlist,
                R.attr.av_download, R.attr.ic_history, R.attr.content_new});
        drawables = new Drawable[]{ta.getDrawable(0), ta.getDrawable(1), ta.getDrawable(2),
                ta.getDrawable(3), ta.getDrawable(4)};
        ta.recycle();
    }

    @Override
    public int getCount() {
        return NAV_TITLES.length + 1 + itemAccess.getCount();
    }

    @Override
    public Object getItem(int position) {
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_NAV) {
            return context.getString(NAV_TITLES[position]);
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
        if (0 <= position && position < NAV_TITLES.length) {
            return VIEW_TYPE_NAV;
        } else if (position < NAV_TITLES.length + 1) {
            return VIEW_TYPE_SECTION_DIVIDER;
        } else {
            return VIEW_TYPE_SUBSCRIPTION;
        }
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);
        View v = null;
        if (viewType == VIEW_TYPE_NAV) {
            v = getNavView((String) getItem(position), position, convertView, parent);
        } else if (viewType == VIEW_TYPE_SECTION_DIVIDER) {
            v = getSectionDividerView(convertView, parent);
        } else {
            v = getFeedView(position - SUBSCRIPTION_OFFSET, convertView, parent);
        }
        if (v != null && viewType != VIEW_TYPE_SECTION_DIVIDER) {
            TextView txtvTitle = (TextView) v.findViewById(R.id.txtvTitle);
            if (position == itemAccess.getSelectedItemIndex()) {
                txtvTitle.setTypeface(null, Typeface.BOLD);
            } else {
                txtvTitle.setTypeface(null, Typeface.NORMAL);
            }
        }
        return v;
    }

    private View getNavView(String title, int position, View convertView, ViewGroup parent) {
        NavHolder holder;
        if (convertView == null) {
            holder = new NavHolder();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.nav_listitem, parent, false);

            holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.count = (TextView) convertView.findViewById(R.id.txtvCount);
            holder.image = (ImageView) convertView.findViewById(R.id.imgvCover);
            convertView.setTag(holder);
        } else {
            holder = (NavHolder) convertView.getTag();
        }

        holder.title.setText(title);

        if (NAV_TITLES[position] == R.string.queue_label) {
            int queueSize = itemAccess.getQueueSize();
            if (queueSize > 0) {
                holder.count.setVisibility(View.VISIBLE);
                holder.count.setText(String.valueOf(queueSize));
            } else {
                holder.count.setVisibility(View.GONE);
            }
        } else if (NAV_TITLES[position] == R.string.all_episodes_label) {
            int unreadItems = itemAccess.getNumberOfUnreadItems();
            if (unreadItems > 0) {
                holder.count.setVisibility(View.VISIBLE);
                holder.count.setText(String.valueOf(unreadItems));
            } else {
                holder.count.setVisibility(View.GONE);
            }
        } else {
            holder.count.setVisibility(View.GONE);
        }

        holder.image.setImageDrawable(drawables[position]);

        return convertView;
    }

    private View getSectionDividerView(View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        convertView = inflater.inflate(R.layout.nav_section_item, parent, false);

        convertView.setEnabled(false);
        convertView.setOnClickListener(null);

        return convertView;
    }

    private View getFeedView(int feedPos, View convertView, ViewGroup parent) {
        FeedHolder holder;
        Feed feed = itemAccess.getItem(feedPos);

        if (convertView == null) {
            holder = new FeedHolder();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.nav_feedlistitem, parent, false);

            holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.image = (ImageView) convertView.findViewById(R.id.imgvCover);
            convertView.setTag(holder);
        } else {
            holder = (FeedHolder) convertView.getTag();
        }

        holder.title.setText(feed.getTitle());

        Picasso.with(context)
                .load(feed.getImageUri())
                .fit()
                .into(holder.image);

        return convertView;
    }

    static class NavHolder {
        TextView title;
        TextView count;
        ImageView image;
    }

    static class FeedHolder {
        TextView title;
        ImageView image;
    }


    public interface ItemAccess {
        public int getCount();

        public Feed getItem(int position);

        public int getSelectedItemIndex();

        public int getQueueSize();

        public int getNumberOfUnreadItems();
    }

}
