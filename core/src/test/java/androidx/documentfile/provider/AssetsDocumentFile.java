package androidx.documentfile.provider;

import android.content.res.AssetManager;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

/**
 * <p>Wraps an Android assets file or folder as a DocumentFile object.</p>
 *
 * <p>This is used to emulate access to the external storage.</p>
 */
public class AssetsDocumentFile extends DocumentFile {

    /**
     * Absolute file path in the assets folder.
     */
    @NonNull
    private final String fileName;

    @NonNull
    private final AssetManager assetManager;

    public AssetsDocumentFile(@NonNull String fileName, @NonNull AssetManager assetManager) {
        super(null);
        this.fileName = fileName;
        this.assetManager = assetManager;
    }

    @Nullable
    @Override
    public DocumentFile createFile(@NonNull String mimeType, @NonNull String displayName) {
        return null;
    }

    @Nullable
    @Override
    public DocumentFile createDirectory(@NonNull String displayName) {
        return null;
    }

    @NonNull
    @Override
    public Uri getUri() {
        return Uri.parse(fileName);
    }

    @Nullable
    @Override
    public String getName() {
        int pos = fileName.indexOf('/');
        if (pos >= 0) {
            return fileName.substring(pos + 1);
        } else {
            return fileName;
        }
    }

    @Nullable
    @Override
    public String getType() {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isFile() {
        return true;
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public long lastModified() {
        return 0;
    }

    @Override
    public long length() {
        return 0;
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @NonNull
    @Override
    public DocumentFile[] listFiles() {
        try {
            String[] files = assetManager.list(fileName);
            if (files == null) {
                return new DocumentFile[0];
            }
            DocumentFile[] result = new DocumentFile[files.length];
            for (int i = 0; i < files.length; i++) {
                String subFileName = fileName + '/' + files[i];
                result[i] = new AssetsDocumentFile(subFileName, assetManager);
            }
            return result;
        } catch (IOException e) {
            return new DocumentFile[0];
        }
    }

    @Override
    public boolean renameTo(@NonNull String displayName) {
        return false;
    }
}
