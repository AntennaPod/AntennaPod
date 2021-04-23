package de.danoeh.antennapod.view;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import de.danoeh.antennapod.R;

public class PlayButton extends AppCompatImageButton {
    private boolean isShowPlay = true;
    private boolean isVideoScreen = false;

    public PlayButton(@NonNull Context context) {
        super(context);
    }

    public PlayButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setIsVideoScreen(boolean isVideoScreen) {
        this.isVideoScreen = isVideoScreen;
    }

    public void setIsShowPlay(boolean showPlay) {
        if (this.isShowPlay != showPlay) {
            this.isShowPlay = showPlay;
            setContentDescription(getContext().getString(showPlay ? R.string.play_label : R.string.pause_label));
            if (isVideoScreen) {
                setImageResource(showPlay ? R.drawable.ic_play_video_white : R.drawable.ic_pause_video_white);
            } else if (!isShown()) {
                setImageResource(showPlay ? R.drawable.ic_play_48dp : R.drawable.ic_pause);
            } else if (showPlay) {
                AnimatedVectorDrawableCompat drawable = AnimatedVectorDrawableCompat.create(
                        getContext(), R.drawable.ic_animate_pause_play);
                setImageDrawable(drawable);
                drawable.start();
            } else {
                AnimatedVectorDrawableCompat drawable = AnimatedVectorDrawableCompat.create(
                        getContext(), R.drawable.ic_animate_play_pause);
                setImageDrawable(drawable);
                drawable.start();
            }
        }
    }
}
