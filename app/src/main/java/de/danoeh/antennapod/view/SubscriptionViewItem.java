package de.danoeh.antennapod.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.utils.TimeUtils;

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

    public void setFeed(final Feed feed, int unreadCount) {
        mFeedTitle.setVisibility(VISIBLE);
        mFeedTitle.setText(feed.getTitle());

        Picasso.with(mContext).load(feed.getImageUri()).centerCrop().fit().into(mImageView, new Callback() {
            @Override
            public void onSuccess() {
                mFeedTitle.setVisibility(GONE);
            }

            @Override
            public void onError() {
            }
        });
        mUnreadCountText.setText(unreadCount + "");
        mTextTime.setText(TimeUtils.getTimeAgo(feed.getLastUpdate().getTime(), mContext));
    }

}
