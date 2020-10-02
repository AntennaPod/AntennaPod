/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Source: https://github.com/android/views-widgets-samples/blob/87e58d1/ViewPager2/app/src/main/java/androidx/viewpager2/integration/testapp/NestedScrollableHost.kt
 * And modified for our need
 */

package de.danoeh.antennapod.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import static androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL;
import static androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL;

/**
 * Layout to wrap a scrollable component inside a ViewPager2. Provided as a solution to the problem
 * where pages of ViewPager2 have nested scrollable elements that scroll in the same direction as
 * ViewPager2. The scrollable element needs to be the immediate and only child of this host layout.
 *
 * <p>This solution has limitations when using multiple levels of nested scrollable elements
 * (e.g. a horizontal RecyclerView in a vertical RecyclerView in a horizontal ViewPager2).
 */
class NestedScrollableHost extends FrameLayout {

    public NestedScrollableHost(@NonNull Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public NestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    private int touchSlop;
    private float initialX = 0f;
    private float initialY = 0f;

    private ViewPager2 getParentViewPager() {
        View v = (View) getParent();
        while (v != null && !(v instanceof ViewPager2)) {
            v = (View) v.getParent();
        }
        return v == null ? null : (ViewPager2) v;
    }

    public boolean onInterceptTouchEvent(MotionEvent e) {
        ViewPager2 parentViewPager = getParentViewPager();
        if (parentViewPager == null) {
            return super.onInterceptTouchEvent(e);
        }

        ViewParent parent = getParent();
        int orientation = parentViewPager.getOrientation();

        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            initialX = e.getX();
            initialY = e.getY();
            parent.requestDisallowInterceptTouchEvent(true);
        } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
            int dx = (int) (e.getX() - initialX);
            int dy = (int) (e.getY() - initialY);
            boolean isVpHorizontal = orientation == ORIENTATION_HORIZONTAL;

            // assuming ViewPager2 touch-slop is 2x touch-slop of child
            float scaledDx = Math.abs(dx) * (isVpHorizontal ? .5f : 1f);
            float scaledDy = Math.abs(dy) * (isVpHorizontal ? 1f : .5f);
            if (scaledDx > touchSlop || scaledDy > touchSlop) {
                int value = isVpHorizontal ? dy : dx;
                if (isVpHorizontal == (scaledDy > scaledDx)) {
                    // Gesture is perpendicular
                    orientation = orientation == ORIENTATION_VERTICAL
                            ? ORIENTATION_HORIZONTAL : ORIENTATION_VERTICAL;
                    value = isVpHorizontal ? dy : dx;
                }

                int direction = (int) -Math.copySign(1, value);
                View child = getChildAt(0);
                if (orientation == ORIENTATION_HORIZONTAL) {
                    parent.requestDisallowInterceptTouchEvent(child.canScrollHorizontally(direction));
                } else {
                    parent.requestDisallowInterceptTouchEvent(child.canScrollVertically(direction));
                }
            }
        }

        return super.onInterceptTouchEvent(e);
    }
}
