package de.danoeh.antennapod.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class NoRelayoutTextView extends AppCompatTextView {
    public NoRelayoutTextView(@NonNull Context context) {
        super(context);
    }

    public NoRelayoutTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NoRelayoutTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void requestLayout() {
        // Deliberate no-op
    }
}
