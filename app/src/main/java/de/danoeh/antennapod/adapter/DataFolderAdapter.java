package de.danoeh.antennapod.adapter;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.StorageUtils;
import de.danoeh.antennapod.dialog.ChooseDataFolderDialog;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;


public class DataFolderAdapter extends RecyclerView.Adapter<DataFolderAdapter.ViewHolder> {
    private final ChooseDataFolderDialog.RunnableWithString selectionHandler;
    private final String currentPath;
    private final List<StoragePath> entries;
    private final String freeSpaceString;
    private Dialog dialog;

    public DataFolderAdapter(Context context, ChooseDataFolderDialog.RunnableWithString selectionHandler) {
        this.entries = getStorageEntries(context);
        this.currentPath = getCurrentPath();
        this.selectionHandler = selectionHandler;
        this.freeSpaceString = context.getString(R.string.choose_data_directory_available_space);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View entryView = inflater.inflate(R.layout.choose_data_folder_dialog_entry, parent, false);
        return new ViewHolder(entryView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StoragePath storagePath = entries.get(position);
        String freeSpace = Converter.byteToString(storagePath.getAvailableSpace());

        holder.path.setText(storagePath.getShortPath());
        holder.size.setText(String.format(freeSpaceString, freeSpace));
        holder.progressBar.setProgress(storagePath.getUsagePercentage());
        holder.root.setOnClickListener((View v) -> selectAndDismiss(storagePath));
        holder.radioButton.setOnClickListener((View v) -> selectAndDismiss(storagePath));
        if (storagePath.getFullPath().equals(currentPath)) {
            holder.radioButton.toggle();
        }
    }

    @Override
    public int getItemCount() {
        if (currentPath == null) {
            return 0;
        } else {
            return entries.size();
        }
    }

    public void setDialog(Dialog dialog) {
        this.dialog = dialog;
    }

    private String getCurrentPath() {
        File dataFolder = UserPreferences.getDataFolder(null);
        if (dataFolder != null) return dataFolder.getAbsolutePath();
        return null;
    }

    private List<StoragePath> getStorageEntries(Context context) {
        File[] mediaDirs = ContextCompat.getExternalFilesDirs(context, null);
        final List<StoragePath> entries = new ArrayList<>(mediaDirs.length);
        for (File dir : mediaDirs) {
            if (isNotWritable(dir)) continue;

            entries.add(new StoragePath(dir.getAbsolutePath()));
        }
        return entries;
    }

    private boolean isNotWritable(File dir) {
        return dir == null || !dir.exists() || !dir.canRead() || !dir.canWrite();
    }

    private void selectAndDismiss(StoragePath storagePath) {
        selectionHandler.run(storagePath.getFullPath());
        dialog.dismiss();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private View root;
        private TextView path;
        private TextView size;
        private RadioButton radioButton;
        private MaterialProgressBar progressBar;

        ViewHolder(View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.root);
            path = itemView.findViewById(R.id.path);
            size = itemView.findViewById(R.id.size);
            radioButton = itemView.findViewById(R.id.radio_button);
            progressBar = itemView.findViewById(R.id.used_space);
        }
    }

    class StoragePath {
        private final String path;

        StoragePath(String path) {
            this.path = path;
        }

        String getShortPath() {
            int prefixIndex = path.indexOf("Android");
            return (prefixIndex > 0) ? path.substring(0, prefixIndex) : path;
        }

        String getFullPath() {
            return this.path;
        }

        long getAvailableSpace() {
            return StorageUtils.getFreeSpaceAvailable(path);
        }

        long getTotalSpace() {
            return StorageUtils.getTotalSpaceAvailable(path);
        }

        int getUsagePercentage() {
            return 100 - (int) (100 * getAvailableSpace() / (float) getTotalSpace());
        }
    }
}