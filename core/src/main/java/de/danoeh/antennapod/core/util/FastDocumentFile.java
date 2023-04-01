package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Android's DocumentFile is slow because every single method call queries the ContentResolver.
 * This queries the ContentResolver a single time with all the information.
 */
public class FastDocumentFile {
    private final String name;
    private final String type;
    private final Uri uri;
    private final long length;
    private final long lastModified;

    public static List<FastDocumentFile> list(Context context, Uri folderUri) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri,
                DocumentsContract.getDocumentId(folderUri));
        Cursor cursor = context.getContentResolver().query(childrenUri, new String[] {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null);
        ArrayList<FastDocumentFile> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            String id = cursor.getString(0);
            Uri uri = DocumentsContract.buildDocumentUriUsingTree(folderUri, id);
            String name = cursor.getString(1);
            long size = cursor.getLong(2);
            long lastModified = cursor.getLong(3);
            String mimeType = cursor.getString(4);
            list.add(new FastDocumentFile(name, mimeType, uri, size, lastModified));
        }
        cursor.close();
        return list;
    }

    public FastDocumentFile(String name, String type, Uri uri, long length, long lastModified) {
        this.name = name;
        this.type = type;
        this.uri = uri;
        this.length = length;
        this.lastModified = lastModified;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Uri getUri() {
        return uri;
    }

    public long getLength() {
        return length;
    }

    public long getLastModified() {
        return lastModified;
    }
}
