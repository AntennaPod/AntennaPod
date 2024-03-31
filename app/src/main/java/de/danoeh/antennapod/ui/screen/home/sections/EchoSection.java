package de.danoeh.antennapod.ui.screen.home.sections;

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
import de.danoeh.antennapod.databinding.HomeSectionEchoBinding;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.StatisticsItem;
import de.danoeh.antennapod.ui.echo.EchoActivity;
import de.danoeh.antennapod.ui.echo.EchoConfig;
import de.danoeh.antennapod.ui.screen.home.HomeFragment;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class EchoSection extends Fragment {
    private HomeSectionEchoBinding viewBinding;
    private Disposable disposable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewBinding = HomeSectionEchoBinding.inflate(inflater);
        viewBinding.titleLabel.setText(getString(R.string.antennapod_echo_year, EchoConfig.RELEASE_YEAR));
        viewBinding.echoButton.setOnClickListener(v -> startActivity(new Intent(getContext(), EchoActivity.class)));
        viewBinding.closeButton.setOnClickListener(v -> hideThisYear());
        updateVisibility();
        return viewBinding.getRoot();
    }

    private void updateVisibility() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(
            () -> {
                DBReader.StatisticsResult statisticsResult = DBReader.getStatistics(
                        false, EchoConfig.jan1(), Long.MAX_VALUE);
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
                .edit().putInt(HomeFragment.PREF_HIDE_ECHO, EchoConfig.RELEASE_YEAR).apply();
        ((MainActivity) getActivity()).loadFragment(HomeFragment.TAG, null);
    }
}
