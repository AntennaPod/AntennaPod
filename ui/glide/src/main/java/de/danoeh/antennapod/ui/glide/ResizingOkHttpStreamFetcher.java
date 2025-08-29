package de.danoeh.antennapod.ui.glide;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.integration.okhttp3.OkHttpStreamFetcher;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import okhttp3.Call;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.bumptech.glide.load.DataSource;


public class ResizingOkHttpStreamFetcher implements DataFetcher<InputStream> {
    private static final String TAG = "ResizingOkHttpFetcher";
    private static final int MAX_DIMENSIONS = 1500;
    private static final long MAX_FILE_SIZE = 1024 * 1024;

    private final OkHttpStreamFetcher delegate;
    private InputStream stream;
    private File tempIn;
    private File tempOut;

    public ResizingOkHttpStreamFetcher(Call.Factory client, GlideUrl url) {
        this.delegate = new OkHttpStreamFetcher(client, url);
    }

    @Override
    public void loadData(@NonNull Priority priority,
                        @NonNull DataFetcher.DataCallback<? super InputStream> callback) {
        delegate.loadData(priority, new DataFetcher.DataCallback<InputStream>() {
            @Override
            public void onDataReady(@Nullable InputStream data) {
                if (data == null) {
                    callback.onDataReady(null);
                    return;
                }
                processStream(data, callback);
            }

            @Override
            public void onLoadFailed(@NonNull Exception e) {
                callback.onLoadFailed(e);
            }
        });
    }

    private void processStream(InputStream data, DataFetcher.DataCallback<? super InputStream> callback) {
        try {
            tempIn = File.createTempFile("resize_", null);
            tempOut = File.createTempFile("resize_", null);
            writeToFile(data, tempIn);

            if (tempIn.length() <= MAX_FILE_SIZE) {
                deliverOriginalImage(callback);
                return;
            }

            resizeImage(callback);

        } catch (IOException e) {
            callback.onLoadFailed(e);
        }
    }

    private void writeToFile(InputStream data, File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            IOUtils.copy(data, out);
        }
    }

    private void deliverOriginalImage(DataFetcher.DataCallback<? super InputStream> callback)
            throws FileNotFoundException {
        stream = new FileInputStream(tempIn);
        callback.onDataReady(stream);
    }

    private void resizeImage(DataFetcher.DataCallback<? super InputStream> callback) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(tempIn), null, options);

            if (!isValidImage(options)) {
                throw new IOException("Not a valid image");
            }

            Bitmap bitmap = createResizedBitmap(options);
            compressAndDeliver(bitmap, callback);

        } catch (IOException e) {
            deliverFallback(callback, e);
        }
    }

    private boolean isValidImage(BitmapFactory.Options options) {
        return options.outWidth != -1 && options.outHeight != -1;
    }

    private Bitmap createResizedBitmap(BitmapFactory.Options options) throws IOException {
        double sampleSize = (double) Math.max(options.outHeight, options.outWidth) / MAX_DIMENSIONS;
        options.inSampleSize = (int) Math.pow(2d, Math.floor(Math.log(sampleSize) / Math.log(2d)));
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(tempIn), null, options);

        IOUtils.closeQuietly(in);

        return bitmap;
    }

    private void compressAndDeliver(Bitmap bitmap, DataFetcher.DataCallback<? super InputStream> callback) throws IOException {
        Bitmap.CompressFormat format = Build.VERSION.SDK_INT < 30
                ? Bitmap.CompressFormat.WEBP : Bitmap.CompressFormat.WEBP_LOSSY;

        int quality = 100;
        while (true) {
            FileOutputStream out = new FileOutputStream(tempOut);
            bitmap.compress(format, quality, out);
            IOUtils.closeQuietly(out);

            if (tempOut.length() > 3 * MAX_FILE_SIZE && quality >= 45) {
                quality -= 40;
            } else if (tempOut.length() > 2 * MAX_FILE_SIZE && quality >= 25) {
                quality -= 20;
            } else if (tempOut.length() > MAX_FILE_SIZE && quality >= 15) {
                quality -= 10;
            } else if (tempOut.length() > MAX_FILE_SIZE && quality >= 10) {
                quality -= 5;
            } else {
                break;
            }
        }
        bitmap.recycle();

        stream = new FileInputStream(tempOut);
        callback.onDataReady(stream);
        Log.d(TAG, "Compressed image from " + tempIn.length() / 1024
                + " to " + tempOut.length() / 1024 + " kB (quality: " + quality + "%)");
    }

    private void deliverFallback(DataFetcher.DataCallback<? super InputStream> callback, IOException e) {
        try {
            stream = new FileInputStream(tempIn);
            callback.onDataReady(stream); // Just deliver the original, non-scaled image
        } catch (FileNotFoundException fileNotFoundException) {
            e.printStackTrace();
            callback.onLoadFailed(fileNotFoundException);
        }
    }

    @Override
    public void cleanup() {
        IOUtils.closeQuietly(stream);
        FileUtils.deleteQuietly(tempIn);
        FileUtils.deleteQuietly(tempOut);
        delegate.cleanup();
    }

    @Override
    public void cancel() {
        delegate.cancel();
    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return delegate.getDataClass();
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return delegate.getDataSource();
    }
}
