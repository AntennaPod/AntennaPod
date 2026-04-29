package de.danoeh.antennapod.actionbutton;

import android.content.Context;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.ui.common.IntentUtils;

public class VisitWebsiteActionButton extends ItemActionButton {

    public VisitWebsiteActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.visit_website_label;
    }

    @Override
    @DrawableRes
    public int getDrawable() {
        return R.drawable.ic_web;
    }

    @Override
    public void onClick(Context context) {
        if (item.getFeed() != null && !item.getFeed().isVerified()) {
            new MaterialAlertDialogBuilder(context)
                    .setTitle("External Link Warning")
                    .setMessage("You are leaving AntennaPod to visit an untrusted site. "
                            + "Never enter personal information on pages you don't trust.")
                    .setPositiveButton("Visit Site", (d, w) -> IntentUtils.openInBrowser(context, item.getLink()))
                    .setNegativeButton("Go Back", null)
                    .show();
        } else {
            IntentUtils.openInBrowser(context, item.getLink());
        }
    }

    @Override
    public int getVisibility() {
        return (item.getLink() == null) ? View.INVISIBLE : View.VISIBLE;
    }
}
