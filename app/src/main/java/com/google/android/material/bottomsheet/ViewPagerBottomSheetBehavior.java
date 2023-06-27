package com.google.android.material.bottomsheet;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.lang.ref.WeakReference;

/**
 * Override {@link #findScrollingChild(View)} to support {@link ViewPager}'s nested scrolling.
 * In order to override package level method and field.
 * This class put in the same package path where {@link BottomSheetBehavior} located.
 * Source: https://medium.com/@hanru.yeh/funny-solution-that-makes-bottomsheetdialog-support-viewpager-with-nestedscrollingchilds-bfdca72235c3
 */
public class ViewPagerBottomSheetBehavior<V extends View> extends BottomSheetBehavior<V> {

    public ViewPagerBottomSheetBehavior() {
        super();
    }

    public ViewPagerBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    View findScrollingChild(View view) {
        if (view.isNestedScrollingEnabled()) {
            return view;
        }

        if (view instanceof ViewPager2) {
            ViewPager2 viewPager = (ViewPager2) view;
            RecyclerView recycler = (RecyclerView) viewPager.getChildAt(0);
            View currentViewPagerChild = recycler.getChildAt(viewPager.getCurrentItem());
            if (currentViewPagerChild != null) {
                return findScrollingChild(currentViewPagerChild);
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                View scrollingChild = findScrollingChild(group.getChildAt(i));
                if (scrollingChild != null) {
                    return scrollingChild;
                }
            }
        }
        return null;
    }

    public void updateScrollingChild() {
        if (viewRef == null) {
            return;
        }
        final View scrollingChild = findScrollingChild(viewRef.get());
        nestedScrollingChildRef = new WeakReference<>(scrollingChild);
    }
}