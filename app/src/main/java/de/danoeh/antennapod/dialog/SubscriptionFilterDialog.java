package de.danoeh.antennapod.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;

import de.danoeh.antennapod.R;

import static de.danoeh.antennapod.fragment.SubscriptionFragment.STRING_ALWAYS;
import static de.danoeh.antennapod.fragment.SubscriptionFragment.STRING_AUTO_DELETE;
import static de.danoeh.antennapod.fragment.SubscriptionFragment.STRING_AUTO_DOWNLOAD;
import static de.danoeh.antennapod.fragment.SubscriptionFragment.STRING_DISABLE;
import static de.danoeh.antennapod.fragment.SubscriptionFragment.STRING_ENABLE;
import static de.danoeh.antennapod.fragment.SubscriptionFragment.STRING_GLOBAL_DEFAULT;
import static de.danoeh.antennapod.fragment.SubscriptionFragment.STRING_KEEP_UPDATED;
import static de.danoeh.antennapod.fragment.SubscriptionFragment.STRING_NEVER;
import static de.danoeh.antennapod.fragment.SubscriptionFragment.STRING_NO_FILTER;

public abstract class SubscriptionFilterDialog {

    private final Context context;
    final Dialog dialog;
    private Spinner spinnerAutoDownload;
    private Spinner spinnerKeepUpdated;
    private Spinner spinnerAutoDelete;
    private Button buttonConfirm;
    private Button buttonCancel;
    private String autoDownload;
    private String keepUpdated;
    private String autoDelete;

    protected SubscriptionFilterDialog(Context theContext,String theAutoDownload, String theKeepUpdated,
                                       String theAutoDelete) {
        this.context = theContext;
        this.autoDownload = theAutoDownload;
        this.keepUpdated = theKeepUpdated;
        this.autoDelete = theAutoDelete;

        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_layout);

        spinnerAutoDownload = dialog.findViewById(R.id.spinnerAutoDownload);
        spinnerKeepUpdated = dialog.findViewById(R.id.spinnerKeepUpdated);
        spinnerAutoDelete = dialog.findViewById(R.id.spinnerAutoDelete);
        buttonConfirm = dialog.findViewById(R.id.buttonConfirm);
        buttonCancel = dialog.findViewById(R.id.buttonCancel);
    }

    public void openDialog() {
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

        spinnerAutoDownload.setSelection(spinnerArrayAutoDown_keepUpdated.indexOf(autoDownload));
        spinnerKeepUpdated.setSelection(spinnerArrayAutoDown_keepUpdated.indexOf(keepUpdated));
        spinnerAutoDelete.setSelection(spinnerArrayAutoDelete.indexOf(autoDelete));

        dialog.setTitle(R.string.filter);

        buttonCancel.setOnClickListener(v -> {
            dialog.cancel();
        });
        buttonConfirm.setOnClickListener(v -> {
            String autoDownloadValue = spinnerAutoDownload.getSelectedItem().toString();
            String keepUpdatedValue = spinnerKeepUpdated.getSelectedItem().toString();
            String autoDeleteValue = spinnerAutoDelete.getSelectedItem().toString();

            final ArrayList<SubscriptionFilter> filterValues = new ArrayList<>();
            filterValues.add(new SubscriptionFilter(STRING_AUTO_DOWNLOAD,autoDownloadValue));
            filterValues.add(new SubscriptionFilter(STRING_KEEP_UPDATED,keepUpdatedValue));
            filterValues.add(new SubscriptionFilter(STRING_AUTO_DELETE,autoDeleteValue));

            updateFilter(filterValues);

            dialog.cancel();
        });
        dialog.show();

    }

    protected abstract void updateFilter(ArrayList<SubscriptionFilter> filterValues);

    public class SubscriptionFilter {
        String option;
        String value;

        public String getOption() {
            return option;
        }

        public String getValue() {
            return value;
        }

        public SubscriptionFilter(String theoption, String thevalue) {

            this.option = theoption;
            this.value = thevalue;
        }
    }
}
