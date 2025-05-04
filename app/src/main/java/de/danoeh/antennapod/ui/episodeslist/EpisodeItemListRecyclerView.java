package de.danoeh.antennapod.ui.episodeslist;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;

public class EpisodeItemListRecyclerView extends RecyclerView {
    private static final String TAG = "EpisodeItemListRecyclerView";
    private static final String PREF_PREFIX_SCROLL_POSITION = "scroll_position_";
    private static final String PREF_PREFIX_SCROLL_OFFSET = "scroll_offset_";

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
        addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));
        setClipToPadding(false);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int horizontalSpacing = (int) getResources().getDimension(R.dimen.additional_horizontal_spacing);
        setPadding(horizontalSpacing, getPaddingTop(), horizontalSpacing, getPaddingBottom());
    }

    public boolean isScrolledToBottom() {
        int visibleEpisodeCount = getChildCount();
        int totalEpisodeCount = layoutManager.getItemCount();
        int firstVisibleEpisode = layoutManager.findFirstVisibleItemPosition();
        return (totalEpisodeCount - visibleEpisodeCount) <= (firstVisibleEpisode + 3);
    }
}
