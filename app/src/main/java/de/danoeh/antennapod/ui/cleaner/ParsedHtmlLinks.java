package de.danoeh.antennapod.ui.cleaner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

public class ParsedHtmlLinks {

    private final Set<String> urls = new HashSet<>();
    private final Set<String> linkTexts = new HashSet<>();

    public ParsedHtmlLinks(String text) {
        Document document = Jsoup.parse(text);
        Elements links = document.select("a[href]");
        for (Element link : links) {
            urls.add(link.attr("href"));
            linkTexts.add(link.text());
        }
        Elements images = document.select("img");
        for (Element img : images) {
            urls.add(img.attr("src"));
        }
        Elements audios = document.select("audio");
        for (Element audio : audios) {
            urls.add(audio.attr("src"));
        }
    }

    public boolean contains(String linkCandidate) {
        var isExactMatchUrl = urls.contains(linkCandidate);
        if (isExactMatchUrl) {
            return true;
        }
        for (String text : linkTexts) {
            if (text.contains(linkCandidate)) {
                return true;
            }
        }
        return false;
    }
}
