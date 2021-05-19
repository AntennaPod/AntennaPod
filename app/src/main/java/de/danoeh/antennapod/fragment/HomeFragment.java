package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.FeedItemFilterGroup;
import de.danoeh.antennapod.fragment.homesections.InboxSection;
import de.danoeh.antennapod.fragment.homesections.QueueSection;
import de.danoeh.antennapod.fragment.homesections.SubsSection;
import de.danoeh.antennapod.fragment.homesections.SurpriseSection;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.ui.common.RecursiveRadioGroup;

/**
 * Shows unread or recently published episodes
 */
public class HomeFragment extends Fragment implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = "HomeFragment";
    public static final String PREF_NAME = "PrefHomeFragment";
    public static final String PREF_SECTIONS = "PrefHomeSections";

    FeedItem selectedItem = null;

    private final String[] defaultSections = new String[]{
            QueueSection.TAG,
            InboxSection.TAG,
            SubsSection.TAG,
            SurpriseSection.TAG};

    private static final String KEY_UP_ARROW = "up_arrow";
    private boolean displayUpArrow;

    Toolbar toolbar;

    LinearLayout homeContainer;
    FragmentContainerView fragmentContainer;
    View divider;

    String getPrefName() {
        return TAG;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.home_fragment, container, false);
        homeContainer = root.findViewById(R.id.homeContainer);
        fragmentContainer = root.findViewById(R.id.homeFragmentContainer);
        divider = root.findViewById(R.id.homeFragmentDivider);

        toolbar = root.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.home_label);
        toolbar.inflateMenu(R.menu.home);
        toolbar.setOnMenuItemClickListener(this);

        //TEST
        saveSettings(Arrays.asList(defaultSections));
        toolbar.setOnLongClickListener(view -> {
            ArrayList<String> list = new ArrayList<>();
            list.add(QueueSection.TAG);
            saveSettings(list);
            return true;
        });

        MenuItemUtils.setupSearchItem(toolbar.getMenu(), (MainActivity) getActivity(), 0, "");

        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) requireActivity()).setupToolbarToggle(toolbar, displayUpArrow);

        loadSections();

        return root;
    }

    private void loadSections() {
        homeContainer.removeAllViews();

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        ArrayList<String> sections = gson.fromJson(prefs.getString(PREF_SECTIONS, gson.toJson(defaultSections)),
                new TypeToken<ArrayList<String>>() {}.getType());

        for (String s : sections) {
            switch (s) {
                case InboxSection.TAG:
                    new InboxSection(this).addSectionTo(homeContainer);
                    break;
                case QueueSection.TAG:
                    new QueueSection(this).addSectionTo(homeContainer);
                    break;
                case SubsSection.TAG:
                    new SubsSection(this).addSectionTo(homeContainer);
                    break;
                case SurpriseSection.TAG:
                    new SurpriseSection(this).addSectionTo(homeContainer);
                    break;
            }
        }

        fillFragmentIfRoom();
    }

    private void fillFragmentIfRoom(){
        //only if enough free space
        if (homeContainer.getChildCount() <= 2) {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction().add(R.id.homeFragmentContainer, new PowerEpisodesFragment(true))
                    .commit();
            fragmentContainer.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
        }
    }

    private View searchBar() {
        //TODO
        return new View(requireContext());
    }

    private void saveSettings(List<String> list){
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        prefs.edit().putString(PREF_SECTIONS, gson.toJson(list)).apply();
        reloadSections();
    }

    private void reloadSections() {
        fragmentContainer.setVisibility(View.GONE);
        divider.setVisibility(View.GONE);

        loadSections();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (!super.onOptionsItemSelected(item)) {
            switch (item.getItemId()) {
                case R.id.add_podcast_item:
                    ((MainActivity) requireActivity()).loadFragment(AddFeedFragment.TAG, null);
                    return true;
                case R.id.homesettings_items:
                    new HomeSettingsDialog(requireContext()).openDialog();
                    return true;
                default:
                    return false;
            }
        }

        return true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }

    private static class HomeSettingsDialog {

        protected Context context;

        public HomeSettingsDialog(Context context) {
            this.context = context;
        }

        public void openDialog() {

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.home_label);

            LayoutInflater inflater = LayoutInflater.from(this.context);
            View layout = inflater.inflate(R.layout.filter_dialog, null, false);
            LinearLayout rows = layout.findViewById(R.id.filter_rows);
            builder.setView(layout);

            for (FeedItemFilterGroup item : FeedItemFilterGroup.values()) {
                RecursiveRadioGroup row = (RecursiveRadioGroup) inflater.inflate(R.layout.filter_dialog_row, null, false);
                RadioButton filter1 = row.findViewById(R.id.filter_dialog_radioButton1);
                RadioButton filter2 = row.findViewById(R.id.filter_dialog_radioButton2);
                filter1.setText(item.values[0].displayName);
                filter1.setTag(item.values[0].filterId);
                filter2.setText(item.values[1].displayName);
                filter2.setTag(item.values[1].filterId);
                rows.addView(row);
            }

            builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
                //TODO
            });
            builder.setNegativeButton(R.string.cancel_label, null);
            builder.create().show();
        }

    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        return FeedItemMenuHandler.onMenuItemClicked(this, item.getItemId(), selectedItem);
    }

    public void setSelectedItem(FeedItem feedItem) {
        selectedItem = feedItem;
    }
}
