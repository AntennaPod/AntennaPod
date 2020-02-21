package de.danoeh.antennapod.core.util.syndication;

import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.util.regex.Pattern;

/**
 * This class is based on <code>HtmlToPlainText</code> from jsoup's examples package.
 *
 * HTML to plain-text. This example program demonstrates the use of jsoup to convert HTML input to lightly-formatted
 * plain-text. That is divergent from the general goal of jsoup's .text() methods, which is to get clean data from a
 * scrape.
 * <p>
 * Note that this is a fairly simplistic formatter -- for real world use you'll want to embrace and extend.
 * </p>
 * <p>
 * To invoke from the command line, assuming you've downloaded the jsoup jar to your current directory:</p>
 * <p><code>java -cp jsoup.jar org.jsoup.examples.HtmlToPlainText url [selector]</code></p>
 * where <i>url</i> is the URL to fetch, and <i>selector</i> is an optional CSS selector.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 * @author AntennaPod open source community
 */
public class HtmlToPlainText {

    /**
     * Use this method to strip off HTML encoding from given text
     * <p>
     * Replaces bullet points with *, ignores colors/bold/...
     *
     * @param str String with any encoding
     * @return Human readable text with minimal HTML formatting
     */
    public static String getPlainText(String str) {
        if (!TextUtils.isEmpty(str) && isHtml(str)) {
            HtmlToPlainText formatter = new HtmlToPlainText();
            Document feedDescription = Jsoup.parse(str);
            str = StringUtils.trim(formatter.getPlainText(feedDescription));
        } else if (TextUtils.isEmpty(str)) {
            str = "";
        }

        return str;
    }

    /**
     * Use this method to determine if a given text has any HTML tag
     *
     * @param str String to be tested for presence of HTML content
     * @return <b>True</b> if text contains any HTML tags<br /><b>False</b> is no HTML tag is found
     */
    private static boolean isHtml(String str) {
        final String HTML_TAG_PATTERN = "<(\"[^\"]*\"|'[^']*'|[^'\">])*>";
        Pattern htmlValidator = TextUtils.isEmpty(HTML_TAG_PATTERN) ? null : Pattern.compile(HTML_TAG_PATTERN);

        return htmlValidator.matcher(str).find();
    }

    /**
     * Format an Element to plain-text
     * @param element the root element to format
     * @return formatted text
     */
    public String getPlainText(Element element) {
        FormattingVisitor formatter = new FormattingVisitor();
        // walk the DOM, and call .head() and .tail() for each node
        NodeTraversor.traverse(formatter, element);

        return formatter.toString();
    }

    // the formatting rules, implemented in a breadth-first DOM traverse
    private static class FormattingVisitor implements NodeVisitor {

        private final StringBuilder accum = new StringBuilder(); // holds the accumulated text

        // hit when the node is first seen
        public void head(Node node, int depth) {
            String name = node.nodeName();
            if (node instanceof TextNode) {
                append(((TextNode) node).text()); // TextNodes carry all user-readable text in the DOM.
            }
            else if (name.equals("li")) {
                append("\n * ");
            }
            else if (name.equals("dt")) {
                append("  ");
            }
            else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr")) {
                append("\n");
            }
        }

        // hit when all of the node's children (if any) have been visited
        public void tail(Node node, int depth) {
            String name = node.nodeName();
            if (StringUtil.in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5")) {
                append("\n");
            } else if (name.equals("a")) {
                append(String.format(" <%s>", node.absUrl("href")));
            }
        }

        // appends text to the string builder with a simple word wrap method
        private void append(String text) {
            if (text.equals(" ") &&
                    (accum.length() == 0 || StringUtil.in(accum.substring(accum.length() - 1), " ", "\n"))) {
                return; // don't accumulate long runs of empty spaces
            }

            accum.append(text);
        }

        @Override
        public String toString() {
            return accum.toString();
        }
    }
}
