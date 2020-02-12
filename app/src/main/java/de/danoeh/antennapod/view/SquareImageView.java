package de.danoeh.antennapod.view;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;
import de.danoeh.antennapod.core.R;

/**
 * From http://stackoverflow.com/a/19449488/6839
 */
public class SquareImageView extends AppCompatImageView {
    private boolean useMinimum = false;

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
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                new int[]{R.styleable.SquareImageView_useMinimum}, 0, 0);
        useMinimum = a.getBoolean(0, false);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int size = getMeasuredWidth();
        if (useMinimum) {
            size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        }
        setMeasuredDimension(size, size);
    }

}