package de.danoeh.antennapod.ui.echo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.echo.databinding.EchoActivityBinding;
import de.danoeh.antennapod.ui.echo.screen.EchoScreen;
import de.danoeh.antennapod.ui.echo.screen.FinalShareScreen;
import de.danoeh.antennapod.ui.echo.screen.HoarderScreen;
import de.danoeh.antennapod.ui.echo.screen.HoursPlayedScreen;
import de.danoeh.antennapod.ui.echo.screen.IntroScreen;
import de.danoeh.antennapod.ui.echo.screen.QueueScreen;
import de.danoeh.antennapod.ui.echo.screen.ThanksScreen;
import de.danoeh.antennapod.ui.echo.screen.TimeReleasePlayScreen;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EchoActivity extends AppCompatActivity {
    private static final String TAG = "EchoActivity";
    private static final int NUM_SCREENS = 7;

    private EchoActivityBinding viewBinding;
    private int currentScreenIdx = -1;
    private boolean progressPaused = false;
    private float progress = 0;
    private EchoProgress echoProgress;
    private Disposable redrawTimer;
    private long timeTouchDown;
    private long timeLastFrame;
    private Disposable disposable;
    private List<EchoScreen> screens;
    private EchoScreen currentScreen;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        screens =  List.of(new IntroScreen(this, getLayoutInflater()),
                new HoursPlayedScreen(this, getLayoutInflater()), new QueueScreen(this, getLayoutInflater()),
                new TimeReleasePlayScreen(this, getLayoutInflater()), new HoarderScreen(this, getLayoutInflater()),
                new ThanksScreen(this, getLayoutInflater()), new FinalShareScreen(this, getLayoutInflater()));
        viewBinding = EchoActivityBinding.inflate(getLayoutInflater());
        viewBinding.closeButton.setOnClickListener(v -> finish());
        viewBinding.screenContainer.setOnTouchListener((v, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                progressPaused = true;
                timeTouchDown = System.currentTimeMillis();
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                progressPaused = false;
                if (timeTouchDown + 500 > System.currentTimeMillis()) {
                    int newScreen;
                    if (event.getX() < 0.5f * viewBinding.screenContainer.getMeasuredWidth()) {
                        newScreen = Math.max(currentScreenIdx - 1, 0);
                    } else {
                        newScreen = Math.min(currentScreenIdx + 1, NUM_SCREENS - 1);
                        if (currentScreenIdx == NUM_SCREENS - 1) {
                            finish();
                        }
                    }
                    progress = newScreen;
                    echoProgress.setProgress(progress);
                    loadScreen(newScreen, false);
                }
            }
            return true;
        });
        echoProgress = new EchoProgress(NUM_SCREENS);
        viewBinding.echoProgressImage.setImageDrawable(echoProgress);
        setContentView(viewBinding.getRoot());
        loadScreen(0, false);
        loadStatistics();
    }

    @Override
    protected void onStart() {
        super.onStart();
        redrawTimer = Flowable.timer(20, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .repeat()
                .subscribe(i -> {
                    if (progressPaused) {
                        return;
                    }
                    currentScreen.postInvalidate();
                    if (progress >= NUM_SCREENS - 0.001f) {
                        return;
                    }
                    long timePassed = System.currentTimeMillis() - timeLastFrame;
                    timeLastFrame = System.currentTimeMillis();
                    if (timePassed > 500) {
                        timePassed = 0;
                    }
                    progress = Math.min(NUM_SCREENS - 0.001f, progress + timePassed / 10000.0f);
                    echoProgress.setProgress(progress);
                    viewBinding.echoProgressImage.postInvalidate();
                    loadScreen((int) progress, false);
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        redrawTimer.dispose();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void loadScreen(int screen, boolean force) {
        if (screen == currentScreenIdx && !force) {
            return;
        }
        currentScreenIdx = screen;
        currentScreen = screens.get(currentScreenIdx);
        runOnUiThread(() -> {
            viewBinding.screenContainer.removeAllViews();
            viewBinding.screenContainer.addView(currentScreen.getView());
        });
    }

    private void loadStatistics() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(
                () -> {
                    DBReader.StatisticsResult statisticsData = DBReader.getStatistics(
                            false, EchoConfig.jan1(), Long.MAX_VALUE);
                    Collections.sort(statisticsData.feedTime, (item1, item2) ->
                            Long.compare(item2.timePlayed, item1.timePlayed));
                    return statisticsData;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    for (EchoScreen screen : screens) {
                        screen.startLoading(result);
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
