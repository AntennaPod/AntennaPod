package de.danoeh.antennapod.ui.statistics.years;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.statistics.StatisticsColorScheme;
import io.reactivex.annotations.Nullable;

public class BarChartView extends AppCompatImageView {
    private BarChartDrawable drawable;

    public BarChartView(Context context) {
        super(context);
        setup();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setup() {
        drawable = new BarChartDrawable();
        setImageDrawable(drawable);
    }

    /**
     * Set of data values to display.
     */
    public void setData(BarChartData data) {
        drawable.data = data;
    }

    public static class BarChartData {
        private final long valueMax;
        private final long[] values;
        private final long[] segmentBorders;

        public BarChartData(long[] values, long[] segmentBorders) {
            this.values = values;
            long valueMax = 0;
            for (long datum : values) {
                valueMax = Math.max(datum, valueMax);
            }
            this.valueMax = valueMax;
            this.segmentBorders = segmentBorders;
        }

        public float getHeight(int item) {
            return (float) Math.max(0.005, (float) values[item] / valueMax);
        }
    }

    private class BarChartDrawable extends Drawable {
        private BarChartData data;
        private final Paint paintBars;
        private final Paint paintGridLines;
        private final Paint paintGridText;
        private static final long ONE_HOUR = 3600000L;

        private BarChartDrawable() {
            paintBars = new Paint();
            paintBars.setStyle(Paint.Style.FILL);
            paintBars.setAntiAlias(true);
            paintGridLines = new Paint();
            paintGridLines.setStyle(Paint.Style.STROKE);
            paintGridLines.setPathEffect(new DashPathEffect(new float[] {10f, 10f}, 0f));
            paintGridLines.setColor(ThemeUtils.getColorFromAttr(getContext(), android.R.attr.textColorSecondary));
            paintGridText = new Paint();
            paintGridText.setAntiAlias(true);
            paintGridText.setColor(ThemeUtils.getColorFromAttr(getContext(), android.R.attr.textColorSecondary));
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            final float width = getBounds().width();
            final float height = getBounds().height();
            final float usableHeight = height * 0.9f;
            final float textPadding = width * 0.06f;
            final float stepSize = (width - textPadding) / (data.values.length + 2);

            paintBars.setStrokeWidth(height * 0.015f);
            paintBars.setColor(StatisticsColorScheme.COLOR_VALUES[0]);
            int colorIndex = 0;
            for (int i = 0; i < data.values.length; i++) {
                if (colorIndex < data.segmentBorders.length && i == data.segmentBorders[colorIndex]) {
                    colorIndex++;
                    paintBars.setColor(StatisticsColorScheme.COLOR_VALUES[
                            colorIndex % StatisticsColorScheme.COLOR_VALUES.length]);
                }

                float x = textPadding + (i + 1) * stepSize;
                float y = (1 - data.getHeight(i)) * usableHeight + height * 0.05f;
                canvas.drawRect(x, y, x + stepSize * 0.95f, usableHeight + height * 0.05f, paintBars);
            }

            float textSize = height * 0.07f;
            paintGridText.setTextSize(textSize);
            float maxLine = (float) (Math.floor(data.valueMax / (10.0 * ONE_HOUR)) * 10 * ONE_HOUR);
            float y = (1 - (maxLine / data.valueMax)) * usableHeight + height * 0.05f;
            canvas.drawLine(0, y, width, y, paintGridLines);
            canvas.drawText(String.valueOf((long) maxLine / ONE_HOUR), 0, y + 1.2f * textSize, paintGridText);

            float midLine = maxLine / 2;
            y = (1 - (midLine / data.valueMax)) * usableHeight + height * 0.05f;
            canvas.drawLine(0, y, width, y, paintGridLines);
            canvas.drawText(String.valueOf((long) midLine / ONE_HOUR), 0, y + 1.2f * textSize, paintGridText);
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
