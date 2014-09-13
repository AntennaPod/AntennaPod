package wseemann.media;

/** 
 * Represents a chapter mark returned by FFmpegMediaMetadataRetriever.
 * */
public class FFmpegChapter
{
    private int id;
    private String title;
    private long start;

    public FFmpegChapter(int id, String title, long start) {
        this.id = id;
        this.title = title;
        this.start = start;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public long getStart() {
        return start;
    }
}
