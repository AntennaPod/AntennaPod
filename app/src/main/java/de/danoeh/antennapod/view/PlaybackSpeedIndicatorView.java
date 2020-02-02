package de.danoeh.antennapod.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class PlaybackSpeedIndicatorView extends View {
    private static final float DEG_2_RAD = (float) (Math.PI / 180);
    private static final float PADDING_ANGLE = 30;

    private final Paint arcPaint = new Paint();
    private final Paint indicatorPaint = new Paint();
    private final Path trianglePath = new Path();
    private float angle = 0;
    private float targetAngle = 0.5f;
    private float degreePerFrame = 2;
    private float paddingArc = 20;
    private float paddingIndicator = 10;

    public PlaybackSpeedIndicatorView(Context context) {
        super(context);
        setup();
    }

    public PlaybackSpeedIndicatorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public PlaybackSpeedIndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private void setup() {
        arcPaint.setAntiAlias(true);
        arcPaint.setColor(Color.GRAY);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        indicatorPaint.setAntiAlias(true);
        indicatorPaint.setColor(Color.GRAY);
        indicatorPaint.setStyle(Paint.Style.FILL);

        trianglePath.setFillType(Path.FillType.EVEN_ODD);
    }

    public void setSpeed(float value) {
        float maxAnglePerDirection = 90 + 45 - 2 * paddingArc;
        if (value >= 1) {
            // Speed values above 3 are probably not too common. Cap at 3 for better differentiation
            targetAngle = maxAnglePerDirection * ((Math.min(3, value) - 1) / 2);
        } else {
            targetAngle = -maxAnglePerDirection * (1 - ((value - 0.5f) * 2));
        }
        degreePerFrame = Math.abs(targetAngle - angle) / 20;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        paddingArc = getMeasuredHeight() / 5f;
        paddingIndicator = getMeasuredHeight() / 10f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float radiusInnerCircle = getWidth() / 8f;
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radiusInnerCircle, indicatorPaint);

        trianglePath.rewind();
        float bigRadius = getHeight() / 2f - paddingIndicator;
        trianglePath.moveTo(getWidth() / 2f + (float) (bigRadius * Math.sin((-angle + 180) * DEG_2_RAD)),
                getHeight() / 2f + (float) (bigRadius * Math.cos((-angle + 180) * DEG_2_RAD)));
        trianglePath.lineTo(getWidth() / 2f + (float) (radiusInnerCircle * Math.sin((-angle + 180 - 90) * DEG_2_RAD)),
                getHeight() / 2f + (float) (radiusInnerCircle * Math.cos((-angle + 180 - 90) * DEG_2_RAD)));
        trianglePath.lineTo(getWidth() / 2f + (float) (radiusInnerCircle * Math.sin((-angle + 180 + 90) * DEG_2_RAD)),
                getHeight() / 2f + (float) (radiusInnerCircle * Math.cos((-angle + 180 + 90) * DEG_2_RAD)));
        trianglePath.close();
        canvas.drawPath(trianglePath, indicatorPaint);

        arcPaint.setStrokeWidth(getHeight() / 15f);
        RectF arcBounds = new RectF(paddingArc, paddingArc, getWidth() - paddingArc, getHeight() - paddingArc);
        canvas.drawArc(arcBounds, -180 - 45, 90 + 45 + angle - PADDING_ANGLE, false, arcPaint);
        canvas.drawArc(arcBounds, -90 + PADDING_ANGLE + angle, 90 + 45 - PADDING_ANGLE - angle, false, arcPaint);

        if (Math.abs(angle - targetAngle) > 0.5) {
            angle += Math.signum(targetAngle - angle) * Math.min(degreePerFrame, Math.abs(targetAngle - angle));
            invalidate();
        }
    }
}
