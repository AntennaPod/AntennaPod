package de.danoeh.antennapod.activity;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.ShareUtils;
import de.danoeh.antennapod.core.util.StorageUtils;
import de.danoeh.antennapod.core.util.playback.MediaPlayerError;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.dialog.SleepTimerDialog;

/**
 * Provides general features which are both needed for playing audio and video
 * files.
 */
public abstract class MediaplayerActivity extends ActionBarActivity
        implements OnSeekBarChangeListener {
    private static final String TAG = "MediaplayerActivity";
    private static final String PREFS = "MediaPlayerActivityPreferences";
    private static final String PREF_SHOW_TIME_LEFT = "showTimeLeft";

    protected PlaybackController controller;

    protected TextView txtvPosition;
    protected TextView txtvLength;
    protected SeekBar sbPosition;
    protected ImageButton butPlay;
    protected ImageButton butRev;
    protected boolean showTimeLeft = false;
    protected TextView txtvRev;
    protected ImageButton butFF;
    protected TextView txtvFF;

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
            public void postStatusMsg(int msg) {
                MediaplayerActivity.this.postStatusMsg(msg);
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
        };

    }

    protected void onPlaybackSpeedChange() {

    }

    protected void onServiceQueried() {
        supportInvalidateOptionsMenu();
    }

    protected void chooseTheme() {
        setTheme(UserPreferences.getTheme());
    }

    protected void setScreenOn(boolean enable) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        chooseTheme();
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");
        StorageUtils.checkStorageAvailability(this);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        orientation = getResources().getConfiguration().orientation;
        getWindow().setFormat(PixelFormat.TRANSPARENT);
    }

    @Override
    protected void onPause() {
        super.onPause();
        controller.reinitServiceIfPaused();
        controller.pause();
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

    protected void onBufferUpdate(float progress) {
        if (sbPosition != null) {
            sbPosition.setSecondaryProgress((int) progress
                    * sbPosition.getMax());
        }
    }

    /**
     * Current screen orientation.
     */
    protected int orientation;

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
        super.onStop();
        Log.d(TAG, "onStop()");
        if (controller != null) {
            controller.release();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mediaplayer, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Playable media = controller.getMedia();

        menu.findItem(R.id.support_item).setVisible(
                media != null && media.getPaymentLink() != null &&
                        (media instanceof FeedMedia) &&
                        ((FeedMedia) media).getItem().getFlattrStatus().flattrable()
        );

        boolean hasWebsiteLink = media != null && media.getWebsiteLink() != null;
        menu.findItem(R.id.visit_website_item).setVisible(hasWebsiteLink);

        boolean isItemAndHasLink = media != null && (media instanceof FeedMedia) && ((FeedMedia) media).getItem().getLink() != null;
        menu.findItem(R.id.share_link_item).setVisible(isItemAndHasLink);
        menu.findItem(R.id.share_link_with_position_item).setVisible(isItemAndHasLink);

        boolean isItemHasDownloadLink = media != null && (media instanceof FeedMedia) && ((FeedMedia) media).getDownload_url() != null;
        menu.findItem(R.id.share_download_url_item).setVisible(isItemHasDownloadLink);
        menu.findItem(R.id.share_download_url_with_position_item).setVisible(isItemHasDownloadLink);

        menu.findItem(R.id.share_item).setVisible(hasWebsiteLink || isItemAndHasLink || isItemHasDownloadLink);

        menu.findItem(R.id.skip_episode_item).setVisible(media != null);
        boolean sleepTimerSet = controller.sleepTimerActive();
        boolean sleepTimerNotSet = controller.sleepTimerNotActive();
        menu.findItem(R.id.set_sleeptimer_item).setVisible(sleepTimerNotSet);
        menu.findItem(R.id.disable_sleeptimer_item).setVisible(sleepTimerSet);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Playable media = controller.getMedia();
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(MediaplayerActivity.this,
                    MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } else if (media != null) {
            switch (item.getItemId()) {
                case R.id.disable_sleeptimer_item:
                    if (controller.serviceAvailable()) {

                        MaterialDialog.Builder stDialog = new MaterialDialog.Builder(this);
                        stDialog.title(R.string.sleep_timer_label);
                        stDialog.content(getString(R.string.time_left_label)
                                + Converter.getDurationStringLong((int) controller
                                .getSleepTimerTimeLeft()));
                        stDialog.positiveText(R.string.disable_sleeptimer_label);
                        stDialog.negativeText(R.string.cancel_label);
                        stDialog.callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                dialog.dismiss();
                                controller.disableSleepTimer();
                            }

                            @Override
                            public void onNegative(MaterialDialog dialog) {
                                dialog.dismiss();
                            }
                        });
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
                case R.id.skip_episode_item:
                    sendBroadcast(new Intent(
                            PlaybackService.ACTION_SKIP_CURRENT_EPISODE));
                    break;
                default:
                    return false;

            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        StorageUtils.checkStorageAvailability(this);
        controller.init();
    }

    /**
     * Called by 'handleStatus()' when the PlaybackService is waiting for
     * a video surface.
     */
    protected abstract void onAwaitingVideoSurface();

    protected abstract void postStatusMsg(int resId);

    protected abstract void clearStatusMsg();

    protected void onPositionObserverUpdate() {
        if (controller != null) {
            int currentPosition = controller.getPosition();
            int duration = controller.getDuration();
            Log.d(TAG, "currentPosition " + Converter
                    .getDurationStringLong(currentPosition));
            if (currentPosition != PlaybackService.INVALID_TIME
                    && duration != PlaybackService.INVALID_TIME
                    && controller.getMedia() != null) {
                txtvPosition.setText(Converter
                        .getDurationStringLong(currentPosition));
                if(showTimeLeft) {
                    txtvLength.setText("-"+Converter
                            .getDurationStringLong(duration - currentPosition));
                }
                else {
                    txtvLength.setText(Converter
                            .getDurationStringLong(duration));
                }
                updateProgressbarPosition(currentPosition, duration);
            } else {
                Log.w(TAG, "Could not react to position observer update because of invalid time");
            }
        }
    }

    private void updateProgressbarPosition(int position, int duration) {
        Log.d(TAG, "updateProgressbarPosition(" + position + ", " + duration +")");
        float progress = ((float) position) / duration;
        sbPosition.setProgress((int) (progress * sbPosition.getMax()));
    }

    /**
     * Load information about the media that is going to be played or currently
     * being played. This method will be called when the activity is connected
     * to the PlaybackService to ensure that the activity has the right
     * FeedMedia object.
     */
    protected boolean loadMediaInfo() {
        Log.d(TAG, "loadMediaInfo()");
        Playable media = controller.getMedia();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        showTimeLeft = prefs.getBoolean(PREF_SHOW_TIME_LEFT,false);
        if (media != null) {
            txtvPosition.setText(Converter.getDurationStringLong((media
                    .getPosition())));

            if (media.getDuration() != 0) {
                txtvLength.setText(Converter.getDurationStringLong(media
                        .getDuration()));
                float progress = ((float) media.getPosition())
                        / media.getDuration();
                sbPosition.setProgress((int) (progress * sbPosition.getMax()));
                if(showTimeLeft) {
                    txtvLength.setText("-"+Converter.getDurationStringLong((media
                                                .getDuration()-media.getPosition())));
                }
            }
            return true;
        } else {
            return false;
        }
    }

    protected void setupGUI() {
        setContentView(getContentViewResourceId());
        sbPosition = (SeekBar) findViewById(R.id.sbPosition);
        txtvPosition = (TextView) findViewById(R.id.txtvPosition);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        showTimeLeft = prefs.getBoolean(PREF_SHOW_TIME_LEFT,false);
        Log.w("timeleft",showTimeLeft? "true":"false");
        txtvLength = (TextView) findViewById(R.id.txtvLength);
        txtvLength.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimeLeft = !showTimeLeft;
                editor.putBoolean(PREF_SHOW_TIME_LEFT,showTimeLeft);
                editor.commit();
                Log.w("timeleft on click",showTimeLeft? "true":"false");
            }
        });

        butPlay = (ImageButton) findViewById(R.id.butPlay);
        butRev = (ImageButton) findViewById(R.id.butRev);
        txtvRev = (TextView) findViewById(R.id.txtvRev);
        if(txtvRev != null) {
            txtvRev.setText(String.valueOf(UserPreferences.getRewindSecs()));
        }
        butFF = (ImageButton) findViewById(R.id.butFF);
        txtvFF = (TextView) findViewById(R.id.txtvFF);
        if(txtvFF != null) {
            txtvFF.setText(String.valueOf(UserPreferences.getFastFowardSecs()));
        }

        // SEEKBAR SETUP

        sbPosition.setOnSeekBarChangeListener(this);

        // BUTTON SETUP

        butPlay.setOnClickListener(controller.newOnPlayButtonClickListener());

        if (butFF != null) {
            butFF.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int curr = controller.getPosition();
                    controller.seekTo(curr + UserPreferences.getFastFowardSecs() * 1000);
                }
            });
            butFF.setOnLongClickListener(new View.OnLongClickListener() {

                int choice;

                @Override
                public boolean onLongClick(View v) {
                    int checked = 0;
                    int rewindSecs = UserPreferences.getFastFowardSecs();
                    final int[] values = getResources().getIntArray(R.array.seek_delta_values);
                    final String[] choices = new String[values.length];
                    for(int i=0; i < values.length; i++) {
                        if (rewindSecs == values[i]) {
                            checked = i;
                        }
                        choices[i] = String.valueOf(values[i]) + " "
                                + getString(R.string.time_seconds);
                    }
                    choice = values[checked];
                    AlertDialog.Builder builder = new AlertDialog.Builder(MediaplayerActivity.this);
                    builder.setTitle(R.string.pref_fast_forward);
                    builder.setSingleChoiceItems(choices, checked,
                            (dialog, which) -> {
                                choice = values[which];
                            });
                    builder.setNegativeButton(R.string.cancel_label, null);
                    builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
                        UserPreferences.setPrefFastForwardSecs(choice);
                        txtvFF.setText(String.valueOf(choice));
                    });
                    builder.create().show();
                    return true;
                }
            });
        }
        if (butRev != null) {
            butRev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int curr = controller.getPosition();
                    controller.seekTo(curr - UserPreferences.getRewindSecs() * 1000);
                }
            });
            butRev.setOnLongClickListener(new View.OnLongClickListener() {

                int choice;

                @Override
                public boolean onLongClick(View v) {
                    int checked = 0;
                    int rewindSecs = UserPreferences.getRewindSecs();
                    final int[] values = getResources().getIntArray(R.array.seek_delta_values);
                    final String[] choices = new String[values.length];
                    for(int i=0; i < values.length; i++) {
                        if (rewindSecs == values[i]) {
                            checked = i;
                        }
                        choices[i] = String.valueOf(values[i]) + " "
                                + getString(R.string.time_seconds);
                    }
                    choice = values[checked];
                    AlertDialog.Builder builder = new AlertDialog.Builder(MediaplayerActivity.this);
                    builder.setTitle(R.string.pref_rewind);
                    builder.setSingleChoiceItems(choices, checked,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    choice = values[which];
                                }
                            });
                    builder.setNegativeButton(R.string.cancel_label, null);
                    builder.setPositiveButton(R.string.confirm_label, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            UserPreferences.setPrefRewindSecs(choice);
                            txtvRev.setText(String.valueOf(choice));
                        }
                    });
                    builder.create().show();
                    return true;
                }
            });
        }

    }

    protected abstract int getContentViewResourceId();

    void handleError(int errorCode) {
        final AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
        errorDialog.setTitle(R.string.error_label);
        errorDialog
                .setMessage(MediaPlayerError.getErrorString(this, errorCode));
        errorDialog.setNeutralButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                }
        );
        errorDialog.create().show();
    }

    float prog;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        if (controller != null) {
            prog = controller.onSeekBarProgressChanged(seekBar, progress, fromUser,
                    txtvPosition);
            if(showTimeLeft && prog!=0) {
                int duration = controller.getDuration();
                txtvLength.setText("-"+Converter
                        .getDurationStringLong(duration - (int) (prog * duration)));
            }
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

}
