package de.danoeh.antennapod.ui.preferences.screen.downloads;

import android.content.Context;
import android.os.StatFs;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.preferences.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DataFolderAdapter extends RecyclerView.Adapter<DataFolderAdapter.ViewHolder> {
    private final Consumer<String> selectionHandler;
    private final String currentPath;
    private final List<StoragePath> entries;
    private final String freeSpaceString;

    public DataFolderAdapter(Context context, @NonNull Consumer<String> selectionHandler) {
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
        Context context = holder.root.getContext();
        String freeSpace = Formatter.formatShortFileSize(context, storagePath.getAvailableSpace());
        String totalSpace = Formatter.formatShortFileSize(context, storagePath.getTotalSpace());

        holder.path.setText(storagePath.getPath());
        holder.size.setText(String.format(freeSpaceString, freeSpace, totalSpace));
        holder.progressBar.setProgress(storagePath.getUsagePercentage());
        View.OnClickListener selectListener = v -> selectionHandler.accept(storagePath.getPath());
        holder.root.setOnClickListener(selectListener);
        holder.radioButton.setOnClickListener(selectListener);

        if (storagePath.getPath().equals(currentPath)) {
            holder.radioButton.toggle();
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    private String getCurrentPath() {
        File dataFolder = UserPreferences.getDataFolder(null);
        if (dataFolder != null) {
            return dataFolder.getAbsolutePath();
        }
        return null;
    }

    private List<StoragePath> getStorageEntries(Context context) {
        final List<File> mediaDirs = new ArrayList<>();
        mediaDirs.addAll(List.of(context.getExternalFilesDirs(null)));
        mediaDirs.addAll(List.of(context.getExternalMediaDirs()));
        final List<StoragePath> entries = new ArrayList<>(mediaDirs.size());
        for (File dir : mediaDirs) {
            if (!isWritable(dir)) {
                continue;
            }
            entries.add(new StoragePath(dir.getAbsolutePath()));
        }
        if (entries.isEmpty() && isWritable(context.getFilesDir())) {
            entries.add(new StoragePath(context.getFilesDir().getAbsolutePath()));
        }
        return entries;
    }

    private boolean isWritable(File dir) {
        return dir != null && dir.exists() && dir.canRead() && dir.canWrite();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View root;
        private final TextView path;
        private final TextView size;
        private final RadioButton radioButton;
        private final ProgressBar progressBar;

        ViewHolder(View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.root);
            path = itemView.findViewById(R.id.path);
            size = itemView.findViewById(R.id.size);
            radioButton = itemView.findViewById(R.id.radio_button);
            progressBar = itemView.findViewById(R.id.used_space);
        }
    }

    static class StoragePath {
        private final String path;

        StoragePath(String path) {
            this.path = path;
        }

        String getPath() {
            return this.path;
        }

        long getAvailableSpace() {
            StatFs stat = new StatFs(path);
            long availableBlocks = stat.getAvailableBlocksLong();
            long blockSize = stat.getBlockSizeLong();
            return availableBlocks * blockSize;
        }

        long getTotalSpace() {
            StatFs stat = new StatFs(path);
            long blockCount = stat.getBlockCountLong();
            long blockSize = stat.getBlockSizeLong();
            return blockCount * blockSize;
        }

        int getUsagePercentage() {
            return 100 - (int) (100 * getAvailableSpace() / (float) getTotalSpace());
        }
    }
}