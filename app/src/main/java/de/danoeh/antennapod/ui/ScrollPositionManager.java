package de.danoeh.antennapod.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Map;

public class ScrollPositionManager {

    private static final String PREF_PREFIX_SCROLL_POSITION = "scroll_position_";
    private static final String PREF_PREFIX_SCROLL_OFFSET = "scroll_offset_";

    private static final Map<String, Pair<Integer, Integer>> scrollPositions = new HashMap<>();

    public static Pair<Integer, Integer> getCurrentScrollPosition(RecyclerView view) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) view.getLayoutManager();
        int firstItem = layoutManager.findFirstVisibleItemPosition();
        View firstItemView = layoutManager.findViewByPosition(firstItem);
        int topOffset;
        if (firstItemView == null) {
            topOffset = 0;
        } else {
            topOffset = firstItemView.getTop();
        }

        return new Pair<>(firstItem, topOffset);
    }

    public static Pair<Integer, Integer> getStoredScrollPosition(String tag) {
        return scrollPositions.get(tag);
    }

    public static void initializeScrollPositionToTop(String tag) {
        storeScrollPosition(new Pair<>(0, 0), tag);
    }

    public static void storeScrollPosition(Pair<Integer, Integer> scrollPosition, String tag) {
        scrollPositions.put(tag, scrollPosition);
    }

    public static void storeCurrentScrollPosition(RecyclerView view, String tag) {
        Pair<Integer, Integer> scrollPosition = getCurrentScrollPosition(view);
        scrollPositions.put(tag, scrollPosition);
    }

    public static void saveCurrentScrollPositionToPrefs(Context context, RecyclerView view, String prefName, String tag) {
        Pair<Integer, Integer> scrollPosition = getCurrentScrollPosition(view);

        context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit()
                .putInt(PREF_PREFIX_SCROLL_POSITION + tag, scrollPosition.first)
                .putInt(PREF_PREFIX_SCROLL_OFFSET + tag, scrollPosition.second)
                .apply();
    }

    public static void restoreScrollPositionFromPrefs(Context context, RecyclerView view, String prefName, String tag) {
        SharedPreferences prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        int position = prefs.getInt(PREF_PREFIX_SCROLL_POSITION + tag, 0);
        int offset = prefs.getInt(PREF_PREFIX_SCROLL_OFFSET + tag, 0);

        LinearLayoutManager layoutManager = (LinearLayoutManager) view.getLayoutManager();
        layoutManager.scrollToPositionWithOffset(position, offset);

    }

    public static void restoreStoredScrollPosition(RecyclerView view, String tag) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) view.getLayoutManager();
        Pair<Integer, Integer> scrollPosition = getStoredScrollPosition(tag);
        layoutManager.scrollToPositionWithOffset(scrollPosition.first, scrollPosition.second);
    }

}
