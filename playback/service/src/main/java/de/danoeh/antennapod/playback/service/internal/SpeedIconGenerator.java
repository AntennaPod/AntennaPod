package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class SpeedIconGenerator {
    private static float cachedSpeed = -1;
    private static Uri cachedUri = null;

    static Uri generateSpeedIcon(Context context, float speed) {
        if (cachedUri != null && Float.compare(cachedSpeed, speed) == 0) {
            return cachedUri;
        }

        int size = 128;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        String text = formatSpeed(speed);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(size * 0.4f);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        if (bounds.width() > size * 0.9f) {
            paint.setTextSize(paint.getTextSize() * size * 0.9f / bounds.width());
            paint.getTextBounds(text, 0, text.length(), bounds);
        }

        float x = size / 2f;
        float y = size / 2f + bounds.height() / 2f;
        canvas.drawText(text, x, y, paint);

        File file = new File(context.getCacheDir(), "speed_icon.png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            return Uri.EMPTY;
        } finally {
            bitmap.recycle();
        }
        cachedSpeed = speed;
        cachedUri = Uri.fromFile(file);
        return cachedUri;
    }

    private static String formatSpeed(float speed) {
        int hundredths = Math.round(speed * 100);
        if (hundredths % 100 == 0) {
            return (hundredths / 100) + "x";
        } else if (hundredths % 10 == 0) {
            return String.format("%.1fx", speed);
        } else {
            return String.format("%.2fx", speed);
        }
    }
}
