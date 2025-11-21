package de.danoeh.antennapod.ui.glide;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Random;

import de.danoeh.antennapod.ui.common.ErrorImageModel;

public final class ErrorImageModelLoader implements ModelLoader<ErrorImageModel, InputStream> {

    public static class Factory implements ModelLoaderFactory<ErrorImageModel, InputStream> {
        @NonNull
        @Override
        public ModelLoader<ErrorImageModel, InputStream> build(@NonNull MultiModelLoaderFactory unused) {
            return new ErrorImageModelLoader();
        }

        @Override
        public void teardown() {
        }
    }

    @Override
    public LoadData<InputStream> buildLoadData(
            @NonNull ErrorImageModel model,
            int width,
            int height,
            @NonNull Options options) {
        return new LoadData<>(new ObjectKey(model.toString()),
                new ErrorImageFetcher(model, width, height));
    }

    @Override
    public boolean handles(@NonNull ErrorImageModel model) {
        return true;
    }

    static class ErrorImageFetcher implements DataFetcher<InputStream> {
        private static final int[] PALETTES = {0xff78909c, 0xffff6f00, 0xff388e3c,
                0xff00838f, 0xff7b1fa2, 0xffb71c1c, 0xff2196f3};
        
        private final ErrorImageModel model;
        private final int width;
        private final int height;

        public ErrorImageFetcher(ErrorImageModel model, int width, int height) {
            this.model = model;
            this.width = width;
            this.height = height;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bitmap);
            
            // Simple red background
            canvas.drawColor(0xFFDC3545); // Bootstrap danger red color

            // Add text
            String text = model.getFallbackText();
            if (!text.isEmpty()) {
                drawWrappedText(canvas, text, width, height, width * 0.9f);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            callback.onDataReady(is);
        }

        private static int randomShadeOfGrey(Random generator) {
            return 0xff777777 + 0x222222 * generator.nextInt(5);
        }

        private void drawWrappedText(Canvas canvas, String text, int width, int height, float maxWidth) {
            int textSize = (int) (height / 4.5f);
            Paint paint = new Paint();
            paint.setColor(0xFFFFFFFF);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setAntiAlias(true);
            paint.setSubpixelText(false);
            paint.setTextSize(textSize);
            // No shadow for error images

            TextPaint textPaint = new TextPaint(paint);

            StaticLayout layout;
            do {
                layout = new StaticLayout(text, textPaint, (int) maxWidth,
                        Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                text = text.substring(0, text.length() - 2);
            } while (layout.getHeight() > height);

            canvas.save();
            canvas.translate((width) / 2f, (height - layout.getHeight()) / 2f);
            layout.draw(canvas);
            canvas.restore();
        }

        @Override
        public void cleanup() {

        }

        @Override
        public void cancel() {

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
