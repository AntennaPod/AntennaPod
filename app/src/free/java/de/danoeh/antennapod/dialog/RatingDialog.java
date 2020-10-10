package de.danoeh.antennapod.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AlertDialog;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.util.IntentUtils;

public class RatingDialog {

    private RatingDialog(){}

    private static final String TAG = RatingDialog.class.getSimpleName();
    private static final int AFTER_DAYS = 7;

    private static WeakReference<Context> mContext;
    private static SharedPreferences mPreferences;
    private static Dialog mDialog;

    private static final String PREFS_NAME = "RatingPrefs";
    private static final String KEY_RATED = "KEY_WAS_RATED";
    private static final String KEY_FIRST_START_DATE = "KEY_FIRST_HIT_DATE";

    public static void init(Context context) {
        mContext = new WeakReference<>(context);
        mPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        long firstDate = mPreferences.getLong(KEY_FIRST_START_DATE, 0);
        if (firstDate == 0) {
            resetStartDate();
        }
    }

    public static void check() {
        if (mDialog != null && mDialog.isShowing()) {
            return;
        }
        if (shouldShow()) {
            try {
                mDialog = createDialog();
                if (mDialog != null) {
                    mDialog.show();
                }
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    private static void rateNow() {
        Context context = mContext.get();
        if (context == null) {
            return;
        }
        IntentUtils.openInBrowser(context, "https://play.google.com/store/apps/details?id=de.danoeh.antennapod");
        saveRated();
    }

    private static boolean rated() {
        return mPreferences.getBoolean(KEY_RATED, false);
    }

    @VisibleForTesting
    public static void saveRated() {
        mPreferences
                .edit()
                .putBoolean(KEY_RATED, true)
                .apply();
    }

    private static void resetStartDate() {
        mPreferences
                .edit()
                .putLong(KEY_FIRST_START_DATE, System.currentTimeMillis())
                .apply();
    }

    private static boolean shouldShow() {
        if (rated()) {
            return false;
        }

        long now = System.currentTimeMillis();
        long firstDate = mPreferences.getLong(KEY_FIRST_START_DATE, now);
        long diff = now - firstDate;
        long diffDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        return diffDays >= AFTER_DAYS;
    }

    @Nullable
    private static AlertDialog createDialog() {
        Context context = mContext.get();
        if (context == null) {
            return null;
        }
        return new AlertDialog.Builder(context)
                .setTitle(R.string.rating_title)
                .setMessage(R.string.rating_message)
                .setPositiveButton(R.string.rating_now_label, (dialog, which) -> rateNow())
                .setNegativeButton(R.string.rating_never_label, (dialog, which) -> saveRated())
                .setNeutralButton(R.string.rating_later_label, (dialog, which) -> resetStartDate())
                .setOnCancelListener(dialog1 -> resetStartDate())
                .create();
    }
}