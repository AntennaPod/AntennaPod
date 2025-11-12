package de.danoeh.antennapod.ui.common;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.google.android.material.snackbar.Snackbar;

/**
 * Utilities for commonly used clipboard functionality.
 */
public final class ClipboardUtils {

    /**
     * Utility function used to copy the text from the given TextView to the clipboard.
     * @param textView TextView to copy the text from.
     * @param labelId ID of string resource to used as label for the copied text within the clipboard.
     */
    public static void copyText(TextView textView, @StringRes int labelId) {
        copyText(textView, labelId, R.string.copied_to_clipboard);
    }

    /**
     * Utility function used to copy the text from the given TextView to the clipboard.
     * @param textView TextView to copy the text from.
     * @param labelId ID of string resource to used as label for the copied text within the clipboard.
     * @param messageId ID of string resource use to display confirmation to user on SDK versions prior to 32.
     */
    public static void copyText(TextView textView, @StringRes int labelId, @StringRes int messageId) {
        Context context = textView.getContext();
        copyText(textView, context.getString(labelId), context.getString(messageId), textView.getText().toString());
    }

    /**
     * Utility function used to copy the given text to the clipboard. The give View is used for context
     * and to display a confirmation Toast to the user on completion where the SDK level is prior to 32.
     * @param view View to copy the text from.
     * @param labelId ID of string resource to use as label for the copied text within the clipboard buffer.
     * @param text Text to copy to the clipboard.
     */
    public static void copyText(View view, @StringRes int labelId, String text) {
        Context context = view.getContext();
        copyText(view, context.getString(labelId), context.getString(R.string.copied_to_clipboard), text);
    }

    private static void copyText(View view, String label, String message, String text) {
        ClipboardManager clipboard = (ClipboardManager) view.getContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text));

        if (Build.VERSION.SDK_INT < 32) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
        }
    }

    private ClipboardUtils() {
        /* Utility classes should not instantiated */
    }
}
