package de.danoeh.antennapod.ui.screen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import org.apache.commons.lang3.Validate;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

/**
 * Shows all episodes from feeds that have a certain tag (possibly filtered by user).
 */
public class EpisodesByFeedTagFragment extends AllEpisodesFragment {
    public static final String TAG = "EpisodesByFeedTagFragment";
    private static final String ARGUMENT_FEED_TAG = "argument.de.danoeh.antennapod.feed_tag";
    private String feedTag;

    public static EpisodesByFeedTagFragment newInstance(String feedTag) {
        EpisodesByFeedTagFragment i = new EpisodesByFeedTagFragment();
        Bundle b = new Bundle();
        b.putString(ARGUMENT_FEED_TAG, feedTag);
        i.setArguments(b);
        return i;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        Bundle args = getArguments();
        Validate.notNull(args);
        feedTag = args.getString(ARGUMENT_FEED_TAG);
        toolbar.setTitle(getContext().getString(R.string.episodes_by_tag_label, feedTag));
        return root;
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        return DBReader.getEpisodes(0, page * EPISODES_PER_PAGE, getFilter(),
                UserPreferences.getAllEpisodesSortOrder(), feedTag);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData(int page) {
        return DBReader.getEpisodes((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE, getFilter(),
                UserPreferences.getAllEpisodesSortOrder(), feedTag);
    }

    @Override
    protected int loadTotalItemCount() {
        return DBReader.getEpisodesByFeedTagCount(feedTag, getFilter());
    }
}
