package de.danoeh.antennapod.menuhandler;

import android.view.Menu;
import android.view.SubMenu;

import de.danoeh.antennapod.R;

public class SortMenuBuilder {

    public static int SORT_DATE_NEW_TO_OLD_ID = 1001;
    public static int SORT_DATE_OLD_TO_NEW_ID = 1002;

    public static void addSortMenu(Menu menu) {
        SubMenu sub = menu.addSubMenu(R.string.sort);
        sub.add(0, SORT_DATE_NEW_TO_OLD_ID, Menu.NONE, R.string.sort_date_new_old);
        sub.add(0, SORT_DATE_OLD_TO_NEW_ID, Menu.NONE, R.string.sort_date_old_new);
    }
}
