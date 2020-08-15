package de.danoeh.antennapod.core.syndication.namespace.atom;

import androidx.core.text.HtmlCompat;

import de.danoeh.antennapod.core.syndication.namespace.Namespace;
import de.danoeh.antennapod.core.syndication.namespace.SyndElement;

/** Represents Atom Element which contains text (content, title, summary). */
public class AtomText extends SyndElement {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_HTML = "html";
    private static final String TYPE_XHTML = "xhtml";

    private final String type;
    private String content;

    public AtomText(String name, Namespace namespace, String type) {
        super(name, namespace);
        this.type = type;
    }

    /** Processes the content according to the type and returns it. */
    public String getProcessedContent() {
        if (type == null) {
            return content;
        } else if (type.equals(TYPE_HTML)) {
            return HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_LEGACY).toString();
        } else if (type.equals(TYPE_XHTML)) {
            return content;
        } else { // Handle as text by default
            return content;
        }
    }

    public void setContent(String content) {
        this.content = content;
    }
}
