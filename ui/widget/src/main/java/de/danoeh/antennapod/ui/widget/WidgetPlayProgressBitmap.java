package de.danoeh.antennapod.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public final class WidgetPlayProgressBitmap {
    static final float PREVIEW_PROGRESS = 0.35f;
    private static final int COLOR_REMAINING = 0xFF888888;

    private WidgetPlayProgressBitmap() {
    }

    public static Bitmap createPlaySizeRing(Context context, float progress) {
        float strokeWidth = context.getResources().getDimension(R.dimen.widget_play_progress_stroke);
        int size = context.getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
        return create(size, strokeWidth, progress);
    }

    public static Bitmap createExtendedPlayRing(Context context, float progress) {
        float strokeWidth = context.getResources().getDimension(R.dimen.widget_play_progress_stroke);
        int size = context.getResources().getDimensionPixelSize(R.dimen.widget_play_extended_ring_size);
        return create(size, strokeWidth, progress);
    }

    public static Bitmap create(int sizePx, float strokeWidthPx, float progress) {
        Paint remainingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        remainingPaint.setStyle(Paint.Style.STROKE);
        remainingPaint.setStrokeWidth(strokeWidthPx);
        remainingPaint.setColor(COLOR_REMAINING);

        Paint playedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        playedPaint.setStyle(Paint.Style.STROKE);
        playedPaint.setStrokeWidth(strokeWidthPx);
        playedPaint.setColor(Color.WHITE);

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float halfStroke = strokeWidthPx / 2f;
        RectF rect = new RectF(halfStroke, halfStroke, sizePx - halfStroke, sizePx - halfStroke);

        canvas.drawArc(rect, -90, 360, false, remainingPaint);
        float clampedProgress = Math.max(0f, Math.min(1f, progress));
        if (clampedProgress > 0) {
            canvas.drawArc(rect, -90, 360 * clampedProgress, false, playedPaint);
        }
        return bitmap;
    }
}
