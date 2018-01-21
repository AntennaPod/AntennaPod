package de.danoeh.antennapod.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import java.util.Locale;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.Flavors;
import de.danoeh.antennapod.core.util.ShareUtils;
import de.danoeh.antennapod.core.util.StorageUtils;
import de.danoeh.antennapod.core.util.Supplier;
import de.danoeh.antennapod.core.util.playback.MediaPlayerError;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.dialog.SleepTimerDialog;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Provides general features which are both needed for playing audio and video
 * files.
 */
public abstract class MediaplayerActivity extends CastEnabledActivity implements OnSeekBarChangeListener {
    private static final String TAG = "MediaplayerActivity";
    private static final String PREFS = "MediaPlayerActivityPreferences";
    private static final String PREF_SHOW_TIME_LEFT = "showTimeLeft";

    PlaybackController controller;

    private TextView txtvPosition;
    private TextView txtvLength;
    SeekBar sbPosition;
    private ImageButton butRev;
    private TextView txtvRev;
    private ImageButton butPlay;
    private ImageButton butFF;
    private TextView txtvFF;
    private ImageButton butSkip;

    private boolean showTimeLeft = false;

    private boolean isFavorite = false;

    private PlaybackController newPlaybackController() {
        return new PlaybackController(this, false) {

            @Override
            public void setupGUI() {
                MediaplayerActivity.this.setupGUI();
            }

            @Override
            public void onPositionObserverUpdate() {
                MediaplayerActivity.this.onPositionObserverUpdate();
            }

            @Override
            public void onBufferStart() {
                MediaplayerActivity.this.onBufferStart();
            }

            @Override
            public void onBufferEnd() {
                MediaplayerActivity.this.onBufferEnd();
            }

            @Override
            public void onBufferUpdate(float progress) {
                MediaplayerActivity.this.onBufferUpdate(progress);
            }

            @Override
            public void handleError(int code) {
                MediaplayerActivity.this.handleError(code);
            }

            @Override
            public void onReloadNotification(int code) {
                MediaplayerActivity.this.onReloadNotification(code);
            }

            @Override
            public void onSleepTimerUpdate() {
                supportInvalidateOptionsMenu();
            }

            @Override
            public ImageButton getPlayButton() {
                return butPlay;
            }

            @Override
            public void postStatusMsg(int msg, boolean showToast) {
                MediaplayerActivity.this.postStatusMsg(msg, showToast);
            }

            @Override
            public void clearStatusMsg() {
                MediaplayerActivity.this.clearStatusMsg();
            }

            @Override
            public boolean loadMediaInfo() {
                return MediaplayerActivity.this.loadMediaInfo();
            }

            @Override
            public void onAwaitingVideoSurface() {
                MediaplayerActivity.this.onAwaitingVideoSurface();
            }

            @Override
            public void onServiceQueried() {
                MediaplayerActivity.this.onServiceQueried();
            }

            @Override
            public void onShutdownNotification() {
                finish();
            }

            @Override
            public void onPlaybackEnd() {
                finish();
            }

            @Override
            public void onPlaybackSpeedChange() {
                MediaplayerActivity.this.onPlaybackSpeedChange();
            }

            @Override
            protected void setScreenOn(boolean enable) {
                super.setScreenOn(enable);
                MediaplayerActivity.this.setScreenOn(enable);
            }

            @Override
            public void onSetSpeedAbilityChanged() {
                MediaplayerActivity.this.onSetSpeedAbilityChanged();
            }
        };
    }

    private static TextView getTxtvFFFromActivity(MediaplayerActivity activity) {
        return activity.txtvFF;
    }
    private static TextView getTxtvRevFromActivity(MediaplayerActivity activity) {
        return activity.txtvRev;
    }

    private void onSetSpeedAbilityChanged() {
        Log.d(TAG, "onSetSpeedAbilityChanged()");
        updatePlaybackSpeedButton();
    }

    private void onPlaybackSpeedChange() {
        updatePlaybackSpeedButtonText();
    }

