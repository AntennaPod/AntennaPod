package de.danoeh.antennapod.ui.glide;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Shader;
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

import de.danoeh.antennapod.ui.common.ImageModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import android.content.Context;
import android.net.Uri;

public final class ImageModelLoader implements ModelLoader<ImageModel, InputStream> {
    public static final String TAG = "ImageModelLoader";

    public static class Factory implements ModelLoaderFactory<ImageModel, InputStream> {
        private final Context context;
        
        public Factory(Context context) {
            this.context = context;
        }
        
        @NonNull
        @Override
        public ModelLoader<ImageModel, InputStream> build(@NonNull MultiModelLoaderFactory unused) {
            return new ImageModelLoader(context);
        }

        @Override
        public void teardown() {

        }
    }

    private final Context context;
    
    public ImageModelLoader(Context context) {
        this.context = context;
    }

    @Override
    public LoadData<InputStream> buildLoadData(
            @NonNull ImageModel model,
            int width,
            int height,
            @NonNull Options options) {
        return new LoadData<>(new ObjectKey(model.toString()), 
                             new ImageModelFetcher(model, width, height, context));
    }

    @Override
    public boolean handles(@NonNull ImageModel model) {
        return true;
    }

    static class ImageModelFetcher implements DataFetcher<InputStream> {
        private static final int[] PALETTES = {0xff78909c, 0xffff6f00, 0xff388e3c,
                0xff00838f, 0xff7b1fa2, 0xffb71c1c, 0xff2196f3};
        
        private final ImageModel model;
        private final int width;
        private final int height;
        private final Context context;

        public ImageModelFetcher(ImageModel model, int width, int height, Context context) {
            this.model = model;
            this.width = width;
            this.height = height;
            this.context = context;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
            try {
                boolean triedNetworkUrl = false;
                
                // Try primary URL first
                if (model.hasPrimaryUrl()) {
                    triedNetworkUrl = true;
                    InputStream stream = loadFromUrl(model.getPrimaryUrl());
                    if (stream != null) {
                        callback.onDataReady(stream);
                        return;
                    }
                }
                
                // Try fallback URL
                if (model.hasFallbackUrl()) {
                    triedNetworkUrl = true;
                    InputStream stream = loadFromUrl(model.getFallbackUrl());
                    if (stream != null) {
                        callback.onDataReady(stream);
                        return;
                    }
                }
                
                if (triedNetworkUrl) {
                    // If we tried network URLs but they failed, report as network error
                    callback.onLoadFailed(new Exception("Network URLs failed"));
                } else {
                    // icon is not defined, generate a placeholder with fallback text
                    callback.onDataReady(generatePlaceholder());
                }
            } catch (RuntimeException e) {
                callback.onLoadFailed(e);
            }
        }

        private InputStream loadFromUrl(String url) {
            if (url == null) {
                return null;
            }
            
            try {
                // Handle content: URLs
                if (url.startsWith(ContentResolver.SCHEME_CONTENT)) {
                    return context.getContentResolver().openInputStream(Uri.parse(url));
                }

                // Handle HTTP/HTTPS URLs
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(10000);
                connection.setInstanceFollowRedirects(true);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return connection.getInputStream();
                } else if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                    // Handle cross-protocol redirects manually
                    String redirectUrl = connection.getHeaderField("Location");
                    if (redirectUrl != null) {
                        return loadFromUrl(redirectUrl); // Recursive call with new URL
                    }
                }
            } catch (RuntimeException | IOException e) {
                // Ignore and try next fallback
            }
            return null;
        }

        private InputStream generatePlaceholder() {
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bitmap);
            final Random generator = new Random(model.getFallbackText().hashCode());
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

            // Add text
            String text = model.getFallbackText();
            if (!text.isEmpty()) {
                drawWrappedText(canvas, text, width, height, width * 0.9f);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            return new ByteArrayInputStream(baos.toByteArray());
        }

        private static int randomShadeOfGrey(Random generator) {
            return 0xff777777 + 0x222222 * generator.nextInt(5);
        }

        private void drawWrappedText(Canvas canvas, String text, int width, int height, float maxWidth) {
            int textSize = (int) (height / 6f);
            Paint paint = new Paint();
            paint.setColor(0xFFFFFFFF);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setAntiAlias(true);
            paint.setSubpixelText(false);
            paint.setTextSize(textSize);
            paint.setShadowLayer(4, 2, 2, 0x80000000);

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
            return DataSource.REMOTE;
        }
    }
}
