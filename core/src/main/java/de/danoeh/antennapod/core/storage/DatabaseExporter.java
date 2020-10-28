package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.format.Formatter;
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

public class DatabaseExporter {
    private static final String TAG = "DatabaseExporter";
    private static final String TEMP_DB_NAME = PodDBAdapter.DATABASE_NAME + "_tmp";

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
                long srcSize = src.size();
                dst.transferFrom(src, 0, srcSize);

                long newDstSize = dst.size();
                if (newDstSize != srcSize) {
                    throw new IOException(String.format(
                            "Unable to write entire database. Expected to write %s, but wrote %s.",
                            Formatter.formatShortFileSize(context, srcSize),
                            Formatter.formatShortFileSize(context, newDstSize)));
                }
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
            File tempDB = context.getDatabasePath(TEMP_DB_NAME);
            inputStream = context.getContentResolver().openInputStream(inputUri);
            FileUtils.copyInputStreamToFile(inputStream, tempDB);

            SQLiteDatabase db = SQLiteDatabase.openDatabase(tempDB.getAbsolutePath(),
                    null, SQLiteDatabase.OPEN_READONLY);
            if (db.getVersion() > PodDBAdapter.VERSION) {
                throw new IOException(context.getString(R.string.import_no_downgrade));
            }
            db.close();

            File currentDB = context.getDatabasePath(PodDBAdapter.DATABASE_NAME);
            currentDB.delete();
            FileUtils.moveFile(tempDB, currentDB);
        } catch (IOException | SQLiteException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw e;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
}
