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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import de.danoeh.antennapod.R;

import static androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL;
import static androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL;


/**
 * Layout to wrap a scrollable component inside a ViewPager2. Provided as a solution to the problem
 * where pages of ViewPager2 have nested scrollable elements that scroll in the same direction as
 * ViewPager2. The scrollable element needs to be the immediate and only child of this host layout.
 *
 * This solution has limitations when using multiple levels of nested scrollable elements
 * (e.g. a horizontal RecyclerView in a vertical RecyclerView in a horizontal ViewPager2).
 */ // KhaledAlharthi/NestedScrollableHost.java
public class NestedScrollableHost extends FrameLayout {

    private ViewPager2 parentViewPager;
    private int touchSlop = 0;
    private float initialX = 0f;
    private float initialY = 0f;
    private int preferVertical = 1;
    private int preferHorizontal = 1;
    private int scrollDirection = 0;

    public NestedScrollableHost(@NonNull Context context) {
        super(context);
        init(context);
    }

    public NestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
        setAttributes(context, attrs);
    }

    public NestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
        setAttributes(context, attrs);
    }

    public NestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs,
                                int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
        setAttributes(context, attrs);
    }

    private void setAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.NestedScrollableHost,
                0, 0);

        try {
            preferHorizontal = a.getInteger(R.styleable.NestedScrollableHost_preferHorizontal, 1);
            preferVertical = a.getInteger(R.styleable.NestedScrollableHost_preferVertical, 1);
            scrollDirection = a.getInteger(R.styleable.NestedScrollableHost_scrollDirection, 0);
        } finally {
            a.recycle();
        }

    }

    private void init(Context context) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();


        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                View v = (View) getParent();
                while (v != null && !(v instanceof ViewPager2) || isntSameDirection(v)) {
                    v = (View) v.getParent();
                }
                parentViewPager = (ViewPager2) v;

                getViewTreeObserver().removeOnPreDrawListener(this);
                return false;
            }
        });
    }

    private Boolean isntSameDirection(View v) {
        int orientation = 0;
        switch (scrollDirection) {
            default:
            case 0:
                return false;
            case 1:
                orientation = ORIENTATION_VERTICAL;
                break;
            case 2:
                orientation = ORIENTATION_HORIZONTAL;
                break;
        }
        return ((v instanceof ViewPager2) && ((ViewPager2) v).getOrientation() != orientation);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        handleInterceptTouchEvent(ev);
        return super.onInterceptTouchEvent(ev);
    }


    private boolean canChildScroll(int orientation, float delta) {
        int direction = (int) -delta;
        View child = getChildAt(0);
        if (orientation == 0) {
            return child.canScrollHorizontally(direction);
        } else if (orientation == 1) {
            return child.canScrollVertically(direction);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void handleInterceptTouchEvent(MotionEvent e) {
        if (parentViewPager == null) {
            return;
        }
        int orientation = parentViewPager.getOrientation();
        boolean preferedDirection = preferHorizontal + preferVertical > 2;

        // Early return if child can't scroll in same direction as parent and theres no prefered scroll direction
        if (!canChildScroll(orientation, -1f) && !canChildScroll(orientation, 1f) && !preferedDirection) {
            return;
        }


        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            initialX = e.getX();
            initialY = e.getY();
            getParent().requestDisallowInterceptTouchEvent(true);
        } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = e.getX() - initialX;
            float dy = e.getY() - initialY;
            boolean isVpHorizontal = orientation == ViewPager2.ORIENTATION_HORIZONTAL;

            // assuming ViewPager2 touch-slop is 2x touch-slop of child
            float scaledDx = Math.abs(dx) * (isVpHorizontal ? 1f : 0.5f) * preferHorizontal;
            float scaledDy = Math.abs(dy) * (isVpHorizontal ? 0.5f : 1f) * preferVertical;
            if (scaledDx > touchSlop || scaledDy > touchSlop) {
                if (isVpHorizontal == (scaledDy > scaledDx)) {
                    // Gesture is perpendicular, allow all parents to intercept
                    getParent().requestDisallowInterceptTouchEvent(preferedDirection);
                } else {
                    // Gesture is parallel, query child if movement in that direction is possible
                    if (canChildScroll(orientation, isVpHorizontal ? dx : dy)) {
                        // Child can scroll, disallow all parents to intercept
                        getParent().requestDisallowInterceptTouchEvent(true);
                    } else {
                        // Child cannot scroll, allow all parents to intercept
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
            }

        }
    }
}
