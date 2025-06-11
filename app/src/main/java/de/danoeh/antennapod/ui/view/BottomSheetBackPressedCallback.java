package de.danoeh.antennapod.ui.view;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import org.jetbrains.annotations.NotNull;

public class BottomSheetBackPressedCallback extends OnBackPressedCallback {
    private final BottomSheetBehavior<?> sheetBehavior;
    private final View view;

    public BottomSheetBackPressedCallback(boolean enabled, BottomSheetBehavior<?> sheetBehavior, View view) {
        super(enabled);
        this.sheetBehavior = sheetBehavior;
        this.view = view;
    }

    @Override
    public void handleOnBackProgressed(@NotNull BackEventCompat backEvent) {
        float height = view.getHeight();
        if (height <= 0f) {
            return;
        }
        view.setTranslationY(height * 0.2f * backEvent.getProgress());
    }

    @Override
    public void handleOnBackPressed() {
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        float from = view.getTranslationY();
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                view.setTranslationY((1.0f - interpolatedTime) * from);
            }
        };
        animation.setDuration(100);
        view.startAnimation(animation);
    }

    @Override
    public void handleOnBackCancelled() {
        view.setTranslationY(0);
    }
}