package de.danoeh.antennapod.core.glide;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Shader;
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

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
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
