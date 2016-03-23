package de.danoeh.antennapod.view;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;

/**
 * Custom view for handling feed item.
 */
public class SubscriptionViewItem extends RelativeLayout {

    private ImageView mImageView;
    private TextView mTextTime;
    private TextView mUnreadCountText;
    private TextView mFeedTitle;
    private Context mContext;

    public SubscriptionViewItem(Context context) {
        super(context);
        init(context);
    }

    public SubscriptionViewItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SubscriptionViewItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    private void init(Context context) {
        mContext = context;
        LayoutInflater mLayoutInflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = mLayoutInflater.inflate(R.layout.subscription_view, this);
        mTextTime = (TextView) view.findViewById(R.id.txtvTime);
        mFeedTitle = (TextView) view.findViewById(R.id.txtvTitle);
        mImageView = (ImageView) view.findViewById(R.id.imgvCover);
        mUnreadCountText = (TextView) view.findViewById(R.id.unread_count_text);
    }

    public void setFeed(Feed feed) {
        mFeedTitle.setVisibility(VISIBLE);
        mFeedTitle.setText(feed.getTitle());
        Glide.with(mContext)
                .load(feed.getImageUri())
                .listener(new RequestListener<Uri, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, Uri model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, Uri model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        mFeedTitle.setVisibility(INVISIBLE);
                        return false;
                    }
                })
                .centerCrop()
                .into(mImageView);
        // Removing the updated time. It could be the latest podcast updated time in the future.
        //mTextTime.setText(TimeUtils.getTimeAgo(feed.getLastUpdate().getTime(), mContext));
        mTextTime.setVisibility(GONE);

        // Could be the count of unread/ not played feed items
        //mUnreadCountText.setText(String.valueOf(feed.getNumOfItems()));
    }

}
