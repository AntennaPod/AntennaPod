package de.danoeh.antennapod.view;

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
     * Set of data values to display.
     */
    public void setData(PieChartData data) {
        drawable.data = data;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        setMeasuredDimension(width, width / 2);
    }

    public static class PieChartData {
        private static final int[] COLOR_VALUES = new int[]{0xFF3775E6, 0xffe51c23, 0xffff9800, 0xff259b24, 0xff9c27b0,
                0xff0099c6, 0xffdd4477, 0xff66aa00, 0xffb82e2e, 0xff316395,
                0xff994499, 0xff22aa99, 0xffaaaa11, 0xff6633cc, 0xff0073e6};

        private final float valueSum;
        private final float[] values;

        public PieChartData(float[] values) {
            this.values = values;
            float valueSum = 0;
            for (float datum : values) {
                valueSum += datum;
            }
            this.valueSum = valueSum;
        }

        public float getSum() {
            return valueSum;
        }

        public float getPercentageOfItem(int index) {
            if (valueSum == 0) {
                return 0;
            }
            return values[index] / valueSum;
        }

        public boolean isLargeEnoughToDisplay(int index) {
            return getPercentageOfItem(index) > 0.05;
        }

        public int getColorOfItem(int index) {
            if (!isLargeEnoughToDisplay(index)) {
                return Color.GRAY;
            }
            return COLOR_VALUES[index % COLOR_VALUES.length];
        }
    }

    private static class PieChartDrawable extends Drawable {
        private static final float PADDING_DEGREES = 3f;
        private static final float STROKE_SIZE = 15f;
        private PieChartData data;
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
            float radius = getBounds().height() - STROKE_SIZE;
            float center = getBounds().width() / 2.f;
            RectF arcBounds = new RectF(center - radius, STROKE_SIZE, center + radius, STROKE_SIZE + radius * 2);

            float startAngle = 180;
            for (int i = 0; i < data.values.length; i++) {
                if (!data.isLargeEnoughToDisplay(i)) {
                    break;
                }
                paint.setColor(data.getColorOfItem(i));
                float padding = i == 0 ? PADDING_DEGREES / 2 : PADDING_DEGREES;
                float sweepAngle = (180f - PADDING_DEGREES) * data.getPercentageOfItem(i);
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
