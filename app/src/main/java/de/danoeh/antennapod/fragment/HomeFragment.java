package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.event.FeedItemEvent;
import de.danoeh.antennapod.core.event.PlaybackHistoryEvent;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
import de.danoeh.antennapod.core.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.fragment.homesections.HomeSection;
import de.danoeh.antennapod.fragment.homesections.InboxSection;
import de.danoeh.antennapod.fragment.homesections.QueueSection;
import de.danoeh.antennapod.fragment.homesections.StatisticsSection;
import de.danoeh.antennapod.fragment.homesections.SubsSection;
import de.danoeh.antennapod.fragment.homesections.SurpriseSection;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.model.feed.FeedItem;
import slush.AdapterAppliedResult;
import slush.Slush;

/**
 * Shows unread or recently published episodes
 */
public class HomeFragment extends Fragment implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = "HomeFragment";
    public static final String PREF_NAME = "PrefHomeFragment";
    public static final String PREF_SECTIONS = "PrefHomeSections";
    public static final String PREF_FRAGMENT = "PrefHomeFragment";

    FeedItem selectedItem = null;

    private final List<SectionTitle> defaultSections = Arrays.asList(
            new SectionTitle(QueueSection.TAG, false),
            new SectionTitle(InboxSection.TAG, false),
            new SectionTitle(StatisticsSection.TAG, false),
            new SectionTitle(SubsSection.TAG, false),
            new SectionTitle(SurpriseSection.TAG, false)
    );

    private static final String KEY_UP_ARROW = "up_arrow";
    private boolean displayUpArrow;

    Toolbar toolbar;

    LinearLayout homeContainer;
    FragmentContainerView fragmentContainer;
    View divider;

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

        MenuItemUtils.setupSearchItem(toolbar.getMenu(), (MainActivity) getActivity(), 0, "");

        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() > 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) requireActivity()).setupToolbarToggle(toolbar, displayUpArrow);

        loadSections();

        return root;
    }

    ArrayList<HomeSection> sections = new ArrayList<>();

    private void loadSections() {
        homeContainer.removeAllViews();
        sections.clear();

        for (SectionTitle s : getSectionsPrefs()) {
            HomeSection section;
            switch (s.tag) {
                case InboxSection.TAG:
                    section = new InboxSection(this);
                    break;
                default:
                case QueueSection.TAG:
                    section = new QueueSection(this);
                    break;
                case SubsSection.TAG:
                    section = new SubsSection(this);
                    break;
                case SurpriseSection.TAG:
                    section = new SurpriseSection(this);
                    break;
                case StatisticsSection.TAG:
                    section = new StatisticsSection(this);
                    break;
            }

            if (!s.hidden) {
                sections.add(section);
                section.addSectionTo(homeContainer);
            }
        }

        fillFragmentIfRoom();
    }

    private void fillFragmentIfRoom(){
        //only if enough free space
        if (homeContainer.getChildCount() <= 2) {
            Fragment fragment;
            SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            switch (prefs.getInt(PREF_FRAGMENT, 0)) {
                default:
                case 0: //Episodes
                    fragment = new EpisodesFragment(true);
                    break;
                case 1: //Inbox
                    fragment = new InboxFragment(true);
                    break;
                case 2: //Queue
                    fragment = new QueueFragment();
                    break;
            }
            getChildFragmentManager()
                    .beginTransaction().replace(R.id.homeFragmentContainer, fragment, PREF_FRAGMENT)
                    .commit();
            fragmentContainer.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
        }
    }

    private View searchBar() {
        //TODO
        return new View(requireContext());
    }

    private List<SectionTitle> getSectionsPrefs() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        return gson.fromJson(prefs.getString(PREF_SECTIONS, gson.toJson(defaultSections)),
                new TypeToken<List<SectionTitle>>() {}.getType());
    }

    private void saveSettings(List<SectionTitle> list){
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
                    openHomeDialog();
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

    private void openHomeDialog() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.home_label);

            LayoutInflater inflater = LayoutInflater.from(requireContext());
            View layout = inflater.inflate(R.layout.home_dialog, null, false);
            RecyclerView dialogRecyclerView = layout.findViewById(R.id.dialogRecyclerView);
            Spinner spinner = layout.findViewById(R.id.homeSpinner);
            String[] bottomHalfOptions = new String[]{
                    getString(R.string.episodes_label),
                    getString(R.string.inbox_label),
                    getString(R.string.queue_label)};
            spinner.setAdapter(
                    new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            bottomHalfOptions));
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    prefs.edit().putInt(PREF_FRAGMENT, i).apply();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            spinner.setSelection(prefs.getInt(PREF_FRAGMENT, 0));

            ArrayList<SectionTitle> list = new ArrayList<>(getSectionsPrefs());

            //enable only if 2 or less sections are selected
            //spinner.setEnabled(list.stream().filter(s -> s.hidden).count() <= 2);

            AdapterAppliedResult<SectionTitle> slush = new Slush.SingleType<SectionTitle>()
                    .setItemLayout(R.layout.home_dialog_item)
                    .setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false))
                    .setItems(list)
                    .onBind((view, sectionTitle) -> {
                        TextView title = view.findViewById(R.id.txtvSectionTitle);
                        CheckBox checkBox = view.findViewById(R.id.checkBox);
                        int res;
                        switch (sectionTitle.tag) {
                            case InboxSection.TAG:
                                res = R.string.new_title;
                                break;
                            default:
                            case QueueSection.TAG:
                                res = R.string.continue_title;
                                break;
                            case SubsSection.TAG:
                                res = R.string.rediscover_title;
                                break;
                            case SurpriseSection.TAG:
                                res = R.string.surprise_title;
                                break;
                            case StatisticsSection.TAG:
                                res = R.string.classics_title;
                                break;
                        }

                        title.setText(getString(res));
                        checkBox.setChecked(!sectionTitle.hidden);
                    })
                    .onItemClick((view, i) -> {
                        CheckBox checkBox = view.findViewById(R.id.checkBox);
                        list.get(i).toggleHidden();
                        checkBox.setChecked(!checkBox.isChecked());
                    })
                    .into(dialogRecyclerView);

            new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    //int activeSections = list.stream().filter(s -> s.hidden).count();
                    //min 1 section active
                    //if (activeSections > 1) {
                        slush.getItemListEditor().moveItem(viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                        Collections.swap(list, viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());

                        //spinner.setEnabled(activeSections <= 2);
                    //}
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }
            }).attachToRecyclerView(dialogRecyclerView);

            builder.setView(layout);

            builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
                saveSettings(list);
                reloadSections();
            });
            builder.setNeutralButton(R.string.reset, (dialog, which) -> {
                saveSettings(defaultSections);
                reloadSections();
            });
            builder.setNegativeButton(R.string.cancel_label, null);
            builder.create().show();
    }

    static class SectionTitle {
        String tag;
        boolean hidden;

        public SectionTitle(String tag, boolean hidden) {
            this.tag = tag;
            this.hidden = hidden;
        }

        public void toggleHidden() {
            hidden = !hidden;
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem menuItem) {
        if (selectedItem == null) {
            //can only happen if it comes from the fragment
            return getChildFragmentManager().findFragmentByTag(PREF_FRAGMENT).onContextItemSelected(menuItem);
        }
        final FeedItem item = selectedItem;
        //reset selectedItem so we know whether it came from the fragment next time
        selectedItem = null;
        return FeedItemMenuHandler.onMenuItemClicked(this, menuItem.getItemId(), item);
    }

    public void setSelectedItem(FeedItem feedItem) {
        selectedItem = feedItem;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) { updateSections(HomeSection.UpdateEvents.FEED_ITEM); }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) { updateSections(HomeSection.UpdateEvents.UNREAD); }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) { updateSections(HomeSection.UpdateEvents.QUEUE); }

    private void updateSections(HomeSection.UpdateEvents event) {
        TransitionManager.beginDelayedTransition(
                homeContainer,
                new ChangeBounds());

        for (HomeSection section: sections) {
            if (section.updateEvents.contains(event)) {
                section.updateItems(event);
            }
        }
    }
}
