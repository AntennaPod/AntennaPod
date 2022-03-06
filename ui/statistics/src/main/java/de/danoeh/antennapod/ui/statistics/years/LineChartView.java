package de.danoeh.antennapod.ui.statistics.years;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.statistics.R;
import io.reactivex.annotations.Nullable;

public class LineChartView extends AppCompatImageView {
    private LineChartDrawable drawable;

    public LineChartView(Context context) {
        super(context);
        setup();
    }

    public LineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public LineChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setup() {
        drawable = new LineChartDrawable();
        setImageDrawable(drawable);
    }

    /**
     * Set of data values to display.
     */
    public void setData(LineChartData data) {
        drawable.data = data;
    }

    public static class LineChartData {
        private final long valueMax;
        private final long[] values;
        private final long[] verticalLines;

        public LineChartData(long[] values, long[] verticalLines) {
            this.values = values;
            long valueMax = 0;
            for (long datum : values) {
                valueMax = Math.max(datum, valueMax);
            }
            this.valueMax = valueMax;
            this.verticalLines = verticalLines;
        }

        public float getHeight(int item) {
            return (float) values[item] / valueMax;
        }
    }

    private class LineChartDrawable extends Drawable {
        private LineChartData data;
        private final Paint paintLine;
        private final Paint paintBackground;
        private final Paint paintVerticalLines;

        private LineChartDrawable() {
            paintLine = new Paint();
            paintLine.setFlags(Paint.ANTI_ALIAS_FLAG);
            paintLine.setStyle(Paint.Style.STROKE);
            paintLine.setStrokeJoin(Paint.Join.ROUND);
            paintLine.setStrokeCap(Paint.Cap.ROUND);
            paintLine.setColor(ThemeUtils.getColorFromAttr(getContext(), R.attr.colorAccent));
            paintBackground = new Paint();
            paintBackground.setStyle(Paint.Style.FILL);
            paintVerticalLines = new Paint();
            paintVerticalLines.setStyle(Paint.Style.STROKE);
            paintVerticalLines.setPathEffect(new DashPathEffect(new float[] {10f, 10f}, 0f));
            paintVerticalLines.setColor(0x66777777);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            float width = getBounds().width();
            float height = getBounds().height();
            float usableHeight = height * 0.9f;
            float stepSize = width / (data.values.length + 1);

            paintVerticalLines.setStrokeWidth(height * 0.005f);
            for (long line : data.verticalLines) {
                canvas.drawLine((line + 1) * stepSize, 0, (line + 1) * stepSize, height, paintVerticalLines);
            }

            paintLine.setStrokeWidth(height * 0.015f);
            Path path = new Path();
            for (int i = 0; i < data.values.length; i++) {
                if (i == 0) {
                    path.moveTo((i + 1) * stepSize, (1 - data.getHeight(i)) * usableHeight + height * 0.05f);
                } else {
                    path.lineTo((i + 1) * stepSize, (1 - data.getHeight(i)) * usableHeight + height * 0.05f);
                }
            }
            canvas.drawPath(path, paintLine);

            path.lineTo(data.values.length * stepSize, height);
            path.lineTo(stepSize, height);
            paintBackground.setShader(new LinearGradient(0, 0, 0, height,
                    (ThemeUtils.getColorFromAttr(getContext(), R.attr.colorAccent) & 0xffffff) + 0x66000000,
                    Color.TRANSPARENT, Shader.TileMode.CLAMP));
            canvas.drawPath(path, paintBackground);
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
