package de.danoeh.antennapod.ui.screen.subscriptions;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.ui.CoverLoader;
import de.danoeh.antennapod.ui.common.ThemeUtils;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;

public class SubscriptionViewHolder extends RecyclerView.ViewHolder {
    public final TextView title;
    public final ImageView coverImage;
    public final TextView count;
    public final FrameLayout selectView;
    public final CheckBox selectCheckbox;
    public final CardView card;
    public final View errorIcon;
    public final WeakReference<Activity> mainActivityRef;

    public SubscriptionViewHolder(@NonNull View itemView, Activity mainActivity) {
        super(itemView);
        title = itemView.findViewById(R.id.titleLabel);
        coverImage = itemView.findViewById(R.id.coverImage);
        count = itemView.findViewById(R.id.countViewPill);
        selectView = itemView.findViewById(R.id.selectContainer);
        selectCheckbox = itemView.findViewById(R.id.selectCheckBox);
        card = itemView.findViewById(R.id.outerContainer);
        errorIcon = itemView.findViewById(R.id.errorIcon);
        this.mainActivityRef = new WeakReference<>(mainActivity);
    }

    public void bind(Feed feed, int columnCount, int counter) {
        if (selectView != null) {
            Drawable drawable = AppCompatResources.getDrawable(selectView.getContext(),
                    R.drawable.ic_checkbox_background);
            selectView.setBackground(drawable); // Setting this in XML crashes API <= 21
        }
        title.setText(feed.getTitle());
        coverImage.setContentDescription(feed.getTitle());
        if (counter > 0) {
            count.setText(NumberFormat.getInstance().format(counter));
            count.setVisibility(View.VISIBLE);
        } else {
            count.setVisibility(View.GONE);
        }

        new CoverLoader(coverImage, CoverLoader.fromFeed(feed)).load();
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