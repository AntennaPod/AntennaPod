package de.danoeh.antennapod.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.common.ThemeUtils;

public class ChapterSeekBar extends androidx.appcompat.widget.AppCompatSeekBar {

    private float top;
    private float width;
    private float center;
    private float bottom;
    private float density;
    private float progressPrimary;
    private float progressSecondary;
    private float[] dividerPos;
    private boolean isHighlighted = false;
    private final Paint paintBackground = new Paint();
    private final Paint paintProgressPrimary = new Paint();

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

        paintBackground.setColor(ThemeUtils.getColorFromAttr(getContext(), R.attr.colorSurfaceVariant));
        paintBackground.setAlpha(128);
        paintProgressPrimary.setColor(ThemeUtils.getColorFromAttr(getContext(), R.attr.colorPrimary));
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
        invalidate();
    }

    public void highlightCurrentChapter() {
        isHighlighted = true;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                isHighlighted = false;
                invalidate();
            }
        }, 1000);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        center = (getBottom() - getPaddingBottom() - getTop() - getPaddingTop()) / 2.0f;
        top = center - density * 1.5f;
        bottom = center + density * 1.5f;
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
        canvas.drawRect(0, top, progressSecondary, bottom, paintBackground);
        canvas.drawRect(0, top, progressPrimary, bottom, paintProgressPrimary);
        canvas.restoreToCount(saveCount);
    }

    private void drawProgressChapters(Canvas canvas) {
        final int saveCount = canvas.save();
        int currChapter = 1;
        float chapterMargin = density * 1.2f;
        float topExpanded = center - density * 2.0f;
        float bottomExpanded = center + density * 2.0f;

        canvas.translate(getPaddingLeft(), getPaddingTop());

        for (int i = 1; i < dividerPos.length; i++) {
            float right = dividerPos[i] * width - chapterMargin;
            float left = dividerPos[i - 1] * width;
            float rightCurr = dividerPos[currChapter] * width - chapterMargin;
            float leftCurr = dividerPos[currChapter - 1] * width;

            canvas.drawRect(left, top, right, bottom, paintBackground);

            if (progressSecondary > 0 && progressSecondary < width) {
                if (right < progressSecondary) {
                    canvas.drawRect(left, top, right, bottom, paintBackground);
                } else if (progressSecondary > left) {
                    canvas.drawRect(left, top, progressSecondary, bottom, paintBackground);
                }
            }

            if (right < progressPrimary) {
                currChapter = i + 1;
                canvas.drawRect(left, top, right, bottom, paintProgressPrimary);
            } else if (isHighlighted || isPressed()) {
                canvas.drawRect(leftCurr, topExpanded, rightCurr, bottomExpanded, paintBackground);
                canvas.drawRect(leftCurr, topExpanded, progressPrimary, bottomExpanded, paintProgressPrimary);
            } else {
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
