package de.danoeh.antennapod.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * From http://stackoverflow.com/a/19449488/6839
 */
public class SquareImageView extends AppCompatImageView {
    public static final int DIRECTION_WIDTH = 0;
    public static final int DIRECTION_HEIGHT = 1;
    public static final int DIRECTION_MINIMUM = 2;

    private int direction = DIRECTION_WIDTH;

    public SquareImageView(Context context) {
        super(context);
    }

    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadAttrs(context, attrs);
    }

    public SquareImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        loadAttrs(context, attrs);
    }

    private void loadAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SquareImageView);
        direction = a.getInt(R.styleable.SquareImageView_direction, DIRECTION_WIDTH);
        a.recycle();
    }

    public void setDirection(int direction) {
        this.direction = direction;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        switch (direction) {
            case DIRECTION_MINIMUM:
                int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
                setMeasuredDimension(size, size);
                break;
            case DIRECTION_HEIGHT:
                setMeasuredDimension(getMeasuredHeight(), getMeasuredHeight());
                break;
            default:
                setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
                break;
        }
    }

}