package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public class MarkFavouriteSwipeAction implements SwipeAction {

    @Override
    public String id() {
        return MARK_FAV;
    }

    @Override
    public int actionIcon() {
        return R.drawable.ic_star;
    }

    @Override
    public int actionColor() {
        return R.attr.icon_yellow;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.add_to_favorite_label);
    }

    @Override
    public void action(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        DBWriter.toggleFavoriteItem(item);
    }

    @Override
    public boolean willRemove(FeedItemFilter filter) {
        return filter.showIsFavorite || filter.showNotFavorite;
    }
}
