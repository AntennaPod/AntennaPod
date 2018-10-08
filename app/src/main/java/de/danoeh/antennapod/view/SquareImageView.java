package de.danoeh.antennapod.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * From http://stackoverflow.com/a/19449488/6839
 */
public class SquareImageView extends AppCompatImageView {

    public SquareImageView(@NonNull Context context) {
        super(context);
    }

    public SquareImageView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageView(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        setMeasuredDimension(width, width);
    }

}