package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.addisonelliott.segmentedbutton.SegmentedButtonGroup;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;

public class HomeFragment extends Fragment implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = "HomeFragment";
    private static final String PREF_NAME = "PrefHomeFragment";
    private static final String PREF_POSITION = "position";
    private static final String KEY_UP_ARROW = "up_arrow";
    public static final int QUICKFILTER_ALL = 0;
    public static final int QUICKFILTER_NEW = 1;
    private static final int QUICKFILTER_DOWNLOADED = 2;
    private static final int QUICKFILTER_FAV = 3;


    private boolean displayUpArrow;

    private Toolbar toolbar;
    private SegmentedButtonGroup floatingQuickFilter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.home_fragment, container, false);
        toolbar = rootView.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.episodes_label);
        toolbar.inflateMenu(R.menu.episodes);
        toolbar.setOnMenuItemClickListener(this);
        MenuItemUtils.setupSearchItem(toolbar.getMenu(), (MainActivity) getActivity(), 0, "");
        Menu menu = toolbar.getMenu();
        menu.findItem(R.id.add_podcast_item).setVisible(true);
        menu.findItem(R.id.mark_all_item).setVisible(true);
        menu.findItem(R.id.filter_items).setVisible(true);
        menu.findItem(R.id.paused_first_item).setVisible(true);
        menu.findItem(R.id.inbox_mode_item).setVisible(true);
        menu.findItem(R.id.mark_all_read_item).setVisible(false);
        menu.findItem(R.id.remove_all_new_flags_item).setVisible(true);
        menu.findItem(R.id.refresh_item).setVisible(false);
        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) requireActivity()).setupToolbarToggle(toolbar, displayUpArrow);

        floatingQuickFilter = rootView.findViewById(R.id.floatingFilter);
        floatingQuickFilter.setOnPositionChangedListener(new SegmentedButtonGroup.OnPositionChangedListener() {
            @Override
            public void onPositionChanged(int position) {
                AllEpisodesFragment child = (AllEpisodesFragment) getChildFragmentManager().getFragments().get(0);
                String newFilter;
                switch (position) {
                    default:
                    case QUICKFILTER_ALL:
                        newFilter = child.getPrefFilter();
                        break;
                    case QUICKFILTER_NEW:
                        newFilter = "unplayed";
                        break;
                    case QUICKFILTER_DOWNLOADED:
                        newFilter = "downloaded";
                        break;
                    case QUICKFILTER_FAV:
                        newFilter = "is_favorite";
                        break;
                }
                child.updateFeedItemFilter(newFilter,position==QUICKFILTER_NEW);
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        //wait for child fragment to load
        AllEpisodesFragment child = (AllEpisodesFragment) getChildFragmentManager().getFragments().get(0);
        child.setSwipeAction();
        child.loadMenuCheked(toolbar.getMenu());
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        setQuickFilterPosition(prefs.getInt(PREF_POSITION, QUICKFILTER_ALL));
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(PREF_POSITION, floatingQuickFilter.getPosition()).apply();
    }

    public void setQuickFilterPosition(int position){
        floatingQuickFilter.setPosition(position, false);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Fragment child = getChildFragmentManager().getFragments().get(0);
        if (item.getItemId() == R.id.add_podcast_item) {
            ((MainActivity) requireActivity()).loadFragment(AddFeedFragment.TAG, null);
            return true;
        } else if (child != null) {
            if (item.getItemId() == R.id.filter_items) {
                setQuickFilterPosition(QUICKFILTER_ALL);
            }
            return child.onOptionsItemSelected(item);
        }
        return false;
    }
}
