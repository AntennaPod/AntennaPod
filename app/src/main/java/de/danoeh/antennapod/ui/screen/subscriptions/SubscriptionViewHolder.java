package de.danoeh.antennapod.ui.screen.subscriptions;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.ui.CoverLoaderHelper;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.glide.CoverLoader;

public class SubscriptionViewHolder extends RecyclerView.ViewHolder {
    public final TextView title;
    public final ImageView coverImage;
    public final TextView count;
    public final ImageView gradient;
    public final ImageView selectIcon;
    public final CardView card;
    public final View errorIcon;
    public final WeakReference<Activity> mainActivityRef;

    public SubscriptionViewHolder(@NonNull View itemView, Activity mainActivity) {
        super(itemView);
        title = itemView.findViewById(R.id.titleLabel);
        coverImage = itemView.findViewById(R.id.coverImage);
        count = itemView.findViewById(R.id.countViewPill);
        gradient = itemView.findViewById(R.id.gradientOverlay);
        selectIcon = itemView.findViewById(R.id.selectedIcon);
        card = itemView.findViewById(R.id.outerContainer);
        errorIcon = itemView.findViewById(R.id.errorIcon);
        this.mainActivityRef = new WeakReference<>(mainActivity);
    }

    public void bind(Feed feed, int columnCount, int counter) {
        title.setText(feed.getTitle());
        coverImage.setContentDescription(feed.getTitle());

        if (counter > 0) {
            count.setText(NumberFormat.getInstance().format(counter));
            count.setVisibility(View.VISIBLE);
        } else {
            count.setVisibility(View.GONE);
        }

        CoverLoader
                .with(coverImage, CoverLoaderHelper.fromFeed(feed))
                .into(coverImage);

        errorIcon.setVisibility(feed.hasLastUpdateFailed() ? View.VISIBLE : View.GONE);

        if (card != null) {
            card.setCardBackgroundColor(ThemeUtils.getColorFromAttr(
                    mainActivityRef.get(), R.attr.colorSurfaceContainer));
        }

        int textPadding = columnCount <= 3 ? 16 : 8;
        title.setPadding(textPadding, textPadding, textPadding, textPadding);

        int textSize = 14;
        if (columnCount == 3) {
            textSize = 15;
        } else if (columnCount == 2) {
            textSize = 16;
        }
        title.setTextSize(textSize);
    }
}