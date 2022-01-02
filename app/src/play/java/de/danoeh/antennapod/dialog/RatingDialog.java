package de.danoeh.antennapod.dialog;

import android.app.Activity;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.VisibleForTesting;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;

public class RatingDialog {

    private RatingDialog() {
    }

    private static final String TAG = RatingDialog.class.getSimpleName();
    private static final int AFTER_DAYS = 14;

    private static WeakReference<Context> mContext;
    private static SharedPreferences mPreferences;

    private static final String PREFS_NAME = "RatingPrefs";
    private static final String KEY_RATED = "KEY_WAS_RATED";
    private static final String KEY_FIRST_START_DATE = "KEY_FIRST_HIT_DATE";
    private static final String KEY_NUMBER_OF_REVIEWS = "NUMBER_OF_REVIEW_ATTEMPTS";

    public static void init(Context context) {
        mContext = new WeakReference<>(context);
        mPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        long firstDate = mPreferences.getLong(KEY_FIRST_START_DATE, 0);
        if (firstDate == 0) {
            resetStartDate();
        }
    }

    public static void check() {
        if (shouldShow()) {
            try {
                showInAppReview();
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    private static void showInAppReview() {
        Context context = mContext.get();
        if (context == null) {
            return;
        }

        ReviewManager manager = ReviewManagerFactory.create(context);
        Task<ReviewInfo> request = manager.requestReviewFlow();

        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow((Activity) context, reviewInfo);
                flow.addOnCompleteListener(task1 -> {
                    int previousAttempts = mPreferences.getInt(KEY_NUMBER_OF_REVIEWS, 0);
                    if (previousAttempts >= 3) {
                        saveRated();
                    } else {
                        resetStartDate();
                        mPreferences
                                .edit()
                                .putInt(KEY_NUMBER_OF_REVIEWS, previousAttempts + 1)
                                .apply();
                    }
                    Log.i("ReviewDialog", "Successfully finished in-app review");
                })
                        .addOnFailureListener(error -> {
                            Log.i("ReviewDialog", "failed in reviewing process");
                        });
            }
        })
                .addOnFailureListener(error -> {
                    Log.i("ReviewDialog",  "failed to get in-app review request");
                });
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
}
