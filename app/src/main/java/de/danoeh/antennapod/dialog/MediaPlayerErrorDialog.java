package de.danoeh.antennapod.dialog;

import android.app.Activity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.event.PlayerErrorEvent;

public class MediaPlayerErrorDialog {
    public static void show(Activity activity, PlayerErrorEvent event) {
        final MaterialAlertDialogBuilder errorDialog = new MaterialAlertDialogBuilder(activity);
        errorDialog.setTitle(R.string.error_label);

        String genericMessage = activity.getString(R.string.playback_error_generic);
        SpannableString errorMessage = new SpannableString(genericMessage + "\n\n" + event.getMessage());
        errorMessage.setSpan(new ForegroundColorSpan(0x88888888),
                genericMessage.length(), errorMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        errorDialog.setMessage(errorMessage);
        errorDialog.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
        errorDialog.create().show();
    }
}
