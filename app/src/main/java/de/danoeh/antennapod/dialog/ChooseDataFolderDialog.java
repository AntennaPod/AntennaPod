package de.danoeh.antennapod.dialog;

import android.content.Context;

import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
            new AlertDialog.Builder(context)
                    .setTitle(R.string.error_label)
                    .setMessage(R.string.external_storage_error_msg)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        View content = View.inflate(context, R.layout.choose_data_folder_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(content)
                .setTitle(R.string.choose_data_directory)
                .setMessage(R.string.choose_data_directory_message)
                .setNegativeButton(R.string.cancel_label, null)
                .create();
        ((RecyclerView) content.findViewById(R.id.recyclerView)).setLayoutManager(new LinearLayoutManager(context));
        ((RecyclerView) content.findViewById(R.id.recyclerView)).setAdapter(adapter);
        adapter.setDialog(dialog);
        dialog.show();
    }

}