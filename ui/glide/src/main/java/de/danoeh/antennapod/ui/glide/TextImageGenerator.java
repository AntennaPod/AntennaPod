package de.danoeh.antennapod.ui.glide;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Shader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Random;

import de.danoeh.antennapod.model.feed.Feed;

class TextImageGenerator {

    private static final int[] PALETTES = {0xff78909c, 0xffff6f00, 0xff388e3c,
            0xff00838f, 0xff7b1fa2, 0xffb71c1c, 0xff2196f3};

    static InputStream generatePlaceholderImage(
            String model,
            String text,
            String feedDownloadUrl,
            int width,
            int height,
            boolean onlyGray,
            boolean showText) {
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        final Random generator = new Random((Feed.PREFIX_GENERATIVE_COVER + feedDownloadUrl).hashCode());
        final int lineGridSteps = 4 + generator.nextInt(4);
        final int slope = width / 4;
        final float shadowWidth = width * 0.01f;
        final float lineDistance = ((float) width / (lineGridSteps - 2));
        final int baseColor = PALETTES[generator.nextInt(PALETTES.length)];

        Paint paint = new Paint();
        int color = randomShadeOfGrey(generator);
        if (!onlyGray) {
            paint.setColor(color);
        }
        paint.setStrokeWidth(lineDistance);
        if (!onlyGray) {
            paint.setColorFilter(new PorterDuffColorFilter(baseColor, PorterDuff.Mode.MULTIPLY));
        }
        Paint paintShadow = new Paint();
        paintShadow.setColor(0xff000000);
        paintShadow.setStrokeWidth(lineDistance);

        int forcedColorChange = 1 + generator.nextInt(lineGridSteps - 2);
        for (int i = lineGridSteps - 1; i >= 0; i--) {
            float linePos = (i - 0.5f) * lineDistance;
            boolean switchColor = generator.nextFloat() < 0.3f || i == forcedColorChange;
            if (switchColor) {
                int newColor = color;
                while (newColor == color) {
                    newColor = randomShadeOfGrey(generator);
                }
                color = newColor;
                paint.setColor(newColor);
                canvas.drawLine(linePos + slope + shadowWidth, -slope,
                        linePos - slope + shadowWidth, height + slope, paintShadow);
            }
            canvas.drawLine(linePos + slope, -slope,
                    linePos - slope, height + slope, paint);
        }

        Paint gradientPaint = new Paint();
        paint.setDither(true);
        gradientPaint.setShader(new LinearGradient(0, 0, 0, height, 0x00000000, 0x55000000, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, gradientPaint);

        // Add text
        if (showText && !text.isEmpty()) {
            TextRenderingUtils.drawWrappedTextWithTruncation(canvas, text, width, height, width * 0.9f, 0xFFFFFFFF);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    private static int randomShadeOfGrey(Random generator) {
        return 0xff777777 + 0x222222 * generator.nextInt(5);
    }
}
