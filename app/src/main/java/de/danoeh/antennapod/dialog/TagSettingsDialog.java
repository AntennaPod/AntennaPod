package de.danoeh.antennapod.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.NavDrawerData;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.databinding.EditTagsDialogBinding;
import de.danoeh.antennapod.view.ItemOffsetDecoration;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TagSettingsDialog extends DialogFragment {
    public static final String TAG = "TagSettingsDialog";
    private static final String ARG_FEED_PREFERENCES = "feed_preferences";
    private static final String ARG_MULTI_FEED_PREFERENCES = "multi_feed_preferences";
    private List<String> displayedTags;
    private EditTagsDialogBinding viewBinding;
    private TagSelectionAdapter adapter;

    private static TagSettingsDialog newInstance() {
        TagSettingsDialog fragment = new TagSettingsDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FEED_PREFERENCES, null);
        fragment.setArguments(args);
        return fragment;
    }

    public static TagSettingsDialog newInstance(FeedPreferences preferences) {
        TagSettingsDialog fragment = newInstance();
        fragment.getArguments().putSerializable(ARG_FEED_PREFERENCES, preferences);
        return fragment;
    }

    public static TagSettingsDialog newInstance(ArrayList<FeedPreferences> preferencesList) {
        TagSettingsDialog fragment = newInstance();
        fragment.getArguments().putSerializable(ARG_MULTI_FEED_PREFERENCES, preferencesList);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        List<FeedPreferences> feedPreferencesList = getFeedPreferences();
        Set<String> commonTags = new HashSet<>(feedPreferencesList.get(0).getTags());

        for (FeedPreferences preference : feedPreferencesList) {
            commonTags.retainAll(preference.getTags());
        }
        displayedTags = new ArrayList<>(commonTags);
        displayedTags.remove(FeedPreferences.TAG_ROOT);

        viewBinding = EditTagsDialogBinding.inflate(getLayoutInflater());
        viewBinding.tagsRecycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        viewBinding.tagsRecycler.addItemDecoration(new ItemOffsetDecoration(getContext(), 4));
        adapter = new TagSelectionAdapter();
        adapter.setHasStableIds(true);
        viewBinding.tagsRecycler.setAdapter(adapter);
        viewBinding.rootFolderCheckbox.setChecked(commonTags.contains(FeedPreferences.TAG_ROOT));

        viewBinding.newTagButton.setOnClickListener(v ->
                addTag(viewBinding.newTagEditText.getText().toString().trim()));

        loadTags();
        viewBinding.newTagEditText.setThreshold(1);
        viewBinding.newTagEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                viewBinding.newTagEditText.showDropDown();
                viewBinding.newTagEditText.requestFocus();
                return false;
            }
        });

        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setView(viewBinding.getRoot());
        dialog.setTitle(R.string.feed_tags_label);
        dialog.setPositiveButton(android.R.string.ok, (d, input) -> {
            addTag(viewBinding.newTagEditText.getText().toString().trim());
            updatePreferencesTags(feedPreferencesList, commonTags);
        });
        dialog.setNegativeButton(R.string.cancel_label, null);
        return dialog.create();
    }

    private void loadTags() {
        Observable.fromCallable(
                () -> {
                    NavDrawerData data = DBReader.getNavDrawerData();
                    List<NavDrawerData.DrawerItem> items = data.items;
                    List<String> folders = new ArrayList<String>();
                    for (NavDrawerData.DrawerItem item : items) {
                        if (item.type == NavDrawerData.DrawerItem.Type.TAG) {
                            folders.add(item.getTitle());
                        }
                    }
                    return folders;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            ArrayAdapter<String> acAdapter = new ArrayAdapter<String>(getContext(),
                                    R.layout.single_tag_text_view, result);
                            viewBinding.newTagEditText.setAdapter(acAdapter);
                        }, error -> {
                            Log.e(TAG, Log.getStackTraceString(error));
                        });
    }

    private void addTag(String name) {
        if (TextUtils.isEmpty(name) || displayedTags.contains(name)) {
            return;
        }
        displayedTags.add(name);
        viewBinding.newTagEditText.setText("");
        adapter.notifyDataSetChanged();
    }

    private List<FeedPreferences> getFeedPreferences() {
        FeedPreferences preferences = (FeedPreferences) getArguments().getSerializable(ARG_FEED_PREFERENCES);
        if (preferences != null) {
            return Collections.singletonList(preferences);
        }
        return (ArrayList<FeedPreferences>) getArguments().getSerializable(ARG_MULTI_FEED_PREFERENCES);
    }

    private void updatePreferencesTags(List<FeedPreferences> feedPreferencesList, Set<String> commonTags) {
        if (viewBinding.rootFolderCheckbox.isChecked()) {
            displayedTags.add(FeedPreferences.TAG_ROOT);
        }
        for (FeedPreferences preferences : feedPreferencesList) {
            ArrayList<String> allTags = new ArrayList<>(preferences.getTags());
            allTags.removeAll(commonTags);
            allTags.addAll(displayedTags);
            preferences.getTags().clear();
            preferences.getTags().addAll(allTags);
            DBWriter.setFeedPreferences(preferences);
        }
    }

    public class TagSelectionAdapter extends RecyclerView.Adapter<TagSelectionAdapter.ViewHolder> {

        @Override
        @NonNull
        public TagSelectionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Chip chip = new Chip(getContext());
            chip.setCloseIconVisible(true);
            chip.setCloseIconResource(R.drawable.ic_delete);
            return new TagSelectionAdapter.ViewHolder(chip);
        }

        @Override
        public void onBindViewHolder(@NonNull TagSelectionAdapter.ViewHolder holder, int position) {
            holder.chip.setText(displayedTags.get(position));
            holder.chip.setOnCloseIconClickListener(v -> {
                displayedTags.remove(position);
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return displayedTags.size();
        }

        @Override
        public long getItemId(int position) {
            return displayedTags.get(position).hashCode();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            Chip chip;

            ViewHolder(Chip itemView) {
                super(itemView);
                chip = itemView;
            }
        }
    }
}
