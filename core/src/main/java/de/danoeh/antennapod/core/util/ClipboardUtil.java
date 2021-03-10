package de.danoeh.antennapod.core.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class ClipboardUtil {

    /**
     * Copies the text to the clipboard.
     *
     * @param context The context to pass.
     * @param label   User-visible label for the clip data.
     * @param text    The actual text in the clip.
     */
    public static void copyToClipboard(final Context context, @NonNull final CharSequence label,
                                       @NonNull final CharSequence text) {
        final ClipboardManager clipboard = ContextCompat.getSystemService(context, ClipboardManager.class);
        if (clipboard != null) {
            final ClipData data = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(data);
        }
    }
}
