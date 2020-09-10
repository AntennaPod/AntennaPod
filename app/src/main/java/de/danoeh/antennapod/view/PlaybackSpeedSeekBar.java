package de.danoeh.antennapod.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;

public class PlaybackSpeedSeekBar extends FrameLayout {
    private SeekBar seekBar;
    private PlaybackController controller;
    private Consumer<Float> progressChangedListener;

    public PlaybackSpeedSeekBar(@NonNull Context context) {
        super(context);
        setup();
    }

    public PlaybackSpeedSeekBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public PlaybackSpeedSeekBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private void setup() {
        View.inflate(getContext(), R.layout.playback_speed_seek_bar, this);
        seekBar = findViewById(R.id.playback_speed);
        findViewById(R.id.butDecSpeed).setOnClickListener(v -> seekBar.setProgress(seekBar.getProgress() - 2));
        findViewById(R.id.butIncSpeed).setOnClickListener(v -> seekBar.setProgress(seekBar.getProgress() + 2));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (controller != null && controller.canSetPlaybackSpeed()) {
                    float playbackSpeed = (progress + 10) / 20.0f;
                    controller.setPlaybackSpeed(playbackSpeed);

                    if (progressChangedListener != null) {
                        progressChangedListener.accept(playbackSpeed);
                    }
                } else if (fromUser) {
                    seekBar.post(() -> updateSpeed());
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
    }

    public void updateSpeed() {
        if (controller != null) {
            seekBar.setProgress(Math.round((20 * controller.getCurrentPlaybackSpeedMultiplier()) - 10));
        }
    }

    public void setController(PlaybackController controller) {
        this.controller = controller;
        updateSpeed();
        if (progressChangedListener != null && controller != null) {
            progressChangedListener.accept(controller.getCurrentPlaybackSpeedMultiplier());
        }
    }

    public void setProgressChangedListener(Consumer<Float> progressChangedListener) {
        this.progressChangedListener = progressChangedListener;
    }
}
