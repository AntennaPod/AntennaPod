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

    private boolean displayUpArrow;

    private Toolbar toolbar;
    private SegmentedButtonGroup floatingFilter;

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
        menu.findItem(R.id.mark_all_read_item).setVisible(false);
        menu.findItem(R.id.remove_all_new_flags_item).setVisible(true);
        menu.findItem(R.id.refresh_item).setVisible(false);
        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) requireActivity()).setupToolbarToggle(toolbar, displayUpArrow);

        floatingFilter = rootView.findViewById(R.id.floatingFilter);
        floatingFilter.setOnPositionChangedListener(new SegmentedButtonGroup.OnPositionChangedListener() {
            @Override
            public void onPositionChanged(int position) {
                AllEpisodesFragment child = (AllEpisodesFragment) getChildFragmentManager().getFragments().get(0);
                String[] newFilter;
                switch (position) {
                    default:
                    case 0: //ALL
                        child.setPrefFilter();
                        child.loadItems();
                        return;
                    case 1: //NEW
                        newFilter = new String[] {"unplayed"};
                        break;
                    case 2: //DOWNL
                        newFilter = new String[] {"downloaded"};
                        break;
                    case 3: //FAV
                        newFilter = new String[] {"is_favorite"};
                        break;
                }
                child.updateFeedItemFilter(newFilter);
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
        ((AllEpisodesFragment) getChildFragmentManager().getFragments().get(0)).setSwipeAction();
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        floatingFilter.setPosition(prefs.getInt(PREF_POSITION, 0), false);
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(PREF_POSITION,floatingFilter.getPosition()).apply();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Fragment child = getChildFragmentManager().getFragments().get(0);
        if (item.getItemId() == R.id.add_podcast_item) {
            ((MainActivity) requireActivity()).loadFragment(AddFeedFragment.TAG, null);
            return true;
        } else if (child != null) {
            if (item.getItemId() == R.id.filter_items) {
                floatingFilter.setPosition(0, true);
            }
            return child.onOptionsItemSelected(item);
        }
        return false;
    }
}
