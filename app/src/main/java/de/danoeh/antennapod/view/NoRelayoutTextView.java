package de.danoeh.antennapod.view;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class NoRelayoutTextView extends AppCompatTextView {
    private boolean requestLayoutEnabled = false;
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
    public void setText(CharSequence text, BufferType type) {
        float textLength = getPaint().measureText(text.toString());
        if (textLength > maxTextLength) {
            maxTextLength = textLength;
            requestLayoutEnabled = true;
        }
        super.setText(text, type);
    }
}
