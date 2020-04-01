package de.danoeh.antennapod.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;
import io.reactivex.annotations.Nullable;

public class EpisodeItemListRecyclerView extends RecyclerView {
    private static final String TAG = "EpisodeItemListRecyclerView";
    private static final String PREF_PREFIX_SCROLL_POSITION = "scroll_position_";
    private static final String PREF_PREFIX_SCROLL_OFFSET = "scroll_offset_";

    private LinearLayoutManager layoutManager;

    public EpisodeItemListRecyclerView(Context context) {
        super(context);
        setup();
    }

    public EpisodeItemListRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public EpisodeItemListRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private void setup() {
        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setRecycleChildrenOnDetach(true);
        setLayoutManager(layoutManager);
        setHasFixedSize(true);
        addItemDecoration(new HorizontalDividerItemDecoration.Builder(getContext()).build());
    }

    public void saveScrollPosition(String tag) {
        int firstItem = layoutManager.findFirstVisibleItemPosition();
        View firstItemView = layoutManager.findViewByPosition(firstItem);
        float topOffset;
        if (firstItemView == null) {
            topOffset = 0;
        } else {
            topOffset = firstItemView.getTop();
        }

        getContext().getSharedPreferences(TAG, Context.MODE_PRIVATE).edit()
                .putInt(PREF_PREFIX_SCROLL_POSITION + tag, firstItem)
                .putInt(PREF_PREFIX_SCROLL_OFFSET + tag, (int) topOffset)
                .apply();
    }

    public void restoreScrollPosition(String tag) {
        SharedPreferences prefs = getContext().getSharedPreferences(TAG, Context.MODE_PRIVATE);
        int position = prefs.getInt(PREF_PREFIX_SCROLL_POSITION + tag, 0);
        int offset = prefs.getInt(PREF_PREFIX_SCROLL_OFFSET + tag, 0);
        if (position > 0 || offset > 0) {
            layoutManager.scrollToPositionWithOffset(position, offset);
        }
    }

    public boolean isScrolledToBottom() {
        int visibleEpisodeCount = getChildCount();
        int totalEpisodeCount = layoutManager.getItemCount();
        int firstVisibleEpisode = layoutManager.findFirstVisibleItemPosition();
        return (totalEpisodeCount - visibleEpisodeCount) <= (firstVisibleEpisode + 3);
    }
}
