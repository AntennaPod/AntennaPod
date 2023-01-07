package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.SimpleChipAdapter;
import de.danoeh.antennapod.databinding.EpisodeFilterDialogBinding;
import de.danoeh.antennapod.model.feed.FeedFilter;
import de.danoeh.antennapod.view.ItemOffsetDecoration;

import java.util.List;

/**
 * Displays a dialog with a text box for filtering episodes and two radio buttons for exclusion/inclusion
 */
public abstract class EpisodeFilterDialog extends MaterialAlertDialogBuilder {
    private final EpisodeFilterDialogBinding viewBinding;
    private final List<String> termList;

    public EpisodeFilterDialog(Context context, FeedFilter filter) {
        super(context);
        viewBinding = EpisodeFilterDialogBinding.inflate(LayoutInflater.from(context));

        setTitle(R.string.episode_filters_label);
        setView(viewBinding.getRoot());

        viewBinding.durationCheckBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> viewBinding.episodeFilterDurationText.setEnabled(isChecked));
        if (filter.hasMinimalDurationFilter()) {
            viewBinding.durationCheckBox.setChecked(true);
            // Store minimal duration in seconds, show in minutes
            viewBinding.episodeFilterDurationText
                    .setText(String.valueOf(filter.getMinimalDurationFilter() / 60));
        } else {
            viewBinding.episodeFilterDurationText.setEnabled(false);
        }

        if (filter.excludeOnly()) {
            termList = filter.getExcludeFilter();
            viewBinding.excludeRadio.setChecked(true);
        } else {
            termList = filter.getIncludeFilter();
            viewBinding.includeRadio.setChecked(true);
        }
        setupWordsList();

        setNegativeButton(R.string.cancel_label, null);
        setPositiveButton(R.string.confirm_label, this::onConfirmClick);
    }

    private void setupWordsList() {
        viewBinding.termsRecycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        viewBinding.termsRecycler.addItemDecoration(new ItemOffsetDecoration(getContext(), 4));
        SimpleChipAdapter adapter = new SimpleChipAdapter(getContext()) {
            @Override
            protected List<String> getChips() {
                return termList;
            }

            @Override
            protected void onRemoveClicked(int position) {
                termList.remove(position);
                notifyDataSetChanged();
            }
        };
        viewBinding.termsRecycler.setAdapter(adapter);
        viewBinding.termsTextInput.setEndIconOnClickListener(v -> {
            String newWord = viewBinding.termsTextInput.getEditText().getText().toString().replace("\"", "").trim();
            if (TextUtils.isEmpty(newWord) || termList.contains(newWord)) {
                return;
            }
            termList.add(newWord);
            viewBinding.termsTextInput.getEditText().setText("");
            adapter.notifyDataSetChanged();
        });
    }

    protected abstract void onConfirmed(FeedFilter filter);

    private void onConfirmClick(DialogInterface dialog, int which) {
        int minimalDuration = -1;
        if (viewBinding.durationCheckBox.isChecked()) {
            try {
                // Store minimal duration in seconds
                minimalDuration = Integer.parseInt(
                        viewBinding.episodeFilterDurationText.getText().toString()) * 60;
            } catch (NumberFormatException e) {
                // Do not change anything on error
            }
        }
        String excludeFilter = "";
        String includeFilter = "";
        if (viewBinding.includeRadio.isChecked()) {
            includeFilter = toFilterString(termList);
        } else {
            excludeFilter = toFilterString(termList);
        }
        onConfirmed(new FeedFilter(includeFilter, excludeFilter, minimalDuration));
    }

    private String toFilterString(List<String> words) {
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append("\"").append(word).append("\" ");
        }
        return result.toString();
    }
}