    private void onServiceQueried() {
        supportInvalidateOptionsMenu();
    }

    void chooseTheme() {
        setTheme(UserPreferences.getTheme());
    }

    void setScreenOn(boolean enable) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        chooseTheme();
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");
        StorageUtils.checkStorageAvailability(this);

        orientation = getResources().getConfiguration().orientation;
        getWindow().setFormat(PixelFormat.TRANSPARENT);
    }

    @Override
    protected void onPause() {
        if(controller != null) {
            controller.reinitServiceIfPaused();
            controller.pause();
        }
        super.onPause();
    }

    /**
     * Should be used to switch to another player activity if the mime type is
     * not the correct one for the current activity.
     */
    protected abstract void onReloadNotification(int notificationCode);

    /**
     * Should be used to inform the user that the PlaybackService is currently
     * buffering.
     */
    protected abstract void onBufferStart();

    /**
     * Should be used to hide the view that was showing the 'buffering'-message.
     */
    protected abstract void onBufferEnd();

    private void onBufferUpdate(float progress) {
        if (sbPosition != null) {
            sbPosition.setSecondaryProgress((int) progress * sbPosition.getMax());
        }
    }

    /**
     * Current screen orientation.
     */
    private int orientation;

    @Override
    protected void onStart() {
        super.onStart();
        if (controller != null) {
            controller.release();
        }
        controller = newPlaybackController();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        if (controller != null) {
            controller.release();
            controller = null; // prevent leak
        }
        super.onStop();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.get(this).trimMemory(level);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Glide.get(this).clearMemory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (Flavors.FLAVOR == Flavors.PLAY) {
            requestCastButton(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mediaplayer, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (controller == null) {
            return false;
        }
        Playable media = controller.getMedia();
        boolean isFeedMedia = media != null && (media instanceof FeedMedia);

        menu.findItem(R.id.support_item).setVisible(isFeedMedia && media.getPaymentLink() != null &&
                        ((FeedMedia) media).getItem() != null &&
                        ((FeedMedia) media).getItem().getFlattrStatus().flattrable()
        );

        boolean hasWebsiteLink = media != null && media.getWebsiteLink() != null;
        menu.findItem(R.id.visit_website_item).setVisible(hasWebsiteLink);

        boolean isItemAndHasLink = isFeedMedia &&
                ((FeedMedia) media).getItem() != null && ((FeedMedia) media).getItem().getLink() != null;
        menu.findItem(R.id.share_link_item).setVisible(isItemAndHasLink);
        menu.findItem(R.id.share_link_with_position_item).setVisible(isItemAndHasLink);

        boolean isItemHasDownloadLink = isFeedMedia && ((FeedMedia) media).getDownload_url() != null;
        menu.findItem(R.id.share_download_url_item).setVisible(isItemHasDownloadLink);
        menu.findItem(R.id.share_download_url_with_position_item).setVisible(isItemHasDownloadLink);
        menu.findItem(R.id.share_file).setVisible(isFeedMedia && ((FeedMedia) media).fileExists());

        menu.findItem(R.id.share_item).setVisible(hasWebsiteLink || isItemAndHasLink || isItemHasDownloadLink);

        menu.findItem(R.id.add_to_favorites_item).setVisible(false);
        menu.findItem(R.id.remove_from_favorites_item).setVisible(false);
        if (isFeedMedia) {
            menu.findItem(R.id.add_to_favorites_item).setVisible(!isFavorite);
            menu.findItem(R.id.remove_from_favorites_item).setVisible(isFavorite);
        }

        boolean sleepTimerSet = controller.sleepTimerActive();
        boolean sleepTimerNotSet = controller.sleepTimerNotActive();
        menu.findItem(R.id.set_sleeptimer_item).setVisible(sleepTimerNotSet);
        menu.findItem(R.id.disable_sleeptimer_item).setVisible(sleepTimerSet);

        if (this instanceof AudioplayerActivity) {
            int[] attrs = {R.attr.action_bar_icon_color};
            TypedArray ta = obtainStyledAttributes(UserPreferences.getTheme(), attrs);
            int textColor = ta.getColor(0, Color.GRAY);
            ta.recycle();
            menu.findItem(R.id.audio_controls).setIcon(new IconDrawable(this,
                    FontAwesomeIcons.fa_sliders).color(textColor).actionBarSize());
        } else {
            menu.findItem(R.id.audio_controls).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (controller == null) {
            return false;
        }
        Playable media = controller.getMedia();
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(MediaplayerActivity.this,
                    MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK);

            View cover = findViewById(R.id.imgvCover);
            if (cover != null && Build.VERSION.SDK_INT >= 16) {
                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(MediaplayerActivity.this,
                        cover, "coverTransition");
                startActivity(intent, options.toBundle());
            } else {
                startActivity(intent);
            }
            return true;
        } else {
            if (media != null) {
                switch (item.getItemId()) {
                    case R.id.add_to_favorites_item:
                        if(media instanceof FeedMedia) {
                            FeedItem feedItem = ((FeedMedia)media).getItem();
                            if(feedItem != null) {
                                DBWriter.addFavoriteItem(feedItem);
                                isFavorite = true;
                                invalidateOptionsMenu();
                                Toast.makeText(this, R.string.added_to_favorites, Toast.LENGTH_SHORT)
                                     .show();
                            }
                        }
                        break;
                    case R.id.remove_from_favorites_item:
                        if(media instanceof FeedMedia) {
                            FeedItem feedItem = ((FeedMedia)media).getItem();
                            if(feedItem != null) {
                                DBWriter.removeFavoriteItem(feedItem);
                                isFavorite = false;
                                invalidateOptionsMenu();
                                Toast.makeText(this, R.string.removed_from_favorites, Toast.LENGTH_SHORT)
                                     .show();
                            }
                        }
                        break;
                    case R.id.disable_sleeptimer_item:
                        if (controller.serviceAvailable()) {

                            MaterialDialog.Builder stDialog = new MaterialDialog.Builder(this);
                            stDialog.title(R.string.sleep_timer_label);
                            stDialog.content(getString(R.string.time_left_label)
                                    + Converter.getDurationStringLong((int) controller
                                    .getSleepTimerTimeLeft()));
                            stDialog.positiveText(R.string.disable_sleeptimer_label);
                            stDialog.negativeText(R.string.cancel_label);
                            stDialog.onPositive((dialog, which) -> {
                                dialog.dismiss();
                                controller.disableSleepTimer();
                            });
                            stDialog.onNegative((dialog, which) -> dialog.dismiss());
                            stDialog.build().show();
                        }
                        break;
                    case R.id.set_sleeptimer_item:
                        if (controller.serviceAvailable()) {
                            SleepTimerDialog td = new SleepTimerDialog(this) {
                                @Override
                                public void onTimerSet(long millis, boolean shakeToReset, boolean vibrate) {
                                    controller.setSleepTimer(millis, shakeToReset, vibrate);
                                }
                            };
                            td.createNewDialog().show();
                        }
                        break;
                    case R.id.audio_controls:
                        MaterialDialog dialog = new MaterialDialog.Builder(this)
                                .title(R.string.audio_controls)
                                .customView(R.layout.audio_controls, true)
                                .neutralText(R.string.close_label)
                                .onNeutral((dialog1, which) -> {
                                    final SeekBar left = (SeekBar) dialog1.findViewById(R.id.volume_left);
                                    final SeekBar right = (SeekBar) dialog1.findViewById(R.id.volume_right);
                                    UserPreferences.setVolume(left.getProgress(), right.getProgress());
                                })
                                .show();
                        final SeekBar barPlaybackSpeed = (SeekBar) dialog.findViewById(R.id.playback_speed);
                        final Button butDecSpeed = (Button) dialog.findViewById(R.id.butDecSpeed);
                        butDecSpeed.setOnClickListener(v -> {
                            if(controller != null && controller.canSetPlaybackSpeed()) {
                                barPlaybackSpeed.setProgress(barPlaybackSpeed.getProgress() - 2);
                            } else {
                                VariableSpeedDialog.showGetPluginDialog(this);
                            }
                        });
                        final Button butIncSpeed = (Button) dialog.findViewById(R.id.butIncSpeed);
                        butIncSpeed.setOnClickListener(v -> {
                            if(controller != null && controller.canSetPlaybackSpeed()) {
                                barPlaybackSpeed.setProgress(barPlaybackSpeed.getProgress() + 2);
                            } else {
                                VariableSpeedDialog.showGetPluginDialog(this);
                            }
                        });

                        final TextView txtvPlaybackSpeed = (TextView) dialog.findViewById(R.id.txtvPlaybackSpeed);
                        float currentSpeed = 1.0f;
                        try {
                            currentSpeed = Float.parseFloat(UserPreferences.getPlaybackSpeed());
                        } catch (NumberFormatException e) {
                            Log.e(TAG, Log.getStackTraceString(e));
                            UserPreferences.setPlaybackSpeed(String.valueOf(currentSpeed));
                        }

                        txtvPlaybackSpeed.setText(String.format("%.2fx", currentSpeed));
                        barPlaybackSpeed.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                if(controller != null && controller.canSetPlaybackSpeed()) {
                                    float playbackSpeed = (progress + 10) / 20.0f;
                                    controller.setPlaybackSpeed(playbackSpeed);
                                    String speedPref = String.format(Locale.US, "%.2f", playbackSpeed);
                                    UserPreferences.setPlaybackSpeed(speedPref);
                                    String speedStr = String.format("%.2fx", playbackSpeed);
                                    txtvPlaybackSpeed.setText(speedStr);
                                } else if(fromUser) {
                                    float speed = Float.valueOf(UserPreferences.getPlaybackSpeed());
                                    barPlaybackSpeed.post(() -> barPlaybackSpeed.setProgress((int) (20 * speed) - 10));
                                }
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {
                                if(controller != null && !controller.canSetPlaybackSpeed()) {
                                    VariableSpeedDialog.showGetPluginDialog(MediaplayerActivity.this);
                                }
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
                            }
                        });
                        barPlaybackSpeed.setProgress((int) (20 * currentSpeed) - 10);

                        final SeekBar barLeftVolume = (SeekBar) dialog.findViewById(R.id.volume_left);
                        barLeftVolume.setProgress(UserPreferences.getLeftVolumePercentage());
                        final SeekBar barRightVolume = (SeekBar) dialog.findViewById(R.id.volume_right);
                        barRightVolume.setProgress(UserPreferences.getRightVolumePercentage());
                        final CheckBox stereoToMono = (CheckBox) dialog.findViewById(R.id.stereo_to_mono);
                        stereoToMono.setChecked(UserPreferences.stereoToMono());
                        if (controller != null && !controller.canDownmix()) {
                            stereoToMono.setEnabled(false);
                            String sonicOnly = getString(R.string.sonic_only);
                            stereoToMono.setText(stereoToMono.getText() + " [" + sonicOnly + "]");
                        }

                        barLeftVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                controller.setVolume(
                                        Converter.getVolumeFromPercentage(progress),
                                        Converter.getVolumeFromPercentage(barRightVolume.getProgress()));
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
                            }
                        });
                        barRightVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                controller.setVolume(
                                        Converter.getVolumeFromPercentage(barLeftVolume.getProgress()),
                                        Converter.getVolumeFromPercentage(progress));
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
                            }
                        });
                        stereoToMono.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            UserPreferences.stereoToMono(isChecked);
                            if (controller != null) {
                                controller.setDownmix(isChecked);
                            }
                        });
                        break;
                    case R.id.visit_website_item:
                        Uri uri = Uri.parse(media.getWebsiteLink());
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                        break;
                    case R.id.support_item:
                        if (media instanceof FeedMedia) {
                            DBTasks.flattrItemIfLoggedIn(this, ((FeedMedia) media).getItem());
                        }
                        break;
                    case R.id.share_link_item:
                        if (media instanceof FeedMedia) {
                            ShareUtils.shareFeedItemLink(this, ((FeedMedia) media).getItem());
                        }
                        break;
                    case R.id.share_download_url_item:
                        if (media instanceof FeedMedia) {
                            ShareUtils.shareFeedItemDownloadLink(this, ((FeedMedia) media).getItem());
                        }
                        break;
                    case R.id.share_link_with_position_item:
                        if (media instanceof FeedMedia) {
                            ShareUtils.shareFeedItemLink(this, ((FeedMedia) media).getItem(), true);
                        }
                        break;
                    case R.id.share_download_url_with_position_item:
                        if (media instanceof FeedMedia) {
                            ShareUtils.shareFeedItemDownloadLink(this, ((FeedMedia) media).getItem(), true);
                        }
                        break;
                    case R.id.share_file:
                        if (media instanceof FeedMedia) {
                            ShareUtils.shareFeedItemFile(this, ((FeedMedia) media));
                        }
                        break;
                    default:
                        return false;
                }
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        StorageUtils.checkStorageAvailability(this);
        if(controller != null) {
            controller.init();
        }
    }

    /**
     * Called by 'handleStatus()' when the PlaybackService is waiting for
     * a video surface.
     */
    protected abstract void onAwaitingVideoSurface();

    protected abstract void postStatusMsg(int resId, boolean showToast);

    protected abstract void clearStatusMsg();

    void onPositionObserverUpdate() {
        if (controller == null || txtvPosition == null || txtvLength == null) {
            return;
        }
        int currentPosition = controller.getPosition();
        int duration = controller.getDuration();
        Log.d(TAG, "currentPosition " + Converter.getDurationStringLong(currentPosition));
        if (currentPosition == PlaybackService.INVALID_TIME ||
                duration == PlaybackService.INVALID_TIME) {
            Log.w(TAG, "Could not react to position observer update because of invalid time");
            return;
        }
        txtvPosition.setText(Converter.getDurationStringLong(currentPosition));
        if (showTimeLeft) {
            txtvLength.setText("-" + Converter.getDurationStringLong(duration - currentPosition));
        } else {
            txtvLength.setText(Converter.getDurationStringLong(duration));
        }
        updateProgressbarPosition(currentPosition, duration);
    }

    private void updateProgressbarPosition(int position, int duration) {
        Log.d(TAG, "updateProgressbarPosition(" + position + ", " + duration + ")");
        if(sbPosition == null) {
            return;
        }
        float progress = ((float) position) / duration;
        sbPosition.setProgress((int) (progress * sbPosition.getMax()));
    }

    /**
     * Load information about the media that is going to be played or currently
     * being played. This method will be called when the activity is connected
     * to the PlaybackService to ensure that the activity has the right
     * FeedMedia object.
     */
    boolean loadMediaInfo() {
        Log.d(TAG, "loadMediaInfo()");
        if(controller == null || controller.getMedia() == null) {
            return false;
        }
        Playable media = controller.getMedia();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        showTimeLeft = prefs.getBoolean(PREF_SHOW_TIME_LEFT, false);
        onPositionObserverUpdate();
        checkFavorite();
        updatePlaybackSpeedButton();
        return true;
    }

    void updatePlaybackSpeedButton() {
        // Only meaningful on AudioplayerActivity, where it is overridden.
    }

    void updatePlaybackSpeedButtonText() {
        // Only meaningful on AudioplayerActivity, where it is overridden.
    }

    /**
     * Abstract directions to skip forward or back (rewind) and encapsulates behavior to get or set preference (including update of UI on the skip buttons).
     */
    public enum SkipDirection {
        SKIP_FORWARD(
                UserPreferences::getFastForwardSecs,
                MediaplayerActivity::getTxtvFFFromActivity,
                UserPreferences::setFastForwardSecs,
                R.string.pref_fast_forward),
        SKIP_REWIND(UserPreferences::getRewindSecs,
                MediaplayerActivity::getTxtvRevFromActivity,
                UserPreferences::setRewindSecs,
                R.string.pref_rewind);

        private final Supplier<Integer> getPrefSecsFn;
        private final Func1<MediaplayerActivity, TextView> getTextViewFn;
        private final Action1<Integer> setPrefSecsFn;
        private final int titleResourceID;

        /**
         *  Constructor for skip direction enum.  Stores references to  utility functions and resource
         *  id's that vary dependending on the direction.
         *
         * @param getPrefSecsFn Handle to function that retrieves current seconds of the skip delta
         * @param getTextViewFn Handle to function that gets the TextView which displays the current skip delta value
         * @param setPrefSecsFn Handle to function that sets the preference (setting) for the skip delta value (and optionally updates the button label with the current values)
         * @param titleResourceID ID of the resource string with the title for a view
         */
        SkipDirection(Supplier<Integer> getPrefSecsFn, Func1<MediaplayerActivity, TextView> getTextViewFn, Action1<Integer> setPrefSecsFn, int titleResourceID) {
            this.getPrefSecsFn = getPrefSecsFn;
            this.getTextViewFn = getTextViewFn;
            this.setPrefSecsFn = setPrefSecsFn;
            this.titleResourceID = titleResourceID;
        }


        public int getPrefSkipSeconds() {
            return(getPrefSecsFn.get());
        }

        /**
         * Updates preferences for a forward or backward skip depending on the direction of the instance, optionally updating the UI.
         *
         * @param seconds Number of seconds to set the preference associated with the direction of the instance.
         * @param activity MediaplyerActivity that contains textview to update the display of the skip delta setting (or null if nothing to update)
         */
        public void setPrefSkipSeconds(int seconds, @Nullable Activity activity) {
            setPrefSecsFn.call(seconds);

            if (activity != null && activity instanceof  MediaplayerActivity)  {
                TextView tv = getTextViewFn.call((MediaplayerActivity)activity);
                if (tv != null) tv.setText(String.valueOf(seconds));
            }
        }
        public int getTitleResourceID() {
            return titleResourceID;
        }
    }

    static public void showSkipPreference(Activity activity, SkipDirection direction) {
        int checked = 0;
        int skipSecs = direction.getPrefSkipSeconds();
        final int[] values = activity.getResources().getIntArray(R.array.seek_delta_values);
        final String[] choices = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            if (skipSecs == values[i]) {
                checked = i;
            }
            choices[i] = String.valueOf(values[i]) + " " + activity.getString(R.string.time_seconds);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(direction.getTitleResourceID());
        builder.setSingleChoiceItems(choices, checked, null);
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            int choice = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
            if (choice < 0 || choice >= values.length) {
                System.err.printf("Choice in showSkipPreference is out of bounds %d", choice);
            } else {
                direction.setPrefSkipSeconds(values[choice], activity);
            }
        });
        builder.create().show();
    }

    void setupGUI() {
        setContentView(getContentViewResourceId());
        sbPosition = (SeekBar) findViewById(R.id.sbPosition);
        txtvPosition = (TextView) findViewById(R.id.txtvPosition);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        showTimeLeft = prefs.getBoolean(PREF_SHOW_TIME_LEFT, false);
        Log.d("timeleft", showTimeLeft ? "true" : "false");
        txtvLength = (TextView) findViewById(R.id.txtvLength);
        if (txtvLength != null) {
            txtvLength.setOnClickListener(v -> {
                showTimeLeft = !showTimeLeft;
                Playable media = controller.getMedia();
                if (media == null) {
                    return;
                }

                String length;
                if (showTimeLeft) {
                    length = "-" + Converter.getDurationStringLong(media.getDuration() - media.getPosition());
                } else {
                    length = Converter.getDurationStringLong(media.getDuration());
                }
                txtvLength.setText(length);

                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(PREF_SHOW_TIME_LEFT, showTimeLeft);
                editor.apply();
                Log.d("timeleft on click", showTimeLeft ? "true" : "false");
            });
        }

        butRev = (ImageButton) findViewById(R.id.butRev);
        txtvRev = (TextView) findViewById(R.id.txtvRev);
        if (txtvRev != null) {
            txtvRev.setText(String.valueOf(UserPreferences.getRewindSecs()));
        }
        butPlay = (ImageButton) findViewById(R.id.butPlay);
        butFF = (ImageButton) findViewById(R.id.butFF);
        txtvFF = (TextView) findViewById(R.id.txtvFF);
        if (txtvFF != null) {
            txtvFF.setText(String.valueOf(UserPreferences.getFastForwardSecs()));
        }
        butSkip = (ImageButton) findViewById(R.id.butSkip);

        // SEEKBAR SETUP

        sbPosition.setOnSeekBarChangeListener(this);

        // BUTTON SETUP

        if (butRev != null) {
            butRev.setOnClickListener(v -> onRewind());
            butRev.setOnLongClickListener(v -> {
                showSkipPreference(MediaplayerActivity.this, SkipDirection.SKIP_REWIND);
                return true;
            });
        }

        butPlay.setOnClickListener(v -> onPlayPause());

        if (butFF != null) {
            butFF.setOnClickListener(v -> onFastForward());
            butFF.setOnLongClickListener(v -> {
                showSkipPreference(MediaplayerActivity.this, SkipDirection.SKIP_FORWARD);
                return false;
            });
        }

        if (butSkip != null) {
            butSkip.setOnClickListener(v -> sendBroadcast(new Intent(PlaybackService.ACTION_SKIP_CURRENT_EPISODE)));
        }
    }

    void onRewind() {
        if (controller == null) {
            return;
        }
        int curr = controller.getPosition();
        controller.seekTo(curr - UserPreferences.getRewindSecs() * 1000);
    }

    void onPlayPause() {
        if(controller == null) {
            return;
        }
        controller.playPause();
    }

    void onFastForward() {
        if (controller == null) {
            return;
        }
        int curr = controller.getPosition();
        controller.seekTo(curr + UserPreferences.getFastForwardSecs() * 1000);
    }

    protected abstract int getContentViewResourceId();

    private void handleError(int errorCode) {
        final AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
        errorDialog.setTitle(R.string.error_label);
        errorDialog.setMessage(MediaPlayerError.getErrorString(this, errorCode));
        errorDialog.setNeutralButton("OK",
                (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                }
        );
        errorDialog.create().show();
    }

    private float prog;

    @Override
    public void onProgressChanged (SeekBar seekBar,int progress, boolean fromUser) {
        if (controller == null || txtvLength == null) {
            return;
        }
        prog = controller.onSeekBarProgressChanged(seekBar, progress, fromUser, txtvPosition);
        if (showTimeLeft && prog != 0) {
            int duration = controller.getDuration();
            String length = "-" + Converter.getDurationStringLong(duration - (int) (prog * duration));
            txtvLength.setText(length);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (controller != null) {
            controller.onSeekBarStartTrackingTouch(seekBar);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (controller != null) {
            controller.onSeekBarStopTrackingTouch(seekBar, prog);
        }
    }

    private void checkFavorite() {
        Playable playable = controller.getMedia();
        if (playable != null && playable instanceof FeedMedia) {
            FeedItem feedItem = ((FeedMedia) playable).getItem();
            if (feedItem != null) {
                Observable.fromCallable(() -> DBReader.getFeedItem(feedItem.getId()))
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        item -> {
                            boolean isFav = item.isTagged(FeedItem.TAG_FAVORITE);
                            if (isFavorite != isFav) {
                                isFavorite = isFav;
                                invalidateOptionsMenu();
                            }
                        }, error -> Log.e(TAG, Log.getStackTraceString(error)));
            }
        }
    }

}
