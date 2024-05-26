package de.danoeh.antennapod.ui.screen.playback.audio;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class NoRelayoutTextView extends AppCompatTextView {
    private boolean requestLayoutEnabled = true;
    private float maxTextLength = 0;

    public NoRelayoutTextView(@NonNull Context context) {
        super(context);
    }

    public NoRelayoutTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NoRelayoutTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void requestLayout() {
        if (requestLayoutEnabled) {
            super.requestLayout();
        }
        requestLayoutEnabled = false;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        requestLayoutEnabled = true;
        super.onRestoreInstanceState(state);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        float textLength = getPaint().measureText(text.toString());
        if (textLength > maxTextLength) {
            maxTextLength = textLength;
            requestLayoutEnabled = true;
        }
        super.setText(text, type);
    }
}
