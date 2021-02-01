package de.danoeh.antennapod.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import de.danoeh.antennapod.core.util.ThemeUtils;

public class ChapterSeekBar extends androidx.appcompat.widget.AppCompatSeekBar {

    private float[] dividerPos;
    private final Paint dividerPaint = new Paint();

    public ChapterSeekBar(Context context) {
        super(context);
        init();
    }

    public ChapterSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChapterSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setBackground(null); // Removes the thumb shadow
        dividerPos = null;
        dividerPaint.setColor(ThemeUtils.getColorFromAttr(getContext(), android.R.attr.windowBackground));
    }

    /**
     * Calculates the positions of the chapter dividers in the progress bar.
     * @param dividerPos of the chapter dividers relative to the duration of the media.
     */
    public void setDividerPos(final float[] dividerPos) {
        if (dividerPos != null && dividerPos.length > 0) {
            float width = (float) (getRight() - getPaddingRight() - getLeft() - getPaddingLeft());
            this.dividerPos = new float[dividerPos.length];

            for (int i = 0; i < dividerPos.length; i++) {
                this.dividerPos[i] = dividerPos[i] * width;
            }

        } else {
            this.dividerPos = null;
        }
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
        canvas.translate(getPaddingLeft(), getPaddingTop());
        for (float pos : dividerPos) {
            canvas.drawRect(pos - 5, getTop(), pos + 5, getBottom(), dividerPaint);
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
