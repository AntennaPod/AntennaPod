package de.danoeh.antennapod.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.bottomsheet.ViewPagerBottomSheetBehavior;

/**
 * Based on https://stackoverflow.com/a/40798214
 */
public class LockableBottomSheetBehavior<V extends View> extends ViewPagerBottomSheetBehavior<V> {
    private boolean isLocked = false;

    public LockableBottomSheetBehavior() {}

    public LockableBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        boolean handled = false;

        if (!isLocked) {
            handled = super.onInterceptTouchEvent(parent, child, event);
        }

        return handled;
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        boolean handled = false;

        if (!isLocked) {
            handled = super.onTouchEvent(parent, child, event);
        }

        return handled;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, V child, View directTargetChild,
                                       View target, int axes, int type) {
        boolean handled = false;

        if (!isLocked) {
            handled = super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type);
        }

        return handled;
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target,
                                  int dx, int dy, int[] consumed, int type) {
        if (!isLocked) {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
        }
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target, int type) {
        if (!isLocked) {
            super.onStopNestedScroll(coordinatorLayout, child, target, type);
        }
    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, V child, View target,
                                    float velocityX, float velocityY) {
        boolean handled = false;

        if (!isLocked) {
            handled = super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
        }

        return handled;
    }
}
