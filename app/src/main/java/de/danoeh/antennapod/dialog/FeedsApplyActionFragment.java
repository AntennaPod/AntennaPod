package de.danoeh.antennapod.dialog;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.widget.Toolbar;

import androidx.fragment.app.Fragment;

import com.leinardi.android.speeddial.SpeedDialView;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;

@SuppressLint("NewApi")
public class FeedsApplyActionFragment extends Fragment implements Toolbar.OnMenuItemClickListener {
    private static final String TAG = "FeedsApplyActionFragmen";
    public static final int ACTION_TEST = 0;


    private ListView mListView;
    private ArrayAdapter<String> mAdapter;
    private SpeedDialView mSpeedDialView;
    private androidx.appcompat.widget.Toolbar toolbar;


    private final List<Feed> feeds = new ArrayList<>();
    private final List<String> titles = new ArrayList<>();

    public static FeedsApplyActionFragment newInstance(List<Feed> feeds, int actions) {
        FeedsApplyActionFragment f = new FeedsApplyActionFragment();
        f.feeds.addAll(feeds);
        for (Feed feed : feeds) {

        }

        return f;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.feeds_apply_action_fragment, container, false);
        toolbar = view.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.feeds_apply_action_options);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        toolbar.setOnMenuItemClickListener(this);

        mListView = view.findViewById(android.R.id.list);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);


        for (Feed feed : feeds) {
            titles.add(feed.getTitle());
        }

        mAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.simple_list_item_multiple_choice_on_start, titles);
        mListView.setAdapter(mAdapter);
        return view;
    }


}
