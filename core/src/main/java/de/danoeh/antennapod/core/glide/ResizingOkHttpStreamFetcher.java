package de.danoeh.antennapod.core.glide;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.integration.okhttp3.OkHttpStreamFetcher;
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

public class ResizingOkHttpStreamFetcher extends OkHttpStreamFetcher {
    private static final String TAG = "ResizingOkHttpStreamFet";
    private static final int MAX_DIMENSIONS = 1500;
    private static final int MAX_FILE_SIZE = 1024 * 1024; // 1 MB

    private FileInputStream stream;
    private File tempIn;
    private File tempOut;

    public ResizingOkHttpStreamFetcher(Call.Factory client, GlideUrl url) {
        super(client, url);
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        super.loadData(priority, new DataCallback<InputStream>() {
            @Override
            public void onDataReady(@Nullable InputStream data) {
                if (data == null) {
                    callback.onDataReady(null);
                    return;
                }
                try {
                    tempIn = File.createTempFile("resize_", null);
                    tempOut = File.createTempFile("resize_", null);
                    OutputStream outputStream = new FileOutputStream(tempIn);
                    IOUtils.copy(data, outputStream);
                    outputStream.close();
                    IOUtils.closeQuietly(data);

                    if (tempIn.length() <= MAX_FILE_SIZE) {
                        try {
                            stream = new FileInputStream(tempIn);
                            callback.onDataReady(stream); // Just deliver the original, non-scaled image
                        } catch (FileNotFoundException fileNotFoundException) {
                            callback.onLoadFailed(fileNotFoundException);
                        }
                        return;
                    }

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    FileInputStream in = new FileInputStream(tempIn);
                    BitmapFactory.decodeStream(in, null, options);
                    IOUtils.closeQuietly(in);

                    if (options.outWidth == -1 || options.outHeight == -1) {
                        throw new IOException("Not a valid image");
                    } else if (Math.max(options.outHeight, options.outWidth) >= MAX_DIMENSIONS) {
                        double sampleSize = (double) Math.max(options.outHeight, options.outWidth) / MAX_DIMENSIONS;
                        options.inSampleSize = (int) Math.pow(2d, Math.floor(Math.log(sampleSize) / Math.log(2d)));
                    }

                    options.inJustDecodeBounds = false;
                    in = new FileInputStream(tempIn);
                    Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);
                    IOUtils.closeQuietly(in);

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
                } catch (Throwable e) {
                    e.printStackTrace();

                    try {
                        stream = new FileInputStream(tempIn);
                        callback.onDataReady(stream); // Just deliver the original, non-scaled image
                    } catch (FileNotFoundException fileNotFoundException) {
                        e.printStackTrace();
                        callback.onLoadFailed(fileNotFoundException);
                    }
                }
            }

            @Override
            public void onLoadFailed(@NonNull Exception e) {
                callback.onLoadFailed(e);
            }
        });
    }

    @Override
    public void cleanup() {
        IOUtils.closeQuietly(stream);
        FileUtils.deleteQuietly(tempIn);
        FileUtils.deleteQuietly(tempOut);
        super.cleanup();
    }
}
