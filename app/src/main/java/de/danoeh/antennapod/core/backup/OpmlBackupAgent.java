package de.danoeh.antennapod.core.backup;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.export.opml.OpmlElement;
import de.danoeh.antennapod.core.export.opml.OpmlReader;
import de.danoeh.antennapod.core.export.opml.OpmlWriter;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.LangUtils;

public class OpmlBackupAgent extends BackupAgentHelper {
    private static final String OPML_BACKUP_KEY = "opml";

    @Override
    public void onCreate() {
        addHelper(OPML_BACKUP_KEY, new OpmlBackupHelper(this));
    }

    private static void LOGD(String tag, String msg) {
        if (BuildConfig.DEBUG && Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, msg);
        }
    }

    private static void LOGD(String tag, String msg, Throwable tr) {
        if (BuildConfig.DEBUG && Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, msg, tr);
        }
    }

    /**
     * Class for backing up and restoring the OPML file.
     */
    private static class OpmlBackupHelper implements BackupHelper {
        private static final String TAG = "OpmlBackupHelper";

        private static final String OPML_ENTITY_KEY = "antennapod-feeds.opml";

        private final Context mContext;

        /**
         * Checksum of restored OPML file
         */
        private byte[] mChecksum;

        public OpmlBackupHelper(Context context) {
            mContext = context;
        }

        @Override
        public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
            Log.d(TAG, "Performing backup");
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            MessageDigest digester = null;
            Writer writer;

            try {
                digester = MessageDigest.getInstance("MD5");
                writer = new OutputStreamWriter(new DigestOutputStream(byteStream, digester),
                        LangUtils.UTF_8);
            } catch (NoSuchAlgorithmException e) {
                writer = new OutputStreamWriter(byteStream, LangUtils.UTF_8);
            }

            try {
                // Write OPML
                new OpmlWriter().writeDocument(DBReader.getFeedList(), writer);

                // Compare checksum of new and old file to see if we need to perform a backup at all
                if (digester != null) {
                    byte[] newChecksum = digester.digest();
                    LOGD(TAG, "New checksum: " + new BigInteger(1, newChecksum).toString(16));

                    // Get the old checksum
                    if (oldState != null) {
                        FileInputStream inState = new FileInputStream(oldState.getFileDescriptor());
                        int len = inState.read();

                        if (len != -1) {
                            byte[] oldChecksum = new byte[len];
                            inState.read(oldChecksum);
                            LOGD(TAG, "Old checksum: " + new BigInteger(1, oldChecksum).toString(16));

                            if (Arrays.equals(oldChecksum, newChecksum)) {
                                LOGD(TAG, "Checksums are the same; won't backup");
                                return;
                            }
                        }
                    }

                    writeNewStateDescription(newState, newChecksum);
                }

                LOGD(TAG, "Backing up OPML");
                byte[] bytes = byteStream.toByteArray();
                data.writeEntityHeader(OPML_ENTITY_KEY, bytes.length);
                data.writeEntityData(bytes, bytes.length);
            } catch (IOException e) {
                Log.e(TAG, "Error during backup", e);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        @Override
        public void restoreEntity(BackupDataInputStream data) {
            LOGD(TAG, "Backup restore");

            if (!OPML_ENTITY_KEY.equals(data.getKey())) {
                LOGD(TAG, "Unknown entity key: " + data.getKey());
                return;
            }

            MessageDigest digester = null;
            Reader reader;

            try {
                digester = MessageDigest.getInstance("MD5");
                reader = new InputStreamReader(new DigestInputStream(data, digester),
                        LangUtils.UTF_8);
            } catch (NoSuchAlgorithmException e) {
                reader = new InputStreamReader(data, LangUtils.UTF_8);
            }

            try {
                ArrayList<OpmlElement> opmlElements = new OpmlReader().readDocument(reader);
                mChecksum = digester == null ? null : digester.digest();
                DownloadRequester downloader = DownloadRequester.getInstance();

                for (OpmlElement opmlElem : opmlElements) {
                    Feed feed = new Feed(opmlElem.getXmlUrl(), null, opmlElem.getText());

                    try {
                        downloader.downloadFeed(mContext, feed);
                    } catch (DownloadRequestException e) {
                        LOGD(TAG, "Error while restoring/downloading feed", e);
                    }
                }
            } catch (XmlPullParserException e) {
                Log.e(TAG, "Error while parsing the OPML file", e);
            } catch (IOException e) {
                Log.e(TAG, "Failed to restore OPML backup", e);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }

        @Override
        public void writeNewStateDescription(ParcelFileDescriptor newState) {
            writeNewStateDescription(newState, mChecksum);
        }

        /**
         * Writes the new state description, which is the checksum of the OPML file.
         *
         * @param newState
         * @param checksum
         */
        private void writeNewStateDescription(ParcelFileDescriptor newState, byte[] checksum) {
            if (checksum == null) {
                return;
            }

            try {
                FileOutputStream outState = new FileOutputStream(newState.getFileDescriptor());
                outState.write(checksum.length);
                outState.write(checksum);
                outState.flush();
                outState.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to write new state description", e);
            }
        }
    }
}
