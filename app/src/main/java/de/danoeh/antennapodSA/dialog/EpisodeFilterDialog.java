package de.danoeh.antennapodSA.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.core.feed.FeedFilter;

/**
 * Displays a dialog with a text box for filtering episodes and two radio buttons for exclusion/inclusion
 */
public abstract class EpisodeFilterDialog extends Dialog {

    private final FeedFilter initialFilter;

    public EpisodeFilterDialog(Context context, FeedFilter filter) {
        super(context);
        this.initialFilter = filter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.episode_filter_dialog);
        final EditText etxtEpisodeFilterText = findViewById(R.id.etxtEpisodeFilterText);
        final RadioButton radioInclude = findViewById(R.id.radio_filter_include);
        final RadioButton radioExclude = findViewById(R.id.radio_filter_exclude);
        final Button butConfirm = findViewById(R.id.butConfirm);
        final Button butCancel = findViewById(R.id.butCancel);

        setTitle(R.string.episode_filters_label);
        setOnCancelListener(dialog -> onCancelled());

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


        butCancel.setOnClickListener(v -> cancel());
        butConfirm.setOnClickListener(v -> {

            String includeString = "";
            String excludeString = "";
            if (radioInclude.isChecked()) {
                includeString = etxtEpisodeFilterText.getText().toString();
            } else {
                excludeString = etxtEpisodeFilterText.getText().toString();
            }

            onConfirmed(new FeedFilter(includeString, excludeString));
            dismiss();
        });
    }

    protected void onCancelled() {

    }

    protected abstract void onConfirmed(FeedFilter filter);
}
