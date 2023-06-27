package de.danoeh.antennapod.view;

import android.animation.ValueAnimator;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Workaround for app:liftOnScroll flickering when in SwipeRefreshLayout
 */
public class LiftOnScrollListener extends RecyclerView.OnScrollListener
        implements NestedScrollView.OnScrollChangeListener {
    private final ValueAnimator animator;
    private boolean animatingToScrolled = false;

    public LiftOnScrollListener(View appBar) {
        animator = ValueAnimator.ofFloat(0, appBar.getContext().getResources().getDisplayMetrics().density * 8);
        animator.addUpdateListener(animation -> appBar.setElevation((float) animation.getAnimatedValue()));
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        elevate(isScrolled(recyclerView));
    }

    private boolean isScrolled(RecyclerView recyclerView) {
        int firstItem = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        if (firstItem < 0) {
            return false;
        } else if (firstItem > 0) {
            return true;
        }
        View firstItemView = recyclerView.getLayoutManager().findViewByPosition(firstItem);
        if (firstItemView == null) {
            return false;
        } else {
            return firstItemView.getTop() < 0;
        }
    }

    @Override
    public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        elevate(scrollY != 0);
    }

    private void elevate(boolean isScrolled) {
        if (isScrolled == animatingToScrolled) {
            return;
        }
        animatingToScrolled = isScrolled;
        if (isScrolled) {
            animator.start();
        } else {
            animator.reverse();
        }
    }
}
