package de.danoeh.antennapod.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import de.danoeh.antennapod.core.util.ThemeUtils;

public class ChapterSeekBar extends androidx.appcompat.widget.AppCompatSeekBar {

    private float dividerMargin;
    private float[] dividerPos;
    private final Paint dividerPaint = new Paint();

    public ChapterSeekBar(Context context) {
        super(context);
        init(context);
    }

    public ChapterSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ChapterSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        setBackground(null); // Removes the thumb shadow
        dividerPos = null;
        dividerMargin = context.getResources().getDisplayMetrics().density;
        dividerPaint.setColor(ThemeUtils.getColorFromAttr(getContext(), android.R.attr.windowBackground));
    }

    /**
     * Sets the relative positions of the chapter dividers.
     * @param dividerPos of the chapter dividers relative to the duration of the media.
     */
    public void setDividerPos(final float[] dividerPos) {
        this.dividerPos = dividerPos;
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        drawProgress(canvas);
        if (dividerPos != null) {
            drawDividers(canvas);
        }
        drawThumb(canvas);
    }

    private void drawProgress(Canvas canvas) {
        final int saveCount = canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        getProgressDrawable().draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    private void drawDividers(Canvas canvas) {
        final int saveCount = canvas.save();
        final float width = (float) (getRight() - getPaddingRight() - getLeft() - getPaddingLeft());
        canvas.translate(getPaddingLeft(), getPaddingTop());
        for (float pos : dividerPos) {
            pos *= width;
            canvas.drawRect(pos - dividerMargin, getTop(), pos + dividerMargin, getBottom(), dividerPaint);
        }
        canvas.restoreToCount(saveCount);
    }

    private void drawThumb(Canvas canvas) {
        final int saveCount = canvas.save();
        canvas.translate(getPaddingLeft() - getThumbOffset(), getPaddingTop());
        getThumb().draw(canvas);
        canvas.restoreToCount(saveCount);
    }
}
