package de.danoeh.antennapod.ui.echo;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

public class EchoProgress extends Drawable {
    private final Paint paint;
    private final int numScreens;
    private float progress = 0;

    public EchoProgress(int numScreens) {
        this.numScreens = numScreens;
        paint = new Paint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(0xffffffff);
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        paint.setStrokeWidth(0.5f * getBounds().height());

        float y = 0.5f * getBounds().height();
        float sectionWidth = 1.0f * getBounds().width() / numScreens;
        float sectionPadding = 0.03f * sectionWidth;

        for (int i = 0; i < numScreens; i++) {
            if (i + 1 < progress) {
                paint.setAlpha(255);
            } else {
                paint.setAlpha(100);
            }
            canvas.drawLine(i * sectionWidth + sectionPadding, y, (i + 1) * sectionWidth - sectionPadding, y, paint);
            if (Math.floor(1.0 * i) == Math.floor(progress)) {
                paint.setAlpha(255);
                canvas.drawLine(i * sectionWidth + sectionPadding, y, i * sectionWidth + sectionPadding
                        + (progress - i) * (sectionWidth - 2 * sectionPadding), y, paint);
            }
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }
}
