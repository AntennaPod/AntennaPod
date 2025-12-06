package de.danoeh.antennapod.ui.glide;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Shader;
import android.net.Uri;

import androidx.annotation.NonNull;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import de.danoeh.antennapod.model.feed.Feed;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class GenerativePlaceholderImageModelLoader implements ModelLoader<String, InputStream> {

    public static class Factory implements ModelLoaderFactory<String, InputStream> {
        @NonNull
        @Override
        public ModelLoader<String, InputStream> build(@NonNull MultiModelLoaderFactory unused) {
            return new GenerativePlaceholderImageModelLoader();
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

    @Override
    public LoadData<InputStream> buildLoadData(@NonNull String model, int width, int height, @NonNull Options options) {
        return new LoadData<>(new ObjectKey(model), new EmbeddedImageFetcher(model, width, height));
    }

    @Override
    public boolean handles(@NonNull String model) {
        return model.startsWith(Feed.PREFIX_GENERATIVE_COVER);
    }

    static class EmbeddedImageFetcher implements DataFetcher<InputStream> {
        private static final int[] PALETTES = {0xff78909c, 0xffff6f00, 0xff388e3c,
                0xff00838f, 0xff7b1fa2, 0xffb71c1c, 0xff2196f3};
        private final String model;
        private final int width;
        private final int height;

        public EmbeddedImageFetcher(String model, int width, int height) {
            this.model = model;
            this.width = width;
            this.height = height;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bitmap);
            final Random generator = new Random(model.hashCode());
            final int lineGridSteps = 4 + generator.nextInt(4);
            final int slope = width / 4;
            final float shadowWidth = width * 0.01f;
            final float lineDistance = ((float) width / (lineGridSteps - 2));
            final int baseColor = PALETTES[generator.nextInt(PALETTES.length)];

            Paint paint = new Paint();
            int color = randomShadeOfGrey(generator);
            paint.setColor(color);
            paint.setStrokeWidth(lineDistance);
            paint.setColorFilter(new PorterDuffColorFilter(baseColor, PorterDuff.Mode.MULTIPLY));
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

            // Add podcast name text
            String podcastName = extractPodcastName(model);
            int textSize = height / 7;
            if (!podcastName.isEmpty()) {
                Paint textPaint = new Paint();
                textPaint.setColor(0xFFFFFFFF);
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setAntiAlias(true);
                textPaint.setSubpixelText(false);
                textPaint.setTextSize(textSize);
                textPaint.setShadowLayer(4, 2, 2, 0x80000000);

                drawWrappedText(
                        canvas,
                        podcastName,
                        Math.round(width / 2f), Math.round(height / 2f),
                        width * 0.9f,
                        textPaint);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            callback.onDataReady(is);
        }

        private static int randomShadeOfGrey(Random generator) {
            return 0xff777777 + 0x222222 * generator.nextInt(5);
        }

        private String extractPodcastName(String model) {
            // Check if title is encoded in URL fragment
            if (model.contains(Feed.SUFFIX_GENERATIVE_COVER_TITLE)) {
                try {
                    String encodedTitle = model.substring(model.indexOf(Feed.SUFFIX_GENERATIVE_COVER_TITLE) + 7);
                    return java.net.URLDecoder.decode(encodedTitle, "UTF-8").trim();
                } catch (Exception e) {
                    // Fall back to folder name extraction
                }
            }
            try {
                Uri uri = Uri.parse(model.replace(Feed.PREFIX_GENERATIVE_COVER, ""));
                List<String> parameterValues = new ArrayList<>();
                for (String parameterName : uri.getQueryParameterNames()) {
                    parameterValues.add(uri.getQueryParameter(parameterName));
                }
                return String.join(" ", parameterValues);
            } catch (Exception e) {
                return "";
            }
        }

        private void drawWrappedText(Canvas canvas, String text, float x, float y, float maxWidth, Paint paint) {
            java.util.List<String> lines = new java.util.ArrayList<>();
            String remaining = text;
            
            for (int lineCount = 0; lineCount < 3 && !remaining.isEmpty(); lineCount++) {
                String line = "";
                int i = 0;
                
                while (i < remaining.length()) {
                    String testChar = remaining.substring(i, i + 1);
                    String testLine = line + testChar;
                    
                    if (paint.measureText(testLine) <= maxWidth) {
                        line = testLine;
                        i++;
                    } else {
                        break;
                    }
                }
                
                // Smart wrap: if only 1-2 chars left before EOL, start next line
                if (i < remaining.length() && i >= 2) {
                    int lastSpace = line.lastIndexOf(' ');
                    if (lastSpace > 0 && line.length() - lastSpace <= 3) {
                        line = line.substring(0, lastSpace);
                        i = lastSpace + 1;
                    }
                }
                
                lines.add(line.trim());
                remaining = i < remaining.length() ? remaining.substring(i).trim() : "";
            }
            
            // Add ellipsis if text was cut off
            if (!remaining.isEmpty() && !lines.isEmpty()) {
                String lastLine = lines.get(lines.size() - 1);
                if (paint.measureText(lastLine + "...") <= maxWidth) {
                    lines.set(lines.size() - 1, lastLine + "...");
                } else {
                    while (!lastLine.isEmpty() && paint.measureText(lastLine + "...") > maxWidth) {
                        lastLine = lastLine.substring(0, lastLine.length() - 1);
                    }
                    lines.set(lines.size() - 1, lastLine + "...");
                }
            }
            
            float lineHeight = paint.getTextSize() * 1.2f;
            float startY = y - (lines.size() - 1) * lineHeight / 2f;
            
            for (int i = 0; i < lines.size(); i++) {
                canvas.drawText(lines.get(i), x, startY + i * lineHeight, paint);
            }
        }

        @Override
        public void cleanup() {
            // nothing to clean up
        }

        @Override
        public void cancel() {
            // cannot cancel
        }

        @NonNull
        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }
    }
}
