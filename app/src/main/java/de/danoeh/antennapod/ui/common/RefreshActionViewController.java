package de.danoeh.antennapod.ui.common;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;

import de.danoeh.antennapod.R;

/**
 * Manages a refresh toolbar action that can spin while background refresh is running.
 */
public class RefreshActionViewController {
    private final MenuItem menuItem;
    private ObjectAnimator animator;

    private RefreshActionViewController(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    @Nullable
    public static RefreshActionViewController attach(Menu menu, @IdRes int menuId,
                                                     Context context, Runnable onClick) {
        if (menu == null || context == null) {
            return null;
        }
        MenuItem item = menu.findItem(menuId);
        if (item == null) {
            return null;
        }
        AppCompatImageView iconView = new AppCompatImageView(context);
        int size = (int) (context.getResources().getDisplayMetrics().density * 24);
        iconView.setLayoutParams(new ViewGroup.LayoutParams(size, size));
        iconView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_shortcut_refresh));
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iconView.setContentDescription(context.getString(R.string.refresh_label));
        iconView.setOnClickListener(v -> {
            if (onClick != null) {
                onClick.run();
            }
        });
        item.setActionView(iconView);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return new RefreshActionViewController(item);
    }

    public void setRefreshing(boolean refreshing) {
        if (menuItem == null || menuItem.getActionView() == null) {
            return;
        }
        View actionView = menuItem.getActionView();
        if (refreshing) {
            if (animator == null) {
                animator = ObjectAnimator.ofFloat(actionView, View.ROTATION, 0f, 360f);
                animator.setInterpolator(new LinearInterpolator());
                animator.setDuration(800);
                animator.setRepeatCount(ValueAnimator.INFINITE);
            }
            if (!animator.isStarted()) {
                animator.start();
            }
        } else {
            if (animator != null && animator.isStarted()) {
                animator.cancel();
            }
            actionView.setRotation(0f);
        }
    }

    public void clear() {
        setRefreshing(false);
    }
}