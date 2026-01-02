package de.danoeh.antennapod.ui.glide;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

public class TextRenderingUtils {
    
    public static void drawWrappedText(
            Canvas canvas,
            String text,
            int canvasWidth,
            int canvasHeight,
            float maxWidth,
            int textColor) {
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(textColor);
        textPaint.setTextSize(Math.min(canvasWidth, canvasHeight) / 10f);
        textPaint.setTextAlign(Paint.Align.LEFT);

        StaticLayout layout = new StaticLayout(text, textPaint, (int) maxWidth,
                Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);

        float x = (canvasWidth - maxWidth) / 2;
        float y = (float) (canvasHeight - layout.getHeight()) / 2;

        canvas.save();
        canvas.translate(x, y);
        layout.draw(canvas);
        canvas.restore();
    }
    
    public static void drawWrappedTextWithTruncation(
            Canvas canvas,
            String text,
            int canvasWidth,
            int canvasHeight,
            float maxWidth,
            int textColor) {
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(textColor);
        textPaint.setTextSize(canvasHeight / 6f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        StaticLayout layout;
        String truncatedText = text;
        do {
            layout = new StaticLayout(truncatedText, textPaint, (int) maxWidth,
                    Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            if (layout.getHeight() > canvasHeight && truncatedText.length() > 2) {
                truncatedText = truncatedText.substring(0, truncatedText.length() - 2);
            } else {
                break;
            }
        } while (true);

        canvas.save();
        canvas.translate(canvasWidth / 2f, (canvasHeight - layout.getHeight()) / 2f);
        layout.draw(canvas);
        canvas.restore();
    }
}
