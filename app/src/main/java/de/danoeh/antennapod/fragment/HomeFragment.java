package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public class HomeFragment extends Fragment implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = "HomeFragment";
    private static final String PREF_NAME = "PrefHomeFragment";
    private static final String KEY_UP_ARROW = "up_arrow";

    private boolean displayUpArrow;

    private Toolbar toolbar;

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
        menu.findItem(R.id.mark_all_read_item).setVisible(true);
        menu.findItem(R.id.remove_all_new_flags_item).setVisible(false);
        menu.findItem(R.id.add_podcast_item).setVisible(true);
        menu.findItem(R.id.refresh_item).setVisible(false);
        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) requireActivity()).setupToolbarToggle(toolbar, displayUpArrow);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Fragment child = getChildFragmentManager().getFragments().get(0);
        if (item.getItemId() == R.id.add_podcast_item) {
            ((MainActivity) requireActivity()).loadFragment(AddFeedFragment.TAG, null);
            return true;
        } else if (child != null) {
            return child.onOptionsItemSelected(item);
        }
        return false;
    }
}
