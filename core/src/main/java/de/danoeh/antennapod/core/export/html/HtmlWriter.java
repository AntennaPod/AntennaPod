package de.danoeh.antennapod.core.export.html;

import android.content.Context;
import android.util.Log;
import de.danoeh.antennapod.core.export.ExportWriter;
import de.danoeh.antennapod.core.feed.Feed;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.List;
import org.apache.commons.io.IOUtils;

/** Writes HTML documents. */
public class HtmlWriter implements ExportWriter {
    private static final String TAG = "HtmlWriter";

    /**
     * Takes a list of feeds and a writer and writes those into an HTML
     * document.
     */
    @Override
    public void writeDocument(List<Feed> feeds, Writer writer, Context context)
            throws IllegalArgumentException, IllegalStateException, IOException {
        Log.d(TAG, "Starting to write document");

        InputStream templateStream = context.getAssets().open("html-export-template.html");
        String template = IOUtils.toString(templateStream, "UTF-8");
        String[] templateParts = template.split("\\{FEEDS\\}");

        writer.append(templateParts[0]);
        for (Feed feed : feeds) {
            writer.append("<li><div><img src=\"");
            writer.append(feed.getImageUrl());
            writer.append("\" /><p>");
            writer.append(feed.getTitle());
            writer.append(" <span><a href=\"");
            writer.append(feed.getLink());
            writer.append("\">Website</a> • <a href=\"");
            writer.append(feed.getDownload_url());
            writer.append("\">Feed</a></span></p></div></li>\n");
        }
        writer.append(templateParts[1]);
        Log.d(TAG, "Finished writing document");
    }

    public String fileExtension() {
        return "html";
    }

}
