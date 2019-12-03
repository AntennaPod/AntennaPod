package de.danoeh.antennapodSA.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Locale;

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.core.feed.util.PlaybackSpeedUtils;
import de.danoeh.antennapodSA.core.preferences.PlaybackPreferences;
import de.danoeh.antennapodSA.core.preferences.UserPreferences;
import de.danoeh.antennapodSA.core.util.Converter;
import de.danoeh.antennapodSA.core.util.playback.Playable;
import de.danoeh.antennapodSA.core.util.playback.PlaybackController;

public class PlaybackControlsDialog extends DialogFragment {
    private static final float PLAYBACK_SPEED_STEP = 0.05f;
    private static final float DEFAULT_MIN_PLAYBACK_SPEED = 0.5f;
    private static final float DEFAULT_MAX_PLAYBACK_SPEED = 2.5f;
    private static final String ARGUMENT_IS_PLAYING_VIDEO = "isPlayingVideo";

    private PlaybackController controller;
    private AlertDialog dialog;
    private boolean isPlayingVideo;

    public static PlaybackControlsDialog newInstance(boolean isPlayingVideo) {
        Bundle arguments = new Bundle();
        arguments.putBoolean(ARGUMENT_IS_PLAYING_VIDEO, isPlayingVideo);
        PlaybackControlsDialog dialog = new PlaybackControlsDialog();
        dialog.setArguments(arguments);
        return dialog;
    }

    public PlaybackControlsDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity(), false) {
            @Override
            public void setupGUI() {
                setupUi();
            }
        };
        controller.init();
        setupUi();
    }

    @Override
    public void onStop() {
        super.onStop();
        controller.release();
        controller = null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        isPlayingVideo = getArguments() != null && getArguments().getBoolean(ARGUMENT_IS_PLAYING_VIDEO);

        dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.audio_controls)
                .setView(R.layout.audio_controls)
                .setPositiveButton(R.string.close_label, (dialog1, which) -> {
                    final SeekBar left = dialog.findViewById(R.id.volume_left);
                    final SeekBar right = dialog.findViewById(R.id.volume_right);
                    UserPreferences.setVolume(left.getProgress(), right.getProgress());
                }).create();
        return dialog;
    }

    private void setupUi() {
        final SeekBar barPlaybackSpeed = (SeekBar) dialog.findViewById(R.id.playback_speed);
        final Button butDecSpeed = (Button) dialog.findViewById(R.id.butDecSpeed);
        butDecSpeed.setOnClickListener(v -> {
            if (controller != null && controller.canSetPlaybackSpeed()) {
                barPlaybackSpeed.setProgress(barPlaybackSpeed.getProgress() - 1);
            } else {
                VariableSpeedDialog.showGetPluginDialog(getContext());
            }
        });
        final Button butIncSpeed = (Button) dialog.findViewById(R.id.butIncSpeed);
        butIncSpeed.setOnClickListener(v -> {
            if (controller != null && controller.canSetPlaybackSpeed()) {
                barPlaybackSpeed.setProgress(barPlaybackSpeed.getProgress() + 1);
            } else {
                VariableSpeedDialog.showGetPluginDialog(getContext());
            }
        });

        final TextView txtvPlaybackSpeed = (TextView) dialog.findViewById(R.id.txtvPlaybackSpeed);
        float currentSpeed = getCurrentSpeed();

        String[] availableSpeeds = UserPreferences.getPlaybackSpeedArray();
        final float minPlaybackSpeed = availableSpeeds.length > 1 ?
                Float.valueOf(availableSpeeds[0]) : DEFAULT_MIN_PLAYBACK_SPEED;
        float maxPlaybackSpeed = availableSpeeds.length > 1 ?
                Float.valueOf(availableSpeeds[availableSpeeds.length - 1]) : DEFAULT_MAX_PLAYBACK_SPEED;
        int progressMax = (int) ((maxPlaybackSpeed - minPlaybackSpeed) / PLAYBACK_SPEED_STEP);
        barPlaybackSpeed.setMax(progressMax);

        txtvPlaybackSpeed.setText(String.format("%.2fx", currentSpeed));
        barPlaybackSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (controller != null && controller.canSetPlaybackSpeed()) {
                    float playbackSpeed = progress * PLAYBACK_SPEED_STEP + minPlaybackSpeed;
                    controller.setPlaybackSpeed(playbackSpeed);
                    String speedPref = String.format(Locale.US, "%.2f", playbackSpeed);

                    PlaybackPreferences.setCurrentlyPlayingTemporaryPlaybackSpeed(playbackSpeed);
                    if (isPlayingVideo) {
                        UserPreferences.setVideoPlaybackSpeed(speedPref);
                    } else {
                        UserPreferences.setPlaybackSpeed(speedPref);
                    }

                    String speedStr = String.format("%.2fx", playbackSpeed);
                    txtvPlaybackSpeed.setText(speedStr);
                } else if (fromUser) {
                    float speed = getCurrentSpeed();
                    barPlaybackSpeed.post(() -> barPlaybackSpeed.setProgress(
                            (int) ((speed - minPlaybackSpeed) / PLAYBACK_SPEED_STEP)));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (controller != null && !controller.canSetPlaybackSpeed()) {
                    VariableSpeedDialog.showGetPluginDialog(getContext());
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        barPlaybackSpeed.setProgress(Math.round((currentSpeed - minPlaybackSpeed) / PLAYBACK_SPEED_STEP));

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

        if (UserPreferences.useExoplayer()) {
            barRightVolume.setEnabled(false);
        }

        final CheckBox skipSilence = (CheckBox) dialog.findViewById(R.id.skipSilence);
        skipSilence.setChecked(UserPreferences.isSkipSilence());
        if (!UserPreferences.useExoplayer()) {
            skipSilence.setEnabled(false);
            String exoplayerOnly = getString(R.string.exoplayer_only);
            skipSilence.setText(skipSilence.getText() + " [" + exoplayerOnly + "]");
        }
        skipSilence.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UserPreferences.setSkipSilence(isChecked);
            controller.setSkipSilence(isChecked);
        });

        barLeftVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
        barRightVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
    }

    private float getCurrentSpeed() {
        Playable media = null;
        if (controller != null) {
            media = controller.getMedia();
        }

        return PlaybackSpeedUtils.getCurrentPlaybackSpeed(media);
    }
}
