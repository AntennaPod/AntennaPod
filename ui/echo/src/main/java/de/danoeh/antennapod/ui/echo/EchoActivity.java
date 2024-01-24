package de.danoeh.antennapod.ui.echo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.core.feed.util.PlaybackSpeedUtils;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.StatisticsItem;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.echo.databinding.EchoActivityBinding;
import de.danoeh.antennapod.ui.echo.screens.BubbleScreen;
import de.danoeh.antennapod.ui.echo.screens.FinalShareScreen;
import de.danoeh.antennapod.ui.echo.screens.RotatingSquaresScreen;
import de.danoeh.antennapod.ui.echo.screens.StripesScreen;
import de.danoeh.antennapod.ui.echo.screens.WaveformScreen;
import de.danoeh.antennapod.ui.echo.screens.WavesScreen;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EchoActivity extends AppCompatActivity {
    public static final int RELEASE_YEAR = 2023;
    private static final String TAG = "EchoActivity";
    private static final int NUM_SCREENS = 7;
    private static final int SHARE_SIZE = 1000;

    private EchoActivityBinding viewBinding;
    private int currentScreen = -1;
    private boolean progressPaused = false;
    private float progress = 0;
    private Drawable currentDrawable;
    private EchoProgress echoProgress;
    private Disposable redrawTimer;
    private long timeTouchDown;
    private long timeLastFrame;
    private Disposable disposable;
    private Disposable disposableFavorite;

    private long totalTime = 0;
    private int totalActivePodcasts = 0;
    private int playedPodcasts = 0;
    private int playedActivePodcasts = 0;
    private String randomUnplayedActivePodcast = "";
    private int queueNumEpisodes = 0;
    private long queueSecondsLeft = 0;
    private long timeBetweenReleaseAndPlay = 0;
    private long oldestDate = 0;
    private final ArrayList<String> favoritePodNames = new ArrayList<>();
    private final ArrayList<Drawable> favoritePodImages = new ArrayList<>();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        viewBinding = EchoActivityBinding.inflate(getLayoutInflater());
        viewBinding.closeButton.setOnClickListener(v -> finish());
        viewBinding.shareButton.setOnClickListener(v -> share());
        viewBinding.echoImage.setOnTouchListener((v, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                progressPaused = true;
                timeTouchDown = System.currentTimeMillis();
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                progressPaused = false;
                if (timeTouchDown + 500 > System.currentTimeMillis()) {
                    int newScreen;
                    if (event.getX() < 0.5f * viewBinding.echoImage.getMeasuredWidth()) {
                        newScreen = Math.max(currentScreen - 1, 0);
                    } else {
                        newScreen = Math.min(currentScreen + 1, NUM_SCREENS - 1);
                        if (currentScreen == NUM_SCREENS - 1) {
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

    private void share() {
        try {
            Bitmap bitmap = Bitmap.createBitmap(SHARE_SIZE, SHARE_SIZE, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            currentDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            currentDrawable.draw(canvas);
            viewBinding.echoImage.setImageDrawable(null);
            viewBinding.echoImage.setImageDrawable(currentDrawable);
            File file = new File(UserPreferences.getDataFolder(null), "AntennaPodEcho.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.close();

            Uri fileUri = FileProvider.getUriForFile(this, getString(R.string.provider_authority), file);
            new ShareCompat.IntentBuilder(this)
                    .setType("image/png")
                    .addStream(fileUri)
                    .setText(getString(R.string.echo_share, RELEASE_YEAR))
                    .setChooserTitle(R.string.share_file_label)
                    .startChooser();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                    viewBinding.echoImage.postInvalidate();
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
        if (disposableFavorite != null) {
            disposableFavorite.dispose();
        }
    }

    private void loadScreen(int screen, boolean force) {
        if (screen == currentScreen && !force) {
            return;
        }
        currentScreen = screen;
        runOnUiThread(() -> {
            viewBinding.echoLogo.setVisibility(currentScreen == 0 ? View.VISIBLE : View.GONE);
            viewBinding.shareButton.setVisibility(currentScreen == 6 ? View.VISIBLE : View.GONE);

            switch (currentScreen) {
                case 0:
                    viewBinding.aboveLabel.setText(R.string.echo_intro_your_year);
                    viewBinding.largeLabel.setText(String.format(getEchoLanguage(), "%d", RELEASE_YEAR));
                    viewBinding.belowLabel.setText(R.string.echo_intro_in_podcasts);
                    viewBinding.smallLabel.setText(R.string.echo_intro_locally);
                    currentDrawable = new BubbleScreen(this);
                    break;
                case 1:
                    viewBinding.aboveLabel.setText(R.string.echo_hours_this_year);
                    viewBinding.largeLabel.setText(String.format(getEchoLanguage(), "%d", totalTime / 3600));
                    viewBinding.belowLabel.setText(getResources()
                            .getQuantityString(R.plurals.echo_hours_podcasts, playedPodcasts, playedPodcasts));
                    viewBinding.smallLabel.setText("");
                    currentDrawable = new WaveformScreen(this);
                    break;
                case 2:
                    viewBinding.largeLabel.setText(String.format(getEchoLanguage(), "%d", queueSecondsLeft / 3600));
                    viewBinding.belowLabel.setText(getResources().getQuantityString(
                            R.plurals.echo_queue_hours_waiting, queueNumEpisodes, queueNumEpisodes));
                    Calendar dec31 = Calendar.getInstance();
                    dec31.set(Calendar.DAY_OF_MONTH, 31);
                    dec31.set(Calendar.MONTH, Calendar.DECEMBER);
                    int daysUntilNextYear = Math.max(1,
                            dec31.get(Calendar.DAY_OF_YEAR) - Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + 1);
                    long secondsPerDay = queueSecondsLeft / daysUntilNextYear;
                    String timePerDay = Converter.getDurationStringLocalized(
                            getLocalizedResources(this, getEchoLanguage()), secondsPerDay * 1000);
                    double hoursPerDay = (double) (secondsPerDay / 3600);
                    int nextYear = RELEASE_YEAR + 1;
                    if (hoursPerDay < 1.5) {
                        viewBinding.aboveLabel.setText(R.string.echo_queue_title_clean);
                        viewBinding.smallLabel.setText(
                                getString(R.string.echo_queue_hours_clean, timePerDay, nextYear));
                    } else if (hoursPerDay <= 24) {
                        viewBinding.aboveLabel.setText(R.string.echo_queue_title_many);
                        viewBinding.smallLabel.setText(
                                getString(R.string.echo_queue_hours_normal, timePerDay, nextYear));
                    } else {
                        viewBinding.aboveLabel.setText(R.string.echo_queue_title_many);
                        viewBinding.smallLabel.setText(getString(R.string.echo_queue_hours_much, timePerDay, nextYear));
                    }
                    currentDrawable = new StripesScreen(this);
                    break;
                case 3:
                    viewBinding.aboveLabel.setText(R.string.echo_listened_after_title);
                    if (timeBetweenReleaseAndPlay <= 1000L * 3600 * 24 * 2.5) {
                        viewBinding.largeLabel.setText(R.string.echo_listened_after_emoji_run);
                        viewBinding.belowLabel.setText(R.string.echo_listened_after_comment_addict);
                    } else {
                        viewBinding.largeLabel.setText(R.string.echo_listened_after_emoji_yoga);
                        viewBinding.belowLabel.setText(R.string.echo_listened_after_comment_easy);
                    }
                    viewBinding.smallLabel.setText(getString(R.string.echo_listened_after_time,
                            Converter.getDurationStringLocalized(
                                getLocalizedResources(this, getEchoLanguage()), timeBetweenReleaseAndPlay)));
                    currentDrawable = new RotatingSquaresScreen(this);
                    break;
                case 4:
                    viewBinding.aboveLabel.setText(R.string.echo_hoarder_title);
                    int percentagePlayed = (int) (100.0 * playedActivePodcasts / totalActivePodcasts);
                    if (percentagePlayed < 25) {
                        viewBinding.largeLabel.setText(R.string.echo_hoarder_emoji_cabinet);
                        viewBinding.belowLabel.setText(R.string.echo_hoarder_subtitle_hoarder);
                        viewBinding.smallLabel.setText(getString(R.string.echo_hoarder_comment_hoarder,
                                percentagePlayed, totalActivePodcasts));
                    } else if (percentagePlayed < 75) {
                        viewBinding.largeLabel.setText(R.string.echo_hoarder_emoji_check);
                        viewBinding.belowLabel.setText(R.string.echo_hoarder_subtitle_medium);
                        viewBinding.smallLabel.setText(getString(R.string.echo_hoarder_comment_medium,
                                percentagePlayed, totalActivePodcasts, randomUnplayedActivePodcast));
                    } else {
                        viewBinding.largeLabel.setText(R.string.echo_hoarder_emoji_clean);
                        viewBinding.belowLabel.setText(R.string.echo_hoarder_subtitle_clean);
                        viewBinding.smallLabel.setText(getString(R.string.echo_hoarder_comment_clean,
                                percentagePlayed, totalActivePodcasts));
                    }
                    currentDrawable = new WavesScreen(this);
                    break;
                case 5:
                    viewBinding.aboveLabel.setText("");
                    viewBinding.largeLabel.setText(R.string.echo_thanks_large);
                    if (oldestDate < jan1()) {
                        String skeleton = DateFormat.getBestDateTimePattern(getEchoLanguage(), "MMMM yyyy");
                        SimpleDateFormat dateFormat = new SimpleDateFormat(skeleton, getEchoLanguage());
                        String dateFrom = dateFormat.format(new Date(oldestDate));
                        viewBinding.belowLabel.setText(getString(R.string.echo_thanks_we_are_glad_old, dateFrom));
                    } else {
                        viewBinding.belowLabel.setText(R.string.echo_thanks_we_are_glad_new);
                    }
                    viewBinding.smallLabel.setText(R.string.echo_thanks_now_favorite);
                    currentDrawable = new RotatingSquaresScreen(this);
                    break;
                case 6:
                    viewBinding.aboveLabel.setText("");
                    viewBinding.largeLabel.setText("");
                    viewBinding.belowLabel.setText("");
                    viewBinding.smallLabel.setText("");
                    currentDrawable = new FinalShareScreen(this, favoritePodNames, favoritePodImages);
                    break;
                default: // Keep
            }
            viewBinding.echoImage.setImageDrawable(currentDrawable);
        });
    }

    private Locale getEchoLanguage() {
        boolean hasTranslation = !getString(R.string.echo_listened_after_title)
                .equals(getLocalizedResources(this, Locale.US).getString(R.string.echo_listened_after_title));
        if (hasTranslation) {
            return Locale.getDefault();
        } else {
            return Locale.US;
        }
    }

    @NonNull
    private Resources getLocalizedResources(Context context, Locale desiredLocale) {
        Configuration conf = context.getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(desiredLocale);
        Context localizedContext = context.createConfigurationContext(conf);
        return localizedContext.getResources();
    }

    private long jan1() {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        date.set(Calendar.DAY_OF_MONTH, 1);
        date.set(Calendar.MONTH, 0);
        date.set(Calendar.YEAR, RELEASE_YEAR);
        return date.getTimeInMillis();
    }

    private void loadStatistics() {
        if (disposable != null) {
            disposable.dispose();
        }
        long timeFilterFrom = jan1();
        long timeFilterTo = Long.MAX_VALUE;
        disposable = Observable.fromCallable(
                () -> {
                    DBReader.StatisticsResult statisticsData = DBReader.getStatistics(
                            false, timeFilterFrom, timeFilterTo);
                    Collections.sort(statisticsData.feedTime, (item1, item2) ->
                            Long.compare(item2.timePlayed, item1.timePlayed));

                    favoritePodNames.clear();
                    for (int i = 0; i < 5 && i < statisticsData.feedTime.size(); i++) {
                        favoritePodNames.add(statisticsData.feedTime.get(i).feed.getTitle());
                    }
                    loadFavoritePodImages(statisticsData);

                    totalActivePodcasts = 0;
                    playedActivePodcasts = 0;
                    playedPodcasts = 0;
                    totalTime = 0;
                    ArrayList<String> unplayedActive = new ArrayList<>();
                    for (StatisticsItem item : statisticsData.feedTime) {
                        totalTime += item.timePlayed;
                        if (item.timePlayed > 0) {
                            playedPodcasts++;
                        }
                        if (item.feed.getPreferences().getKeepUpdated()) {
                            totalActivePodcasts++;
                            if (item.timePlayed > 0) {
                                playedActivePodcasts++;
                            } else {
                                unplayedActive.add(item.feed.getTitle());
                            }
                        }
                    }
                    if (!unplayedActive.isEmpty()) {
                        randomUnplayedActivePodcast = unplayedActive.get((int) (Math.random() * unplayedActive.size()));
                    }

                    List<FeedItem> queue = DBReader.getQueue();
                    queueNumEpisodes = queue.size();
                    queueSecondsLeft = 0;
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

                    timeBetweenReleaseAndPlay = DBReader.getTimeBetweenReleaseAndPlayback(timeFilterFrom, timeFilterTo);
                    oldestDate = statisticsData.oldestDate;
                    return statisticsData;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> loadScreen(currentScreen, true),
                        error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    void loadFavoritePodImages(DBReader.StatisticsResult statisticsData) {
        if (disposableFavorite != null) {
            disposableFavorite.dispose();
        }
        disposableFavorite = Observable.fromCallable(
                () -> {
                    favoritePodImages.clear();
                    for (int i = 0; i < 5 && i < statisticsData.feedTime.size(); i++) {
                        BitmapDrawable cover = new BitmapDrawable(getResources(), (Bitmap) null);
                        try {
                            final int size = SHARE_SIZE / 3;
                            final int radius = (i == 0) ? (size / 16) : (size / 8);
                            cover = new BitmapDrawable(getResources(), Glide.with(this)
                                    .asBitmap()
                                    .load(statisticsData.feedTime.get(i).feed.getImageUrl())
                                    .apply(new RequestOptions()
                                            .fitCenter()
                                            .transform(new RoundedCorners(radius)))
                                    .submit(size, size)
                                    .get(5, TimeUnit.SECONDS));
                        } catch (Exception e) {
                            Log.d(TAG, "Loading cover: " + e.getMessage());
                        }
                        favoritePodImages.add(cover);
                    }
                    return statisticsData;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> { },
                        error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
