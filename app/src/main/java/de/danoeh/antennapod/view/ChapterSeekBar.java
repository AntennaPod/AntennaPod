package de.danoeh.antennapod.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import de.danoeh.antennapod.ui.common.ThemeUtils;

public class ChapterSeekBar extends androidx.appcompat.widget.AppCompatSeekBar {

    private float top;
    private float width;
    private float bottom;
    private float density;
    private float progressPrimary;
    private float progressSecondary;
    private float[] dividerPos;
    private final Paint paintBackground = new Paint();
    private final Paint paintProgressPrimary = new Paint();
    private final Paint paintProgressSecondary = new Paint();

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
        density = context.getResources().getDisplayMetrics().density;
        paintBackground.setColor(ThemeUtils.getColorFromAttr(getContext(),
                de.danoeh.antennapod.core.R.attr.currently_playing_background));
        paintBackground.setAlpha(128);
        paintProgressPrimary.setColor(ThemeUtils.getColorFromAttr(getContext(),
                de.danoeh.antennapod.core.R.attr.colorPrimary));
        paintProgressSecondary.setColor(ThemeUtils.getColorFromAttr(getContext(),
                de.danoeh.antennapod.core.R.attr.seek_background));
    }

    /**
     * Sets the relative positions of the chapter dividers.
     * @param dividerPos of the chapter dividers relative to the duration of the media.
     */
    public void setDividerPos(final float[] dividerPos) {
        if (dividerPos != null) {
            this.dividerPos = new float[dividerPos.length + 2];
            this.dividerPos[0] = 0;
            System.arraycopy(dividerPos, 0, this.dividerPos, 1, dividerPos.length);
            this.dividerPos[this.dividerPos.length - 1] = 1;
        } else {
            this.dividerPos = null;
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        top = getTop() + density * 7.5f;
        bottom = getBottom() - density * 7.5f;
        width = (float) (getRight() - getPaddingRight() - getLeft() - getPaddingLeft());
        progressSecondary = getSecondaryProgress() / (float) getMax() * width;
        progressPrimary = getProgress() / (float) getMax() * width;

        if (dividerPos == null) {
            drawProgress(canvas);
        } else {
            drawProgressChapters(canvas);
        }
        drawThumb(canvas);
    }

    private void drawProgress(Canvas canvas) {
        final int saveCount = canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        canvas.drawRect(0, top, width, bottom, paintBackground);
        canvas.drawRect(0, top, progressSecondary, bottom, paintProgressSecondary);
        canvas.drawRect(0, top, progressPrimary, bottom, paintProgressPrimary);
        canvas.restoreToCount(saveCount);
    }

    private void drawProgressChapters(Canvas canvas) {
        final int saveCount = canvas.save();
        int currChapter = 1;
        float chapterMargin = density * 0.6f;
        float topExpanded = getTop() + density * 7;
        float bottomExpanded = getBottom() - density * 7;

        canvas.translate(getPaddingLeft(), getPaddingTop());

        for (int i = 1; i < dividerPos.length; i++) {
            float right = dividerPos[i] * width - chapterMargin;
            float left = dividerPos[i - 1] * width + chapterMargin;
            float rightCurr = dividerPos[currChapter] * width - chapterMargin;
            float leftCurr = dividerPos[currChapter - 1] * width + chapterMargin;

            canvas.drawRect(left, top, right, bottom, paintBackground);

            if (right < progressPrimary) {
                currChapter = i + 1;
                canvas.drawRect(left, top, right, bottom, paintProgressPrimary);
            } else if (isPressed()) {
                canvas.drawRect(leftCurr, topExpanded, rightCurr, bottomExpanded, paintBackground);
                canvas.drawRect(leftCurr, topExpanded, progressPrimary, bottomExpanded, paintProgressPrimary);
            } else {
                if (progressSecondary > leftCurr) {
                    canvas.drawRect(leftCurr, top, progressSecondary, bottom, paintProgressSecondary);
                }
                canvas.drawRect(leftCurr, top, progressPrimary, bottom, paintProgressPrimary);
            }
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
