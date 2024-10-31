package de.danoeh.antennapod.ui.discovery;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import de.danoeh.antennapod.event.DiscoveryDefaultUpdateEvent;
import de.danoeh.antennapod.net.discovery.BuildConfig;
import de.danoeh.antennapod.net.discovery.PodcastIndexApi;
import de.danoeh.antennapod.net.discovery.PodcastIndexTrendingLoader;
import de.danoeh.antennapod.net.discovery.PodcastSearchResult;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter;
import de.danoeh.antennapod.ui.discovery.databinding.FragmentOnlineSearchBinding;
import de.danoeh.antennapod.ui.discovery.databinding.SelectLanguageDialogBinding;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Searches iTunes store for top podcasts and displays results in a list.
 */
public class DiscoveryFragment extends Fragment implements Toolbar.OnMenuItemClickListener {
    public static final String TAG = "DiscoveryFragment";
    private SharedPreferences prefs;

    private OnlineSearchAdapter adapter;
    private FragmentOnlineSearchBinding viewBinding;
    private List<PodcastSearchResult> searchResults;
    private List<PodcastSearchResult> topList;
    private Disposable disposable;
    private String language = "US";
    private int[] categories = null;
    private boolean hidden;
    private boolean needsConfirm;

    public DiscoveryFragment() {
        // Required empty public constructor
    }

