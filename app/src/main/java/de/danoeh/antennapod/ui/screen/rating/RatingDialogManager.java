package de.danoeh.antennapod.ui.screen.rating;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

import android.util.Log;
import androidx.fragment.app.FragmentActivity;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.StatisticsItem;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import kotlin.Pair;

public class RatingDialogManager {
    private static final int AFTER_DAYS = 20;
    private static final String TAG = "RatingDialog";
    private static final String PREFS_NAME = "RatingPrefs";
    private static final String KEY_RATED = "KEY_WAS_RATED";
    private static final String KEY_FIRST_START_DATE = "KEY_FIRST_HIT_DATE";

    private final SharedPreferences preferences;
    private final FragmentActivity fragmentActivity;
    private Disposable disposable;

    public RatingDialogManager(FragmentActivity activity) {
        this.fragmentActivity = activity;
        preferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void showIfNeeded() {
        //noinspection ConstantConditions
        if (isRated() || BuildConfig.DEBUG || "free".equals(BuildConfig.FLAVOR)) {
            return;
        } else if (!enoughTimeSinceInstall()) {
            return;
        }

        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(
                () -> {
                    DBReader.StatisticsResult statisticsData = DBReader.getStatistics(false, 0, Long.MAX_VALUE);
                    long totalTime = 0;
                    for (StatisticsItem item : statisticsData.feedTime) {
                        totalTime += item.timePlayed;
                    }
                    return new Pair<>(totalTime, statisticsData.oldestDate);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    long totalTime = result.getFirst();
                    long oldestDate = result.getSecond();
                    if (totalTime < TimeUnit.SECONDS.convert(15, TimeUnit.HOURS)) {
                        return;
                    } else if (oldestDate > System.currentTimeMillis()
                            - TimeUnit.MILLISECONDS.convert(AFTER_DAYS, TimeUnit.DAYS)) {
                        return; // In case the app was opened but nothing was played
                    }
                    RatingDialogFragment.newInstance(result.getFirst(), result.getSecond())
                            .show(fragmentActivity.getSupportFragmentManager(), TAG);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private boolean isRated() {
        return preferences.getBoolean(KEY_RATED, false);
    }

    public void saveRated() {
        preferences.edit().putBoolean(KEY_RATED, true).apply();
    }

    public void resetStartDate() {
        preferences.edit().putLong(KEY_FIRST_START_DATE, System.currentTimeMillis()).apply();
    }

    private boolean enoughTimeSinceInstall() {
        if (preferences.getLong(KEY_FIRST_START_DATE, 0) == 0) {
            resetStartDate();
            return false;
        }
        long now = System.currentTimeMillis();
        long firstDate = preferences.getLong(KEY_FIRST_START_DATE, now);
        long diff = now - firstDate;
        long diffDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        return diffDays >= AFTER_DAYS;
    }
}
