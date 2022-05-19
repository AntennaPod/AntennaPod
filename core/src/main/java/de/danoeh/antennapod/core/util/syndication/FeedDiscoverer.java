package de.danoeh.antennapod.core.util.syndication;

import android.net.Uri;
import androidx.collection.ArrayMap;
import android.text.TextUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Finds RSS/Atom URLs in a HTML document using the auto-discovery techniques described here:
 * <p/>
 * http://www.rssboard.org/rss-autodiscovery
 * <p/>
 * http://blog.whatwg.org/feed-autodiscovery
 */
public class FeedDiscoverer {

    private static final String MIME_RSS = "application/rss+xml";
    private static final String MIME_ATOM = "application/atom+xml";

    /**
     * Discovers links to RSS and Atom feeds in the given File which must be a HTML document.
     *
     * @return A map which contains the feed URLs as keys and titles as values (the feed URL is also used as a title if
     * a title cannot be found).
     */
    public Map<String, String> findLinks(File in, String baseUrl) throws IOException {
        return findLinks(Jsoup.parse(in), baseUrl);
    }

    /**
     * Discovers links to RSS and Atom feeds in the given File which must be a HTML document.
     *
     * @return A map which contains the feed URLs as keys and titles as values (the feed URL is also used as a title if
     * a title cannot be found).
     */
    public Map<String, String> findLinks(String in, String baseUrl) {
        return findLinks(Jsoup.parse(in), baseUrl);
    }

    private Map<String, String> findLinks(Document document, String baseUrl) {
        Map<String, String> res = new ArrayMap<>();
        Elements links = document.head().getElementsByTag("link");
        for (Element link : links) {
            String rel = link.attr("rel");
            String href = link.attr("href");
            if (!TextUtils.isEmpty(href) &&
                    (rel.equals("alternate") || rel.equals("feed"))) {
                String type = link.attr("type");
                if (type.equals(MIME_RSS) || type.equals(MIME_ATOM)) {
                    String title = link.attr("title");
                    String processedUrl = processURL(baseUrl, href);
                    if (processedUrl != null) {
                        res.put(processedUrl,
                                (TextUtils.isEmpty(title)) ? href : title);
                    }
                }
            }
        }
        return res;
    }

    private String processURL(String baseUrl, String strUrl) {
        Uri uri = Uri.parse(strUrl);
        if (uri.isRelative()) {
            Uri res = Uri.parse(baseUrl).buildUpon().path(strUrl).build();
            return (res != null) ? res.toString() : null;
        } else {
            return strUrl;
        }
    }
}
