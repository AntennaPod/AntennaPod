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

public class PlaybackSpeedSeekBar extends FrameLayout {
    private SeekBar seekBar;
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
                float playbackSpeed = (progress + 10) / 20.0f;
                if (progressChangedListener != null) {
                    progressChangedListener.accept(playbackSpeed);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public void updateSpeed(float speedMultiplier) {
        seekBar.setProgress(Math.round((20 * speedMultiplier) - 10));
    }

    public void setProgressChangedListener(Consumer<Float> progressChangedListener) {
        this.progressChangedListener = progressChangedListener;
    }

    public float getCurrentSpeed() {
        return (seekBar.getProgress() + 10) / 20.0f;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        seekBar.setEnabled(enabled);
        findViewById(R.id.butDecSpeed).setEnabled(enabled);
        findViewById(R.id.butIncSpeed).setEnabled(enabled);
    }
}
