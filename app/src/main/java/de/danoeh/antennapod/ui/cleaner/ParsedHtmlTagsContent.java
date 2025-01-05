package de.danoeh.antennapod.ui.cleaner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

public class ParsedHtmlTagsContent {

    private static final Set<String> supportedTags = Set.of(
            "a", "head", "meta", "title", "script", "menuitem", "data", "video", "track", "object", "code", "img",
            "area", "source", "audio", "iframe", "option"
    );
    private static final Set<String> supportedAttrs = Set.of(
            "href", "content", "src", "attributionsrc", "icon", "value", "data"
    );

    private final Set<String> tagsContent = new HashSet<>();

    public ParsedHtmlTagsContent(String text) {
        Document document = Jsoup.parse(text);
        for (String tag : supportedTags) {
            Elements tags = document.select(tag);
            for (Element curTag : tags) {
                for (String attr : supportedAttrs) {
                    tagsContent.add(curTag.attr(attr));
                }
                tagsContent.add(curTag.text());
                tagsContent.add(curTag.html());
            }
        }
    }

    public boolean contains(String linkCandidate) {
        for (String text : tagsContent) {
            if (text.contains(linkCandidate)) {
                return true;
            }
        }
        return false;
    }
}
