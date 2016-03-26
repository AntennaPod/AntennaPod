package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import jp.shts.android.library.TriangleLabelView;

/**
 * Adapter for subscriptions
 */
public class SubscriptionsAdapter extends BaseAdapter {

    private NavListAdapter.ItemAccess itemAccess;

    private final Context context;

    public SubscriptionsAdapter(Context context, NavListAdapter.ItemAccess itemAccess) {
        this.itemAccess = itemAccess;
        this.context = context;
    }

    public void setItemAccess(NavListAdapter.ItemAccess itemAccess) {
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
    public long getItemId(int position) {
        return itemAccess.getItem(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        final Feed feed = (Feed) getItem(position);
        if (feed == null) return null;

        if (convertView == null) {
            holder = new Holder();

            LayoutInflater layoutInflater =
                    (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.subscription_item, parent, false);
            holder.feedTitle = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.imageView = (ImageView) convertView.findViewById(R.id.imgvCover);
            holder.count = (TriangleLabelView) convertView.findViewById(R.id.triangleCountView);


            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.feedTitle.setText(feed.getTitle());
        holder.count.setPrimaryText(String.valueOf(itemAccess.getFeedCounter(feed.getId())));
        Glide.with(context)
                .load(feed.getImageUri())
                .placeholder(R.color.light_gray)
                .error(R.color.light_gray)
                .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                .fitCenter()
                .dontAnimate()
                .listener(new RequestListener<Uri, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, Uri model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, Uri model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        holder.feedTitle.setVisibility(View.INVISIBLE);
                        return false;
                    }
                })
                .into(holder.imageView);

        return convertView;
    }

    static class Holder {
        public TextView feedTitle;
        public ImageView imageView;
        public TriangleLabelView count;
    }
}
