package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputLayout;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.SimpleChipAdapter;
import de.danoeh.antennapod.databinding.EpisodeFilterDialogBinding;
import de.danoeh.antennapod.model.feed.FeedFilter;
import de.danoeh.antennapod.view.ItemOffsetDecoration;

import java.util.List;

/**
 * Displays a dialog with a text box for filtering episodes and two radio buttons for exclusion/inclusion
 */
public abstract class EpisodeFilterDialog extends AlertDialog.Builder {
    private final EpisodeFilterDialogBinding viewBinding;
    private final List<String> includedWords;
    private final List<String> excludedWords;

    public EpisodeFilterDialog(Context context, FeedFilter filter) {
        super(context);
        viewBinding = EpisodeFilterDialogBinding.inflate(LayoutInflater.from(context));
        includedWords = filter.getIncludeFilter();
        excludedWords = filter.getExcludeFilter();

        setTitle(R.string.episode_filters_label);
        setView(viewBinding.getRoot());

        if (filter.hasMinimalDurationFilter()) {
            viewBinding.durationCheckBox.setChecked(true);
            // Store minimal duration in seconds, show in minutes
            viewBinding.episodeFilterDurationText
                    .setText(String.valueOf(filter.getMinimalDurationFilter() / 60));
        }
        setupWordsList(viewBinding.excludedWordsRecycler, viewBinding.newExcludedWordTextInput, excludedWords);
        setupWordsList(viewBinding.includedWordsRecycler, viewBinding.newIncludedWordTextInput, includedWords);

        setNegativeButton(R.string.cancel_label, null);
        setPositiveButton(R.string.confirm_label, this::onConfirmClick);
    }

    private void setupWordsList(RecyclerView recyclerView, TextInputLayout textInput, List<String> words) {
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.addItemDecoration(new ItemOffsetDecoration(getContext(), 4));
        SimpleChipAdapter adapter = new SimpleChipAdapter(getContext()) {
            @Override
            protected List<String> getChips() {
                return words;
            }

            @Override
            protected void onRemoveClicked(int position) {
                words.remove(position);
                notifyDataSetChanged();
            }
        };
        recyclerView.setAdapter(adapter);
        textInput.setEndIconOnClickListener(v -> {
            String newWord = textInput.getEditText().getText().toString().replace("\"", "").trim();
            if (TextUtils.isEmpty(newWord) || words.contains(newWord)) {
                return;
            }
            words.add(newWord);
            textInput.getEditText().setText("");
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
        onConfirmed(new FeedFilter(toFilterString(includedWords), toFilterString(excludedWords), minimalDuration));
    }

    private String toFilterString(List<String> words) {
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append("\"").append(word).append("\" ");
        }
        return result.toString();
    }
}
