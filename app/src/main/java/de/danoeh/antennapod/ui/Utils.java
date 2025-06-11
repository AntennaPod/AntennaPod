package de.danoeh.antennapod.ui;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import de.danoeh.antennapod.R;

/**
 * Utility class that contains helper methods
 */
public class Utils {

    private Utils() {

    }


    /**
     * Copies the given text to the clipboard.
     * @param context required context in order to get the clipboard service
     * @param text the text to copy
     * @param label label of the clipped data. It is not displayed to the user, but it used by accessibility services
     */
    public static void copyToClipboard(Context context, String text, String label) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        if ( clipboard != null) {
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }
}
