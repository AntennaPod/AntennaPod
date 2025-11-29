package de.danoeh.antennapod.ui.screen.playback.video;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Custom SeekBar that prevents accidental seeking during system gestures.
 * Ignores touches from the system gesture zone and vertical swipes.
 */
public class GestureAwareSeekBar extends AppCompatSeekBar {
    private static final String TAG = "GestureAwareSeekBar";
    private static final float HORIZONTAL_DRAG_THRESHOLD = 1.5f;
    private static final int FALLBACK_GESTURE_ZONE_DP = 48;
    
    private float initialTouchX;
    private float initialTouchY;
    private int touchSlop;
    private int systemGestureBottom;
    private boolean isHorizontalDrag = false;
    private boolean isDragging = false;
    private boolean touchStartedInGestureZone = false;
    private boolean hasDeterminedDirection = false;
    
    public GestureAwareSeekBar(Context context) {
        super(context);
        init(context);
    }
    
    public GestureAwareSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public GestureAwareSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
        
        float density = context.getResources().getDisplayMetrics().density;
        systemGestureBottom = (int) (FALLBACK_GESTURE_ZONE_DP * density);
        
        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            updateSystemGestureInsets(insets);
            return insets;
        });
    }
    
    private void updateSystemGestureInsets(WindowInsetsCompat insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Insets gestureInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
            systemGestureBottom = gestureInsets.bottom;
            Log.d(TAG, "System gesture bottom inset: " + systemGestureBottom + "px");
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return handleActionDown(event);
                
            case MotionEvent.ACTION_MOVE:
                return handleActionMove(event);
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return handleActionUpOrCancel(event);
                
            default:
                return super.onTouchEvent(event);
        }
    }
    
    private boolean handleActionDown(MotionEvent event) {
        initialTouchX = event.getX();
        initialTouchY = event.getY();
        isDragging = false;
        isHorizontalDrag = false;
        hasDeterminedDirection = false;
        
        int[] location = new int[2];
        getLocationOnScreen(location);
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int touchYOnScreen = location[1] + (int) event.getY();
        
        touchStartedInGestureZone = (screenHeight - touchYOnScreen) < systemGestureBottom;
        
        if (touchStartedInGestureZone) {
            Log.d(TAG, "Touch started in system gesture zone");
        }
        
        return super.onTouchEvent(event);
    }
    
    private boolean handleActionMove(MotionEvent event) {
        float deltaX = Math.abs(event.getX() - initialTouchX);
        float deltaY = Math.abs(event.getY() - initialTouchY);
        
        if (!hasDeterminedDirection && (deltaX < touchSlop && deltaY < touchSlop)) {
            return super.onTouchEvent(event);
        }
        
        if (!hasDeterminedDirection) {
            hasDeterminedDirection = true;
            isHorizontalDrag = (deltaX > touchSlop) && (deltaX > deltaY * HORIZONTAL_DRAG_THRESHOLD);
            Log.d(TAG, String.format("Direction: horizontal=%b, deltaX=%.1f, deltaY=%.1f", 
                    isHorizontalDrag, deltaX, deltaY));
        }
        
        if (touchStartedInGestureZone && !isHorizontalDrag) {
            Log.d(TAG, "Ignoring swipe from gesture zone");
            return false;
        }
        
        if (isHorizontalDrag) {
            isDragging = true;
            return super.onTouchEvent(event);
        }
        
        if (deltaY > deltaX) {
            return false;
        }
        
        return super.onTouchEvent(event);
    }
    
    private boolean handleActionUpOrCancel(MotionEvent event) {
        float deltaX = Math.abs(event.getX() - initialTouchX);
        float deltaY = Math.abs(event.getY() - initialTouchY);
        boolean isTap = (deltaX < touchSlop && deltaY < touchSlop);
        
        if (isTap) {
            boolean result = super.onTouchEvent(event);
            resetState();
            return result;
        }
        
        if (touchStartedInGestureZone && !isHorizontalDrag) {
            resetState();
            return false;
        }
        
        if (isHorizontalDrag && isDragging) {
            boolean result = super.onTouchEvent(event);
            resetState();
            return result;
        }
        
        resetState();
        return super.onTouchEvent(event);
    }
    
    private void resetState() {
        isDragging = false;
        isHorizontalDrag = false;
        touchStartedInGestureZone = false;
        hasDeterminedDirection = false;
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestApplyInsets();
    }
}
