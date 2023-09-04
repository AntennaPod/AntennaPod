package de.danoeh.antennapod.ui.glide;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

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
    public void loadData(@NonNull Priority priority, @NonNull DataFetcher.DataCallback<? super InputStream> callback) {
        super.loadData(priority, new DataFetcher.DataCallback<InputStream>() {
            @Override
            public void onDataReady(@Nullable InputStream data) {
                if (data == null) {
                    callback.onDataReady(null);
                    return;
                }
                try {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    IOUtils.copy(data, outputStream);
                    IOUtils.closeQuietly(data);

                    byte[] inputData = outputStream.toByteArray();
                    Bitmap originalBitmap = BitmapFactory.decodeByteArray(inputData, 0, inputData.length);

                    int originalSize = inputData.length;
                    int quality = 100;

                    while (originalSize > MAX_FILE_SIZE && quality >= 10) {
                        outputStream.reset();
                        originalBitmap.compress(Bitmap.CompressFormat.WEBP, quality, outputStream);
                        byte[] compressedData = outputStream.toByteArray();

                        if (compressedData.length > MAX_FILE_SIZE) {
                            if (quality >= 45) {
                                quality -= 40;
                            } else if (quality >= 25) {
                                quality -= 20;
                            } else if (quality >= 15) {
                                quality -= 10;
                            } else {
                                quality -= 5;
                            }
                        } else {
                            inputData = compressedData;
                            originalSize = compressedData.length;
                        }
                    }

                    ByteArrayInputStream inputStream = new ByteArrayInputStream(inputData);
                    callback.onDataReady(inputStream);

                    int compressedSize = inputData.length;
                    Log.d(TAG, "Compressed image from " + originalSize / 1024 + " to " + compressedSize / 1024 + " kB (quality: " + quality + "%)");

                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onLoadFailed(e);
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
