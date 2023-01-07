/*
 * Copyright (C) 2016 Shota Saito
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Source: https://github.com/shts/TriangleLabelView
 * Modified for our need; see AntennaPod #5925 for context
 */

package de.danoeh.antennapod.ui.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class TriangleLabelView extends View {

    private static final int DEGREES_LEFT = -45;
    private static final int DEGREES_RIGHT = 45;
    private final PaintHolder primary = new PaintHolder();
    private float topPadding;
    private float bottomPadding;
    private float centerPadding;
    private Paint trianglePaint;
    private int width;
    private int height;
    private Corner corner;

    public TriangleLabelView(final Context context) {
        this(context, null);
    }

    public TriangleLabelView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TriangleLabelView(final Context context, final AttributeSet attrs,
                             final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public TriangleLabelView(final Context context, final AttributeSet attrs,
                             final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(final Context context, final AttributeSet attrs) {
        final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TriangleLabelView);

        this.topPadding = ta.getDimension(R.styleable.TriangleLabelView_labelTopPadding, dp2px(7));
        this.centerPadding = ta.getDimension(R.styleable.TriangleLabelView_labelCenterPadding, dp2px(3));
        this.bottomPadding = ta.getDimension(R.styleable.TriangleLabelView_labelBottomPadding, dp2px(3));

        final int backgroundColor = ta.getColor(R.styleable.TriangleLabelView_backgroundColor,
                Color.parseColor("#66000000"));
        this.primary.color = ta.getColor(R.styleable.TriangleLabelView_primaryTextColor, Color.WHITE);

        this.primary.size = ta.getDimension(R.styleable.TriangleLabelView_primaryTextSize, sp2px(11));

        final String primary = ta.getString(R.styleable.TriangleLabelView_primaryText);
        if (primary != null) {
            this.primary.text = primary;
        }

        this.corner = Corner.from(ta.getInt(R.styleable.TriangleLabelView_corner, 1));

        ta.recycle();

        this.primary.initPaint();

        trianglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trianglePaint.setColor(backgroundColor);

        this.primary.resetStatus();
    }

    public void setPrimaryText(final String text) {
        primary.text = text;
        primary.resetStatus();
        relayout();
    }

    public Corner getCorner() {
        return corner;
    }

    public void setCorner(final Corner corner) {
        this.corner = corner;
        relayout();
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();

        // translate
        canvas.translate(0, (float) ((height * Math.sqrt(2)) - height));

        // rotate
        if (corner.left()) {
            canvas.rotate(DEGREES_LEFT, 0, height);
        } else {
            canvas.rotate(DEGREES_RIGHT, width, height);
        }

        // draw triangle
        @SuppressLint("DrawAllocation")
        final Path path = new Path();
        path.moveTo(0, height);
        path.lineTo(width / 2f, 0);
        path.lineTo(width, height);
        path.close();
        canvas.drawPath(path, trianglePaint);

        // draw primaryText
        canvas.drawText(primary.text, (width) / 2f,
                (topPadding + centerPadding + primary.height), primary.paint);
        canvas.restore();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        height = (int) (topPadding + centerPadding + bottomPadding + primary.height);
        width = 2 * height;
        final int realHeight = (int) (height * Math.sqrt(2));
        setMeasuredDimension(width, realHeight);
    }

    public int dp2px(final float dpValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public float sp2px(final float spValue) {
        final float scale = getContext().getResources().getDisplayMetrics().scaledDensity;
        return spValue * scale;
    }

    /**
     * Should be called whenever what we're displaying could have changed.
     */
    private void relayout() {
        invalidate();
        requestLayout();
    }

    public enum Corner {
        TOP_LEFT(1),
        TOP_RIGHT(2);
        private final int type;

        Corner(final int type) {
            this.type = type;
        }

        private static Corner from(final int type) {
            for (final Corner c : values()) {
                if (c.type == type) {
                    return c;
                }
            }
            return Corner.TOP_LEFT;
        }

        private boolean left() {
            return this == TOP_LEFT;
        }
    }

    private static class PaintHolder {
        String text = "";
        Paint paint;
        int color;
        float size;
        float height;
        float width;

        void initPaint() {
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(color);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(size);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        void resetStatus() {
            final Rect rectText = new Rect();
            paint.getTextBounds(text, 0, text.length(), rectText);
            width = rectText.width();
            height = rectText.height();
        }
    }
}
