package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.Html;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.StorageUtils;

public class ChooseDataFolderDialog {

    public static abstract class RunnableWithString implements Runnable {
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
        File dataFolder = UserPreferences.getDataFolder(null);
        HashMap<String, List<String>> options = getStorageOptions(context);
        final List<String> entries = options.get("entries");
        final List<String> folders = options.get("folders");

        if (dataFolder == null || entries.isEmpty()) {
            new MaterialDialog.Builder(context)
                    .title(R.string.error_label)
                    .content(R.string.external_storage_error_msg)
                    .neutralText(android.R.string.ok)
                    .show();
            return;
        }

        int selectedIndex = folders.indexOf(dataFolder.getAbsolutePath());
        MaterialDialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.choose_data_directory)
                .content(R.string.choose_data_directory_message)
                .items(entries)
                .itemsCallbackSingleChoice(selectedIndex, (dialog1, itemView, which, text) -> {
                    String folder = folders.get(which);
                    handlerFunc.run(folder);
                    return true;
                })
                .negativeText(R.string.cancel_label)
                .cancelable(true)
                .build();
        dialog.show();
    }

    private static HashMap<String, List<String>> getStorageOptions(Context context) {
        File[] mediaDirs = ContextCompat.getExternalFilesDirs(context, null);
        final List<String> folders = new ArrayList<>(mediaDirs.length);
        final List<String> entries = new ArrayList<>(mediaDirs.length);
        for (File dir : mediaDirs) {
            if (isNotWritable(dir)) continue;

            String path = dir.getAbsolutePath();
            String location = getStorageLocation(path);
            String availableSpace = getAvailableSpace(path);
            folders.add(path);
            entries.add(storageEntry(location, availableSpace));
        }
        return new HashMap<String, List<String>>() {{
            put("folders", folders);
            put("entries", entries);
        }};
    }

    private static String storageEntry(String location, String availableSpace) {
        String html = String.format("<small>%1$s [%2$s]</small>", location, availableSpace);
        return fromHtmlVersioned(html).toString();
    }

    private static String getAvailableSpace(String path) {
        long spaceAvailable = StorageUtils.getFreeSpaceAvailable(path);
        return Converter.byteToString(spaceAvailable);
    }

    private static String getStorageLocation(String path) {
        int prefixIndex = path.indexOf("Android");
        return (prefixIndex > 0) ? path.substring(0, prefixIndex) : path;
    }

    private static boolean isNotWritable(File dir) {
        return dir == null || !dir.exists() || !dir.canRead() || !dir.canWrite();
    }

    @SuppressWarnings("deprecation")
    private static CharSequence fromHtmlVersioned(final String html) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return Html.fromHtml(html);
        }
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
    }

}