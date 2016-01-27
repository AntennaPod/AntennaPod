package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.view.SubscriptionViewItem;

/**
 * Adapter for subscriptions
 */
public class SubscriptionsAdapter extends BaseAdapter {

    private NavListAdapter.ItemAccess mItemAccess;

    private Context mContext;

    public SubscriptionsAdapter(Context context, NavListAdapter.ItemAccess itemAccess) {
        mItemAccess = itemAccess;
        mContext = context;
    }

    public void setItemAccess(NavListAdapter.ItemAccess itemAccess) {
        mItemAccess = itemAccess;
    }

    @Override
    public int getCount() {
        return mItemAccess.getCount();
    }

    @Override
    public Object getItem(int position) {
        return mItemAccess.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        final Feed item = (Feed) getItem(position);
        if (item == null) return null;

        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater =
                    (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.subscription_item, parent, false);
            holder.itemView = (SubscriptionViewItem) convertView.findViewById(R.id.subscription_item);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.itemView.setFeed(item);
        return convertView;
    }

    static class Holder {
        SubscriptionViewItem itemView;
    }
}
