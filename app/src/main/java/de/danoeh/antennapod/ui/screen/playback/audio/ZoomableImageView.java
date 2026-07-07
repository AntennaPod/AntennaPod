package de.danoeh.antennapod.ui.screen.playback.audio;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * An ImageView that supports pinch-to-zoom and panning on top of a fit-center base scale.
 *
 * <p>The user zoom and pan are stored in a supplementary matrix that is kept separate from the
 * fit-center base matrix. This way the zoom survives drawable changes (e.g. when the chapter image
 * changes while the fullscreen view stays open) and only resets to the default fit when the view is
 * recreated.</p>
 */
public class ZoomableImageView extends AppCompatImageView {
    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 5f;

    private final Matrix baseMatrix = new Matrix();
    private final Matrix suppMatrix = new Matrix();
    private final Matrix drawMatrix = new Matrix();
    private final RectF displayRect = new RectF();
    private final float[] matrixValues = new float[9];
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    public ZoomableImageView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        super.setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        updateBaseMatrix();
        applyMatrix();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        updateBaseMatrix();
        applyMatrix();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void updateBaseMatrix() {
        Drawable drawable = getDrawable();
        if (drawable == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();
        if (drawableWidth <= 0 || drawableHeight <= 0) {
            return;
        }
        float scale = Math.min((float) getWidth() / drawableWidth, (float) getHeight() / drawableHeight);
        float translateX = (getWidth() - drawableWidth * scale) / 2f;
        float translateY = (getHeight() - drawableHeight * scale) / 2f;
        baseMatrix.reset();
        baseMatrix.postScale(scale, scale);
        baseMatrix.postTranslate(translateX, translateY);
    }

    private float getSuppScale() {
        suppMatrix.getValues(matrixValues);
        return matrixValues[Matrix.MSCALE_X];
    }

    private void applyMatrix() {
        checkBounds();
        drawMatrix.set(baseMatrix);
        drawMatrix.postConcat(suppMatrix);
        setImageMatrix(drawMatrix);
    }

    private void checkBounds() {
        Drawable drawable = getDrawable();
        if (drawable == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        drawMatrix.set(baseMatrix);
        drawMatrix.postConcat(suppMatrix);
        displayRect.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawMatrix.mapRect(displayRect);

        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float deltaX = 0;
        float deltaY = 0;

        if (displayRect.width() <= viewWidth) {
            deltaX = (viewWidth - displayRect.width()) / 2f - displayRect.left;
        } else if (displayRect.left > 0) {
            deltaX = -displayRect.left;
        } else if (displayRect.right < viewWidth) {
            deltaX = viewWidth - displayRect.right;
        }

        if (displayRect.height() <= viewHeight) {
            deltaY = (viewHeight - displayRect.height()) / 2f - displayRect.top;
        } else if (displayRect.top > 0) {
            deltaY = -displayRect.top;
        } else if (displayRect.bottom < viewHeight) {
            deltaY = viewHeight - displayRect.bottom;
        }

        suppMatrix.postTranslate(deltaX, deltaY);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float currentScale = getSuppScale();
            float targetScale = currentScale * scaleFactor;
            if (targetScale < MIN_SCALE) {
                scaleFactor = MIN_SCALE / currentScale;
            } else if (targetScale > MAX_SCALE) {
                scaleFactor = MAX_SCALE / currentScale;
            }
            suppMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            applyMatrix();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
            performClick();
            return true;
        }

        @Override
        public boolean onScroll(@Nullable MotionEvent firstEvent, @NonNull MotionEvent secondEvent,
                                float distanceX, float distanceY) {
            if (scaleDetector.isInProgress() || getSuppScale() <= MIN_SCALE) {
                return false;
            }
            suppMatrix.postTranslate(-distanceX, -distanceY);
            applyMatrix();
            return true;
        }
    }
}
