package de.danoeh.antennapod.ui.episodeslist;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;

public class EpisodeItemListRecyclerView extends RecyclerView {
    private LinearLayoutManager layoutManager;

    public EpisodeItemListRecyclerView(@NonNull Context context) {
        super(new ContextThemeWrapper(context, R.style.FastScrollRecyclerView));
        setup();
    }

    public EpisodeItemListRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(new ContextThemeWrapper(context, R.style.FastScrollRecyclerView), attrs);
        setup();
    }

    public EpisodeItemListRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(new ContextThemeWrapper(context, R.style.FastScrollRecyclerView), attrs, defStyleAttr);
        setup();
    }

    private void setup() {
        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setRecycleChildrenOnDetach(true);
        setLayoutManager(layoutManager);
        setHasFixedSize(true);
        setClipToPadding(false);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int horizontalSpacing = (int) getResources().getDimension(R.dimen.additional_horizontal_spacing);
        setPadding(horizontalSpacing, getPaddingTop(), horizontalSpacing, getPaddingBottom());
    }

    public Pair<Integer, Integer> getScrollPosition() {
        int firstItem = layoutManager.findFirstVisibleItemPosition();
        View firstItemView = layoutManager.findViewByPosition(firstItem);
        int topOffset = firstItemView == null ? 0 : firstItemView.getTop();
        return new Pair<>(firstItem, topOffset);
    }

    public void restoreScrollPosition(Pair<Integer, Integer> scrollPosition) {
        if (scrollPosition == null || (scrollPosition.first == 0 && scrollPosition.second == 0)) {
            return;
        }
        layoutManager.scrollToPositionWithOffset(scrollPosition.first, scrollPosition.second);
    }

    public boolean isScrolledToBottom() {
        int visibleEpisodeCount = getChildCount();
        int totalEpisodeCount = layoutManager.getItemCount();
        int firstVisibleEpisode = layoutManager.findFirstVisibleItemPosition();
        return (totalEpisodeCount - visibleEpisodeCount) <= (firstVisibleEpisode + 3);
    }
}
