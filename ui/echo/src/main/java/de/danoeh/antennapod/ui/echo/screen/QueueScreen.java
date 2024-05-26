package de.danoeh.antennapod.ui.echo.screen;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.common.Converter;
import de.danoeh.antennapod.ui.echo.EchoConfig;
import de.danoeh.antennapod.ui.echo.R;
import de.danoeh.antennapod.ui.echo.background.StripesBackground;
import de.danoeh.antennapod.ui.echo.databinding.SimpleEchoScreenBinding;
import de.danoeh.antennapod.ui.episodes.PlaybackSpeedUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.Calendar;

public class QueueScreen extends EchoScreen {
    private static final String TAG = "QueueScreen";
    private final SimpleEchoScreenBinding viewBinding;
    private Disposable disposable;

    public QueueScreen(Context context, LayoutInflater layoutInflater) {
        super(context);
        viewBinding = SimpleEchoScreenBinding.inflate(layoutInflater);
        viewBinding.backgroundImage.setImageDrawable(new StripesBackground(context));
    }

    private void display(int queueNumEpisodes, long queueSecondsLeft) {
        viewBinding.largeLabel.setText(String.format(getEchoLanguage(), "%d", queueSecondsLeft / 3600));
        viewBinding.belowLabel.setText(context.getResources().getQuantityString(
                R.plurals.echo_queue_hours_waiting, queueNumEpisodes, queueNumEpisodes));

        Calendar dec31 = Calendar.getInstance();
        dec31.set(Calendar.DAY_OF_MONTH, 31);
        dec31.set(Calendar.MONTH, Calendar.DECEMBER);
        int daysUntilNextYear = Math.max(1,
                dec31.get(Calendar.DAY_OF_YEAR) - Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + 1);
        long secondsPerDay = queueSecondsLeft / daysUntilNextYear;
        String timePerDay = Converter.getDurationStringLocalized(
                getLocalizedResources(getEchoLanguage()), secondsPerDay * 1000, true);
        double hoursPerDay = secondsPerDay / 3600.0;
        int nextYear = EchoConfig.RELEASE_YEAR + 1;
        if (hoursPerDay < 1.5) {
            viewBinding.aboveLabel.setText(R.string.echo_queue_title_clean);
            viewBinding.smallLabel.setText(
                    context.getString(R.string.echo_queue_hours_clean, timePerDay, nextYear));
        } else if (hoursPerDay <= 24) {
            viewBinding.aboveLabel.setText(R.string.echo_queue_title_many);
            viewBinding.smallLabel.setText(
                    context.getString(R.string.echo_queue_hours_normal, timePerDay, nextYear));
        } else {
            viewBinding.aboveLabel.setText(R.string.echo_queue_title_many);
            viewBinding.smallLabel.setText(context.getString(R.string.echo_queue_hours_much, timePerDay, nextYear));
        }
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
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(DBReader::getQueue)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(queue -> {
                    long queueSecondsLeft = 0;
                    for (FeedItem item : queue) {
                        float playbackSpeed = 1;
                        if (UserPreferences.timeRespectsSpeed()) {
                            playbackSpeed = PlaybackSpeedUtils.getCurrentPlaybackSpeed(item.getMedia());
                        }
                        if (item.getMedia() != null) {
                            long itemTimeLeft = item.getMedia().getDuration() - item.getMedia().getPosition();
                            queueSecondsLeft += itemTimeLeft / playbackSpeed;
                        }
                    }
                    queueSecondsLeft /= 1000;
                    display(queue.size(), queueSecondsLeft);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
