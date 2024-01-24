package de.danoeh.antennapod.ui.home.sections;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.StatisticsItem;
import de.danoeh.antennapod.databinding.HomeSectionEchoBinding;
import de.danoeh.antennapod.ui.echo.EchoActivity;
import de.danoeh.antennapod.ui.home.HomeFragment;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.Calendar;

public class EchoSection extends Fragment {
    private HomeSectionEchoBinding viewBinding;
    private Disposable disposable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewBinding = HomeSectionEchoBinding.inflate(inflater);
        viewBinding.titleLabel.setText(getString(R.string.antennapod_echo_year, EchoActivity.RELEASE_YEAR));
        viewBinding.echoButton.setOnClickListener(v -> startActivity(new Intent(getContext(), EchoActivity.class)));
        viewBinding.closeButton.setOnClickListener(v -> hideThisYear());
        updateVisibility();
        return viewBinding.getRoot();
    }

    private long jan1() {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        date.set(Calendar.DAY_OF_MONTH, 1);
        date.set(Calendar.MONTH, 0);
        date.set(Calendar.YEAR, EchoActivity.RELEASE_YEAR);
        return date.getTimeInMillis();
    }

    private void updateVisibility() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(
            () -> {
                DBReader.StatisticsResult statisticsResult = DBReader.getStatistics(false, jan1(), Long.MAX_VALUE);
                long totalTime = 0;
                for (StatisticsItem feedTime : statisticsResult.feedTime) {
                    totalTime += feedTime.timePlayed;
                }
                return totalTime;
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(totalTime -> {
                boolean shouldShow = (totalTime >= 3600 * 10);
                viewBinding.getRoot().setVisibility(shouldShow ? View.VISIBLE : View.GONE);
                if (!shouldShow) {
                    hideThisYear();
                }
            }, Throwable::printStackTrace);
    }

    void hideThisYear() {
        getContext().getSharedPreferences(HomeFragment.PREF_NAME, Context.MODE_PRIVATE)
                .edit().putInt(HomeFragment.PREF_HIDE_ECHO, EchoActivity.RELEASE_YEAR).apply();
        ((MainActivity) getActivity()).loadFragment(HomeFragment.TAG, null);
    }
}
