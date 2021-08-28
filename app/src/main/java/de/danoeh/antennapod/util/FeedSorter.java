package de.danoeh.antennapod.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.NavDrawerData;

public class FeedSorter {
    public static List<NavDrawerData.DrawerItem> sortFeeds(List<NavDrawerData.DrawerItem> items) {
        Comparator<NavDrawerData.DrawerItem> comparator;
        int feedOrder = UserPreferences.getFeedOrder();
        if (feedOrder == UserPreferences.FEED_ORDER_COUNTER) {
            comparator = (lhs, rhs) -> {
                NavDrawerData.FeedDrawerItem _lhs = (NavDrawerData.FeedDrawerItem) lhs;
                NavDrawerData.FeedDrawerItem _rhs = (NavDrawerData.FeedDrawerItem) rhs;
                long counterLhs = _lhs.counter;
                long counterRhs = _rhs.counter;
                if (counterLhs > counterRhs) {
                    // reverse natural order: podcast with most unplayed episodes first
                    return -1;
                } else if (counterLhs == counterRhs) {
                    return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
                } else {
                    return 1;
                }
            };
        } else if (feedOrder == UserPreferences.FEED_ORDER_ALPHABETICAL) {
            comparator = (lhs, rhs) -> {
                String t1 = lhs.getTitle();
                String t2 = rhs.getTitle();
                if (t1 == null) {
                    return 1;
                } else if (t2 == null) {
                    return -1;
                } else {
                    return t1.compareToIgnoreCase(t2);
                }
            };
        } else if (feedOrder == UserPreferences.FEED_ORDER_MOST_PLAYED) {
            comparator = (lhs, rhs) -> {
                NavDrawerData.FeedDrawerItem _lhs = (NavDrawerData.FeedDrawerItem) lhs;
                NavDrawerData.FeedDrawerItem _rhs = (NavDrawerData.FeedDrawerItem) rhs;
                long counterLhs = _lhs.counter;
                long counterRhs = _rhs.counter;
                if (counterLhs > counterRhs) {
                    // podcast with most played episodes first
                    return -1;
                } else if (counterLhs == counterRhs) {
                    return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
                } else {
                    return 1;
                }
            };
        } else {
            comparator = (lhs, rhs) -> {
                NavDrawerData.FeedDrawerItem _lhs = (NavDrawerData.FeedDrawerItem) lhs;
                NavDrawerData.FeedDrawerItem _rhs = (NavDrawerData.FeedDrawerItem) rhs;
                long dateLhs = _lhs.mostRecentPubDate > 0 ? _lhs.mostRecentPubDate : 0;
                long dateRhs = _rhs.mostRecentPubDate > 0 ? _rhs.mostRecentPubDate : 0;
                return Long.compare(dateRhs, dateLhs);
            };
        }

        Collections.sort(items, comparator);
        return items;
    }
}