    /**
     * Replace adapter data with provided search results from SearchTask.
     *
     * @param result List of Podcast objects containing search results
     */
    private void updateData(List<PodcastSearchResult> result) {
        this.searchResults = result;
        adapter.clear();
        if (result != null && !result.isEmpty()) {
            viewBinding.gridView.setVisibility(View.VISIBLE);
            viewBinding.empty.setVisibility(View.GONE);
            for (PodcastSearchResult p : result) {
                adapter.add(p);
            }
            adapter.notifyDataSetInvalidated();
        } else {
            viewBinding.gridView.setVisibility(View.GONE);
            viewBinding.empty.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getActivity().getSharedPreferences(PodcastIndexTrendingLoader.PREFS, Context.MODE_PRIVATE);
        language = prefs.getString(PodcastIndexTrendingLoader.PREF_KEY_LANGUAGE, Locale.getDefault().getLanguage());
        hidden = prefs.getBoolean(PodcastIndexTrendingLoader.PREF_KEY_HIDDEN_DISCOVERY_COUNTRY, false);
        needsConfirm = prefs.getBoolean(PodcastIndexTrendingLoader.PREF_KEY_NEEDS_CONFIRM, true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentOnlineSearchBinding.inflate(inflater);
        adapter = new OnlineSearchAdapter(getActivity(), new ArrayList<>());
        viewBinding.gridView.setAdapter(adapter);

        viewBinding.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        viewBinding.toolbar.inflateMenu(R.menu.countries_menu);
        MenuItem discoverHideItem = viewBinding.toolbar.getMenu().findItem(R.id.discover_hide_item);
        discoverHideItem.setChecked(hidden);
        viewBinding.toolbar.setOnMenuItemClickListener(this);

        viewBinding.gridView.setOnItemClickListener((parent, view1, position, id) -> {
            PodcastSearchResult podcast = searchResults.get(position);
            if (podcast.feedUrl == null) {
                return;
            }
            startActivity(new OnlineFeedviewActivityStarter(getContext(), podcast.feedUrl).getIntent());
        });
        loadToplist();
        return viewBinding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
        adapter = null;
    }

    private void loadToplist() {
        if (disposable != null) {
            disposable.dispose();
        }

        viewBinding.gridView.setVisibility(View.GONE);
        viewBinding.txtvError.setVisibility(View.GONE);
        viewBinding.butRetry.setVisibility(View.GONE);
        viewBinding.butRetry.setText(R.string.retry_label);
        viewBinding.empty.setVisibility(View.GONE);
        viewBinding.progressBar.setVisibility(View.VISIBLE);

        if (hidden) {
            viewBinding.gridView.setVisibility(View.GONE);
            viewBinding.txtvError.setVisibility(View.VISIBLE);
            viewBinding.txtvError.setText(getResources().getString(R.string.discover_is_hidden));
            viewBinding.butRetry.setVisibility(View.GONE);
            viewBinding.empty.setVisibility(View.GONE);
            viewBinding.progressBar.setVisibility(View.GONE);
            return;
        }
        //noinspection ConstantConditions
        if (BuildConfig.FLAVOR.equals("free") && needsConfirm) {
            viewBinding.txtvError.setVisibility(View.VISIBLE);
            viewBinding.txtvError.setText("");
            viewBinding.butRetry.setVisibility(View.VISIBLE);
            viewBinding.butRetry.setText(R.string.discover_confirm);
            viewBinding.butRetry.setOnClickListener(v -> {
                prefs.edit().putBoolean(PodcastIndexTrendingLoader.PREF_KEY_NEEDS_CONFIRM, false).apply();
                needsConfirm = false;
                loadToplist();
            });
            viewBinding.empty.setVisibility(View.GONE);
            viewBinding.progressBar.setVisibility(View.GONE);
            return;
        }

        setupCategoryUi();
        disposable = Observable.fromCallable(() ->
                        PodcastIndexTrendingLoader.loadTrending(language,
                                categories == null ? null : StringUtils.join(categories, ','),
                                Integer.MAX_VALUE, DBReader.getFeedList()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    podcasts -> {
                        viewBinding.progressBar.setVisibility(View.GONE);
                        topList = podcasts;
                        updateData(topList);
                    }, error -> {
                        Log.e(TAG, Log.getStackTraceString(error));
                        viewBinding.progressBar.setVisibility(View.GONE);
                        viewBinding.txtvError.setText(error.getMessage());
                        viewBinding.txtvError.setVisibility(View.VISIBLE);
                        viewBinding.butRetry.setOnClickListener(v -> loadToplist());
                        viewBinding.butRetry.setVisibility(View.VISIBLE);
                    });
    }

    private void setupCategoryUi() {
        viewBinding.categoriesContainer.removeAllViews();
        viewBinding.categoriesScrollView.scrollTo(0, 0);
        if (categories == null) {
            for (PodcastIndexApi.TopLevelCategory category : PodcastIndexApi.getTopLevelCategories()) {
                Chip chip = new Chip(getContext());
                chip.setText(category.name);
                chip.setOnClickListener(v -> loadCategories(category.subCategories));
                viewBinding.categoriesContainer.addView(chip);
            }
        } else if (categories.length > 1) {
            for (int category : categories) {
                Chip chip = new Chip(getContext());
                chip.setText(PodcastIndexApi.getCategoryName(category));
                chip.setOnClickListener(v -> loadCategories(new int[] {category}));
                viewBinding.categoriesContainer.addView(chip);
            }
        }
    }

    private void loadCategories(int[] categories) {
        this.categories = categories;
        loadToplist();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.discover_hide_item) {
            item.setChecked(!item.isChecked());
            hidden = item.isChecked();
            prefs.edit().putBoolean(PodcastIndexTrendingLoader.PREF_KEY_HIDDEN_DISCOVERY_COUNTRY, hidden).apply();

            EventBus.getDefault().post(new DiscoveryDefaultUpdateEvent());
            loadToplist();
            return true;
        } else if (itemId == R.id.discover_language_item) {
            LayoutInflater inflater = getLayoutInflater();
            SelectLanguageDialogBinding dialogBinding = SelectLanguageDialogBinding.inflate(inflater);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
            builder.setView(dialogBinding.getRoot());

            List<String> languagesArray = new ArrayList<>(Arrays.asList(Locale.getISOLanguages()));
            Map<String, String> languageNames = new HashMap<>();
            Map<String, String> languageCodes = new HashMap<>();
            for (String code : languagesArray) {
                String languageName = new Locale(code).getDisplayLanguage();
                languageNames.put(code, languageName);
                languageCodes.put(languageName, code);
            }

            List<String> languagesSort = new ArrayList<>(languageNames.values());
            Collections.sort(languagesSort);

            ArrayAdapter<String> dataAdapter =
                    new ArrayAdapter<>(this.getContext(), android.R.layout.simple_list_item_1, languagesSort);
            MaterialAutoCompleteTextView editText =
                    (MaterialAutoCompleteTextView) dialogBinding.textInput.getEditText();
            editText.setAdapter(dataAdapter);
            editText.setText(languageNames.get(language));
            editText.setOnClickListener(view -> {
                if (editText.getText().length() != 0) {
                    editText.setText("");
                    editText.postDelayed(editText::showDropDown, 100);
                }
            });
            editText.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    editText.setText("");
                    editText.postDelayed(editText::showDropDown, 100);
                }
            });

            builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                String languageName = editText.getText().toString();
                if (languageCodes.containsKey(languageName)) {
                    language = languageCodes.get(languageName);
                    MenuItem discoverHideItem = viewBinding.toolbar.getMenu().findItem(R.id.discover_hide_item);
                    discoverHideItem.setChecked(false);
                    hidden = false;
                }

                prefs.edit().putBoolean(PodcastIndexTrendingLoader.PREF_KEY_HIDDEN_DISCOVERY_COUNTRY, hidden).apply();
                prefs.edit().putString(PodcastIndexTrendingLoader.PREF_KEY_LANGUAGE, language).apply();

                EventBus.getDefault().post(new DiscoveryDefaultUpdateEvent());
                loadToplist();
            });
            builder.setNegativeButton(R.string.cancel_label, null);
            builder.show();
            return true;
        }
        return false;
    }
}
