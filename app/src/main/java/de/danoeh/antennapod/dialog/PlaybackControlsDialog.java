package de.danoeh.antennapod.dialog;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import java.util.Locale;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.util.PlaybackSpeedUtils;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;

public class PlaybackControlsDialog extends DialogFragment {
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
        final SeekBar barPlaybackSpeed = dialog.findViewById(R.id.playback_speed);
        final Button butDecSpeed = dialog.findViewById(R.id.butDecSpeed);
        butDecSpeed.setOnClickListener(v -> {
            if (controller != null && controller.canSetPlaybackSpeed()) {
                barPlaybackSpeed.setProgress(barPlaybackSpeed.getProgress() - 2);
            } else {
                VariableSpeedDialog.showGetPluginDialog(getContext());
            }
        });
        final Button butIncSpeed = (Button) dialog.findViewById(R.id.butIncSpeed);
        butIncSpeed.setOnClickListener(v -> {
            if (controller != null && controller.canSetPlaybackSpeed()) {
                barPlaybackSpeed.setProgress(barPlaybackSpeed.getProgress() + 2);
            } else {
                VariableSpeedDialog.showGetPluginDialog(getContext());
            }
        });

        final TextView txtvPlaybackSpeed = dialog.findViewById(R.id.txtvPlaybackSpeed);
        float currentSpeed = getCurrentSpeed();

        txtvPlaybackSpeed.setText(String.format("%.2fx", currentSpeed));
        barPlaybackSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (controller != null && controller.canSetPlaybackSpeed()) {
                    float playbackSpeed = (progress + 10) / 20.0f;
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
                    barPlaybackSpeed.post(() -> barPlaybackSpeed.setProgress(Math.round((20 * speed) - 10)));
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
        barPlaybackSpeed.setProgress(Math.round((20 * currentSpeed) - 10));

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
