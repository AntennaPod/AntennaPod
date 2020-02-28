package de.danoeh.antennapod.adapter.actionbutton;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import androidx.annotation.AttrRes;
import androidx.annotation.StringRes;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;

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
    @AttrRes
    public int getDrawable() {
        return R.attr.location_web_site;
    }

    @Override
    public void onClick(Context context) {
        Uri uri = Uri.parse(item.getLink());
        context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    @Override
    public int getVisibility() {
        return (item.getLink() != null) ? View.INVISIBLE : View.VISIBLE;
    }
}
