package de.danoeh.antennapod.view;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

public class PagerIndicatorView extends View {
    private final Paint paint = new Paint();
    private float position = 0;
    private int numPages = 0;
    private int disabledPage = -1;
    private int circleColor = 0;
    private int circleColorHighlight = -1;

    public PagerIndicatorView(Context context) {
        super(context);
        setup();
    }

    public PagerIndicatorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public PagerIndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private void setup() {
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        int[] colorAttrs = new int[]{android.R.attr.textColorSecondary};
        TypedArray a = getContext().obtainStyledAttributes(colorAttrs);
        circleColorHighlight = a.getColor(0, 0xffffffff);
        circleColor = (Integer) new ArgbEvaluator().evaluate(0.8f, 0x00ffffff, circleColorHighlight);
        a.recycle();
    }

    public void setViewPager(ViewPager2 pager) {
        numPages = pager.getAdapter().getItemCount();
        pager.getAdapter().registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                numPages = pager.getAdapter().getItemCount();
                invalidate();
            }
        });
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                PagerIndicatorView.this.position = position + positionOffset;
                if (ViewCompat.getLayoutDirection(pager) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                    PagerIndicatorView.this.position = numPages - 1 - PagerIndicatorView.this.position;
                }
                invalidate();
            }
        });
    }

    public void setDisabledPage(int disabledPage) {
        this.disabledPage = disabledPage;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0; i < numPages; i++) {
            if ((int) Math.floor(position) == i) {
                // This is the current dot
                drawCircle(canvas, i, (float) (1 - (position - Math.floor(position))));
            } else if ((int) Math.ceil(position) == i) {
                // This is the next dot
                drawCircle(canvas, i, (float) (position - Math.floor(position)));
            } else {
                drawCircle(canvas, i, 0);
            }
        }
    }

    private void drawCircle(Canvas canvas, int position, float frac) {
        float availableHeight = canvas.getHeight() - getPaddingTop() - getPaddingBottom();
        float circleRadiusSmall = availableHeight * 0.26f;
        float circleRadiusBig = availableHeight * 0.35f;
        float circleRadiusDelta = (circleRadiusBig - circleRadiusSmall);
        float start = 0.5f * (canvas.getWidth() - numPages * 1.5f * availableHeight);
        paint.setStrokeWidth(availableHeight * 0.3f);

        if (position == disabledPage) {
            paint.setStyle(Paint.Style.STROKE);
        } else {
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
        }

        paint.setColor((Integer) new ArgbEvaluator().evaluate(frac, circleColor, circleColorHighlight));
        canvas.drawCircle(start + (position * 1.5f + 0.75f) * availableHeight, 0.5f * availableHeight + getPaddingTop(),
                circleRadiusSmall + frac * circleRadiusDelta, paint);
    }
}