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
        try (final ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "wt");
                final FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor())
        ) {
            exportToStream(fileOutputStream, context);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw e;
        }
    }

    public static void exportToStream(FileOutputStream outFileStream, Context context) throws IOException {
        final File currentDb = context.getDatabasePath(PodDBAdapter.DATABASE_NAME);
        try (final FileChannel src = new FileInputStream(currentDb).getChannel();
                final FileChannel dst = outFileStream.getChannel()
        ) {
            if (currentDb.exists()) {
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
        }
    }

    public static void importBackup(Uri inputUri, Context context) throws IOException {
        try (final InputStream inputStream = context.getContentResolver().openInputStream(inputUri)) {
            File tempDB = context.getDatabasePath(TEMP_DB_NAME);
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
        }
    }
}
