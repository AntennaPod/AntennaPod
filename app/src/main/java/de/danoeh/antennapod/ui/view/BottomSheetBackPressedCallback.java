package de.danoeh.antennapod.ui.view;

import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import org.jetbrains.annotations.NotNull;

public class BottomSheetBackPressedCallback extends OnBackPressedCallback {
    private final BottomSheetBehavior<?> sheetBehavior;

    public BottomSheetBackPressedCallback(BottomSheetBehavior<?> sheetBehavior) {
        super(true);
        this.sheetBehavior = sheetBehavior;
    }

    @Override
    public void handleOnBackStarted(@NotNull BackEventCompat backEvent) {
        sheetBehavior.startBackProgress(backEvent);
    }

    @Override
    public void handleOnBackProgressed(@NotNull BackEventCompat backEvent) {
        sheetBehavior.updateBackProgress(backEvent);
    }

    @Override
    public void handleOnBackPressed() {
        sheetBehavior.handleBackInvoked();
    }

    @Override
    public void handleOnBackCancelled() {
        sheetBehavior.cancelBackProgress();
    }
}