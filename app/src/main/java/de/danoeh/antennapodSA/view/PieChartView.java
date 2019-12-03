package de.danoeh.antennapodSA.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import io.reactivex.annotations.Nullable;

public class PieChartView extends AppCompatImageView {
    private PieChartDrawable drawable;

    public PieChartView(Context context) {
        super(context);
        setup();
    }

    public PieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public PieChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setup() {
        drawable = new PieChartDrawable();
        setImageDrawable(drawable);
    }

    /**
     * Set array od names, array of values and array of colors.
     */
    public void setData(float[] dataValues) {
        drawable.dataValues = dataValues;
        drawable.valueSum = 0;
        for (float datum : dataValues) {
            drawable.valueSum += datum;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        setMeasuredDimension(width, width / 2);
    }

    private static class PieChartDrawable extends Drawable {
        private static final float MIN_DEGREES = 10f;
        private static final float PADDING_DEGREES = 3f;
        private static final float STROKE_SIZE = 15f;
        private static final int[] COLOR_VALUES = new int[]{0xFF3775E6, 0xffe51c23, 0xffff9800, 0xff259b24, 0xff9c27b0,
                0xff0099c6, 0xffdd4477, 0xff66aa00, 0xffb82e2e, 0xff316395,
                0xff994499, 0xff22aa99, 0xffaaaa11, 0xff6633cc, 0xff0073e6};
        private float[] dataValues;
        private float valueSum;
        private final Paint paint;

        private PieChartDrawable() {
            paint = new Paint();
            paint.setFlags(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(STROKE_SIZE);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (valueSum == 0) {
                return;
            }
            float radius = getBounds().height() - STROKE_SIZE;
            float center = getBounds().width() / 2.f;
            RectF arcBounds = new RectF(center - radius, STROKE_SIZE, center + radius, STROKE_SIZE + radius * 2);

            float startAngle = 180;
            for (int i = 0; i < dataValues.length; i++) {
                float datum = dataValues[i];
                float sweepAngle = (180f - PADDING_DEGREES) * (datum / valueSum);
                if (sweepAngle < MIN_DEGREES) {
                    break;
                }
                paint.setColor(COLOR_VALUES[i % COLOR_VALUES.length]);
                float padding = i == 0 ? PADDING_DEGREES / 2 : PADDING_DEGREES;
                canvas.drawArc(arcBounds, startAngle + padding, sweepAngle - padding, false, paint);
                startAngle = startAngle + sweepAngle;
            }

            paint.setColor(Color.GRAY);
            float sweepAngle = 360 - startAngle - PADDING_DEGREES / 2;
            if (sweepAngle > PADDING_DEGREES) {
                canvas.drawArc(arcBounds, startAngle + PADDING_DEGREES, sweepAngle - PADDING_DEGREES, false, paint);
            }
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }
    }
}
