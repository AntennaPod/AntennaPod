package de.danoeh.antennapod.ui.cleaner;

import androidx.annotation.NonNull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlainTextLinksConverter {
    private static final Pattern HTTP_LINK_REGEX = Pattern.compile(
            "(?:https?://(?:www\\.)?|www\\.)"   // http(s)://[www.] OR www.
                    + "[-a-zA-Z0-9@:%._+~#=]{1,256}"  // Domain name
                    + "\\.[a-zA-Z]{2,6}\\b"           // Top-level domain
                    + "[-a-zA-Z0-9@:%_+.*~#?!&$/=()\\[\\],;]*", // Path, query params
            Pattern.CASE_INSENSITIVE
    );
    protected static final List<String> NOT_ALLOWED_END_CHARS = List.of(
            ".", ",", ";", ":", "?", "!", ")", "(", "[", "]", "-", "_", "~", "#", "@", "$", "*", "+");

    private static final String STARTS_WITH_HTTP = "(?i)https?://.*";
    private static final String ANCHOR_TAG = "a";
    private static final String ANCHOR_ADDRESS = "href";

    /**
     * Provided text can be an HTML document or plain text.
     * It may contain a mixture of plain-text links and HTML links.
     * Only plain-text links will be converted to HTML {@code <a>} tags.
     */
    public static String convertLinksToHtml(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        try {
            Document doc = Jsoup.parse(text);
            convertLinksToHtml(doc);
            return doc.body().html();
        } catch (Exception e) {
            return text;
        }
    }

    public static void convertLinksToHtml(Document doc) {
        if (doc == null) {
            return;
        }
        NodeTraversor.traverse(new LinkConvertingVisitor(), doc.body());
    }

    /**
     * Ensures that URLs are only converted if they are not already part of an existing anchor tag.
     * Document structure remains untouched, logic affects only {@link TextNode} - leaf element with no tags in it.
     * One {@link TextNode} is replaced with multiple {@link Element}s:
     * <li>{@link TextNode} with text before the link</li>
     * <li>{@link Element} with the link tag</li>
     * <li>{@link TextNode} with text after the link</li>
     */
    private static class LinkConvertingVisitor implements NodeVisitor {
        @Override
        public void head(@NonNull Node node, int depth) {
            if (!(node instanceof TextNode textNode)) {
                return;
            } else if (isInsideAnchor(textNode)) {
                return;
            }
            String originalText = textNode.getWholeText();
            Matcher matcher = HTTP_LINK_REGEX.matcher(originalText);

            if (!matcher.find()) {
                return;
            }
            List<Node> newNodes = new ArrayList<>();
            int lastEnd = 0;
            matcher.reset();

            while (matcher.find()) {
                String url = matcher.group();
                if (endsWithPunctuation(url)) {
                    continue;
                }
                if (matcher.start() > lastEnd) {
                    newNodes.add(new TextNode(originalText.substring(lastEnd, matcher.start())));
                }
                newNodes.add(link(url));
                lastEnd = matcher.end();
            }

            if (lastEnd < originalText.length()) {
                newNodes.add(new TextNode(originalText.substring(lastEnd)));
            }

            if (!newNodes.isEmpty()) {
                Node parent = textNode.parent();
                if (parent instanceof Element parentElement) {
                    int index = textNode.siblingIndex();
                    textNode.remove();
                    parentElement.insertChildren(index, newNodes);
                }
            }
        }

        private static Element link(String detectedUrl) {
            var url = detectedUrl;
            if (!detectedUrl.matches(STARTS_WITH_HTTP)) {
                url = "https://" + url;
            }
            return new Element(ANCHOR_TAG).attr(ANCHOR_ADDRESS, url).text(detectedUrl);
        }

        @Override
        public void tail(@NonNull Node node, int depth) {
            //not needed
        }
    }

    private static boolean isInsideAnchor(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof Element currentElement) {
                if (ANCHOR_TAG.equalsIgnoreCase(currentElement.tagName())) {
                    return true;
                }
            }
            current = current.parent();
        }
        return false;
    }

    private static boolean endsWithPunctuation(String url) {
        for (String endChar : NOT_ALLOWED_END_CHARS) {
            if (url.endsWith(endChar)) {
                return true;
            }
        }
        return false;
    }
}
