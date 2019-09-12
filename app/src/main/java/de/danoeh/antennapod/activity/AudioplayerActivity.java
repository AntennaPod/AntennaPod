package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;

/**
 * Activity for playing audio files.
 */
public class AudioplayerActivity extends MediaplayerInfoActivity {
    private static final String TAG = "AudioPlayerActivity";

    private final AtomicBoolean isSetup = new AtomicBoolean(false);

    @Override
    protected void onResume() {
        super.onResume();
        if (TextUtils.equals(getIntent().getAction(), Intent.ACTION_VIEW)) {
            playExternalMedia(getIntent(), MediaType.AUDIO);
        } else if (PlaybackService.isCasting()) {
            Intent intent = PlaybackService.getPlayerActivityIntent(this);
            if (intent.getComponent() != null &&
                    !intent.getComponent().getClassName().equals(AudioplayerActivity.class.getName())) {
                saveCurrentFragment();
                finish();
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onReloadNotification(int notificationCode) {
        if (notificationCode == PlaybackService.EXTRA_CODE_CAST) {
            Log.d(TAG, "ReloadNotification received, switching to Castplayer now");
            saveCurrentFragment();
            finish();
            startActivity(new Intent(this, CastplayerActivity.class));

        } else {
            super.onReloadNotification(notificationCode);
        }
    }

    @Override
    protected void updatePlaybackSpeedButton() {
        if(butPlaybackSpeed == null) {
            return;
        }
        if (controller == null) {
            butPlaybackSpeed.setVisibility(View.GONE);
            return;
        }
        updatePlaybackSpeedButtonText();
        ViewCompat.setAlpha(butPlaybackSpeed, controller.canSetPlaybackSpeed() ? 1.0f : 0.5f);
        butPlaybackSpeed.setVisibility(View.VISIBLE);
    }

    @Override
    protected void updatePlaybackSpeedButtonText() {
        if(butPlaybackSpeed == null) {
            return;
        }
        if (controller == null) {
            butPlaybackSpeed.setVisibility(View.GONE);
            return;
        }
        float speed = 1.0f;
        if(controller.canSetPlaybackSpeed()) {
            speed = getPlaybackSpeedForMedia();
        }
        String speedStr = new DecimalFormat("0.00x").format(speed);
        butPlaybackSpeed.setText(speedStr);
    }

    @Override
    protected void setupGUI() {
        if(isSetup.getAndSet(true)) {
            return;
        }
        super.setupGUI();
        if(butCastDisconnect != null) {
            butCastDisconnect.setVisibility(View.GONE);
        }
        if(butPlaybackSpeed != null) {
            butPlaybackSpeed.setOnClickListener(v -> {
                if (controller == null) {
                    return;
                }
                if (controller.canSetPlaybackSpeed()) {
                    String[] availableSpeeds = UserPreferences.getPlaybackSpeedArray();
                    DecimalFormatSymbols format = new DecimalFormatSymbols(Locale.US);
                    format.setDecimalSeparator('.');

                    float currentSpeedValue = controller.getCurrentPlaybackSpeedMultiplier();
                    if (currentSpeedValue == -1) {
                        currentSpeedValue = getPlaybackSpeedForMedia();
                    }

                    String currentSpeed = new DecimalFormat("0.00", format).format(currentSpeedValue);

                    // Provide initial value in case the speed list has changed
                    // out from under us
                    // and our current speed isn't in the new list
                    String newSpeed;
                    if (availableSpeeds.length > 0) {
                        newSpeed = availableSpeeds[0];
                    } else {
                        newSpeed = "1.00";
                    }

                    for (int i = 0; i < availableSpeeds.length; i++) {
                        if (availableSpeeds[i].equals(currentSpeed)) {
                            if (i == availableSpeeds.length - 1) {
                                newSpeed = availableSpeeds[0];
                            } else {
                                newSpeed = availableSpeeds[i + 1];
                            }
                            break;
                        }
                    }

                    storeNewMediaPlaybackSpeed(newSpeed);
                    UserPreferences.setPlaybackSpeed(newSpeed);
                    controller.setPlaybackSpeed(Float.parseFloat(newSpeed));
                    onPositionObserverUpdate();
                } else {
                    VariableSpeedDialog.showGetPluginDialog(this);
                }
            });
            butPlaybackSpeed.setOnLongClickListener(v -> {
                VariableSpeedDialog.showDialog(this);
                return true;
            });
            butPlaybackSpeed.setVisibility(View.VISIBLE);
        }
    }

    private void storeNewMediaPlaybackSpeed(String speed) {
        Playable media = controller.getMedia();
        if (media instanceof FeedMedia) {
            ((FeedMedia) media).updateLastPlaybackSpeed(speed);
        }
    }
}
