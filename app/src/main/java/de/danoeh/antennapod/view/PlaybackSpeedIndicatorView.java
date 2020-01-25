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
    private final Paint arcPaint = new Paint();
    private final Paint indicatorPaint = new Paint();
    private final Path trianglePath = new Path();
    float angle = 0;
    float targetAngle = 0.5f;
    float degreePerFrame = 2;
    float paddingArc = 20;
    float paddingIndicator = 10;
    double deg2rad = Math.PI / 180;
    float paddingAngle = 30;

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
        arcPaint.setStrokeWidth(10);

        indicatorPaint.setAntiAlias(true);
        indicatorPaint.setColor(Color.GRAY);
        indicatorPaint.setStyle(Paint.Style.FILL);

        trianglePath.setFillType(Path.FillType.EVEN_ODD);
    }

    public void setSpeed(float value) {
        float MAX_ANGLE_PER_DIRECTION = 90+45- 2*paddingArc;
        if (value >= 1) {
            targetAngle = MAX_ANGLE_PER_DIRECTION * ((value-1)/3);
        } else {
            targetAngle = -MAX_ANGLE_PER_DIRECTION * (1-((value - 0.5f)*2));
        }
        degreePerFrame = Math.abs(targetAngle - angle) / 20;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        paddingArc = getMeasuredHeight()/5;
        paddingIndicator = getMeasuredHeight()/10;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float radiusInnerCircle = getWidth()/8;
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, radiusInnerCircle, indicatorPaint);

        trianglePath.rewind();
        float bigRadius = getHeight() / 2 - paddingIndicator;
        trianglePath.moveTo(getWidth() / 2 + (float)(bigRadius * Math.sin((-angle + 180) * deg2rad)),
                getHeight() / 2 + (float)(bigRadius * Math.cos((-angle + 180) * deg2rad)));
        trianglePath.lineTo(getWidth() / 2 + (float)(radiusInnerCircle * Math.sin((-angle + 180 -90) * deg2rad)),
                getHeight() / 2 + (float)(radiusInnerCircle * Math.cos((-angle + 180-90) * deg2rad)));
        trianglePath.lineTo(getWidth() / 2 + (float)(radiusInnerCircle * Math.sin((-angle + 180+90) * deg2rad)),
                getHeight() / 2 + (float)(radiusInnerCircle * Math.cos((-angle + 180+90) * deg2rad)));
        trianglePath.close();
        canvas.drawPath(trianglePath, indicatorPaint);

        RectF arcBounds = new RectF(paddingArc, paddingArc, getWidth()-paddingArc, getHeight()-paddingArc);
        canvas.drawArc(arcBounds, -180-45, 90+45+angle-paddingAngle, false, arcPaint);
        canvas.drawArc(arcBounds, -90 + paddingAngle+angle, 90+45 - paddingAngle-angle, false, arcPaint);


        if (Math.abs(angle - targetAngle) > 0.5) {
            angle += Math.signum(targetAngle - angle) * Math.min(degreePerFrame, Math.abs(targetAngle - angle));
            invalidate();
        }
    }
}
