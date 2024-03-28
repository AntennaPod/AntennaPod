package de.danoeh.antennapod.ui.echo.screen;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.common.Converter;
import de.danoeh.antennapod.ui.echo.EchoConfig;
import de.danoeh.antennapod.ui.echo.R;
import de.danoeh.antennapod.ui.echo.background.RotatingSquaresBackground;
import de.danoeh.antennapod.ui.echo.databinding.SimpleEchoScreenBinding;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TimeReleasePlayScreen extends EchoScreen {
    private static final String TAG = "TimeReleasePlayScreen";
    private final SimpleEchoScreenBinding viewBinding;
    private Disposable disposable;

    public TimeReleasePlayScreen(Context context, LayoutInflater layoutInflater) {
        super(context);
        viewBinding = SimpleEchoScreenBinding.inflate(layoutInflater);
        viewBinding.aboveLabel.setText(R.string.echo_listened_after_title);
        viewBinding.backgroundImage.setImageDrawable(new RotatingSquaresBackground(context));
    }

    private void display(long timeBetweenReleaseAndPlay) {
        if (timeBetweenReleaseAndPlay <= 1000L * 3600 * 24 * 2.5) {
            viewBinding.largeLabel.setText(R.string.echo_listened_after_emoji_run);
            viewBinding.belowLabel.setText(R.string.echo_listened_after_comment_addict);
        } else {
            viewBinding.largeLabel.setText(R.string.echo_listened_after_emoji_yoga);
            viewBinding.belowLabel.setText(R.string.echo_listened_after_comment_easy);
        }
        viewBinding.smallLabel.setText(context.getString(R.string.echo_listened_after_time,
                Converter.getDurationStringLocalized(
                        getLocalizedResources(getEchoLanguage()), timeBetweenReleaseAndPlay, true)));
    }

    @Override
    public View getView() {
        return viewBinding.getRoot();
    }

    @Override
    public void postInvalidate() {
        viewBinding.backgroundImage.postInvalidate();
    }

    @Override
    public void startLoading(DBReader.StatisticsResult statisticsResult) {
        super.startLoading(statisticsResult);
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() ->
                        DBReader.getTimeBetweenReleaseAndPlayback(EchoConfig.jan1(), Long.MAX_VALUE))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::display, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
