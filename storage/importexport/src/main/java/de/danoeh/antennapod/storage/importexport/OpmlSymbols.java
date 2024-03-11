package de.danoeh.antennapod.storage.importexport;

/**
 * Contains symbols for reading and writing OPML documents.
 */
final class OpmlSymbols {
    public static final String XML_FEATURE_INDENT_OUTPUT = "http://xmlpull.org/v1/doc/features.html#indent-output";

    public static final String HEAD = "head";
    public static final String BODY = "body";
    public static final String TITLE = "title";
    public static final String OPML = "opml";
    static final String OUTLINE = "outline";
    static final String TEXT = "text";
    static final String XMLURL = "xmlUrl";
    static final String HTMLURL = "htmlUrl";
    static final String TYPE = "type";
    static final String VERSION = "version";
    static final String DATE_CREATED = "dateCreated";

    private OpmlSymbols() {

    }
}
