package de.danoeh.antennapod.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class CircularProgressBar extends View {
    public static final float MINIMUM_PERCENTAGE = 0.005f;
    public static final float MAXIMUM_PERCENTAGE = 1 - MINIMUM_PERCENTAGE;

    private final Paint paintBackground = new Paint();
    private final Paint paintProgress = new Paint();
    private float percentage = 0;
    private float targetPercentage = 0;
    private Object tag = null;
    private final RectF bounds = new RectF();

    public CircularProgressBar(Context context) {
        super(context);
        setup(null);
    }

    public CircularProgressBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup(attrs);
    }

    public CircularProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(attrs);
    }

    private void setup(@Nullable AttributeSet attrs) {
        paintBackground.setAntiAlias(true);
        paintBackground.setStyle(Paint.Style.STROKE);

        paintProgress.setAntiAlias(true);
        paintProgress.setStyle(Paint.Style.STROKE);
        paintProgress.setStrokeCap(Paint.Cap.ROUND);

        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.CircularProgressBar);
        int color = typedArray.getColor(R.styleable.CircularProgressBar_foregroundColor, Color.GREEN);
        typedArray.recycle();
        paintProgress.setColor(color);
        paintBackground.setColor(color);
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

        float padding = getHeight() * 0.07f;
        paintBackground.setStrokeWidth(getHeight() * 0.02f);
        paintProgress.setStrokeWidth(padding);
        bounds.set(padding, padding, getWidth() - padding, getHeight() - padding);
        canvas.drawArc(bounds, 0, 360, false, paintBackground);

        if (MINIMUM_PERCENTAGE <= percentage && percentage <= MAXIMUM_PERCENTAGE) {
            canvas.drawArc(bounds, -90, percentage * 360, false, paintProgress);
        }

        if (Math.abs(percentage - targetPercentage) > MINIMUM_PERCENTAGE) {
            float speed = 0.02f;
            if (Math.abs(targetPercentage - percentage) < 0.1 && targetPercentage > percentage) {
                speed = 0.006f;
            }
            float delta = Math.min(speed, Math.abs(targetPercentage - percentage));
            float direction = ((targetPercentage - percentage) > 0 ? 1f : -1f);
            percentage += delta * direction;
            invalidate();
        }
    }
}
