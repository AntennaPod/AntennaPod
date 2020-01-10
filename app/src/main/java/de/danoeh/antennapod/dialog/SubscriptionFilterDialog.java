package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.SubscriptionFilter;

import static de.danoeh.antennapod.core.feed.SubscriptionFilter.STRING_ALWAYS;
import static de.danoeh.antennapod.core.feed.SubscriptionFilter.STRING_DISABLE;
import static de.danoeh.antennapod.core.feed.SubscriptionFilter.STRING_ENABLE;
import static de.danoeh.antennapod.core.feed.SubscriptionFilter.STRING_GLOBAL_DEFAULT;
import static de.danoeh.antennapod.core.feed.SubscriptionFilter.STRING_NEVER;
import static de.danoeh.antennapod.core.feed.SubscriptionFilter.STRING_NO_FILTER;

public abstract class SubscriptionFilterDialog {

    private Context context;
    private Spinner spinnerAutoDownload;
    private Spinner spinnerKeepUpdated;
    private Spinner spinnerAutoDelete;
    private SubscriptionFilter.AutoDownload autoDownload;
    private SubscriptionFilter.KeepUpdated keepUpdated;
    private SubscriptionFilter.AutoDelete autoDelete;
    private AlertDialog dialog;
    protected SubscriptionFilterDialog(Context theContext, SubscriptionFilter.AutoDownload theAutoDownload, SubscriptionFilter.KeepUpdated theKeepUpdated,
                                       SubscriptionFilter.AutoDelete theAutoDelete) {
        this.context = theContext;
        this.autoDownload = theAutoDownload;
        this.keepUpdated = theKeepUpdated;
        this.autoDelete = theAutoDelete;

    }

    public AlertDialog openDialog() {

        View content = View.inflate(context, R.layout.dialog_layout, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setView(content);
        spinnerAutoDownload = content.findViewById(R.id.spinnerAutoDownload);
        spinnerKeepUpdated = content.findViewById(R.id.spinnerKeepUpdated);
        spinnerAutoDelete = content.findViewById(R.id.spinnerAutoDelete);

        ArrayList<String> spinnerArrayAutoDown_keepUpdated = new ArrayList<>();
        ArrayList<String> spinnerArrayAutoDelete = new ArrayList<>();

        spinnerArrayAutoDown_keepUpdated.add(STRING_ENABLE);
        spinnerArrayAutoDown_keepUpdated.add(STRING_DISABLE);
        spinnerArrayAutoDown_keepUpdated.add(STRING_NO_FILTER);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item,
                spinnerArrayAutoDown_keepUpdated);
        spinnerAutoDownload.setAdapter(spinnerArrayAdapter);
        spinnerKeepUpdated.setAdapter(spinnerArrayAdapter);

        spinnerArrayAutoDelete.add(STRING_ALWAYS);
        spinnerArrayAutoDelete.add(STRING_NEVER);
        spinnerArrayAutoDelete.add(STRING_GLOBAL_DEFAULT);
        spinnerArrayAutoDelete.add(STRING_NO_FILTER);
        ArrayAdapter<String> spinnerArrayAdapter1 = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item,
                spinnerArrayAutoDelete);
        spinnerAutoDelete.setAdapter(spinnerArrayAdapter1);

        spinnerAutoDownload.setSelection(spinnerArrayAutoDown_keepUpdated.indexOf(autoDownload.toString()));
        spinnerKeepUpdated.setSelection(spinnerArrayAutoDown_keepUpdated.indexOf(keepUpdated.toString()));
        spinnerAutoDelete.setSelection(spinnerArrayAutoDelete.indexOf(autoDelete.toString()));

        builder.setNegativeButton(R.string.cancel_label, (dialog, which) -> dialog.dismiss());
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {

            updateFilter(SubscriptionFilter.AutoDownload.parse(spinnerAutoDownload.getSelectedItem().toString()),
                    SubscriptionFilter.KeepUpdated.parse(spinnerKeepUpdated.getSelectedItem().toString()),
                    SubscriptionFilter.AutoDelete.parse(spinnerAutoDelete.getSelectedItem().toString()));
            dialog.dismiss();
        });
        dialog = builder.create();

        return dialog;
    }

    protected abstract void updateFilter(SubscriptionFilter.AutoDownload autoDownload, SubscriptionFilter.KeepUpdated keepUpdated, SubscriptionFilter.AutoDelete autoDelete);

}
