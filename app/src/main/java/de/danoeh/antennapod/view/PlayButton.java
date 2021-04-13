package de.danoeh.antennapod.view;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.common.ThemeUtils;

public class PlayButton extends AppCompatImageButton {
    private boolean isVideoScreen;

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
        setContentDescription(getContext().getString(showPlay ? R.string.play_label : R.string.pause_label));
        if (isVideoScreen) {
            setImageResource(showPlay ? R.drawable.ic_av_play_white_80dp : R.drawable.ic_av_pause_white_80dp);
        } else {
            setImageResource(ThemeUtils.getDrawableFromAttr(getContext(), showPlay ? R.attr.av_play : R.attr.av_pause));
        }
    }
}
