package de.danoeh.antennapod.ui.glide;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
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
                0xff00838f, 0xff7b1fa2, 0xffb71c1c, 0xff2196f3, 0xff009688,
                0xffe91e63, 0xff3f51b5, 0xff673ab7, 0xffff5722};
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
            final int w = width > 0 ? width : 500;
            final int h = height > 0 ? height : 500;
            if (model.startsWith(Feed.PREFIX_SEARCH_COVER)) {
                loadSearchCover(w, h, callback);
                return;
            }
            final Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bitmap);
            final Random generator = new Random(model.hashCode());
            final int lineGridSteps = 4 + generator.nextInt(4);
            final int slope = w / 4;
            final float shadowWidth = w * 0.01f;
            final float lineDistance = ((float) w / (lineGridSteps - 2));
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
                            linePos - slope + shadowWidth, h + slope, paintShadow);
                }
                canvas.drawLine(linePos + slope, -slope,
                        linePos - slope, h + slope, paint);
            }

            Paint gradientPaint = new Paint();
            paint.setDither(true);
            gradientPaint.setShader(new LinearGradient(0, 0, 0, h, 0x00000000, 0x55000000, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, gradientPaint);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            callback.onDataReady(is);
        }

        private void loadSearchCover(int w, int h, @NonNull DataCallback<? super InputStream> callback) {
            String searchString = model.substring(Feed.PREFIX_SEARCH_COVER.length());
            final Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bitmap);
            final Random generator = new Random(searchString.hashCode());
            final int baseColor = PALETTES[generator.nextInt(PALETTES.length)];

            canvas.drawColor(baseColor);

            Paint gradientPaint = new Paint();
            gradientPaint.setDither(true);
            gradientPaint.setShader(new LinearGradient(0, 0, w, h, 0x33ffffff, 0x33000000, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, gradientPaint);

            if (!TextUtils.isEmpty(searchString)) {
                TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                textPaint.setColor(0xffffffff);
                textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                textPaint.setShadowLayer(4f, 2f, 2f, 0x55000000);

                float padding = w * 0.12f;
                int maxTextWidth = Math.max(1, (int) (w - 2 * padding));

                float textSize;
                if (searchString.length() <= 4) {
                    textSize = w * 0.20f;
                } else if (searchString.length() <= 10) {
                    textSize = w * 0.15f;
                } else if (searchString.length() <= 20) {
                    textSize = w * 0.11f;
                } else {
                    textSize = w * 0.08f;
                }
                textPaint.setTextSize(textSize);

                StaticLayout layout = StaticLayout.Builder.obtain(searchString, 0, searchString.length(),
                                textPaint, maxTextWidth)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .setIncludePad(false)
                        .setMaxLines(4)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .build();

                while (layout.getHeight() > h * 0.65f && textSize > w * 0.05f) {
                    textSize *= 0.85f;
                    textPaint.setTextSize(textSize);
                    layout = StaticLayout.Builder.obtain(searchString, 0, searchString.length(),
                                    textPaint, maxTextWidth)
                            .setAlignment(Layout.Alignment.ALIGN_CENTER)
                            .setIncludePad(false)
                            .setMaxLines(4)
                            .setEllipsize(TextUtils.TruncateAt.END)
                            .build();
                }

                canvas.save();
                float textX = (w - layout.getWidth()) / 2f;
                float textY = (h - layout.getHeight()) / 2f;
                canvas.translate(textX, textY);
                layout.draw(canvas);
                canvas.restore();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            callback.onDataReady(is);
        }

        private static int randomShadeOfGrey(Random generator) {
            return 0xff777777 + 0x222222 * generator.nextInt(5);
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
