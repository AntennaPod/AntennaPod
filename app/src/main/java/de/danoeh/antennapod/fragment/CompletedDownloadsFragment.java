package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.DownloadedEpisodesListAdapter;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.dialog.EpisodesApplyActionFragment;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Displays all running downloads and provides a button to delete them
 */
public class CompletedDownloadsFragment extends ListFragment {

    private static final String TAG = CompletedDownloadsFragment.class.getSimpleName();

    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED |
            EventDistributor.DOWNLOADLOG_UPDATE |
            EventDistributor.UNREAD_ITEMS_UPDATE;

    private List<FeedItem> items;
    private DownloadedEpisodesListAdapter listAdapter;

    private boolean viewCreated = false;

    private Subscription subscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        loadItems();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventDistributor.getInstance().register(contentUpdate);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventDistributor.getInstance().unregister(contentUpdate);
        if(subscription != null) {
            subscription.unsubscribe();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(subscription != null) {
            subscription.unsubscribe();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listAdapter = null;
        viewCreated = false;
        if(subscription != null) {
            subscription.unsubscribe();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (viewCreated && items != null) {
            onFragmentLoaded();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // add padding
        final ListView lv = getListView();
        lv.setClipToPadding(false);
        final int vertPadding = getResources().getDimensionPixelSize(R.dimen.list_vertical_padding);
        lv.setPadding(0, vertPadding, 0, vertPadding);

        viewCreated = true;
        if (items != null && getActivity() != null) {
            onFragmentLoaded();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        position -= l.getHeaderViewsCount();
        long[] ids = FeedItemUtil.getIds(items);
        ((MainActivity) getActivity()).loadChildFragment(ItemFragment.newInstance(ids, position));
    }

    private void onFragmentLoaded() {
        if (listAdapter == null) {
            listAdapter = new DownloadedEpisodesListAdapter(getActivity(), itemAccess);
            setListAdapter(listAdapter);
        }
        setListShown(true);
        listAdapter.notifyDataSetChanged();
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(!isAdded()) {
            return;
        }
        super.onCreateOptionsMenu(menu, inflater);
        if(items != null) {
            inflater.inflate(R.menu.downloads_completed, menu);
            MenuItem episodeActions = menu.findItem(R.id.episode_actions);
            if(items.size() > 0) {
                int[] attrs = {R.attr.action_bar_icon_color};
                TypedArray ta = getActivity().obtainStyledAttributes(UserPreferences.getTheme(), attrs);
                int textColor = ta.getColor(0, Color.GRAY);
                ta.recycle();
                episodeActions.setIcon(new IconDrawable(getActivity(),
                        FontAwesomeIcons.fa_gears).color(textColor).actionBarSize());
                episodeActions.setVisible(true);
            } else {
                episodeActions.setVisible(false);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.episode_actions:
                EpisodesApplyActionFragment fragment = EpisodesApplyActionFragment
                        .newInstance(items, EpisodesApplyActionFragment.ACTION_REMOVE);
                ((MainActivity) getActivity()).loadChildFragment(fragment);
                return true;
            default:
                return false;
        }
    }

    private final DownloadedEpisodesListAdapter.ItemAccess itemAccess = new DownloadedEpisodesListAdapter.ItemAccess() {
        @Override
        public int getCount() {
            return (items != null) ? items.size() : 0;
        }

        @Override
        public FeedItem getItem(int position) {
            if (items != null && 0 <= position && position < items.size()) {
                return items.get(position);
            } else {
                return null;
            }
        }

        @Override
        public void onFeedItemSecondaryAction(FeedItem item) {
            DBWriter.deleteFeedMediaOfItem(getActivity(), item.getMedia().getId());
        }
    };

    private final EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & EVENTS) != 0) {
                loadItems();
            }
        }
    };

    private void loadItems() {
        if(subscription != null) {
            subscription.unsubscribe();
        }
        if (items == null && viewCreated) {
            setListShown(false);
        }
        subscription = Observable.fromCallable(DBReader::getDownloadedItems)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null) {
                        items = result;
                        if (viewCreated && getActivity() != null) {
                            onFragmentLoaded();
                        }
                    }
                }, error ->  Log.e(TAG, Log.getStackTraceString(error)));
    }

}
