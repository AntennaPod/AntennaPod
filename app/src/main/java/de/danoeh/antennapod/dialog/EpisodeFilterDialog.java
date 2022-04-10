package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

import androidx.appcompat.app.AlertDialog;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedFilter;

/**
 * Displays a dialog with a text box for filtering episodes and two radio buttons for exclusion/inclusion
 */
public abstract class EpisodeFilterDialog extends AlertDialog.Builder {
    private final FeedFilter initialFilter;

    public EpisodeFilterDialog(Context context, FeedFilter filter) {

        super(context);
        initialFilter = filter;
        setTitle(R.string.episode_filters_label);
        View rootView = View.inflate(context, R.layout.episode_filter_dialog, null);
        setView(rootView);

        final EditText etxtEpisodeFilterText = rootView.findViewById(R.id.etxtEpisodeFilterText);
        final EditText etxtEpisodeFilterDurationText = rootView.findViewById(R.id.etxtEpisodeFilterDurationText);
        final RadioButton radioInclude = rootView.findViewById(R.id.radio_filter_include);
        final RadioButton radioExclude = rootView.findViewById(R.id.radio_filter_exclude);
        final CheckBox checkboxDuration = rootView.findViewById(R.id.checkbox_filter_duration);

        if (initialFilter.includeOnly()) {
            radioInclude.setChecked(true);
            etxtEpisodeFilterText.setText(initialFilter.getIncludeFilter());
        } else if(initialFilter.excludeOnly()) {
            radioExclude.setChecked(true);
            etxtEpisodeFilterText.setText(initialFilter.getExcludeFilter());
        } else {
            radioExclude.setChecked(false);
            radioInclude.setChecked(false);
            etxtEpisodeFilterText.setText("");
        }
        if (initialFilter.hasMinimalDurationFilter()) {
            checkboxDuration.setChecked(true);
            // Store minimal duration in seconds, show in minutes
            etxtEpisodeFilterDurationText.setText(String.valueOf(initialFilter.getMinimalDurationFilter() / 60));
        }

        setNegativeButton(R.string.cancel_label, null);
        setPositiveButton(R.string.confirm_label, (dialog, which) -> {
                    String includeString = "";
                    String excludeString = "";
                    int minimalDuration = -1;
                    if (radioInclude.isChecked()) {
                        includeString = etxtEpisodeFilterText.getText().toString();
                    } else {
                        excludeString = etxtEpisodeFilterText.getText().toString();
                    }
                    if (checkboxDuration.isChecked()) {
                        try {
                            // Store minimal duration in seconds
                            minimalDuration = Integer.parseInt(etxtEpisodeFilterDurationText.getText().toString()) * 60;
                        } catch (NumberFormatException e) {
                            // Do not change anything on error
                        }
                    }
                    onConfirmed(new FeedFilter(includeString, excludeString, minimalDuration));
                }
        );
    }

    protected abstract void onConfirmed(FeedFilter filter);
}
