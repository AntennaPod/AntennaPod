package de.danoeh.antennapod.core.glide;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
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
        private static final int[] PALETTE = {0xff42a5f5, 0xff1e88e5, 0xff1565c0, 0xff0d47a1, 0xff283593};
        private static final int LINE_GRID_STEPS = 6;
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
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            float lineDistance = ((float) width / (LINE_GRID_STEPS - 2));

            Random generator = new Random(model.hashCode());
            Paint paint = new Paint();
            int color = PALETTE[generator.nextInt(PALETTE.length)];
            paint.setColor(color);
            paint.setStrokeWidth(lineDistance);
            Paint paintShadow = new Paint();
            paintShadow.setColor(0xff000000);
            paintShadow.setStrokeWidth(lineDistance);

            int forcedLine = generator.nextInt(LINE_GRID_STEPS); // Needs at least one line
            for (int i = LINE_GRID_STEPS - 1; i >= 0; i--) {
                float linePos = (i - 0.5f) * lineDistance;
                boolean switchColor = generator.nextBoolean() || i == forcedLine;
                if (switchColor) {
                    int newColor = color;
                    while (newColor == color) {
                        newColor = PALETTE[generator.nextInt(PALETTE.length)];
                    }
                    color = newColor;
                    paint.setColor(newColor);
                    float shadowWidth = lineDistance * 0.05f;
                    canvas.drawLine(linePos + lineDistance + shadowWidth, -lineDistance,
                            linePos - lineDistance + shadowWidth, height + lineDistance, paintShadow);
                }
                canvas.drawLine(linePos + lineDistance, -lineDistance,
                        linePos - lineDistance, height + lineDistance, paint);
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
