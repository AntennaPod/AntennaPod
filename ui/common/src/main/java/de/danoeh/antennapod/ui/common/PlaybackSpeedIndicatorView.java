package de.danoeh.antennapod.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
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
    private static final float VALUE_UNSET = -4242;

    private final Paint arcPaint = new Paint();
    private final Paint indicatorPaint = new Paint();
    private final Path trianglePath = new Path();
    private final RectF arcBounds = new RectF();
    private float angle = VALUE_UNSET;
    private float targetAngle = VALUE_UNSET;
    private float degreePerFrame = 2;
    private float paddingArc = 20;
    private float paddingIndicator = 10;

    public PlaybackSpeedIndicatorView(Context context) {
        super(context);
        setup(null);
    }

    public PlaybackSpeedIndicatorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup(attrs);
    }

    public PlaybackSpeedIndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(attrs);
    }

    private void setup(@Nullable AttributeSet attrs) {
        setSpeed(1.0f); // Set default angle to 1.0
        targetAngle = VALUE_UNSET; // Do not move to first value that is set externally

        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.PlaybackSpeedIndicatorView);
        int color = typedArray.getColor(R.styleable.PlaybackSpeedIndicatorView_foregroundColor, Color.GREEN);
        typedArray.recycle();
        arcPaint.setColor(color);
        indicatorPaint.setColor(color);

        arcPaint.setAntiAlias(true);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        indicatorPaint.setAntiAlias(true);
        indicatorPaint.setStyle(Paint.Style.FILL);

        trianglePath.setFillType(Path.FillType.EVEN_ODD);
    }

    public void setSpeed(float value) {
        float maxAnglePerDirection = 90 + 45 - 2 * paddingArc;
        // Speed values above 3 are probably not too common. Cap at 3 for better differentiation
        float normalizedValue = Math.min(2.5f, value - 0.5f) / 2.5f; // Linear between 0 and 1
        float target = -maxAnglePerDirection + 2 * maxAnglePerDirection * normalizedValue;
        if (targetAngle == VALUE_UNSET) {
            angle = target;
        }
        targetAngle = target;
        degreePerFrame = Math.abs(targetAngle - angle) / 20;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        paddingArc = getMeasuredHeight() / 4.5f;
        paddingIndicator = getMeasuredHeight() / 6f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float radiusInnerCircle = getWidth() / 10f;
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
        arcBounds.set(paddingArc, paddingArc, getWidth() - paddingArc, getHeight() - paddingArc);
        canvas.drawArc(arcBounds, -180 - 45, 90 + 45 + angle - PADDING_ANGLE, false, arcPaint);
        canvas.drawArc(arcBounds, -90 + PADDING_ANGLE + angle, 90 + 45 - PADDING_ANGLE - angle, false, arcPaint);

        if (Math.abs(angle - targetAngle) > 0.5 && targetAngle != VALUE_UNSET) {
            angle += Math.signum(targetAngle - angle) * Math.min(degreePerFrame, Math.abs(targetAngle - angle));
            invalidate();
        }
    }
}
