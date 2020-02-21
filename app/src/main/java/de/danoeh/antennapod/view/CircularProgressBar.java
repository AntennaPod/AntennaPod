package de.danoeh.antennapod.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class CircularProgressBar extends View {
    private static final float EPSILON = 0.005f;

    private final Paint paintBackground = new Paint();
    private final Paint paintProgress = new Paint();
    private float percentage = 0;
    private float targetPercentage = 0;
    private Object tag = null;

    public CircularProgressBar(Context context) {
        super(context);
        setup();
    }

    public CircularProgressBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public CircularProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private void setup() {
        paintBackground.setAntiAlias(true);
        paintBackground.setStyle(Paint.Style.STROKE);

        paintProgress.setAntiAlias(true);
        paintProgress.setStyle(Paint.Style.STROKE);
        paintProgress.setStrokeCap(Paint.Cap.ROUND);

        int[] colorAttrs = new int[] { android.R.attr.textColorPrimary, android.R.attr.textColorSecondary };
        TypedArray a = getContext().obtainStyledAttributes(colorAttrs);
        paintProgress.setColor(a.getColor(0, 0xffffffff));
        paintBackground.setColor(a.getColor(1, 0xffffffff));
        a.recycle();
    }

    /**
     * Sets the percentage to be displayed.
     * @param percentage Number from 0 to 1
     * @param tag When the tag is the same as last time calling setPercentage, the update is animated
     */
    public void setPercentage(float percentage, Object tag) {
        targetPercentage = percentage;

        if (tag == null || !tag.equals(this.tag)) {
            // Do not animate
            this.percentage = percentage;
            this.tag = tag;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float padding = getHeight() * 0.06f;
        paintBackground.setStrokeWidth(getHeight() * 0.02f);
        paintProgress.setStrokeWidth(padding);
        RectF bounds = new RectF(padding, padding, getWidth() - padding, getHeight() - padding);
        canvas.drawArc(bounds, 0, 360, false, paintBackground);

        if (percentage > EPSILON && 1 - percentage > EPSILON) {
            canvas.drawArc(bounds, -90, percentage * 360, false, paintProgress);
        }

        if (Math.abs(percentage - targetPercentage) > EPSILON) {
            float delta = Math.min(0.02f, Math.abs(targetPercentage - percentage));
            percentage += delta * ((targetPercentage - percentage) > 0 ? 1f : -1f);
            invalidate();
        }
    }
}
