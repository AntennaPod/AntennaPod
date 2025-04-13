package de.danoeh.antennapod.storage.importexport;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.format.Formatter;
import android.util.Log;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DatabaseExporter {
    private static final String TAG = "DatabaseExporter";
    private static final String TEMP_DB_NAME = PodDBAdapter.DATABASE_NAME + "_tmp";

    public static void exportToDocument(Uri uri, Context context) throws IOException {
        ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "wt");
        int bytesCopied = -1;
        int resultingFileSize = 0;
        try (FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor())) {
            bytesCopied = exportToStream(fileOutputStream, context);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw e;
        } finally {
            resultingFileSize = (int) pfd.getStatSize();
            IOUtils.closeQuietly(pfd);
        }
        if (resultingFileSize != bytesCopied) {
            throw new IOException(String.format(
                    "Unable to write entire database. Expected to write %s, but wrote %s.",
                    Formatter.formatShortFileSize(context, bytesCopied),
                    Formatter.formatShortFileSize(context, resultingFileSize)));
        }
    }

    public static int exportToStream(FileOutputStream outFileStream, Context context) throws IOException {
        File currentDB = context.getDatabasePath(PodDBAdapter.DATABASE_NAME);
        if (!currentDB.exists()) {
            throw new IOException("Cannot access current database");
        }
        try (InputStream src = new FileInputStream(currentDB)) {
            return IOUtils.copy(src, outFileStream);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw e;
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
            boolean success = currentDB.delete();
            if (!success) {
                throw new IOException("Unable to delete old database");
            }
            FileUtils.moveFile(tempDB, currentDB);
        } catch (IOException | SQLiteException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw e;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
}
