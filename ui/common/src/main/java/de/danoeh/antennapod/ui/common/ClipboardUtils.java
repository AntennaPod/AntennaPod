package de.danoeh.antennapod.ui.common;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.material.snackbar.Snackbar;

/**
 * Utilities for commonly used clipboard functionality.
 */
public abstract class ClipboardUtils {

    /**
     * Utility class used to copy the content of a TextView to the clipboard when the view is clicked.
     */
    public static class TextViewCopyOnClickListener extends ViewCopyOnClickListener {
        public TextViewCopyOnClickListener(@NonNull Context context, @StringRes int labelId) {
            super(context, labelId, R.string.copied_to_clipboard);
        }

        @NonNull
        @Override
        protected String getText(View view) {
            if (view instanceof TextView textView) {
                return textView.getText().toString();
            }

            return "";
        }
    }

    /**
     * Abstract utility class used to copy user-defined text to the clipboard when a view is clicked.
     */
    public abstract static class ViewCopyOnClickListener implements View.OnClickListener {
        private final String label;
        private final String message;

        public ViewCopyOnClickListener(@NonNull Context context, @StringRes int labelId, @StringRes int messageId) {
            this(context.getString(labelId), context.getString(messageId));
        }

        public ViewCopyOnClickListener(String label, String message) {
            this.label = label;
            this.message = message;
        }

        @Override
        public void onClick(@NonNull View view) {
            ClipboardManager clipboard = (ClipboardManager) view.getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);

            clipboard.setPrimaryClip(ClipData.newPlainText(label, getText(view)));

            if (Build.VERSION.SDK_INT < 32) {
                Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
            }
        }

        protected abstract String getText(View view);
    }
}
