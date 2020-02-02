package de.danoeh.antennapod.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator;
import androidx.viewpager.widget.ViewPager;

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

        int[] colorAttrs = new int[] { android.R.attr.textColorSecondary };
        TypedArray a = getContext().obtainStyledAttributes(colorAttrs);
        circleColorHighlight = a.getColor(0, 0xffffffff);
        circleColor = (Integer) new ArgbEvaluator().evaluate(0.8f, 0x00ffffff, circleColorHighlight);
        a.recycle();
    }

    public void setViewPager(ViewPager pager) {
        numPages = pager.getAdapter().getCount();
        pager.getAdapter().registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                numPages = pager.getAdapter().getCount();
                invalidate();
            }
        });
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                PagerIndicatorView.this.position = position + positionOffset;
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
        float circleRadiusSmall = canvas.getHeight() * 0.26f;
        float circleRadiusBig = canvas.getHeight() * 0.35f;
        float circleRadiusDelta = (circleRadiusBig - circleRadiusSmall);
        float start = 0.5f * (canvas.getWidth() - numPages * 1.5f * canvas.getHeight());
        paint.setStrokeWidth(canvas.getHeight() * 0.3f);

        if (position == disabledPage) {
            paint.setStyle(Paint.Style.STROKE);
        } else {
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
        }

        paint.setColor((Integer) new ArgbEvaluator().evaluate(frac, circleColor, circleColorHighlight));
        canvas.drawCircle(start + (position * 1.5f + 0.75f) * canvas.getHeight(), 0.5f * canvas.getHeight(),
                circleRadiusSmall + frac * circleRadiusDelta, paint);
    }
}