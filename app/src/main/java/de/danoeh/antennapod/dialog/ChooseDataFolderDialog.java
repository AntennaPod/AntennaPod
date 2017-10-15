package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.Html;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.ArrayList;
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
        if (dataFolder == null) {
            new MaterialDialog.Builder(context)
                    .title(R.string.error_label)
                    .content(R.string.external_storage_error_msg)
                    .neutralText(android.R.string.ok)
                    .show();
            return;
        }
        String dataFolderPath = dataFolder.getAbsolutePath();
        int selectedIndex = -1;
        int index = 0;
        File[] mediaDirs = ContextCompat.getExternalFilesDirs(context, null);
        final List<String> folders = new ArrayList<>(mediaDirs.length);
        final List<CharSequence> choices = new ArrayList<>(mediaDirs.length);
        for (File dir : mediaDirs) {
            if(dir == null || !dir.exists() || !dir.canRead() || !dir.canWrite()) {
                continue;
            }
            String path = dir.getAbsolutePath();
            folders.add(path);
            if(dataFolderPath.equals(path)) {
                selectedIndex = index;
            }
            int prefixIndex = path.indexOf("Android");
            String choice = (prefixIndex > 0) ? path.substring(0, prefixIndex) : path;
            long bytes = StorageUtils.getFreeSpaceAvailable(path);
            String item = String.format(
                    "<small>%1$s [%2$s]</small>", choice, Converter.byteToString(bytes));
            choices.add(fromHtmlVersioned(item));
            index++;
        }
        if (choices.isEmpty()) {
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
                .items(choices)
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

    @SuppressWarnings("deprecation")
    private static CharSequence fromHtmlVersioned(final String html) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return Html.fromHtml(html);
        }
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
    }

}