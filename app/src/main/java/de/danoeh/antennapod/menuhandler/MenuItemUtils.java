package de.danoeh.antennapod.menuhandler;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.Menu;
import android.view.MenuItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;

/**
 * Utilities for menu items
 */
public class MenuItemUtils extends de.danoeh.antennapod.core.menuhandler.MenuItemUtils {

    @SuppressWarnings("ResourceType")
    public static void refreshLockItem(Context context, Menu menu) {
        final MenuItem queueLock = menu.findItem(de.danoeh.antennapod.R.id.queue_lock);
        int[] lockIcons = new int[] { de.danoeh.antennapod.R.attr.ic_lock_open, de.danoeh.antennapod.R.attr.ic_lock_closed };
        TypedArray ta = context.obtainStyledAttributes(lockIcons);
        if (UserPreferences.isQueueLocked()) {
            queueLock.setTitle(de.danoeh.antennapod.R.string.unlock_queue);
            queueLock.setIcon(ta.getDrawable(0));
        } else {
            queueLock.setTitle(de.danoeh.antennapod.R.string.lock_queue);
            queueLock.setIcon(ta.getDrawable(1));
        }
        ta.recycle();
    }

}
