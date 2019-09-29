package de.danoeh.antennapod.dialog;

import android.content.Context;

import com.afollestad.materialdialogs.MaterialDialog;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.DataFolderAdapter;

public class ChooseDataFolderDialog {

    public abstract static class RunnableWithString implements Runnable {
        public RunnableWithString() {
            super();
        }
        public abstract void run(final String arg);
        @Override public void run() {
            throw new IllegalArgumentException("Expect one String parameter.");
        }
    }

    private ChooseDataFolderDialog() {}

    public static void showDialog(final Context context, RunnableWithString handlerFunc) {
        DataFolderAdapter adapter = new DataFolderAdapter(context, handlerFunc);

        if (adapter.getItemCount() == 0) {
            new MaterialDialog.Builder(context)
                    .title(R.string.error_label)
                    .content(R.string.external_storage_error_msg)
                    .neutralText(android.R.string.ok)
                    .show();
            return;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.choose_data_directory)
                .content(R.string.choose_data_directory_message)
                .adapter(adapter, null)
                .negativeText(R.string.cancel_label)
                .cancelable(true)
                .build();
        adapter.setDialog(dialog);
        dialog.show();
    }

}