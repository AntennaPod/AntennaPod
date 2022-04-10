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
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.statistics.R;

import java.util.List;

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
    public void setData(List<DBReader.MonthlyStatisticsItem> data) {
        drawable.data = data;
        drawable.maxValue = 1;
        for (DBReader.MonthlyStatisticsItem item : data) {
            drawable.maxValue = Math.max(drawable.maxValue, item.timePlayed);
        }
    }

    private class BarChartDrawable extends Drawable {
        private static final long ONE_HOUR = 3600000L;
        private List<DBReader.MonthlyStatisticsItem> data;
        private long maxValue = 1;
        private final Paint paintBars;
        private final Paint paintGridLines;
        private final Paint paintGridText;
        private final int[] colors = {0, 0xff9c27b0};

        private BarChartDrawable() {
            colors[0] = ThemeUtils.getColorFromAttr(getContext(), R.attr.colorAccent);
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
            final float barHeight = height * 0.9f;
            final float textPadding = width * 0.05f;
            final float stepSize = (width - textPadding) / (data.size() + 2);
            final float textSize = height * 0.06f;
            paintGridText.setTextSize(textSize);

            paintBars.setStrokeWidth(height * 0.015f);
            paintBars.setColor(colors[0]);
            int colorIndex = 0;
            int lastYear = data.size() > 0 ? data.get(0).year : 0;
            for (int i = 0; i < data.size(); i++) {
                float x = textPadding + (i + 1) * stepSize;
                if (lastYear != data.get(i).year) {
                    lastYear = data.get(i).year;
                    colorIndex++;
                    paintBars.setColor(colors[colorIndex % 2]);
                    if (i < data.size() - 2) {
                        canvas.drawText(String.valueOf(data.get(i).year), x + stepSize,
                                barHeight + (height - barHeight + textSize) / 2, paintGridText);
                    }
                    canvas.drawLine(x, height, x, barHeight, paintGridText);
                }

                float valuePercentage = (float) Math.max(0.005, (float) data.get(i).timePlayed / maxValue);
                float y = (1 - valuePercentage) * barHeight;
                canvas.drawRect(x, y, x + stepSize * 0.95f, barHeight, paintBars);
            }

            float maxLine = (float) (Math.floor(maxValue / (10.0 * ONE_HOUR)) * 10 * ONE_HOUR);
            float y = (1 - (maxLine / maxValue)) * barHeight;
            canvas.drawLine(0, y, width, y, paintGridLines);
            canvas.drawText(String.valueOf((long) maxLine / ONE_HOUR), 0, y + 1.2f * textSize, paintGridText);

            float midLine = maxLine / 2;
            y = (1 - (midLine / maxValue)) * barHeight;
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
