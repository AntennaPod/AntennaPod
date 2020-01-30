package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import de.danoeh.antennapod.core.R;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class DatabaseExporter {
    private static final String TAG = "DatabaseExporter";
    private static final byte[] SQLITE3_MAGIC = "SQLite format 3\0".getBytes();

    public static boolean validateDB(Uri inputUri, Context context) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(inputUri)) {
            byte[] magicBuf = new byte[SQLITE3_MAGIC.length];
            if (inputStream.read(magicBuf) == magicBuf.length) {
                return Arrays.equals(SQLITE3_MAGIC, magicBuf);
            }
        }
        return false;
    }

    public static void exportToDocument(Uri uri, Context context) throws IOException {
        ParcelFileDescriptor pfd = null;
        FileOutputStream fileOutputStream = null;
        try {
            pfd = context.getContentResolver().openFileDescriptor(uri, "w");
            fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
            exportToStream(fileOutputStream, context);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw e;
        } finally {
            IOUtils.closeQuietly(fileOutputStream);

            if (pfd != null) {
                try {
                    pfd.close();
                } catch (IOException e) {
                    Log.d(TAG, "Unable to close ParcelFileDescriptor");
                }
            }
        }
    }

    public static void exportToStream(FileOutputStream outFileStream, Context context) throws IOException {
        FileChannel src = null;
        FileChannel dst = null;
        try {
            File currentDB = context.getDatabasePath(PodDBAdapter.DATABASE_NAME);

            if (currentDB.exists()) {
                src = new FileInputStream(currentDB).getChannel();
                dst = outFileStream.getChannel();
                dst.transferFrom(src, 0, src.size());
            } else {
                throw new IOException("Can not access current database");
            }
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw e;
        } finally {
            IOUtils.closeQuietly(src);
            IOUtils.closeQuietly(dst);
        }
    }

    public static void importBackup(Uri inputUri, Context context) throws IOException {
        InputStream inputStream = null;
        try {
            if (!validateDB(inputUri, context)) {
                throw new IOException(context.getString(R.string.import_bad_file));
            }

            File currentDB = context.getDatabasePath(PodDBAdapter.DATABASE_NAME);
            inputStream = context.getContentResolver().openInputStream(inputUri);
            FileUtils.copyInputStreamToFile(inputStream, currentDB);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw e;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
}
