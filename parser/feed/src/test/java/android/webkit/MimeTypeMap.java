package android.webkit;

public class MimeTypeMap {
    public static MimeTypeMap getSingleton()
    {
        return new MimeTypeMap();
    }

    public String getMimeTypeFromExtension(String extension) {
        return "mp3";
    }
}
