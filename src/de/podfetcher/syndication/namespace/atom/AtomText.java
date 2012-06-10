package de.podfetcher.syndication.namespace.atom;

import de.podfetcher.syndication.namespace.Namespace;
import de.podfetcher.syndication.namespace.SyndElement;
import de.podfetcher.syndication.util.HtmlUnescaper;

/** Represents Atom Element which contains text (content, title, summary). */
public class AtomText extends SyndElement {
	public static final String TYPE_TEXT = "text";
	public static final String TYPE_HTML = "html";
	public static final String TYPE_XHTML = "xhtml";
	
	private String type;
	private String content;
	
	public AtomText(String name, Namespace namespace, String type) {
		super(name, namespace);
		this.type = type;
	}
	
	/** Processes the content according to the type and returns it. */
	public String getProcessedContent() {
		if (type.equals(TYPE_HTML)) {
			return HtmlUnescaper.unescape(content);
		} else if (type.equals(TYPE_XHTML)) {
			return content;
		} else {	// Handle as text by default
			return content;
		}
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getType() {
		return type;
	}
	
	

}
