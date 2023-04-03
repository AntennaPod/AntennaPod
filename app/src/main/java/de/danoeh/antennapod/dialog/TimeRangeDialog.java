package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.text.format.DateFormat;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.common.ThemeUtils;

import java.util.Locale;

public class TimeRangeDialog extends MaterialAlertDialogBuilder {

    public TimeRangeDialog(@NonNull Context context) {
        super(context);
        setView(new TimeRangeView(context));
    }

    class TimeRangeView extends View {
        private final Paint paintBackground = new Paint();
        private final Paint paintSelected = new Paint();
        private final Paint paintText = new Paint();
        private int from = 0;
        private int to = 6;
        private final RectF bounds = new RectF();
        int touching = 0;

        public TimeRangeView(Context context) {
            super(context);
            setup();
        }

        private void setup() {
            paintBackground.setAntiAlias(true);
            paintBackground.setStyle(Paint.Style.STROKE);
            paintBackground.setColor(ThemeUtils.getColorFromAttr(getContext(), android.R.attr.textColorPrimary));

            paintSelected.setAntiAlias(true);
            paintSelected.setStyle(Paint.Style.STROKE);
            paintSelected.setStrokeCap(Paint.Cap.ROUND);
            paintSelected.setColor(ThemeUtils.getColorFromAttr(getContext(), R.attr.colorAccent));

            paintText.setAntiAlias(true);
            paintText.setStyle(Paint.Style.FILL);
            paintText.setColor(ThemeUtils.getColorFromAttr(getContext(), android.R.attr.textColorPrimary));
            paintText.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
                    && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            } else if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
                super.onMeasure(widthMeasureSpec, widthMeasureSpec);
            } else if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
                super.onMeasure(heightMeasureSpec, heightMeasureSpec);
            } else if (MeasureSpec.getSize(widthMeasureSpec) < MeasureSpec.getSize(heightMeasureSpec)) {
                super.onMeasure(widthMeasureSpec, widthMeasureSpec);
            } else {
                super.onMeasure(heightMeasureSpec, heightMeasureSpec);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float size = getHeight(); // square
            float padding = size * 0.1f;
            paintBackground.setStrokeWidth(size * 0.005f);
            bounds.set(padding, padding, size - padding, size - padding);

            canvas.drawArc(bounds, 0, 360, false, paintBackground);
            for (int i = 0; i < 24; i++) {
                Point p1 = radToPoint(i / 24.0f * 360.f, size / 2 - 1.6f * padding);
                Point p2 = radToPoint(i / 24.0f * 360.f, size / 2 - ((i % 6 == 0) ? 2.1f : 1.8f) * padding);
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paintBackground);
            }

            float angleFrom = (float) from / 24 * 360 - 90;
            float angleDistance = (float) ((to - from + 24) % 24) / 24 * 360;
            paintSelected.setStrokeWidth(padding / 6);
            paintSelected.setStyle(Paint.Style.STROKE);
            canvas.drawArc(bounds, angleFrom, angleDistance, false, paintSelected);
            paintSelected.setStyle(Paint.Style.FILL);
            Point p1 = radToPoint(angleFrom + 90, size / 2 - padding);
            canvas.drawCircle(p1.x, p1.y, padding / 2, paintSelected);
            Point p2 = radToPoint(angleFrom + angleDistance + 90, size / 2 - padding);
            canvas.drawCircle(p2.x, p2.y, padding / 2, paintSelected);

            paintText.setTextSize(0.7f * padding);
            String timeRange;
            if (DateFormat.is24HourFormat(getContext())) {
                timeRange = String.format(Locale.getDefault(), "%02d:00 - %02d:00", from, to);
            } else {
                timeRange = String.format(Locale.getDefault(), "%02d:00 %s - %02d:00 %s", from % 12,
                        from >= 12 ? "PM" : "AM", to % 12, to >= 12 ? "PM" : "AM");
            }
            canvas.drawText(timeRange, size / 2, size / 2, paintText);
        }

        protected Point radToPoint(float angle, float radius) {
            return new Point((int) (getWidth() / 2 + radius * Math.sin(-angle * Math.PI / 180 + Math.PI)),
                    (int) (getHeight() / 2 + radius * Math.cos(-angle * Math.PI / 180 + Math.PI)));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            getParent().requestDisallowInterceptTouchEvent(true);
            Point center = new Point(getWidth() / 2, getHeight() / 2);
            double angleRad = Math.atan2(center.y - event.getY(), center.x - event.getX());
            float angle = (float) (angleRad * (180 / Math.PI));
            angle += 360 + 360 - 90;
            angle %= 360;

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float fromDistance = Math.abs(angle - (float) from / 24 * 360);
                float toDistance = Math.abs(angle - (float) to / 24 * 360);
                if (fromDistance < 15 || fromDistance > (360 - 15)) {
                    touching = 1;
                    return true;
                } else if (toDistance < 15 || toDistance > (360 - 15)) {
                    touching = 2;
                    return true;
                }
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                int newTime = (int) (24 * (angle / 360.0));
                if (touching == 1) {
                    from = newTime;
                    invalidate();
                    return true;
                } else if (touching == 2) {
                    to = newTime;
                    invalidate();
                    return true;
                }
            } else if (touching != 0) {
                touching = 0;
                return true;
            }
            return super.onTouchEvent(event);
        }
    }
}
